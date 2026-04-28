package jp.apple.arad.data;

public final class PlayerSnapshot {

    public final float  x;
    public final float  z;
    public final float  yaw;
    public final String name;

    public PlayerSnapshot(float x, float z, float yaw, String name) {
        this.x    = x;
        this.z    = z;
        this.yaw  = yaw;
        this.name = name;
    }
}