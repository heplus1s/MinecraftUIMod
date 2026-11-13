package dev.jdguo.land.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkGridTest {
    @Test
    void blockCoordinatesBecomeWholeChunksIncludingNegatives() {
        assertEquals(0, ChunkGrid.chunkOfBlock(0));
        assertEquals(0, ChunkGrid.chunkOfBlock(15));
        assertEquals(1, ChunkGrid.chunkOfBlock(16));
        assertEquals(-1, ChunkGrid.chunkOfBlock(-1));
        assertEquals(-1, ChunkGrid.chunkOfBlock(-16));
        assertEquals(-2, ChunkGrid.chunkOfBlock(-17));
    }

    @Test
    void onlySharedEdgesCountAsAdjacent() {
        assertTrue(ChunkGrid.edgeAdjacent(0, 0, 1, 0));
        assertTrue(ChunkGrid.edgeAdjacent(0, 0, 0, -1));
        assertFalse(ChunkGrid.edgeAdjacent(0, 0, 1, 1));
        assertFalse(ChunkGrid.edgeAdjacent(0, 0, 0, 0));
    }
}
