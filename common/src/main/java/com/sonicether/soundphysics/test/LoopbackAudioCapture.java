package com.sonicether.soundphysics.test;

import com.sonicether.soundphysics.Loggers;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.SOFTLoopback;

import java.io.*;
import java.nio.ByteBuffer;

public class LoopbackAudioCapture {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int BYTES_PER_FRAME = CHANNELS * (BITS_PER_SAMPLE / 8); // 4
    private static final int CHUNK_SIZE = 1024; // samples per render call

    private static long device;
    private static volatile long pendingDevice = 0; // Set by LibraryMixin before initialize()
    private static volatile boolean running = false;
    private static volatile boolean capturing = false;
    private static ByteArrayOutputStream captureBuffer;
    private static Thread renderThread;

    public static void initialize(long loopbackDevice) {
        device = loopbackDevice;
        running = true;
        renderThread = new Thread(LoopbackAudioCapture::renderLoop, "EchoField-AudioRender");
        renderThread.setDaemon(true);
        renderThread.start();
        Loggers.log("LoopbackAudioCapture: initialized, render thread started (48kHz stereo 16-bit)");
    }

    private static void renderLoop() {
        ByteBuffer renderBuffer = BufferUtils.createByteBuffer(CHUNK_SIZE * BYTES_PER_FRAME);
        long chunkDurationNs = (long) CHUNK_SIZE * 1_000_000_000L / SAMPLE_RATE;

        while (running) {
            long startNs = System.nanoTime();

            // Render audio (this advances OpenAL's internal processing)
            SOFTLoopback.alcRenderSamplesSOFT(device, renderBuffer, CHUNK_SIZE);

            // If capturing, copy rendered samples to capture buffer
            if (capturing && captureBuffer != null) {
                byte[] data = new byte[CHUNK_SIZE * BYTES_PER_FRAME];
                renderBuffer.get(data);
                renderBuffer.rewind();
                synchronized (captureBuffer) {
                    try {
                        captureBuffer.write(data);
                    } catch (Exception e) {
                        Loggers.log("LoopbackAudioCapture: write error: " + e.getMessage());
                    }
                }
            }
            renderBuffer.clear();

            // Maintain real-time pace
            long elapsedNs = System.nanoTime() - startNs;
            long sleepNs = chunkDurationNs - elapsedNs;
            if (sleepNs > 1_000_000) {
                try {
                    Thread.sleep(sleepNs / 1_000_000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static synchronized void startCapture() {
        captureBuffer = new ByteArrayOutputStream();
        capturing = true;
        Loggers.log("LoopbackAudioCapture: capture started");
    }

    public static synchronized String stopCapture(String outputPath) throws IOException {
        capturing = false;
        byte[] audioData;
        synchronized (captureBuffer) {
            audioData = captureBuffer.toByteArray();
        }
        captureBuffer = null;
        writeWav(outputPath, audioData);
        Loggers.log("LoopbackAudioCapture: wrote " + audioData.length + " bytes (" +
                String.format("%.1f", (double) audioData.length / BYTES_PER_FRAME / SAMPLE_RATE) + "s) to " + outputPath);
        return outputPath;
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static boolean isInitialized() {
        return running && renderThread != null && renderThread.isAlive();
    }

    /** Called by LibraryMixin to store the device handle before initialize() is called */
    public static void setDeviceHandle(long loopbackDevice) {
        pendingDevice = loopbackDevice;
    }

    /** Get the pending device handle (set by LibraryMixin) */
    public static long getPendingDevice() {
        return pendingDevice;
    }

    private static void writeWav(String path, byte[] audioData) throws IOException {
        int dataSize = audioData.length;
        try (FileOutputStream fos = new FileOutputStream(path);
             DataOutputStream dos = new DataOutputStream(fos)) {
            // RIFF header
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(36 + dataSize));
            dos.writeBytes("WAVE");
            // fmt chunk
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short) 1)); // PCM
            dos.writeShort(Short.reverseBytes((short) CHANNELS));
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * BYTES_PER_FRAME)); // byte rate
            dos.writeShort(Short.reverseBytes((short) BYTES_PER_FRAME)); // block align
            dos.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE));
            // data chunk
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));
            dos.write(audioData);
        }
    }

    public static void shutdown() {
        running = false;
        capturing = false;
        if (renderThread != null) {
            renderThread.interrupt();
            try {
                renderThread.join(2000);
            } catch (InterruptedException ignored) {}
            renderThread = null;
        }
        Loggers.log("LoopbackAudioCapture: shutdown");
    }
}
