package dev.jdguo.land.ui.net;

import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Claim picture the land server already sends on {@code landmod:claims}.
 * This is the client reader only. The save format and protection stay in landmod.
 */
public record ClaimSnapshot(int generation, List<PlacedClaim> claims) {
    public static final int PROTOCOL = 2;
    private static final int MAX_CLAIMS = 1_000_000;

    public ClaimSnapshot {
        claims = List.copyOf(claims);
    }

    public static ClaimSnapshot read(PacketByteBuf buf) {
        int protocol = buf.readVarInt();
        if (protocol != PROTOCOL) {
            throw new IllegalArgumentException("Unsupported land claim packet protocol " + protocol);
        }
        int generation = buf.readVarInt();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_CLAIMS) {
            throw new IllegalArgumentException("Unreasonable land claim count " + count);
        }
        List<PlacedClaim> claims = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            claims.add(new PlacedClaim(
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUuid(),
                    buf.readString(),
                    buf.readString(),
                    buf.readUuid()));
        }
        return new ClaimSnapshot(generation, claims);
    }

    public record PlacedClaim(
            String dimension,
            int chunkX,
            int chunkZ,
            UUID owner,
            String ownerName,
            String name,
            UUID parcelId) {
    }
}
