package com.sonicether.soundphysics.eap.emitter;

public enum SampleSubcategory {
    CRICKET("insects/cricket"),
    CICADA("insects/cicada"),
    KATYDID("insects/katydid"),
    GENERIC_NIGHT("insects/generic_night"),
    TREE_FROG("frogs/tree_frog"),
    BULLFROG("frogs/bullfrog"),
    CHORUS_FROG("frogs/chorus_frog"),
    SPRING_PEEPER("frogs/spring_peeper"),
    WATER_FLOW_SAMPLE("water/flow"),
    WATER_STILL("water/still"),
    WATER_DRIP_SAMPLE("water/drip"),
    WATER_RAIN_SAMPLE("water/rain");

    public final String resourcePath;

    SampleSubcategory(String resourcePath) {
        this.resourcePath = resourcePath;
    }
}
