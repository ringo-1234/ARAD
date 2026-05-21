package jp.apple.arad.station;

import jp.apple.arad.data.StationSnapshot;
import net.minecraft.world.World;

import java.util.*;

public final class StationRegistry {

    public static final StationRegistry INSTANCE = new StationRegistry();
    private final Map<String, TileEntityStation> loadedStationMap = new LinkedHashMap<>();
    private final Map<String, StationSnapshot> stationCache = new LinkedHashMap<>();

    private StationRegistry() {
    }

    private static StationSnapshot toSnapshot(TileEntityStation te) {
        if (te.getWorld() == null)
            return null;
        return new StationSnapshot(
                te.getStationId(),
                te.getStationName(),
                (float) (te.getPos().getX() + 0.5),
                (float) (te.getPos().getZ() + 0.5),
                te.getWorld().provider.getDimension(),
                te.isDoorLeft(),
                te.isDoorRight(),
                te.isSpawnReversed(),
                te.getDwellTimeTicks());
    }

    private static int toBlockCoord(float centerCoord) {
        return (int) Math.floor(centerCoord);
    }

    public void register(TileEntityStation te) {
        if (te == null)
            return;
        loadedStationMap.put(te.getStationId(), te);
        StationSnapshot snapshot = toSnapshot(te);
        if (snapshot != null) {
            purgeStaleSnapshotsAtSamePos(te.getWorld(), snapshot);
            stationCache.put(snapshot.id, snapshot);
            if (te.getWorld() != null && !te.getWorld().isRemote) {
                StationCacheStore.get(te.getWorld()).upsert(snapshot);
            }
        }
    }

    public void unregister(String stationId) {
        loadedStationMap.remove(stationId);
    }

    public void loadFromWorld(World world) {
        stationCache.clear();
        if (world == null || world.isRemote)
            return;
        for (StationSnapshot s : StationCacheStore.get(world).getAllSnapshots()) {
            stationCache.put(s.id, s);
        }
    }

    public void clearLoaded() {
        loadedStationMap.clear();
    }

    public void removeFromCache(World world, String stationId) {
        loadedStationMap.remove(stationId);
        stationCache.remove(stationId);
        if (world != null && !world.isRemote) {
            StationCacheStore.get(world).remove(stationId);
        }
    }

    public void removeFromCache(String stationId) {
        removeFromCache(null, stationId);
    }

    public void removeFromCacheByPos(World world, int dim, int x, int z) {
        List<String> removeIds = new ArrayList<>();
        for (StationSnapshot s : stationCache.values()) {
            if (s.dim != dim)
                continue;
            if (toBlockCoord(s.x) != x || toBlockCoord(s.z) != z)
                continue;
            removeIds.add(s.id);
        }
        for (String id : removeIds) {
            removeFromCache(world, id);
        }
    }

    public void removeFromCacheByPos(int dim, int x, int z) {
        removeFromCacheByPos(null, dim, x, z);
    }

    public void clear() {
        loadedStationMap.clear();
        stationCache.clear();
    }

    public Collection<TileEntityStation> getAll() {
        return Collections.unmodifiableCollection(loadedStationMap.values());
    }

    public TileEntityStation get(String stationId) {
        return loadedStationMap.get(stationId);
    }

    public StationSnapshot getSnapshot(String stationId) {
        return stationCache.get(stationId);
    }

    public List<StationSnapshot> toSnapshots() {
        return new ArrayList<>(stationCache.values());
    }

    public TileEntityStation findNearest(int dim, double x, double z, double maxDist) {
        TileEntityStation nearest = null;
        double best = maxDist * maxDist;
        for (TileEntityStation te : loadedStationMap.values()) {
            if (te.getWorld() == null)
                continue;
            if (te.getWorld().provider.getDimension() != dim)
                continue;
            double dx = te.getPos().getX() + 0.5 - x;
            double dz = te.getPos().getZ() + 0.5 - z;
            double d2 = dx * dx + dz * dz;
            if (d2 < best) {
                best = d2;
                nearest = te;
            }
        }
        return nearest;
    }

    private void purgeStaleSnapshotsAtSamePos(World world, StationSnapshot snapshot) {
        int bx = toBlockCoord(snapshot.x);
        int bz = toBlockCoord(snapshot.z);
        List<String> staleIds = new ArrayList<>();
        for (StationSnapshot s : stationCache.values()) {
            if (s.id.equals(snapshot.id))
                continue;
            if (s.dim != snapshot.dim)
                continue;
            if (toBlockCoord(s.x) != bx || toBlockCoord(s.z) != bz)
                continue;
            staleIds.add(s.id);
        }
        for (String staleId : staleIds) {
            loadedStationMap.remove(staleId);
            stationCache.remove(staleId);
            if (world != null && !world.isRemote) {
                StationCacheStore.get(world).remove(staleId);
            }
        }
    }
}
