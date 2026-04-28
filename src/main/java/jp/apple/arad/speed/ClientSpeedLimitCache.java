package jp.apple.arad.speed;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

@SideOnly(Side.CLIENT)
public final class ClientSpeedLimitCache {

    public static final ClientSpeedLimitCache INSTANCE = new ClientSpeedLimitCache();

    private final Map<String, Integer> cache = new HashMap<>();

    private ClientSpeedLimitCache() {}

    public void onDataReceived(List<RailSpeedEntry> entries) {
        cache.clear();
        for (RailSpeedEntry e : entries) {
            if (e.speedLimitKmh > 0) cache.put(e.key, e.speedLimitKmh);
        }
    }

    public int getSpeedLimit(String key) {
        Integer v = cache.get(key);
        return v != null ? v : 0;
    }

    public Collection<Map.Entry<String, Integer>> getAllEntries() {
        return Collections.unmodifiableSet(cache.entrySet());
    }

    public void clear() {
        cache.clear();
    }
}