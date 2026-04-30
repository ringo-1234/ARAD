package jp.apple.arad.controller;

import jp.apple.arad.data.StationSnapshot;
import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import jp.apple.arad.route.Route;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.station.StationRegistry;
import jp.apple.arad.station.TileEntityStation;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.FormationManager;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class AutoDriveController {
    private static final int DOOR_CLOSE_TICKS = 100;
    private static final int STOP_TO_DOOR_OPEN_TICKS = 40;
    private static final int DEFAULT_DWELL_TICKS = 400;
    private static final int LAUNCH_GRACE_TICKS = 100;
    private static final int CONTROL_INTERVAL_TICKS = 1;
    private static final int NOTCH_MAX = 5;
    private static final int NOTCH_FULL_BRAKE = -6;
    private static final int NOTCH_EB = -7;
    private static final int ACCEL_INTERVAL = 5;
    private static final int BRAKE_INTERVAL = 2;
    private static final float KMH_TO_RTM = 1.0f / 72.0f;
    private static final int LIMIT_HYSTERESIS_KMH = 7;
    private static final int LIMIT_OVER_BRAKE_KMH = 2;
    private static final int LIMIT_OVER_FULL_BRAKE_KMH = 8;
    private static final float LIMIT_OVER_COAST_TOLERANCE_KMH = 0.7f;
    private static final float LIMIT_REACH_TOLERANCE_KMH = 1.2f;
    private static final double LIMIT_BRAKE_MARGIN = 4.0;
    private static final int LIMIT_BLOCK_DETECT_RADIUS_XZ = 1;
    private static final int LIMIT_BLOCK_DETECT_DEPTH = 4;
    private static final double COL_EB_DIST = 30.0;
    private static final double COL_MARGIN = 50.0;
    private static final double COL_WARN_DIST = 240.0;
    private static final double CAR_LENGTH_BLOCKS = 20.0;
    private static final double AHEAD_DETECT_MARGIN = 40.0;
    private static final double AHEAD_DETECT_MAX = 1200.0;
    private static final int PREDECESSOR_REF_REFRESH_TICKS = 20;
    private static final int AHEAD_DIST_REFRESH_TICKS = 2;
    private static final double AHEAD_CENTER_OFFSET = 6.0;
    private static final int MISSING_FORMATION_GRACE_TICKS = 40;
    private static final int MISSING_LEAD_GRACE_TICKS = 20;
    private static final int AHEAD_PATH_MAX_POINTS = 256;
    private static final int AHEAD_PATH_MAX_DEPTH = 24;
    private static final int AHEAD_NEXT_SEARCH = 2;
    private static final double TANGENT_MIN_NORM = 1.0e-4;
    private static final double NEXT_MAP_MIN_DOT = -0.1;
    private static final int ARRIVE_CONFIRM_TICKS = 5;
    private static final double ARRIVE_DIST_IN = 2.0;
    private static final double ARRIVE_DIST_OUT = 3.2;
    private static final float ARRIVE_SPEED_IN = 0.03f;
    private static final float ARRIVE_SPEED_OUT = 0.05f;
    private static final double STATION_CAPTURE_DIST = ARRIVE_DIST_OUT + 1.2;
    private static final double STOP_BUFFER = 0.35;
    private static final double BRAKE_HYSTERESIS = 6.0;
    private static final double PHASE_MARGIN = 1.15;
    private static final int PHASE_LEVEL = 5;
    private static final float LAUNCH_FORCE_SPEED = 0.04f;
    private static final int LAUNCH_FORCE_NOTCH = 3;
    private final long formationId;
    private final String routeId;
    private final int lineFormationIndex;
    private long predecessorFormationId;
    private int currentStationIdx = 0;
    private int dwellTimer = DEFAULT_DWELL_TICKS;
    private boolean dwellInitialized = false;
    private int targetNotch = 0;
    private int appliedNotch = 0;
    private int notchStepTimer = 0;
    private int arriveConfirmTicks = 0;
    private boolean arrivalLatched = false;
    private int launchGraceTicks = 0;
    private int stuckTicks = 0;
    private int activeBlockLimitKmh = 0;
    private int pendingBlockLimitKmh = 0;
    private double pendingBlockLimitRemain = -1.0;
    private boolean blockLimitCoastMode = false;
    private BlockPos lastTriggeredLimitPos = null;
    private boolean odometerReady = false;
    private double odometerLastX = 0.0;
    private double odometerLastZ = 0.0;
    private float[] decelCache = null;
    private int controlAccumTicks = 0;
    private EntityTrainBase predecessorTailCache = null;
    private int predecessorRefCacheTicks = 0;
    private int aheadDistCacheTicks = 0;
    private double aheadDistCache = -1.0;
    private int lastCommandedNotch = Integer.MIN_VALUE;
    private int missingFormationTicks = 0;
    private int missingLeadTicks = 0;
    private DriveState state = DriveState.EN_ROUTE;

    public AutoDriveController(long formationId, String routeId) {
        this(formationId, routeId, 0, 0L);
    }

    public AutoDriveController(long formationId, String routeId, int lineFormationIndex, long predecessorFormationId) {
        this.formationId = formationId;
        this.routeId = routeId;
        this.lineFormationIndex = lineFormationIndex;
        this.predecessorFormationId = predecessorFormationId;
        this.predecessorRefCacheTicks = (int) (Math.abs(formationId) % PREDECESSOR_REF_REFRESH_TICKS);
        this.aheadDistCacheTicks = (int) (Math.abs(formationId) % (AHEAD_DIST_REFRESH_TICKS + 1));
    }

    private static int calcStopNotch(double dist, float speed, float[] decel) {
        if (dist <= 0.45) return NOTCH_FULL_BRAKE;
        float baseDecel = decel[4];
        double idealSpeed = Math.sqrt(Math.max(0.0, 2.0 * baseDecel * Math.max(dist - 0.25, 0.0)));
        double err = speed - idealSpeed;
        if (err > 0.03) return NOTCH_FULL_BRAKE;
        if (err > 0.01) return -3;
        if (dist > 4.0 && err < -0.05) return 1;
        return 0;
    }

    private static double signedForwardDist(EntityTrainBase lead, double tx, double tz) {
        double yawRad = Math.toRadians(lead.rotationYaw);
        return (tx - lead.posX) * -Math.sin(yawRad) + (tz - lead.posZ) * Math.cos(yawRad);
    }

    private static double distXZ(double x0, double z0, double x1, double z1) {
        return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(z1 - z0, 2));
    }

    private static float[] getDecelTable(EntityTrainBase lead) {
        try {
            float[] raw = lead.getResourceState().getResourceSet().getConfig().deccelerations;
            float[] res = new float[9];
            for (int i = 0; i < 9; i++) res[i] = i < raw.length ? Math.abs(raw[i]) : 0.002f;
            return res;
        } catch (Exception e) {
            return new float[]{0.0002f, 0.0005f, 0.001f, 0.0015f, 0.002f, 0.0025f, 0.003f, 0.0035f, 0.01f};
        }
    }

    private static int calcCruiseNotch(float current, float target) {
        if (target <= 0.0f) return 0;
        float r = current / target;
        if (r < 0.88f) return 5;
        if (r < 0.94f) return 3;
        if (r < 0.98f) return 1;
        if (r > 1.05f) return -2;
        if (r > 1.02f) return -1;
        return 0;
    }

    private static int calcRailSplit(RailMap rm) {
        int split = (int) (rm.getLength() * 2.0);
        split = Math.max(8, split);
        split = Math.min(128, split);
        return split;
    }

    private static int pickForwardStep(RailMap rm, int split, int idx, double hintX, double hintZ) {
        double bestDot = -Double.MAX_VALUE;
        int bestStep = 0;
        double[] p = rm.getRailPos(split, idx);
        double px = p[1], pz = p[0];
        if (idx < split) {
            double[] pn = rm.getRailPos(split, idx + 1);
            double vx = pn[1] - px;
            double vz = pn[0] - pz;
            double n = Math.sqrt(vx * vx + vz * vz);
            if (n > TANGENT_MIN_NORM) {
                double dot = (vx / n) * hintX + (vz / n) * hintZ;
                if (dot > bestDot) {
                    bestDot = dot;
                    bestStep = 1;
                }
            }
        }
        if (idx > 0) {
            double[] pp = rm.getRailPos(split, idx - 1);
            double vx = pp[1] - px;
            double vz = pp[0] - pz;
            double n = Math.sqrt(vx * vx + vz * vz);
            if (n > TANGENT_MIN_NORM) {
                double dot = (vx / n) * hintX + (vz / n) * hintZ;
                if (dot > bestDot) {
                    bestDot = dot;
                    bestStep = -1;
                }
            }
        }
        return bestStep;
    }

    private static PathProjection projectToAheadPath(List<PathPoint> path, double tx, double tz) {
        if (path == null || path.size() < 2) return null;
        double bestLateral = Double.MAX_VALUE;
        double bestLongitudinal = -1.0;
        for (int i = 0; i < path.size() - 1; i++) {
            PathPoint a = path.get(i);
            PathPoint b = path.get(i + 1);
            double sx = b.x - a.x;
            double sz = b.z - a.z;
            double segLen2 = sx * sx + sz * sz;
            if (segLen2 < TANGENT_MIN_NORM) continue;
            double ux = tx - a.x;
            double uz = tz - a.z;
            double t = (ux * sx + uz * sz) / segLen2;
            if (t < 0.0) t = 0.0;
            else if (t > 1.0) t = 1.0;
            double nx = a.x + sx * t;
            double nz = a.z + sz * t;
            double lateral = distXZ(tx, tz, nx, nz);
            if (lateral < bestLateral) {
                bestLateral = lateral;
                bestLongitudinal = a.s + Math.sqrt(segLen2) * t;
            }
        }
        if (bestLongitudinal < 0.0) return null;
        return new PathProjection(bestLongitudinal, bestLateral);
    }

    private static double calcBrakeDist(float speed, float decelPerTick) {
        if (decelPerTick <= 0.0f || speed <= 0.0f) return 0.0;
        return (double) (speed * speed) / (2.0 * decelPerTick);
    }

    public boolean tick(World world) {
        Formation formation = FormationManager.getInstance().getFormation(formationId);
        if (formation == null) {
            if (++missingFormationTicks < MISSING_FORMATION_GRACE_TICKS) return true;
            return false;
        }
        missingFormationTicks = 0;
        if (++controlAccumTicks < CONTROL_INTERVAL_TICKS) return true;
        int dt = controlAccumTicks;
        controlAccumTicks = 0;

        Route route = RouteManager.get(world).getRoute(routeId);
        if (route == null || currentStationIdx >= route.stationIds.size()) {
            killFormation(formation);
            return false;
        }
        EntityTrainBase lead = getLeadTrain(formation);
        if (lead == null || lead.isDead) {
            if (++missingLeadTicks < MISSING_LEAD_GRACE_TICKS) return true;
            return false;
        }
        missingLeadTicks = 0;

        switch (state) {
            case STOP_WAIT_OPEN:
                tickStopWaitOpen(formation, dt);
                break;
            case DOOR_OPEN:
                tickDoorOpen(formation, lead, route.stationIds, dt);
                break;
            case DOOR_CLOSE_WAIT:
                tickDoorCloseWait(formation, route.stationIds, dt);
                break;
            case EN_ROUTE:
            case BRAKING:
                tickEnRoute(world, formation, lead, route.stationIds, dt);
                break;
            case TERMINAL:
                killFormation(formation);
                return false;
        }
        stepNotch(formation, dt);
        return true;
    }

    private void stepNotch(Formation formation, int dt) {
        if (targetNotch == NOTCH_EB) {
            appliedNotch = NOTCH_EB;
            applyNotchDirect(formation, NOTCH_EB);
            return;
        }
        if (appliedNotch == targetNotch) return;
        if (targetNotch < appliedNotch) {
            boolean urgentBrake = (targetNotch <= -3) || ((appliedNotch - targetNotch) >= 3);
            if (urgentBrake) {
                appliedNotch = targetNotch;
                applyNotchDirect(formation, appliedNotch);
                notchStepTimer = 0;
            } else {
                notchStepTimer += dt;
                while (notchStepTimer >= BRAKE_INTERVAL && appliedNotch > targetNotch) {
                    appliedNotch--;
                    applyNotchDirect(formation, appliedNotch);
                    notchStepTimer -= BRAKE_INTERVAL;
                }
            }
        } else {
            notchStepTimer += dt;
            while (notchStepTimer >= ACCEL_INTERVAL && appliedNotch < targetNotch) {
                appliedNotch++;
                applyNotchDirect(formation, appliedNotch);
                notchStepTimer -= ACCEL_INTERVAL;
            }
        }
    }

    private void tickEnRoute(World world, Formation formation, EntityTrainBase lead, List<String> stationIds, int dt) {
        float speed = Math.abs(lead.getSpeed());
        float[] decel = getDecelTableCached(lead);
        updateLimitBlockState(world, lead, dt);
        double brakePhaseStart = calcBrakeDist(speed, decel[PHASE_LEVEL]) * PHASE_MARGIN;
        String targetStationId = (currentStationIdx >= 0 && currentStationIdx < stationIds.size())
                ? stationIds.get(currentStationIdx) : null;
        StationSnapshot targetSnap = (targetStationId != null) ? StationRegistry.INSTANCE.getSnapshot(targetStationId) : null;
        if (targetSnap != null && targetSnap.dim != world.provider.getDimension()) {
            targetSnap = null;
        }
        boolean inLaunchGrace = launchGraceTicks > 0;
        if (inLaunchGrace) launchGraceTicks = Math.max(0, launchGraceTicks - dt);
        if (speed < 0.005f) stuckTicks += dt;
        else stuckTicks = 0;
        double aheadDist = getAheadFormationDist(lead, brakePhaseStart);
        double distToTarget = Double.MAX_VALUE;
        double forwardDistSt = Double.MAX_VALUE;
        if (!inLaunchGrace && targetSnap != null) {
            double tx = targetSnap.x;
            double tz = targetSnap.z;
            distToTarget = distXZ(lead.posX, lead.posZ, tx, tz);
            forwardDistSt = distToTarget;
            boolean isStoppedAtStation = (speed < 0.01f && distToTarget < 3.0);
            boolean inArrivalBand = (forwardDistSt >= -0.5 && forwardDistSt <= ARRIVE_DIST_IN && speed <= ARRIVE_SPEED_IN);
            boolean inArrivalOuterBand = (forwardDistSt >= -0.5 && forwardDistSt <= ARRIVE_DIST_OUT && speed <= ARRIVE_SPEED_OUT);
            if (inArrivalOuterBand) arrivalLatched = true;
            if (inArrivalBand) arriveConfirmTicks += dt;
            else arriveConfirmTicks = 0;
            if (arrivalLatched || (inArrivalBand && arriveConfirmTicks >= ARRIVE_CONFIRM_TICKS) || isStoppedAtStation) {
                holdStationStop(formation);
                arrivalLatched = false;
                arriveConfirmTicks = 0;
                stuckTicks = 0;
                dwellInitialized = false;
                state = DriveState.STOP_WAIT_OPEN;
                dwellTimer = STOP_TO_DOOR_OPEN_TICKS;
                return;
            }
        }
        double effectiveDist = Double.MAX_VALUE;
        if (aheadDist >= 0) effectiveDist = aheadDist - COL_EB_DIST;
        if (forwardDistSt >= 0) effectiveDist = Math.min(effectiveDist, forwardDistSt - STOP_BUFFER);
        if (aheadDist >= 0 && aheadDist <= COL_EB_DIST) {
            hardStop(formation);
            stuckTicks = 0;
            return;
        }
        if (inLaunchGrace && speed < LAUNCH_FORCE_SPEED && (aheadDist < 0.0 || aheadDist > COL_MARGIN)) {
            state = DriveState.EN_ROUTE;
            targetNotch = LAUNCH_FORCE_NOTCH;
            if (appliedNotch < LAUNCH_FORCE_NOTCH) {
                appliedNotch = LAUNCH_FORCE_NOTCH;
                applyNotchDirect(formation, LAUNCH_FORCE_NOTCH);
            }
            notchStepTimer = 0;
            return;
        }
        if (stuckTicks >= 80 && (aheadDist < 0.0 || aheadDist > COL_MARGIN)) {
            state = DriveState.EN_ROUTE;
            targetNotch = NOTCH_MAX;
            appliedNotch = NOTCH_MAX;
            applyNotchDirect(formation, NOTCH_MAX);
            notchStepTimer = 0;
            stuckTicks = 0;
            return;
        }
        boolean shouldBrake = (state == DriveState.BRAKING)
                ? (effectiveDist < brakePhaseStart + BRAKE_HYSTERESIS)
                : (effectiveDist < brakePhaseStart);
        if (!shouldBrake
                && targetSnap != null
                && forwardDistSt >= 0.0
                && forwardDistSt <= STATION_CAPTURE_DIST
                && speed > ARRIVE_SPEED_IN) {
            shouldBrake = true;
        }
        if (shouldBrake) {
            state = DriveState.BRAKING;
            int stopNotch = calcStopNotch(effectiveDist, speed, decel);
            if (targetSnap != null && forwardDistSt >= 0.0 && forwardDistSt <= STATION_CAPTURE_DIST) {
                if (speed > ARRIVE_SPEED_OUT) {
                    stopNotch = Math.min(stopNotch, -2);
                } else if (speed > ARRIVE_SPEED_IN) {
                    stopNotch = Math.min(stopNotch, -1);
                }
            }
            setTarget(stopNotch);
            return;
        }

        if (applyPendingLimitControl(speed, decel)) return;
        if (applyActiveLimitControl(speed)) return;

        state = DriveState.EN_ROUTE;
        setTarget(NOTCH_MAX);
    }

    private void tickStopWaitOpen(Formation formation, int dt) {
        holdStationStop(formation);
        stuckTicks = 0;
        dwellTimer -= dt;
        if (dwellTimer <= 0) {
            state = DriveState.DOOR_OPEN;
            dwellInitialized = false;
        }
    }

    private void tickDoorOpen(Formation formation, EntityTrainBase lead, List<String> stationIds, int dt) {
        TileEntityStation cs = getStation(stationIds, currentStationIdx);
        StationSnapshot ss = getStationSnapshot(stationIds, currentStationIdx);
        if (!dwellInitialized) {
            int dwell = DEFAULT_DWELL_TICKS;
            byte doorData = 3;
            if (cs != null) {
                dwell = cs.getDwellTimeTicks();
                doorData = cs.getDoorData();
            } else if (ss != null) {
                dwell = ss.dwellTicks;
                doorData = ss.getDoorData();
            }
            dwellTimer = dwell;
            dwellInitialized = true;
            applyDoorState(formation, doorData);
            arrivalLatched = false;
            arriveConfirmTicks = 0;
        }
        holdStationStop(formation);
        stuckTicks = 0;
        dwellTimer -= dt;
        if (dwellTimer <= 0) {
            byte doorData = (cs != null) ? cs.getDoorData() : (ss != null ? ss.getDoorData() : (byte) 3);
            if (doorData == 0) {
                departAfterStop(formation);
            } else {
                applyDoorState(formation, (byte) 0);
                state = DriveState.DOOR_CLOSE_WAIT;
                dwellTimer = DOOR_CLOSE_TICKS;
                dwellInitialized = false;
            }
        }
    }

    private void tickDoorCloseWait(Formation formation, List<String> stationIds, int dt) {
        holdStationStop(formation);
        stuckTicks = 0;
        dwellTimer -= dt;
        if (dwellTimer <= 0) {
            departAfterStop(formation);
        }
    }

    private void departAfterStop(Formation formation) {
        currentStationIdx++;
        arrivalLatched = false;
        arriveConfirmTicks = 0;
        launchGraceTicks = LAUNCH_GRACE_TICKS;
        controlAccumTicks = 0;
        odometerReady = false;
        state = DriveState.EN_ROUTE;
        EntityTrainBase lead = getLeadTrain(formation);
        if (lead != null && !lead.isDead) applyRoleFront(lead);
        applyNotchDirect(formation, 0);
        targetNotch = 1;
        appliedNotch = 0;
        notchStepTimer = 0;
        dwellInitialized = false;
    }

    private void applyNotchDirect(Formation formation, int notch) {
        if (lastCommandedNotch == notch) return;
        lastCommandedNotch = notch;
        for (FormationEntry e : formation.entries) {
            if (e.train != null) e.train.setNotch(notch);
        }
    }

    private void applyRoleFront(EntityTrainBase lead) {
        lead.setVehicleState(TrainState.TrainStateType.Role, TrainState.Role_Front.data);
    }

    private void applyDoorState(Formation formation, byte doorData) {
        for (FormationEntry e : formation.entries) {
            if (e != null && e.train != null && !e.train.isDead) {
                e.train.setVehicleState(TrainState.TrainStateType.Door, doorData);
            }
        }
    }

    private void setTarget(int notch) {
        targetNotch = Math.max(NOTCH_EB, Math.min(NOTCH_MAX, notch));
    }

    private void hardStop(Formation f) {
        targetNotch = NOTCH_EB;
        appliedNotch = NOTCH_EB;
        applyNotchDirect(f, NOTCH_EB);
        notchStepTimer = 0;
    }

    private void holdStationStop(Formation f) {
        targetNotch = NOTCH_FULL_BRAKE;
        appliedNotch = NOTCH_FULL_BRAKE;
        applyNotchDirect(f, NOTCH_FULL_BRAKE);
        notchStepTimer = 0;
    }

    private void updateLimitBlockState(World world, EntityTrainBase lead, int dt) {
        if (lead == null || lead.isDead || world == null) return;

        double moved = 0.0;
        if (odometerReady) {
            moved = distXZ(odometerLastX, odometerLastZ, lead.posX, lead.posZ);
            if (moved > 20.0) moved = 0.0;
        }
        odometerLastX = lead.posX;
        odometerLastZ = lead.posZ;
        odometerReady = true;

        if (pendingBlockLimitKmh > 0 && pendingBlockLimitRemain > 0.0 && moved > 0.0) {
            pendingBlockLimitRemain -= moved;
        }

        TileEntitySpeedLimitSign sign = findLimitBlockUnderTrain(world, lead);
        if (sign == null) {
            lastTriggeredLimitPos = null;
            return;
        }

        BlockPos signPos = sign.getPos();
        if (signPos.equals(lastTriggeredLimitPos)) return;

        lastTriggeredLimitPos = signPos;
        pendingBlockLimitKmh = sign.getSpeedLimitKmh();
        pendingBlockLimitRemain = Math.max(0, sign.getStartOffsetBlocks());
        blockLimitCoastMode = false;
    }

    private TileEntitySpeedLimitSign findLimitBlockUnderTrain(World world, EntityTrainBase lead) {
        int baseX = (int) Math.floor(lead.posX);
        int baseY = (int) Math.floor(lead.posY);
        int baseZ = (int) Math.floor(lead.posZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = 0; dy <= LIMIT_BLOCK_DETECT_DEPTH; dy++) {
            int y = baseY - dy;
            for (int dx = -LIMIT_BLOCK_DETECT_RADIUS_XZ; dx <= LIMIT_BLOCK_DETECT_RADIUS_XZ; dx++) {
                for (int dz = -LIMIT_BLOCK_DETECT_RADIUS_XZ; dz <= LIMIT_BLOCK_DETECT_RADIUS_XZ; dz++) {
                    pos.setPos(baseX + dx, y, baseZ + dz);
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof TileEntitySpeedLimitSign) {
                        return (TileEntitySpeedLimitSign) te;
                    }
                }
            }
        }
        return null;
    }

    private boolean applyPendingLimitControl(float speed, float[] decel) {
        if (pendingBlockLimitKmh <= 0) return false;

        float pendingLimitSpeed = pendingBlockLimitKmh * KMH_TO_RTM;
        if (pendingBlockLimitRemain <= 0.0) {
            activatePendingLimit();
            if (speed > pendingLimitSpeed) {
                state = DriveState.BRAKING;
                setTarget(NOTCH_FULL_BRAKE);
                return true;
            }
            return false;
        }

        if (speed <= pendingLimitSpeed) return false;

        double needBrakeDist = calcSpeedDropDist(speed, pendingLimitSpeed, decel[PHASE_LEVEL]) * PHASE_MARGIN;
        if (pendingBlockLimitRemain > needBrakeDist + LIMIT_BRAKE_MARGIN) return false;

        state = DriveState.BRAKING;
        if (pendingBlockLimitRemain <= 0.8) {
            setTarget(NOTCH_FULL_BRAKE);
            return true;
        }

        double ratio = pendingBlockLimitRemain / Math.max(needBrakeDist, 0.001);
        if (ratio < 0.70) {
            setTarget(NOTCH_FULL_BRAKE);
        } else if (ratio < 0.90) {
            setTarget(-4);
        } else if (ratio < 1.10) {
            setTarget(-3);
        } else {
            setTarget(-2);
        }
        return true;
    }

    private void activatePendingLimit() {
        if (pendingBlockLimitKmh <= 0) return;
        activeBlockLimitKmh = pendingBlockLimitKmh;
        pendingBlockLimitKmh = 0;
        pendingBlockLimitRemain = -1.0;
        blockLimitCoastMode = false;
    }

    private boolean applyActiveLimitControl(float speed) {
        if (activeBlockLimitKmh <= 0) {
            blockLimitCoastMode = false;
            return false;
        }

        float limitSpeed = activeBlockLimitKmh * KMH_TO_RTM;
        float lowBandSpeed = Math.max(0.0f, (activeBlockLimitKmh - LIMIT_HYSTERESIS_KMH) * KMH_TO_RTM);

        state = DriveState.EN_ROUTE;
        if (speed > limitSpeed + LIMIT_OVER_FULL_BRAKE_KMH * KMH_TO_RTM) {
            setTarget(NOTCH_FULL_BRAKE);
            return true;
        }
        if (speed > limitSpeed + LIMIT_OVER_BRAKE_KMH * KMH_TO_RTM) {
            setTarget(-2);
            return true;
        }
        if (speed > limitSpeed + LIMIT_OVER_COAST_TOLERANCE_KMH * KMH_TO_RTM) {
            setTarget(-1);
            return true;
        }

        if (blockLimitCoastMode) {
            if (speed <= lowBandSpeed) {
                blockLimitCoastMode = false;
            } else {
                setTarget(0);
                return true;
            }
        }

        if (speed >= limitSpeed - LIMIT_REACH_TOLERANCE_KMH * KMH_TO_RTM) {
            blockLimitCoastMode = true;
            setTarget(0);
            return true;
        }

        setTarget(NOTCH_MAX);
        return true;
    }

    private static double calcSpeedDropDist(float currentSpeed, float targetSpeed, float decelPerTick) {
        if (decelPerTick <= 0.0f || currentSpeed <= targetSpeed) return 0.0;
        double v2 = (double) currentSpeed * (double) currentSpeed;
        double u2 = (double) targetSpeed * (double) targetSpeed;
        return (v2 - u2) / (2.0 * decelPerTick);
    }

    private float[] getDecelTableCached(EntityTrainBase lead) {
        if (decelCache == null) {
            decelCache = getDecelTable(lead);
        }
        return decelCache;
    }

    private static double calcAheadDetectDist(double brakePhaseStart) {
        return Math.min(
                AHEAD_DETECT_MAX,
                Math.max(COL_WARN_DIST, brakePhaseStart + COL_MARGIN + CAR_LENGTH_BLOCKS + AHEAD_DETECT_MARGIN));
    }

    private double getAheadFormationDist(EntityTrainBase lead, double brakePhaseStart) {
        if (lead == null) return -1.0;
        if (aheadDistCacheTicks > 0) {
            aheadDistCacheTicks--;
            return aheadDistCache;
        }

        double detectDist = calcAheadDetectDist(brakePhaseStart) + COL_MARGIN;
        EntityTrainBase tail = resolvePredecessorTail();
        if (tail == null) {
            aheadDistCache = -1.0;
            aheadDistCacheTicks = AHEAD_DIST_REFRESH_TICKS;
            return aheadDistCache;
        }

        double centerDistSq = distSq2D(lead, tail);
        if (centerDistSq < 1.0 || centerDistSq >= detectDist * detectDist) {
            aheadDistCache = -1.0;
        } else {
            aheadDistCache = Math.sqrt(centerDistSq) - AHEAD_CENTER_OFFSET;
        }
        aheadDistCacheTicks = AHEAD_DIST_REFRESH_TICKS;
        return aheadDistCache;
    }

    private EntityTrainBase resolvePredecessorTail() {
        if (predecessorFormationId <= 0L || predecessorFormationId == formationId) {
            predecessorTailCache = null;
            predecessorRefCacheTicks = 0;
            return null;
        }

        if (predecessorTailCache != null && !predecessorTailCache.isDead && predecessorRefCacheTicks > 0) {
            predecessorRefCacheTicks--;
            return predecessorTailCache;
        }

        if (FormationManager.getInstance() == null) {
            predecessorTailCache = null;
            predecessorRefCacheTicks = 0;
            return null;
        }

        Formation predecessor = FormationManager.getInstance().getFormation(predecessorFormationId);
        if (predecessor == null || predecessor.entries == null || predecessor.entries.length == 0) {
            predecessorTailCache = null;
            predecessorRefCacheTicks = 0;
            return null;
        }

        EntityTrainBase tail = null;
        for (int i = predecessor.entries.length - 1; i >= 0; i--) {
            FormationEntry e = predecessor.entries[i];
            if (e == null || e.train == null || e.train.isDead) continue;
            tail = e.train;
            break;
        }
        predecessorTailCache = tail;
        predecessorRefCacheTicks = PREDECESSOR_REF_REFRESH_TICKS;
        return predecessorTailCache;
    }

    private static double distSq2D(EntityTrainBase a, EntityTrainBase b) {
        double dx = b.posX - a.posX;
        double dz = b.posZ - a.posZ;
        return dx * dx + dz * dz;
    }

    private List<PathPoint> buildAheadPathOnRail(World world, EntityTrainBase lead, StationSnapshot targetSnap) {
        if (world == null || lead == null) return null;
        int minY = (int) lead.posY - 4;
        TileEntityLargeRailBase startRail =
                TileEntityLargeRailBase.getRailFromCoordinates(world, lead.posX, lead.posY + 1.0, lead.posZ, minY);
        if (startRail == null) return null;
        RailMap map = startRail.getRailMap(null);
        if (map == null) return null;
        double hintX;
        double hintZ;
        if (targetSnap != null) {
            double vx = targetSnap.x - lead.posX;
            double vz = targetSnap.z - lead.posZ;
            double n = Math.sqrt(vx * vx + vz * vz);
            if (n > TANGENT_MIN_NORM) {
                hintX = vx / n;
                hintZ = vz / n;
            } else {
                double yawRad = Math.toRadians(lead.rotationYaw);
                hintX = -Math.sin(yawRad);
                hintZ = Math.cos(yawRad);
            }
        } else {
            double yawRad = Math.toRadians(lead.rotationYaw);
            hintX = -Math.sin(yawRad);
            hintZ = Math.cos(yawRad);
        }
        List<PathPoint> points = new ArrayList<>();
        points.add(new PathPoint(lead.posX, lead.posZ, 0.0));
        double total = 0.0;
        double curX = lead.posX;
        double curY = lead.posY;
        double curZ = lead.posZ;
        RailMap prevMap = null;
        RailMap current = map;
        for (int depth = 0; depth < AHEAD_PATH_MAX_DEPTH
                && current != null
                && total < COL_WARN_DIST
                && points.size() < AHEAD_PATH_MAX_POINTS; depth++) {
            int split = calcRailSplit(current);
            int idx = current.getNearlestPoint(split, curX, curZ);
            int step = pickForwardStep(current, split, idx, hintX, hintZ);
            if (step == 0) break;
            boolean reachedEdge = false;
            int i = idx;
            while (total < COL_WARN_DIST && points.size() < AHEAD_PATH_MAX_POINTS) {
                int ni = i + step;
                if (ni < 0 || ni > split) {
                    reachedEdge = true;
                    break;
                }
                double[] p0 = current.getRailPos(split, i);
                double[] p1 = current.getRailPos(split, ni);
                double x0 = p0[1], z0 = p0[0];
                double x1 = p1[1], z1 = p1[0];
                double segLen = distXZ(x0, z0, x1, z1);
                i = ni;
                if (segLen < TANGENT_MIN_NORM) continue;
                total += segLen;
                if (total > COL_WARN_DIST) break;
                hintX = (x1 - x0) / segLen;
                hintZ = (z1 - z0) / segLen;
                curX = x1;
                curZ = z1;
                curY = current.getRailHeight(split, ni);
                points.add(new PathPoint(x1, z1, total));
            }
            if (!reachedEdge) break;
            RailMap next = findConnectedRailMap(world, curX, curY, curZ, current, prevMap, hintX, hintZ);
            prevMap = current;
            current = next;
        }
        return points;
    }

    private RailMap findConnectedRailMap(
            World world, double x, double y, double z,
            RailMap current, RailMap previous,
            double hintX, double hintZ) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        RailMap best = null;
        double bestDot = NEXT_MAP_MIN_DOT;
        for (int dy = 1; dy >= -2; dy--) {
            for (int dx = -AHEAD_NEXT_SEARCH; dx <= AHEAD_NEXT_SEARCH; dx++) {
                for (int dz = -AHEAD_NEXT_SEARCH; dz <= AHEAD_NEXT_SEARCH; dz++) {
                    RailMap candidate = TileEntityLargeRailBase.getRailMapFromCoordinates(
                            world, null, bx + dx + 0.5, by + dy, bz + dz + 0.5);
                    if (candidate == null) continue;
                    if (candidate.equals(current)) continue;
                    if (candidate.equals(previous)) continue;
                    int split = calcRailSplit(candidate);
                    int idx = candidate.getNearlestPoint(split, x, z);
                    int step = pickForwardStep(candidate, split, idx, hintX, hintZ);
                    if (step == 0) continue;
                    int ni = idx + step;
                    if (ni < 0 || ni > split) continue;
                    double[] p0 = candidate.getRailPos(split, idx);
                    double[] p1 = candidate.getRailPos(split, ni);
                    double vx = p1[1] - p0[1];
                    double vz = p1[0] - p0[0];
                    double n = Math.sqrt(vx * vx + vz * vz);
                    if (n < TANGENT_MIN_NORM) continue;
                    double dot = (vx / n) * hintX + (vz / n) * hintZ;
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private EntityTrainBase getLeadTrain(Formation f) {
        if (f == null || f.entries == null) return null;
        for (FormationEntry e : f.entries) {
            if (e != null && e.train != null) return e.train;
        }
        return null;
    }

    private TileEntityStation getStation(List<String> ids, int idx) {
        return (idx >= 0 && idx < ids.size()) ? StationRegistry.INSTANCE.get(ids.get(idx)) : null;
    }

    private StationSnapshot getStationSnapshot(List<String> ids, int idx) {
        return (idx >= 0 && idx < ids.size()) ? StationRegistry.INSTANCE.getSnapshot(ids.get(idx)) : null;
    }

    private void killFormation(Formation f) {
        for (FormationEntry e : f.entries) if (e.train != null) e.train.setDead();
    }

    public long getFormationId() {
        return this.formationId;
    }

    public String getRouteId() {
        return this.routeId;
    }

    public int getLineFormationIndex() {
        return this.lineFormationIndex;
    }

    public long getPredecessorFormationId() {
        return this.predecessorFormationId;
    }

    public void setPredecessorFormationId(long predecessorFormationId) {
        if (this.predecessorFormationId == predecessorFormationId) return;
        this.predecessorFormationId = predecessorFormationId;
        this.predecessorTailCache = null;
        this.predecessorRefCacheTicks = 0;
        this.aheadDistCacheTicks = 0;
        this.aheadDistCache = -1.0;
    }

    private static final class PathPoint {
        final double x;
        final double z;
        final double s;

        PathPoint(double x, double z, double s) {
            this.x = x;
            this.z = z;
            this.s = s;
        }
    }

    private static final class PathProjection {
        final double longitudinal;
        final double lateral;

        PathProjection(double longitudinal, double lateral) {
            this.longitudinal = longitudinal;
            this.lateral = lateral;
        }
    }
}
