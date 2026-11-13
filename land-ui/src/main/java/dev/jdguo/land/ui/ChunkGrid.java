package dev.jdguo.land.ui;

/**
 * Chunk-grid geometry. A parcel is made of whole 16×16 chunks that share an edge.
 * Diagonal chunks are not adjacent, and nothing smaller than a chunk is selectable.
 */
public final class ChunkGrid {
    private ChunkGrid() {
    }

    public static int chunkOfBlock(int block) {
        return block >> 4;
    }

    public static boolean edgeAdjacent(int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
        int dx = Math.abs(chunkX1 - chunkX2);
        int dz = Math.abs(chunkZ1 - chunkZ2);
        return dx + dz == 1;
    }

    public static long pack(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
