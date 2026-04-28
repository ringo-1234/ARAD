package jp.apple.arad.route;

import jp.apple.arad.data.RouteSnapshot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;

public final class Route {

    public final String       id;
    public String             name;
    public final List<String> stationIds  = new ArrayList<>();
    public int                trainCount  = 0;

    public Route(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    public RouteSnapshot toSnapshot() {
        return new RouteSnapshot(id, name, new ArrayList<>(stationIds), trainCount);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id",         id);
        tag.setString("name",       name);
        tag.setInteger("trainCount", trainCount);

        NBTTagList sl = new NBTTagList();
        for (String sid : stationIds) sl.appendTag(new NBTTagString(sid));
        tag.setTag("stations", sl);
        return tag;
    }

    public static Route fromNBT(NBTTagCompound tag) {
        Route r = new Route(tag.getString("id"), tag.getString("name"));
        r.trainCount = tag.hasKey("trainCount") ? tag.getInteger("trainCount") : 0;
        NBTTagList sl = tag.getTagList("stations", 8);
        for (int i = 0; i < sl.tagCount(); i++) r.stationIds.add(sl.getStringTagAt(i));
        return r;
    }
}