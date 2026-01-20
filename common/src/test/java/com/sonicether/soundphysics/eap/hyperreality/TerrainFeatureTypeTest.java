package com.sonicether.soundphysics.eap.hyperreality;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TerrainFeatureTypeTest {

    @Test
    void allSevenTypesExist() {
        assertEquals(7, TerrainFeatureType.values().length);
        assertNotNull(TerrainFeatureType.EDGE);
        assertNotNull(TerrainFeatureType.STEP);
        assertNotNull(TerrainFeatureType.DROP);
        assertNotNull(TerrainFeatureType.WALL);
        assertNotNull(TerrainFeatureType.CEILING);
        assertNotNull(TerrainFeatureType.PASSAGE);
        assertNotNull(TerrainFeatureType.SOLID_OBJECT);
    }

    @Test
    void allThreeFamiliesExist() {
        assertEquals(3, TerrainFeatureType.Family.values().length);
        assertNotNull(TerrainFeatureType.Family.VOID);
        assertNotNull(TerrainFeatureType.Family.SURFACE);
        assertNotNull(TerrainFeatureType.Family.GROUND);
    }

    @Test
    void voidFamily_containsEdgeDropPassage() {
        assertEquals(TerrainFeatureType.Family.VOID, TerrainFeatureType.EDGE.family());
        assertEquals(TerrainFeatureType.Family.VOID, TerrainFeatureType.DROP.family());
        assertEquals(TerrainFeatureType.Family.VOID, TerrainFeatureType.PASSAGE.family());
    }

    @Test
    void surfaceFamily_containsWallCeilingSolidObject() {
        assertEquals(TerrainFeatureType.Family.SURFACE, TerrainFeatureType.WALL.family());
        assertEquals(TerrainFeatureType.Family.SURFACE, TerrainFeatureType.CEILING.family());
        assertEquals(TerrainFeatureType.Family.SURFACE, TerrainFeatureType.SOLID_OBJECT.family());
    }

    @Test
    void groundFamily_containsStep() {
        assertEquals(TerrainFeatureType.Family.GROUND, TerrainFeatureType.STEP.family());
    }

    @Test
    void maxRanges_matchSpec() {
        assertEquals(24f, TerrainFeatureType.EDGE.maxRange());
        assertEquals(12f, TerrainFeatureType.STEP.maxRange());
        assertEquals(24f, TerrainFeatureType.DROP.maxRange());
        assertEquals(24f, TerrainFeatureType.WALL.maxRange());
        assertEquals(24f, TerrainFeatureType.CEILING.maxRange());
        assertEquals(24f, TerrainFeatureType.PASSAGE.maxRange());
        assertEquals(24f, TerrainFeatureType.SOLID_OBJECT.maxRange());
    }

    @Test
    void priorities_matchSpec() {
        assertEquals(5, TerrainFeatureType.EDGE.priority());
        assertEquals(2, TerrainFeatureType.STEP.priority());
        assertEquals(5, TerrainFeatureType.DROP.priority());
        assertEquals(3, TerrainFeatureType.WALL.priority());
        assertEquals(2, TerrainFeatureType.CEILING.priority());
        assertEquals(4, TerrainFeatureType.PASSAGE.priority());
        assertEquals(3, TerrainFeatureType.SOLID_OBJECT.priority());
    }

    @Test
    void safetyCriticalTypes_haveHighestPriority() {
        assertEquals(5, TerrainFeatureType.EDGE.priority());
        assertEquals(5, TerrainFeatureType.DROP.priority());
        for (TerrainFeatureType type : TerrainFeatureType.values()) {
            assertTrue(type.priority() <= 5);
            assertTrue(type.priority() >= 1);
        }
    }

    @Test
    void familyDensityCaps_matchSpec() {
        assertEquals(16, TerrainFeatureType.Family.VOID.densityCap());
        assertEquals(20, TerrainFeatureType.Family.SURFACE.densityCap());
        assertEquals(12, TerrainFeatureType.Family.GROUND.densityCap());
    }

    @Test
    void totalDensityCap_fits48SourcePool() {
        int total = 0;
        for (TerrainFeatureType.Family f : TerrainFeatureType.Family.values()) {
            total += f.densityCap();
        }
        assertEquals(48, total);
    }

    @Test
    void familyGains_matchSpec() {
        assertEquals(0.08f, TerrainFeatureType.Family.VOID.baseGain(), 0.001f);
        assertEquals(0.06f, TerrainFeatureType.Family.SURFACE.baseGain(), 0.001f);
        assertEquals(0.03f, TerrainFeatureType.Family.GROUND.baseGain(), 0.001f);
    }

    @Test
    void familyPriorityWeights_matchSpec() {
        assertEquals(1.5f, TerrainFeatureType.Family.VOID.priorityWeight(), 0.001f);
        assertEquals(1.0f, TerrainFeatureType.Family.SURFACE.priorityWeight(), 0.001f);
        assertEquals(0.7f, TerrainFeatureType.Family.GROUND.priorityWeight(), 0.001f);
    }

    @Test
    void familyWeight_delegatesToFamily() {
        assertEquals(1.5f, TerrainFeatureType.EDGE.familyWeight(), 0.001f);
        assertEquals(1.0f, TerrainFeatureType.WALL.familyWeight(), 0.001f);
        assertEquals(0.7f, TerrainFeatureType.STEP.familyWeight(), 0.001f);
    }

    @Test
    void typesGroupedByFamily_areComplete() {
        List<TerrainFeatureType> voids = Arrays.stream(TerrainFeatureType.values())
                .filter(t -> t.family() == TerrainFeatureType.Family.VOID)
                .collect(Collectors.toList());
        assertEquals(3, voids.size());

        List<TerrainFeatureType> surfaces = Arrays.stream(TerrainFeatureType.values())
                .filter(t -> t.family() == TerrainFeatureType.Family.SURFACE)
                .collect(Collectors.toList());
        assertEquals(3, surfaces.size());

        List<TerrainFeatureType> grounds = Arrays.stream(TerrainFeatureType.values())
                .filter(t -> t.family() == TerrainFeatureType.Family.GROUND)
                .collect(Collectors.toList());
        assertEquals(1, grounds.size());
    }
}
