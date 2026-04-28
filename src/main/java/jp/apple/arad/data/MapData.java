package jp.apple.arad.data;

import jp.apple.arad.cache.CachedRail;
import jp.apple.arad.cache.RailCacheManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class MapData {

    public static final MapData INSTANCE = new MapData();

    private final List<FormationSnapshot> formations = new ArrayList<>();
    private final List<PlayerSnapshot>    players    = new ArrayList<>();
    private final List<StationSnapshot> stations = new ArrayList<>();
    private final List<RouteSnapshot>   routes   = new ArrayList<>();

    private MapData() {}

    public void onWorldJoin() {
        RailCacheManager.INSTANCE.onWorldJoin(resolveServerId());
        formations.clear();
        players.clear();
        stations.clear();
        routes.clear();
    }

    public void onWorldLeave() {
        RailCacheManager.INSTANCE.onWorldLeave();
        formations.clear();
        players.clear();
        stations.clear();
        routes.clear();

    }

    public void onRailDataReceived(String chunkKey, List<CachedRail> segments) {
        RailCacheManager.INSTANCE.updateChunk(chunkKey, segments);
    }

    public void onFormationDataReceived(List<FormationSnapshot> snapshots) {
        formations.clear();
        formations.addAll(snapshots);
    }

    public void onPlayerDataReceived(List<PlayerSnapshot> snapshots) {
        players.clear();
        players.addAll(snapshots);
    }

    public void onStationDataReceived(List<StationSnapshot> s) {
        stations.clear(); stations.addAll(s);
    }
    public void onRouteDataReceived(List<RouteSnapshot> r) {
        routes.clear(); routes.addAll(r);
    }
    public List<StationSnapshot> getStations() {
        return Collections.unmodifiableList(stations);
    }
    public List<RouteSnapshot> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public List<CachedRail> getRails() {
        return RailCacheManager.INSTANCE.getAllSegments();
    }

    public List<FormationSnapshot> getFormations() {
        return Collections.unmodifiableList(formations);
    }

    public List<PlayerSnapshot> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int cachedChunkCount() {
        return RailCacheManager.INSTANCE.cachedChunkCount();
    }

    public static String resolveServerId() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.isSingleplayer() && mc.getIntegratedServer() != null) {
                return "sp_" + mc.getIntegratedServer().getFolderName();
            }
            if (mc.getCurrentServerData() != null) {
                String ip = mc.getCurrentServerData().serverIP
                        .replace(":", "_")
                        .replace(".", "_");
                return "mp_" + ip;
            }
        } catch (Exception ignored) {}
        return "default";
    }
}