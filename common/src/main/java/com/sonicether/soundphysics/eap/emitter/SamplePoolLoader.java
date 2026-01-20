package com.sonicether.soundphysics.eap.emitter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Loads sample-based audio (.ogg) from mod resources and manages OpenAL buffer pools.
 *
 * <p>On init, reads a manifest.txt listing all available .ogg files per subcategory,
 * decodes them via STBVorbis into PCM, and uploads to OpenAL buffers.
 * Provides random buffer selection with no-repeat history.
 *
 * <p>Thread safety: all public methods are synchronized. Call from the render/audio thread only.
 */
public final class SamplePoolLoader {

    private static final Logger LOGGER = LogManager.getLogger("EchoField - SamplePool");
    private static final String RESOURCE_ROOT = "/assets/soundphysics/eap/samples/";

    private final Map<SampleSubcategory, List<Integer>> pools = new EnumMap<>(SampleSubcategory.class);
    private final Set<Integer> allBufferIds = new HashSet<>();
    private boolean loaded = false;

    /**
     * Loads all sample pools from mod resources.
     * Reads manifest.txt from each subcategory directory to discover .ogg files.
     */
    public synchronized void loadAll() {
        if (loaded) return;

        LOGGER.info("SamplePoolLoader: beginning sample load from classpath...");

        try {
            for (SampleSubcategory sub : SampleSubcategory.values()) {
                List<Integer> buffers = new ArrayList<>();
                String manifestPath = RESOURCE_ROOT + sub.resourcePath + "/manifest.txt";

                try (InputStream manifestStream = getClass().getResourceAsStream(manifestPath)) {
                    if (manifestStream == null) {
                        LOGGER.warn("No manifest for subcategory {}: {}", sub.name(), manifestPath);
                        continue;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(manifestStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        String oggPath = RESOURCE_ROOT + sub.resourcePath + "/" + line;
                        try {
                            int bufferId = loadOggBuffer(oggPath);
                            if (bufferId != 0) {
                                buffers.add(bufferId);
                                allBufferIds.add(bufferId);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Exception loading sample {}: {}", oggPath, e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to read manifest for {}: {}", sub.name(), e.getMessage());
                }

                if (!buffers.isEmpty()) {
                    pools.put(sub, buffers);
                    LOGGER.info("Loaded {} samples for {}", buffers.size(), sub.name());
                }
            }
        } catch (Exception e) {
            LOGGER.error("SamplePoolLoader: fatal error during load", e);
        }

        loaded = true;
        LOGGER.info("SamplePoolLoader: loaded {} total buffers across {} subcategories",
                allBufferIds.size(), pools.size());
    }

    /**
     * Returns a random buffer for the given subcategory, avoiding recently played buffers.
     *
     * @param subcategory the sample subcategory to pick from
     * @param recentHistory array of recently played buffer IDs (use -1 for empty slots)
     * @param rng random source
     * @return OpenAL buffer ID, or 0 if no samples available
     */
    public synchronized int getBuffer(SampleSubcategory subcategory, int[] recentHistory, Random rng) {
        List<Integer> pool = pools.get(subcategory);
        if (pool == null || pool.isEmpty()) return 0;

        if (pool.size() == 1) return pool.get(0);

        // Build candidate list excluding recent history
        List<Integer> candidates = new ArrayList<>(pool.size());
        for (int bufferId : pool) {
            boolean recent = false;
            for (int h : recentHistory) {
                if (h == bufferId) {
                    recent = true;
                    break;
                }
            }
            if (!recent) candidates.add(bufferId);
        }

        // If all are in history, fall back to full pool
        if (candidates.isEmpty()) candidates = pool;

        int selected = candidates.get(rng.nextInt(candidates.size()));

        // Shift history and record the selected buffer
        System.arraycopy(recentHistory, 0, recentHistory, 1, recentHistory.length - 1);
        recentHistory[0] = selected;

        return selected;
    }

    /**
     * Returns true if the given buffer ID belongs to a pooled sample.
     * Used by EmitterPool to protect sample buffers from deletion.
     */
    public synchronized boolean isPooledBuffer(int bufferId) {
        return allBufferIds.contains(bufferId);
    }

    /**
     * Deletes all OpenAL buffers and releases resources.
     */
    public synchronized void shutdown() {
        for (int bufferId : allBufferIds) {
            AL10.alDeleteBuffers(bufferId);
        }
        allBufferIds.clear();
        pools.clear();
        loaded = false;
        LOGGER.info("SamplePoolLoader: shut down");
    }

    /**
     * Loads a single .ogg file from the classpath, decodes it to PCM via STBVorbis,
     * and creates an OpenAL buffer.
     *
     * @param resourcePath classpath resource path to the .ogg file
     * @return OpenAL buffer ID, or 0 on failure
     */
    private int loadOggBuffer(String resourcePath) {
        byte[] oggBytes;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.warn("Sample not found: {}", resourcePath);
                return 0;
            }
            oggBytes = is.readAllBytes();
        } catch (IOException e) {
            LOGGER.warn("Failed to read sample {}: {}", resourcePath, e.getMessage());
            return 0;
        }

        if (oggBytes.length == 0) {
            LOGGER.warn("Empty sample file: {}", resourcePath);
            return 0;
        }

        // Copy OGG data to direct buffer for STBVorbis
        ByteBuffer oggBuffer = MemoryUtil.memAlloc(oggBytes.length);
        try {
            oggBuffer.put(oggBytes).flip();
            return decodeAndUpload(oggBuffer, resourcePath);
        } finally {
            MemoryUtil.memFree(oggBuffer);
        }
    }

    /**
     * Decodes an OGG vorbis buffer to PCM and uploads to OpenAL.
     */
    private int decodeAndUpload(ByteBuffer oggData, String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errorBuf = stack.mallocInt(1);

            long decoder = STBVorbis.stb_vorbis_open_memory(oggData, errorBuf, null);
            if (decoder == 0) {
                LOGGER.warn("STBVorbis failed to open {}: error code {}", name, errorBuf.get(0));
                return 0;
            }

            try {
                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);

                int channels = info.channels();
                int sampleRate = info.sample_rate();
                int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

                if (totalSamples <= 0 || channels <= 0 || sampleRate <= 0) {
                    LOGGER.warn("Invalid audio params in {}: ch={} rate={} samples={}",
                            name, channels, sampleRate, totalSamples);
                    return 0;
                }

                // Decode interleaved short PCM
                int totalShorts = totalSamples * channels;
                ShortBuffer pcm = MemoryUtil.memAllocShort(totalShorts);
                try {
                    int decoded = STBVorbis.stb_vorbis_get_samples_short_interleaved(
                            decoder, channels, pcm);

                    if (decoded <= 0) {
                        LOGGER.warn("STBVorbis decoded 0 samples from {}", name);
                        return 0;
                    }

                    pcm.limit(decoded * channels);

                    int format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    int bufferId = AL10.alGenBuffers();
                    AL10.alBufferData(bufferId, format, pcm, sampleRate);

                    int error = AL10.alGetError();
                    if (error != AL10.AL_NO_ERROR) {
                        LOGGER.warn("OpenAL error uploading {}: 0x{}", name, Integer.toHexString(error));
                        AL10.alDeleteBuffers(bufferId);
                        return 0;
                    }

                    return bufferId;
                } finally {
                    MemoryUtil.memFree(pcm);
                }
            } finally {
                STBVorbis.stb_vorbis_close(decoder);
            }
        }
    }
}
