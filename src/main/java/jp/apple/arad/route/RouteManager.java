package jp.apple.arad.route;

import jp.apple.arad.data.RouteSnapshot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.*;

public final class RouteManager extends WorldSavedData {

    public static final String DATA_NAME = "arad_routes";

    private final Map<String, Route> routes = new LinkedHashMap<>();

    public RouteManager(String name) { super(name); }

    public static RouteManager get(World world) {
        RouteManager mgr = (RouteManager) world.loadData(RouteManager.class, DATA_NAME);
        if (mgr == null) {
            mgr = new RouteManager(DATA_NAME);
            world.setData(DATA_NAME, mgr);
        }
        return mgr;
    }

    public String createRoute(String name) {
        String id = UUID.randomUUID().toString();
        routes.put(id, new Route(id, name));
        markDirty();
        return id;
    }

    public void deleteRoute(String routeId) {
        routes.remove(routeId);
        markDirty();
    }

    public void addStation(String routeId, String stationId) {
        Route r = routes.get(routeId);
        if (r == null) return;
        if (!r.stationIds.contains(stationId)) r.stationIds.add(stationId);
        markDirty();
    }

    public void removeStation(String routeId, String stationId) {
        Route r = routes.get(routeId);
        if (r == null) return;
        r.stationIds.remove(stationId);
        markDirty();
    }

    public void setTrainCount(String routeId, int count) {
        Route r = routes.get(routeId);
        if (r == null) return;
        r.trainCount = Math.max(0, count);
        markDirty();
    }

    public Route getRoute(String routeId) { return routes.get(routeId); }

    public Collection<Route> getAll() {
        return Collections.unmodifiableCollection(routes.values());
    }

    public List<RouteSnapshot> toSnapshots() {
        List<RouteSnapshot> list = new ArrayList<>(routes.size());
        for (Route r : routes.values()) list.add(r.toSnapshot());
        return list;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        routes.clear();
        NBTTagList rl = nbt.getTagList("routes", 10);
        for (int i = 0; i < rl.tagCount(); i++) {
            Route r = Route.fromNBT(rl.getCompoundTagAt(i));
            routes.put(r.id, r);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList rl = new NBTTagList();
        for (Route r : routes.values()) rl.appendTag(r.toNBT());
        nbt.setTag("routes", rl);
        return nbt;
    }
}