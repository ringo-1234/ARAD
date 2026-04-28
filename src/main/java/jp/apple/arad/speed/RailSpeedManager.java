package jp.apple.arad.speed;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.*;

public final class RailSpeedManager extends WorldSavedData {

    public static final String DATA_NAME = "arad_rail_speed";

    private final Map<String, Integer> speedMap = new LinkedHashMap<>();

    public RailSpeedManager(String name) {
        super(name);
    }

    public static RailSpeedManager get(World world) {
        RailSpeedManager mgr = (RailSpeedManager) world.loadData(RailSpeedManager.class, DATA_NAME);
        if (mgr == null) {
            mgr = new RailSpeedManager(DATA_NAME);
            world.setData(DATA_NAME, mgr);
        }
        return mgr;
    }

    public void setSpeedLimit(String key, int kmh) {
        if (kmh <= 0) {
            speedMap.remove(key);
        } else {
            speedMap.put(key, kmh);
        }
        markDirty();
    }

    public int getSpeedLimit(String key) {
        Integer v = speedMap.get(key);
        return v != null ? v : 0;
    }

    public List<RailSpeedEntry> toEntries() {
        List<RailSpeedEntry> list = new ArrayList<>(speedMap.size());
        for (Map.Entry<String, Integer> e : speedMap.entrySet()) {
            list.add(new RailSpeedEntry(e.getKey(), e.getValue()));
        }
        return list;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        speedMap.clear();
        NBTTagList list = nbt.getTagList("speeds", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            speedMap.put(tag.getString("key"), tag.getInteger("kmh"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> e : speedMap.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("key", e.getKey());
            tag.setInteger("kmh", e.getValue());
            list.appendTag(tag);
        }
        nbt.setTag("speeds", list);
        return nbt;
    }
}