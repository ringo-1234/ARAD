package jp.apple.arad.station;

import jp.apple.arad.data.StationSnapshot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StationCacheStore extends WorldSavedData {

    public static final String DATA_NAME = "arad_station_cache";

    private final Map<String, StationSnapshot> snapshotMap = new LinkedHashMap<>();

    public StationCacheStore(String name) {
        super(name);
    }

    public static StationCacheStore get(World world) {
        StationCacheStore store = (StationCacheStore) world.loadData(StationCacheStore.class, DATA_NAME);
        if (store == null) {
            store = new StationCacheStore(DATA_NAME);
            world.setData(DATA_NAME, store);
        }
        return store;
    }

    public void upsert(StationSnapshot snapshot) {
        if (snapshot == null || snapshot.id == null || snapshot.id.isEmpty()) return;
        snapshotMap.put(snapshot.id, snapshot);
        markDirty();
    }

    public void remove(String stationId) {
        if (stationId == null || stationId.isEmpty()) return;
        if (snapshotMap.remove(stationId) != null) {
            markDirty();
        }
    }

    public List<StationSnapshot> getAllSnapshots() {
        return new ArrayList<>(snapshotMap.values());
    }

    public StationSnapshot getSnapshot(String stationId) {
        return snapshotMap.get(stationId);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        snapshotMap.clear();
        NBTTagList list = nbt.getTagList("stations", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            StationSnapshot s = new StationSnapshot(
                    tag.getString("id"),
                    tag.getString("name"),
                    tag.getFloat("x"),
                    tag.getFloat("z"),
                    tag.getInteger("dim"),
                    !tag.hasKey("doorLeft") || tag.getBoolean("doorLeft"),
                    !tag.hasKey("doorRight") || tag.getBoolean("doorRight"),
                    tag.hasKey("spawnReversed") && tag.getBoolean("spawnReversed"),
                    tag.hasKey("dwellTicks") ? tag.getInteger("dwellTicks") : 400
            );
            if (s.id != null && !s.id.isEmpty()) {
                snapshotMap.put(s.id, s);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (StationSnapshot s : snapshotMap.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", s.id == null ? "" : s.id);
            tag.setString("name", s.name == null ? "" : s.name);
            tag.setFloat("x", s.x);
            tag.setFloat("z", s.z);
            tag.setInteger("dim", s.dim);
            tag.setBoolean("doorLeft", s.doorLeft);
            tag.setBoolean("doorRight", s.doorRight);
            tag.setBoolean("spawnReversed", s.spawnReversed);
            tag.setInteger("dwellTicks", s.dwellTicks);
            list.appendTag(tag);
        }
        nbt.setTag("stations", list);
        return nbt;
    }
}
