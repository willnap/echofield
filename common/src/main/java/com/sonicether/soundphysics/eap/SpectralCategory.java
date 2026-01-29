package com.sonicether.soundphysics.eap;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.IdentityHashMap;
import java.util.Map;

/** Maps SoundTypes to acoustic hardness categories with 3-band absorption multipliers. */
public enum SpectralCategory {
<<<<<<< ours
    //                     lowMult midMult highMult lfDecay lfGain   lowAbs midAbs highAbs
    HARD(1.0f, 1.0f, 1.0f, 1.0f, 1.0f,           0.02f, 0.03f, 0.05f),  // stone/metal: very reflective
    SOFT(0.5f, 1.0f, 2.0f, 0.7f, 0.7f,            0.25f, 0.40f, 0.55f),  // wool/snow: porous absorber
    FOLIAGE(0.3f, 1.2f, 1.8f, 1.1f, 0.9f,         0.10f, 0.20f, 0.35f),  // leaves/grass: transparent to LF
    WOOD(0.7f, 1.0f, 1.3f, 0.9f, 0.85f,           0.10f, 0.08f, 0.12f),  // membrane absorber
    DEFAULT(1.0f, 1.0f, 1.0f, 1.0f, 1.0f,         0.05f, 0.05f, 0.05f);  // conservative estimate

    private final float lowMult, midMult, highMult, lfDecayRatio, lfGain;
    private final float lowAbs, midAbs, highAbs;

    SpectralCategory(float low, float mid, float high, float lfDecayRatio, float lfGain,
                     float lowAbs, float midAbs, float highAbs) {
        this.lowMult = low; this.midMult = mid; this.highMult = high;
        this.lfDecayRatio = lfDecayRatio; this.lfGain = lfGain;
        this.lowAbs = lowAbs; this.midAbs = midAbs; this.highAbs = highAbs;
=======
    HARD(1.0f, 1.0f, 1.0f),
    SOFT(0.5f, 1.0f, 2.0f),
    FOLIAGE(0.3f, 1.2f, 1.8f),
    WOOD(0.7f, 1.0f, 1.3f),
    DEFAULT(1.0f, 1.0f, 1.0f);

    private final float lowMult, midMult, highMult;

    SpectralCategory(float low, float mid, float high) {
        this.lowMult = low; this.midMult = mid; this.highMult = high;
>>>>>>> theirs
    }

    public float lowMult() { return lowMult; }
    public float midMult() { return midMult; }
    public float highMult() { return highMult; }
<<<<<<< ours
    public float lfDecayRatio() { return lfDecayRatio; }
    public float lfGain() { return lfGain; }
    /** Physical absorption coefficient for low frequencies (~250 Hz). */
    public float lowAbs() { return lowAbs; }
    /** Physical absorption coefficient for mid frequencies (~2 kHz). */
    public float midAbs() { return midAbs; }
    /** Physical absorption coefficient for high frequencies (~8 kHz). */
    public float highAbs() { return highAbs; }

    private static volatile Map<SoundType, SpectralCategory> LOOKUP;

    private static Map<SoundType, SpectralCategory> getLookup() {
        if (LOOKUP == null) {
            synchronized (SpectralCategory.class) {
                if (LOOKUP == null) {
                    Map<SoundType, SpectralCategory> map = new IdentityHashMap<>();

                    for (SoundType t : new SoundType[]{
                            SoundType.STONE, SoundType.METAL, SoundType.GLASS, SoundType.ANVIL,
                            SoundType.COPPER, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS,
                            SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE,
                            SoundType.NETHERITE_BLOCK, SoundType.TUFF, SoundType.CALCITE,
                            SoundType.AMETHYST, SoundType.BASALT, SoundType.BONE_BLOCK,
                            SoundType.NETHER_BRICKS, SoundType.LANTERN, SoundType.CHAIN,
                            SoundType.LODESTONE, SoundType.DRIPSTONE_BLOCK,
                            SoundType.POINTED_DRIPSTONE, SoundType.NETHER_ORE,
                            SoundType.NETHER_GOLD_ORE, SoundType.GILDED_BLACKSTONE,
                            SoundType.MUD_BRICKS, SoundType.PACKED_MUD,
                            SoundType.AMETHYST_CLUSTER, SoundType.SMALL_AMETHYST_BUD,
                            SoundType.MEDIUM_AMETHYST_BUD, SoundType.LARGE_AMETHYST_BUD,
                            SoundType.NETHERRACK, SoundType.DECORATED_POT,
                            SoundType.DECORATED_POT_CRACKED, SoundType.SCULK_SENSOR,
                            SoundType.SCULK_CATALYST, SoundType.SCULK, SoundType.SCULK_VEIN,
                            SoundType.SCULK_SHRIEKER, SoundType.FROGLIGHT, SoundType.ANCIENT_DEBRIS
                    }) { map.put(t, HARD); }

                    for (SoundType t : new SoundType[]{
                            SoundType.WOOL, SoundType.SNOW, SoundType.POWDER_SNOW,
                            SoundType.MOSS, SoundType.MOSS_CARPET, SoundType.HONEY_BLOCK,
                            SoundType.SLIME_BLOCK, SoundType.SOUL_SAND, SoundType.SOUL_SOIL,
                            SoundType.SAND, SoundType.MUD, SoundType.CORAL_BLOCK,
                            SoundType.GRAVEL, SoundType.ROOTED_DIRT,
                            SoundType.SUSPICIOUS_SAND, SoundType.SUSPICIOUS_GRAVEL
                    }) { map.put(t, SOFT); }

                    for (SoundType t : new SoundType[]{
                            SoundType.GRASS, SoundType.WET_GRASS, SoundType.AZALEA_LEAVES,
                            SoundType.CHERRY_LEAVES, SoundType.VINE, SoundType.WEEPING_VINES,
                            SoundType.TWISTING_VINES, SoundType.CAVE_VINES, SoundType.CROP,
                            SoundType.HARD_CROP, SoundType.SWEET_BERRY_BUSH,
                            SoundType.NETHER_WART, SoundType.NETHER_SPROUTS, SoundType.ROOTS,
                            SoundType.HANGING_ROOTS, SoundType.BAMBOO_SAPLING,
                            SoundType.CHERRY_SAPLING, SoundType.SPORE_BLOSSOM,
                            SoundType.AZALEA, SoundType.FLOWERING_AZALEA,
                            SoundType.BIG_DRIPLEAF, SoundType.SMALL_DRIPLEAF,
                            SoundType.GLOW_LICHEN, SoundType.FUNGUS, SoundType.SHROOMLIGHT,
                            SoundType.WART_BLOCK, SoundType.NYLIUM, SoundType.FROGSPAWN,
                            SoundType.LILY_PAD, SoundType.SCAFFOLDING,
                            SoundType.MANGROVE_ROOTS, SoundType.MUDDY_MANGROVE_ROOTS,
                            SoundType.CANDLE
                    }) { map.put(t, FOLIAGE); }

                    for (SoundType t : new SoundType[]{
                            SoundType.WOOD, SoundType.BAMBOO, SoundType.BAMBOO_WOOD,
                            SoundType.NETHER_WOOD, SoundType.CHERRY_WOOD, SoundType.STEM,
                            SoundType.LADDER, SoundType.HANGING_SIGN,
                            SoundType.NETHER_WOOD_HANGING_SIGN,
                            SoundType.BAMBOO_WOOD_HANGING_SIGN,
                            SoundType.CHERRY_WOOD_HANGING_SIGN, SoundType.CHISELED_BOOKSHELF
                    }) { map.put(t, WOOD); }

                    LOOKUP = map;
                }
            }
        }
        return LOOKUP;
=======

    private static final Map<SoundType, SpectralCategory> LOOKUP = new IdentityHashMap<>();

    static {
        for (SoundType t : new SoundType[]{
                SoundType.STONE, SoundType.METAL, SoundType.GLASS, SoundType.ANVIL,
                SoundType.COPPER, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS,
                SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE,
                SoundType.NETHERITE_BLOCK, SoundType.TUFF, SoundType.CALCITE,
                SoundType.AMETHYST, SoundType.BASALT, SoundType.BONE_BLOCK,
                SoundType.NETHER_BRICKS, SoundType.LANTERN, SoundType.CHAIN,
                SoundType.LODESTONE, SoundType.DRIPSTONE_BLOCK,
                SoundType.POINTED_DRIPSTONE, SoundType.NETHER_ORE,
                SoundType.NETHER_GOLD_ORE, SoundType.GILDED_BLACKSTONE,
                SoundType.MUD_BRICKS, SoundType.PACKED_MUD,
                SoundType.AMETHYST_CLUSTER, SoundType.SMALL_AMETHYST_BUD,
                SoundType.MEDIUM_AMETHYST_BUD, SoundType.LARGE_AMETHYST_BUD,
                SoundType.NETHERRACK, SoundType.DECORATED_POT,
                SoundType.DECORATED_POT_CRACKED, SoundType.SCULK_SENSOR,
                SoundType.SCULK_CATALYST, SoundType.SCULK, SoundType.SCULK_VEIN,
                SoundType.SCULK_SHRIEKER, SoundType.FROGLIGHT, SoundType.ANCIENT_DEBRIS
        }) { LOOKUP.put(t, HARD); }

        for (SoundType t : new SoundType[]{
                SoundType.WOOL, SoundType.SNOW, SoundType.POWDER_SNOW,
                SoundType.MOSS, SoundType.MOSS_CARPET, SoundType.HONEY_BLOCK,
                SoundType.SLIME_BLOCK, SoundType.SOUL_SAND, SoundType.SOUL_SOIL,
                SoundType.SAND, SoundType.MUD, SoundType.CORAL_BLOCK,
                SoundType.GRAVEL, SoundType.ROOTED_DIRT,
                SoundType.SUSPICIOUS_SAND, SoundType.SUSPICIOUS_GRAVEL
        }) { LOOKUP.put(t, SOFT); }

        for (SoundType t : new SoundType[]{
                SoundType.GRASS, SoundType.WET_GRASS, SoundType.AZALEA_LEAVES,
                SoundType.CHERRY_LEAVES, SoundType.VINE, SoundType.WEEPING_VINES,
                SoundType.TWISTING_VINES, SoundType.CAVE_VINES, SoundType.CROP,
                SoundType.HARD_CROP, SoundType.SWEET_BERRY_BUSH,
                SoundType.NETHER_WART, SoundType.NETHER_SPROUTS, SoundType.ROOTS,
                SoundType.HANGING_ROOTS, SoundType.BAMBOO_SAPLING,
                SoundType.CHERRY_SAPLING, SoundType.SPORE_BLOSSOM,
                SoundType.AZALEA, SoundType.FLOWERING_AZALEA,
                SoundType.BIG_DRIPLEAF, SoundType.SMALL_DRIPLEAF,
                SoundType.GLOW_LICHEN, SoundType.FUNGUS, SoundType.SHROOMLIGHT,
                SoundType.WART_BLOCK, SoundType.NYLIUM, SoundType.FROGSPAWN,
                SoundType.LILY_PAD, SoundType.SCAFFOLDING,
                SoundType.MANGROVE_ROOTS, SoundType.MUDDY_MANGROVE_ROOTS,
                SoundType.CANDLE
        }) { LOOKUP.put(t, FOLIAGE); }

        for (SoundType t : new SoundType[]{
                SoundType.WOOD, SoundType.BAMBOO, SoundType.BAMBOO_WOOD,
                SoundType.NETHER_WOOD, SoundType.CHERRY_WOOD, SoundType.STEM,
                SoundType.LADDER, SoundType.HANGING_SIGN,
                SoundType.NETHER_WOOD_HANGING_SIGN,
                SoundType.BAMBOO_WOOD_HANGING_SIGN,
                SoundType.CHERRY_WOOD_HANGING_SIGN, SoundType.CHISELED_BOOKSHELF
        }) { LOOKUP.put(t, WOOD); }
>>>>>>> theirs
    }

    /** Returns spectral category for the given SoundType; DEFAULT for unmapped types. */
    public static SpectralCategory fromSoundType(SoundType soundType) {
<<<<<<< ours
        return getLookup().getOrDefault(soundType, DEFAULT);
=======
        return LOOKUP.getOrDefault(soundType, DEFAULT);
>>>>>>> theirs
    }

    /** Convenience: derives spectral category from a BlockState via its SoundType. */
    public static SpectralCategory fromBlockState(BlockState blockState) {
        return fromSoundType(blockState.getSoundType());
    }
}
