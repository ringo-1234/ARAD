package jp.apple.arad.speed;

public final class RailSpeedEntry {

    public final String key;

    public int speedLimitKmh;

    public RailSpeedEntry(String key, int speedLimitKmh) {
        this.key       = key;
        this.speedLimitKmh = speedLimitKmh;
    }

    public float toRTMSpeed() {
        return speedLimitKmh / 72.0f;
    }

    public static String makeKey(int dim, int x, int y, int z) {
        return dim + ":" + x + ":" + y + ":" + z;
    }
}