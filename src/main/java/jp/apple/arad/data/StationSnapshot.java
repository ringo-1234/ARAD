package jp.apple.arad.data;

public final class StationSnapshot {

    public final String id;
    public final String name;
    public final float  x;
    public final float  z;
    public final int    dim;
    public final boolean doorLeft;
    public final boolean doorRight;
    public final boolean spawnReversed;
    public final int dwellTicks;

    public StationSnapshot(String id, String name, float x, float z, int dim) {
        this(id, name, x, z, dim, true, true, false, 400);
    }

    public StationSnapshot(String id, String name, float x, float z, int dim,
                           boolean doorLeft, boolean doorRight, boolean spawnReversed, int dwellTicks) {
        this.id   = id;
        this.name = name;
        this.x    = x;
        this.z    = z;
        this.dim  = dim;
        this.doorLeft = doorLeft;
        this.doorRight = doorRight;
        this.spawnReversed = spawnReversed;
        this.dwellTicks = Math.max(20, dwellTicks);
    }

    public byte getDoorData() {
        if (doorLeft && doorRight) return 3;
        if (doorRight) return 1;
        if (doorLeft) return 2;
        return 0;
    }
}
