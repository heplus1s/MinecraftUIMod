package dev.jdguo.land.ui;

import dev.jdguo.land.ui.net.ClaimSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client copy of claimed chunks, replaced as a whole when the server sends a snapshot. */
public final class ClientClaims {
    private static volatile View view = View.EMPTY;

    private ClientClaims() {
    }

    public static void replace(ClaimSnapshot snapshot) {
        view = View.from(snapshot);
    }

    public static boolean received() {
        return view.received;
    }

    public static Entry get(String dimension, int chunkX, int chunkZ) {
        return view.get(dimension, chunkX, chunkZ);
    }

    public static boolean regionHas(String dimension, int regionX, int regionZ) {
        return view.regionHas(dimension, regionX, regionZ);
    }

    public static int regionHash(String dimension, int regionX, int regionZ) {
        return view.regionHash(dimension, regionX, regionZ);
    }

    public static int generation() {
        return view.generation;
    }

    public static int parcelChunks(String dimension, UUID parcelId) {
        return view.parcelChunks(dimension, parcelId);
    }

    public static List<ParcelView> parcels(String dimension) {
        return view.parcels(dimension);
    }

    public record Entry(int chunkX, int chunkZ, UUID owner, String ownerName, String name, UUID parcelId) {
    }

    public record ParcelView(
            UUID id,
            String name,
            String ownerName,
            String dimension,
            int chunkCount,
            List<Long> packedChunks) {
        public String shortId() {
            return id.toString().replace("-", "").substring(0, 8);
        }

        public String label() {
            String title = name == null || name.isBlank() ? "未命名宗地" : name;
            return title + "（" + (ownerName == null || ownerName.isBlank() ? "未知" : ownerName) + "） · "
                    + chunkCount + " 块 · 每块 16×16";
        }
    }

    private static final class View {
        static final View EMPTY = new View(0, false, Map.of(), Map.of());

        private final int generation;
        private final boolean received;
        private final Map<String, Map<Long, Entry>> chunks;
        private final Map<String, Map<Long, List<Entry>>> regions;

        private View(int generation, boolean received, Map<String, Map<Long, Entry>> chunks,
                     Map<String, Map<Long, List<Entry>>> regions) {
            this.generation = generation;
            this.received = received;
            this.chunks = chunks;
            this.regions = regions;
        }

        static View from(ClaimSnapshot snapshot) {
            Map<String, Map<Long, Entry>> chunks = new HashMap<>();
            Map<String, Map<Long, List<Entry>>> regions = new HashMap<>();
            for (ClaimSnapshot.PlacedClaim claim : snapshot.claims()) {
                Entry entry = new Entry(claim.chunkX(), claim.chunkZ(), claim.owner(), claim.ownerName(), claim.name(), claim.parcelId());
                chunks.computeIfAbsent(claim.dimension(), key -> new HashMap<>())
                        .put(ChunkGrid.pack(claim.chunkX(), claim.chunkZ()), entry);
                int regionX = claim.chunkX() >> 5;
                int regionZ = claim.chunkZ() >> 5;
                regions.computeIfAbsent(claim.dimension(), key -> new HashMap<>())
                        .computeIfAbsent(ChunkGrid.pack(regionX, regionZ), key -> new ArrayList<>())
                        .add(entry);
            }
            for (Map<Long, List<Entry>> byRegion : regions.values()) {
                for (List<Entry> entries : byRegion.values()) {
                    entries.sort(Comparator.comparingInt(Entry::chunkX).thenComparingInt(Entry::chunkZ));
                }
            }
            return new View(snapshot.generation(), true, chunks, regions);
        }

        Entry get(String dimension, int chunkX, int chunkZ) {
            Map<Long, Entry> inDimension = chunks.get(dimension);
            if (inDimension == null) {
                return null;
            }
            return inDimension.get(ChunkGrid.pack(chunkX, chunkZ));
        }

        boolean regionHas(String dimension, int regionX, int regionZ) {
            Map<Long, List<Entry>> inDimension = regions.get(dimension);
            return inDimension != null && inDimension.containsKey(ChunkGrid.pack(regionX, regionZ));
        }

        int regionHash(String dimension, int regionX, int regionZ) {
            int hash = 17;
            hash = 31 * hash + generation;
            hash = 31 * hash + dimension.hashCode();
            hash = 31 * hash + regionX;
            hash = 31 * hash + regionZ;
            Map<Long, List<Entry>> inDimension = regions.get(dimension);
            if (inDimension == null) {
                return hash;
            }
            List<Entry> entries = inDimension.get(ChunkGrid.pack(regionX, regionZ));
            if (entries == null) {
                return hash;
            }
            for (Entry entry : entries) {
                hash = 31 * hash + entry.chunkX();
                hash = 31 * hash + entry.chunkZ();
                hash = 31 * hash + entry.owner().hashCode();
                hash = 31 * hash + entry.ownerName().hashCode();
                hash = 31 * hash + entry.name().hashCode();
                hash = 31 * hash + entry.parcelId().hashCode();
            }
            return hash;
        }

        int parcelChunks(String dimension, UUID parcelId) {
            if (parcelId == null) {
                return 0;
            }
            Map<Long, Entry> inDimension = chunks.get(dimension);
            if (inDimension == null) {
                return 0;
            }
            int count = 0;
            for (Entry entry : inDimension.values()) {
                if (parcelId.equals(entry.parcelId())) {
                    count++;
                }
            }
            return count;
        }

        List<ParcelView> parcels(String dimension) {
            Map<Long, Entry> inDimension = chunks.get(dimension);
            if (inDimension == null) {
                return List.of();
            }
            Map<UUID, List<Entry>> grouped = new LinkedHashMap<>();
            for (Entry entry : inDimension.values()) {
                grouped.computeIfAbsent(entry.parcelId(), key -> new ArrayList<>()).add(entry);
            }
            List<ParcelView> views = new ArrayList<>();
            for (List<Entry> entries : grouped.values()) {
                entries.sort(Comparator.comparingInt(Entry::chunkX).thenComparingInt(Entry::chunkZ));
                Entry first = entries.get(0);
                List<Long> packed = new ArrayList<>();
                for (Entry entry : entries) {
                    packed.add(ChunkGrid.pack(entry.chunkX(), entry.chunkZ()));
                }
                views.add(new ParcelView(first.parcelId(), first.name(), first.ownerName(), dimension, entries.size(), List.copyOf(packed)));
            }
            views.sort(Comparator.comparing(ParcelView::name).thenComparing(view -> view.id().toString()));
            return views;
        }
    }
}
