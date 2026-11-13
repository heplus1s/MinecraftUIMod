package dev.jdguo.land.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Whole chunks chosen on the world map or the minimap waypoint screen. */
public final class MapPick {
    public static final int MAX = 512;

    public record Chunk(String dimension, int x, int z) {
    }

    private static final LinkedHashSet<Chunk> SELECTED = new LinkedHashSet<>();
    private static String notice = "";

    private MapPick() {
    }

    public static synchronized List<Chunk> selected() {
        return List.copyOf(SELECTED);
    }

    public static synchronized boolean contains(String dimension, int chunkX, int chunkZ) {
        return SELECTED.contains(new Chunk(dimension, chunkX, chunkZ));
    }

    public static synchronized String notice() {
        return notice;
    }

    public static synchronized void clear() {
        SELECTED.clear();
        notice = "已清除地图上的选取。";
    }

    public static synchronized String toggle(String dimension, int chunkX, int chunkZ) {
        if (dimension == null || dimension.isBlank()) {
            notice = "还不知道这一块在哪个世界。";
            return notice;
        }
        Chunk chunk = new Chunk(dimension, chunkX, chunkZ);
        if (SELECTED.remove(chunk)) {
            notice = "已取消区块 (" + chunkX + ", " + chunkZ + ")。";
            return notice;
        }
        if (SELECTED.size() >= MAX) {
            notice = "一次最多选取 " + MAX + " 块。";
            return notice;
        }
        SELECTED.add(chunk);
        notice = "已选取区块 (" + chunkX + ", " + chunkZ + ")，每块都是 16×16。";
        return notice;
    }

    public static synchronized String toggleBlockText(String dimension, String blockX, String blockZ) {
        Integer x = parse(blockX);
        Integer z = parse(blockZ);
        if (x == null || z == null) {
            notice = "地图上的坐标还不是数字，没有选取。";
            return notice;
        }
        return toggle(dimension, ChunkGrid.chunkOfBlock(x), ChunkGrid.chunkOfBlock(z));
    }

    public static synchronized List<Chunk> inDimension(String dimension) {
        List<Chunk> chunks = new ArrayList<>();
        for (Chunk chunk : SELECTED) {
            if (chunk.dimension.equals(dimension)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    static synchronized void resetForTests() {
        SELECTED.clear();
        notice = "";
    }

    private static Integer parse(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
