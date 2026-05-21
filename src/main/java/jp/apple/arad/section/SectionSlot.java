package jp.apple.arad.section;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public final class SectionSlot {

    public static final double CAPTURE_RADIUS = 2.0;

    public static final int MAP_SIZE = 8;

    public static final int SPEED_FREE = -1;

    public String name;

    public BlockPos signalPos;

    public int passX;
    public int passZ;

    public int[] speedMap;

    public SectionSlot() {
        this.name = "";
        this.signalPos = BlockPos.ORIGIN;
        this.passX = 0;
        this.passZ = 0;
        this.speedMap = defaultSpeedMap();
    }

    public SectionSlot(BlockPos signalPos, int captureRadius) {
        this.name = "";
        this.signalPos = signalPos;
        this.passX = signalPos.getX();
        this.passZ = signalPos.getZ();
        this.speedMap = defaultSpeedMap();
    }

    public SectionSlot(String name, BlockPos signalPos, int passX, int passZ) {
        this.name = name == null ? "" : name;
        this.signalPos = signalPos;
        this.passX = passX;
        this.passZ = passZ;
        this.speedMap = defaultSpeedMap();
    }

    public SectionSlot(String name, BlockPos signalPos, int passX, int passZ, int[] speedMap) {
        this.name = name == null ? "" : name;
        this.signalPos = signalPos;
        this.passX = passX;
        this.passZ = passZ;
        this.speedMap = clampSpeedMap(speedMap);
    }

    public static int[] defaultSpeedMap() {
        int[] map = new int[MAP_SIZE];
        map[0] = SPEED_FREE;
        map[1] = 0;
        map[2] = 25;
        map[3] = 45;
        map[4] = 75;
        map[5] = SPEED_FREE;
        map[6] = SPEED_FREE;
        map[7] = SPEED_FREE;
        return map;
    }

    private static int[] clampSpeedMap(int[] src) {
        int[] map = defaultSpeedMap();
        if (src == null)
            return map;
        for (int i = 0; i < MAP_SIZE && i < src.length; i++) {
            map[i] = (src[i] < 0) ? SPEED_FREE : Math.min(src[i], 9999);
        }
        return map;
    }

    public static SectionSlot fromNBT(NBTTagCompound tag) {
        SectionSlot s = new SectionSlot();
        if (tag.hasKey("name"))
            s.name = tag.getString("name");
        if (tag.hasKey("sigPos"))
            s.signalPos = BlockPos.fromLong(tag.getLong("sigPos"));

        if (tag.hasKey("passX")) {
            s.passX = tag.getInteger("passX");
        } else if (s.signalPos != null) {
            s.passX = s.signalPos.getX();
        }
        if (tag.hasKey("passZ")) {
            s.passZ = tag.getInteger("passZ");
        } else if (s.signalPos != null) {
            s.passZ = s.signalPos.getZ();
        }

        if (tag.hasKey("speedMap")) {
            s.speedMap = clampSpeedMap(tag.getIntArray("speedMap"));
        } else {
            s.speedMap = defaultSpeedMap();
        }

        return s;
    }

    public boolean isCaptured(double trainX, double trainZ) {
        double dx = trainX - (passX + 0.5);
        double dz = trainZ - (passZ + 0.5);
        return (dx * dx + dz * dz) <= (CAPTURE_RADIUS * CAPTURE_RADIUS);
    }

    public int getSpeedKmh(int signalLevel) {
        if (signalLevel < 0 || signalLevel >= MAP_SIZE)
            return SPEED_FREE;
        return speedMap[signalLevel];
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", name == null ? "" : name);
        tag.setLong("sigPos", signalPos.toLong());
        tag.setInteger("passX", passX);
        tag.setInteger("passZ", passZ);
        tag.setIntArray("speedMap", speedMap);
        return tag;
    }

    public SectionSlot copy() {
        int[] mapCopy = new int[MAP_SIZE];
        System.arraycopy(speedMap, 0, mapCopy, 0, MAP_SIZE);
        return new SectionSlot(name, signalPos, passX, passZ, mapCopy);
    }
}