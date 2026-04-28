package jp.apple.arad.cache;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;

public final class CachedRail {

    public final float[] xPoints;
    public final float[] zPoints;
    public final int coreX, coreY, coreZ;

    public CachedRail(float[] x, float[] z, int coreX, int coreY, int coreZ) {
        this.xPoints = x;
        this.zPoints = z;
        this.coreX   = coreX;
        this.coreY   = coreY;
        this.coreZ   = coreZ;
    }

    public int size() {
        return xPoints.length;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("x",  toNBTList(xPoints));
        tag.setTag("z",  toNBTList(zPoints));
        tag.setInteger("cx", coreX);
        tag.setInteger("cy", coreY);
        tag.setInteger("cz", coreZ);
        return tag;
    }

    public static CachedRail fromNBT(NBTTagCompound tag) {
        float[] x  = fromNBTList(tag.getTagList("x", 5));
        float[] z  = fromNBTList(tag.getTagList("z", 5));
        int cx = tag.hasKey("cx") ? tag.getInteger("cx") : 0;
        int cy = tag.hasKey("cy") ? tag.getInteger("cy") : 0;
        int cz = tag.hasKey("cz") ? tag.getInteger("cz") : 0;
        return new CachedRail(x, z, cx, cy, cz);
    }

    private static NBTTagList toNBTList(float[] arr) {
        NBTTagList list = new NBTTagList();
        for (float v : arr) list.appendTag(new NBTTagFloat(v));
        return list;
    }

    private static float[] fromNBTList(NBTTagList list) {
        float[] arr = new float[list.tagCount()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.getFloatAt(i);
        return arr;
    }
}