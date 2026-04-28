package jp.apple.arad.data;

import jp.apple.arad.network.PacketStationRouteData;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.station.StationRegistry;
import jp.apple.arad.cache.CachedRail;
import jp.apple.arad.cache.RailCacheManager;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.network.PacketFormationData;
import jp.apple.arad.network.PacketRailData;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.FormationManager;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

public final class ServerData {

    public static final ServerData INSTANCE = new ServerData();

    private int formationTickCounter = 0;

    private ServerData() {}

    public void onServerTick(World world) {
        if (world.isRemote) return;
        if (++formationTickCounter >= 20) {
            formationTickCounter = 0;
            sendFormationsAndPlayers(world);
        }
    }

    public void onChunkLoad(Chunk chunk, World world) {
        if (world.isRemote) return;

        int    dim = world.provider.getDimension();
        String key = RailCacheManager.makeChunkKey(dim, chunk.x, chunk.z);

        List<CachedRail> segs = extractFromChunk(chunk);

        AradPacketHandler.CHANNEL.sendToAll(new PacketRailData(key, segs));
    }

    public void onPlayerLogin(EntityPlayerMP player, World world) {
        if (world.isRemote) return;

        int dim = world.provider.getDimension();
        int centerCx = player.chunkCoordX;
        int centerCz = player.chunkCoordZ;
        int viewDist = FMLCommonHandler.instance().getMinecraftServerInstance()
                .getPlayerList().getViewDistance();
        int scanRadius = Math.max(2, viewDist + 1);

        for (int cx = centerCx - scanRadius; cx <= centerCx + scanRadius; cx++) {
            for (int cz = centerCz - scanRadius; cz <= centerCz + scanRadius; cz++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk == null) continue;
                String key = RailCacheManager.makeChunkKey(dim, cx, cz);
                List<CachedRail> segs = extractFromChunk(chunk);
                AradPacketHandler.CHANNEL.sendTo(new PacketRailData(key, segs), player);
            }
        }

        List<FormationSnapshot> formations = collectFormationSnapshots();
        List<PlayerSnapshot>    players    = collectPlayerSnapshots();
        List<StationSnapshot>   stations   = StationRegistry.INSTANCE.toSnapshots();
        List<RouteSnapshot>     routes     = RouteManager.get(world).toSnapshots();
        AradPacketHandler.CHANNEL.sendTo(new PacketFormationData(formations, players), player);
        AradPacketHandler.CHANNEL.sendTo(new PacketStationRouteData(stations, routes), player);
    }

    private void sendFormationsAndPlayers(World world) {
        List<FormationSnapshot> formations = collectFormationSnapshots();
        List<PlayerSnapshot>    players    = collectPlayerSnapshots();
        AradPacketHandler.CHANNEL.sendToAll(new PacketFormationData(formations, players));
    }

    public List<CachedRail> extractFromChunkPublic(Chunk chunk) {
        return extractFromChunk(chunk);
    }

    private List<CachedRail> extractFromChunk(Chunk chunk) {
        List<CachedRail> result = new ArrayList<>();
        for (TileEntity te : chunk.getTileEntityMap().values()) {
            if (te instanceof TileEntityLargeRailCore) {
                result.addAll(extractFromCore((TileEntityLargeRailCore) te));
            }
        }
        return result;
    }

    private List<CachedRail> extractFromCore(TileEntityLargeRailCore core) {
        List<CachedRail> result = new ArrayList<>();
        RailMap[] maps = core.getAllRailMaps();
        if (maps == null) return result;

        BlockPos corePos = core.getPos();

        for (RailMap rm : maps) {
            if (rm == null) continue;
            int split = Math.min(128, Math.max(4, (int) (rm.getLength() * 2.0)));
            float[] x = new float[split + 1];
            float[] z = new float[split + 1];
            for (int i = 0; i <= split; i++) {
                double[] pos = rm.getRailPos(split, i);
                x[i] = (float) pos[1];
                z[i] = (float) pos[0];
            }
            result.add(new CachedRail(x, z, corePos.getX(), corePos.getY(), corePos.getZ()));
        }
        return result;
    }

    public List<FormationSnapshot> collectFormationSnapshots() {
        List<FormationSnapshot> result = new ArrayList<>();
        if (FormationManager.getInstance() == null) return result;

        for (Formation formation : FormationManager.getInstance().getFormations().values()) {
            if (formation.entries == null) continue;

            List<float[]> cars          = new ArrayList<>();
            float         speed         = 0f;
            boolean       hasAliveTrain = false;
            String        displayName   = null;

            for (FormationEntry entry : formation.entries) {
                if (entry == null || entry.train == null || entry.train.isDead) continue;

                EntityTrainBase train = entry.train;

                if (!hasAliveTrain) {
                    speed = train.getSpeed();
                    hasAliveTrain = true;
                    try {
                        if (train.getResourceState() != null
                                && train.getResourceState().getResourceSet() != null) {
                            displayName = train.getResourceState().getResourceSet()
                                    .getConfig().getName();
                        }
                    } catch (Exception ignored) {}
                }

                cars.add(new float[]{
                        (float) train.posX,
                        (float) train.posZ,
                        train.rotationYaw
                });
            }

            if (!hasAliveTrain || cars.isEmpty()) continue;

            if (displayName == null || displayName.isEmpty()) {
                displayName = String.format("F%03d", formation.id % 1000);
            }

            result.add(new FormationSnapshot(displayName, speed, formation.getNotch(), cars));
        }
        return result;
    }

    public List<PlayerSnapshot> collectPlayerSnapshots() {
        List<PlayerSnapshot> result = new ArrayList<>();
        try {
            List<EntityPlayerMP> players = FMLCommonHandler.instance()
                    .getMinecraftServerInstance()
                    .getPlayerList()
                    .getPlayers();

            for (EntityPlayerMP p : players) {
                result.add(new PlayerSnapshot(
                        (float) p.posX,
                        (float) p.posZ,
                        p.rotationYaw,
                        p.getName()
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
