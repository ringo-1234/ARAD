package jp.apple.arad.controller;

import jp.apple.arad.route.Route;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.spawn.FormationSpawner;
import jp.apple.arad.data.StationSnapshot;
import jp.apple.arad.station.StationRegistry;
import jp.apple.arad.station.TileEntityStation;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.FormationManager;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public final class AutoDriveManager {

    public static final AutoDriveManager INSTANCE = new AutoDriveManager();

    private static final double MIN_SAFE_SPAWN_DIST_FALLBACK = 80.0;
    private static final double BLOCKS_PER_CAR = 25.0;
    private static final double SPAWN_MARGIN = 20.0;
    private static final double SPAWN_CLEAR_RADIUS  = 30.0;
    private static final int SCHEDULE_CHECK_INTERVAL_TICKS = 10;

    private final Map<Long, AutoDriveController> controllers = new HashMap<>();
    private final Map<String, RouteSchedule> schedules = new HashMap<>();
    private int scheduleCheckCooldown = 0;

    private AutoDriveManager() {}

    public void tick(World world) {
        if (world.isRemote) return;

        Iterator<Map.Entry<Long, AutoDriveController>> it = controllers.entrySet().iterator();
        while (it.hasNext()) {
            AutoDriveController ctrl = it.next().getValue();
            boolean alive = ctrl.tick(world);
            if (!alive) {
                long deadFormationId = ctrl.getFormationId();
                long deadPredecessorId = ctrl.getPredecessorFormationId();
                it.remove();
                onFormationDied(ctrl.getRouteId(), deadFormationId, deadPredecessorId);
            }
        }

        if (scheduleCheckCooldown > 0) {
            scheduleCheckCooldown--;
            return;
        }
        scheduleCheckCooldown = SCHEDULE_CHECK_INTERVAL_TICKS - 1;

        for (Map.Entry<String, RouteSchedule> entry : schedules.entrySet()) {
            tickSchedule(world, entry.getKey(), entry.getValue());
        }
    }

    private void tickSchedule(World world, String routeId, RouteSchedule sched) {
        int needed = sched.totalTrainCount - sched.activeCount;
        if (needed <= 0) return;

        Route route = RouteManager.get(world).getRoute(routeId);
        if (route == null || route.stationIds.size() < 2) return;

        TileEntityStation firstStation = resolveStation(world, route.stationIds.get(0));
        if (firstStation == null) return;

        double sx = firstStation.getPos().getX() + 0.5;
        double sz = firstStation.getPos().getZ() + 0.5;

        if (sched.activeCount == 0) {

            if (!isAnyFormationWithin(sx, sz, SPAWN_CLEAR_RADIUS)) {
                spawnFormation(world, routeId, route, sched, sx, sz);
            }
            return;
        }

        if (!isLastSpawnedFarEnough(sched, sx, sz)) return;

        if (isAnyFormationWithin(sx, sz, SPAWN_CLEAR_RADIUS)) return;

        spawnFormation(world, routeId, route, sched, sx, sz);
    }

    private boolean isLastSpawnedFarEnough(RouteSchedule sched, double sx, double sz) {
        if (sched.lastSpawnedFormationId == 0L) return true;

        Formation f = FormationManager.getInstance().getFormation(sched.lastSpawnedFormationId);
        if (f == null) return true;

        for (FormationEntry e : f.entries) {
            if (e == null || e.train == null || e.train.isDead) continue;
            double dx   = e.train.posX - sx;
            double dz   = e.train.posZ - sz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < sched.safeSpawnDist) return false;

            break;
        }
        return true;
    }

    private static boolean isAnyFormationWithin(double sx, double sz, double maxDist) {
        double maxDistSq = maxDist * maxDist;
        for (Formation f : FormationManager.getInstance().getFormations().values()) {
            for (FormationEntry e : f.entries) {
                if (e == null || e.train == null || e.train.isDead) continue;
                double dx = e.train.posX - sx;
                double dz = e.train.posZ - sz;
                if (dx * dx + dz * dz < maxDistSq) return true;
            }
        }
        return false;
    }

    private void spawnFormation(World world, String routeId, Route route,
                                RouteSchedule sched, double sx, double sz) {
        TileEntityStation firstStation  = resolveStation(world, route.stationIds.get(0));
        TileEntityStation secondStation = resolveStation(world, route.stationIds.get(1));
        if (firstStation == null) return;

        ItemStack item = firstStation.getFormationItem();
        if (item.isEmpty()) return;

        long formationId = FormationSpawner.spawnAt(world, firstStation, secondStation, item);
        if (formationId == 0L) return;

        long predecessorFormationId = sched.lastSpawnedFormationId;
        int lineFormationIndex = sched.nextLineFormationIndex++;

        double safeSpawnDist = MIN_SAFE_SPAWN_DIST_FALLBACK;
        Formation f = FormationManager.getInstance().getFormation(formationId);
        if (f != null) {
            int carCount = 0;
            for (FormationEntry e : f.entries) {
                if (e != null && e.train != null && !e.train.isDead) carCount++;
            }
            if (carCount > 0) {
                safeSpawnDist = carCount * BLOCKS_PER_CAR + SPAWN_MARGIN;
            }
        }

        AutoDriveController ctrl = new AutoDriveController(
                formationId, routeId, lineFormationIndex, predecessorFormationId);
        controllers.put(formationId, ctrl);
        sched.activeCount++;
        sched.lastSpawnedFormationId = formationId;
        sched.safeSpawnDist          = safeSpawnDist;

        jp.apple.arad.AradCore.LOGGER.info(
                "[Arad] 編成スポーン: route={} lineId={}/{} formationId={} predecessor={} active={}/{} safeSpawnDist={}",
                routeId, routeId, lineFormationIndex, formationId, predecessorFormationId,
                sched.activeCount, sched.totalTrainCount, safeSpawnDist);
    }

    private void onFormationDied(String routeId, long deadFormationId, long deadPredecessorId) {
        RouteSchedule sched = schedules.get(routeId);
        if (sched == null) return;
        sched.activeCount = Math.max(0, sched.activeCount - 1);

        for (AutoDriveController ctrl : controllers.values()) {
            if (!routeId.equals(ctrl.getRouteId())) continue;
            if (ctrl.getPredecessorFormationId() == deadFormationId) {
                ctrl.setPredecessorFormationId(deadPredecessorId);
            }
        }

        if (sched.lastSpawnedFormationId == deadFormationId) {
            sched.lastSpawnedFormationId = deadPredecessorId;
        }
    }

    private TileEntityStation resolveStation(World world, String stationId) {
        if (stationId == null || stationId.isEmpty()) return null;
        TileEntityStation loaded = StationRegistry.INSTANCE.get(stationId);
        if (loaded != null && !loaded.isInvalid()) return loaded;

        StationSnapshot snap = StationRegistry.INSTANCE.getSnapshot(stationId);
        if (snap == null || snap.dim != world.provider.getDimension()) return null;

        int cx = ((int) Math.floor(snap.x)) >> 4;
        int cz = ((int) Math.floor(snap.z)) >> 4;
        Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
        if (chunk == null) return null;

        for (TileEntity te : chunk.getTileEntityMap().values()) {
            if (!(te instanceof TileEntityStation)) continue;
            TileEntityStation station = (TileEntityStation) te;
            if (!stationId.equals(station.getStationId())) continue;
            StationRegistry.INSTANCE.register(station);
            return station;
        }
        return null;
    }

    public void startSchedule(World world, String routeId, int trainCount) {
        stopRoute(world, routeId);
        if (trainCount <= 0) return;

        Route route = RouteManager.get(world).getRoute(routeId);
        if (route == null || route.stationIds.size() < 2) return;

        schedules.put(routeId, new RouteSchedule(trainCount));
        scheduleCheckCooldown = 0;
    }

    public void stopRoute(World world, String routeId) {
        schedules.remove(routeId);

        Iterator<Map.Entry<Long, AutoDriveController>> it = controllers.entrySet().iterator();
        while (it.hasNext()) {
            AutoDriveController ctrl = it.next().getValue();
            if (routeId.equals(ctrl.getRouteId())) {
                Formation f = FormationManager.getInstance().getFormation(ctrl.getFormationId());
                if (f != null) killFormation(f);
                it.remove();
            }
        }
    }

    public void restoreFromRouteManager(World world) {
        if (world.isRemote) return;
        controllers.clear();
        schedules.clear();
        scheduleCheckCooldown = 0;

        RouteManager rm = RouteManager.get(world);
        for (Route r : rm.getAll()) {
            if (r.trainCount > 0) {
                startSchedule(world, r.id, r.trainCount);
            }
        }
    }

    public boolean isActive(long formationId) {
        return controllers.containsKey(formationId);
    }

    public AutoDriveController getController(long formationId) {
        return controllers.get(formationId);
    }

    private static void killFormation(Formation formation) {
        for (FormationEntry e : formation.entries) {
            if (e != null && e.train != null && !e.train.isDead) e.train.setDead();
        }
    }

    private static final class RouteSchedule {
        final int totalTrainCount;
        int activeCount;

        long lastSpawnedFormationId = 0L;
        int nextLineFormationIndex = 1;

        double safeSpawnDist = MIN_SAFE_SPAWN_DIST_FALLBACK;

        RouteSchedule(int total) {
            this.totalTrainCount = total;
            this.activeCount     = 0;
        }
    }
}
