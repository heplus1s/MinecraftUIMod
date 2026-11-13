package dev.jdguo.land.ui;

import java.util.Collection;

/**
 * Decides which existing land command to send. The server still owns the rules.
 * A click on the map never pretends a chunk was bought.
 */
public final class PurchasePlan {
    public enum Mode {
        WHOLE_PARCEL,
        SPLIT_CHUNK
    }

    public record Decision(String command, String refusal) {
        public boolean allowed() {
            return command != null;
        }

        public static Decision command(String command) {
            return new Decision(command, null);
        }

        public static Decision refuse(String reason) {
            return new Decision(null, reason);
        }
    }

    private PurchasePlan() {
    }

    public static Decision claim(boolean known, boolean alreadyClaimed) {
        if (known && alreadyClaimed) {
            return Decision.refuse("脚下这一块已经有主人。");
        }
        return Decision.command("land claim");
    }

    public static Decision join(String token, boolean inside, boolean adjacent) {
        if (token == null || token.isBlank()) {
            return Decision.refuse("先选中一块宗地。");
        }
        if (inside) {
            return Decision.refuse("你已经站在这块宗地里。");
        }
        if (!adjacent) {
            return Decision.refuse("需要与这块宗地共边。斜对角不算相邻。");
        }
        return Decision.command("land claim join " + token);
    }

    public static Decision mark() {
        return Decision.command("land mark");
    }

    public static Decision split(String rawName) {
        String cleaned = ParcelNameCheck.clean(rawName);
        if (cleaned == null) {
            return Decision.refuse("名称最长 32 个字，且不能含有控制字符。");
        }
        if (cleaned.isEmpty()) {
            return Decision.command("land split");
        }
        return Decision.command("land split " + cleaned);
    }

    public static String token(String name, String shortId) {
        String cleaned = ParcelNameCheck.clean(name);
        if (cleaned != null && !cleaned.isEmpty()) {
            return cleaned;
        }
        return shortId == null ? "" : shortId;
    }

    public static boolean adjacentToAny(int chunkX, int chunkZ, Collection<Long> packedChunks) {
        if (packedChunks == null) {
            return false;
        }
        for (long packed : packedChunks) {
            int otherX = (int) (packed >> 32);
            int otherZ = (int) packed;
            if (ChunkGrid.edgeAdjacent(chunkX, chunkZ, otherX, otherZ)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(int chunkX, int chunkZ, Collection<Long> packedChunks) {
        if (packedChunks == null) {
            return false;
        }
        return packedChunks.contains(ChunkGrid.pack(chunkX, chunkZ));
    }
}
