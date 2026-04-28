package jp.apple.arad.handler;

import jp.apple.arad.cache.CachedRail;
import jp.apple.arad.cache.RailCacheManager;
import jp.apple.arad.controller.AutoDriveManager;
import jp.apple.arad.data.RouteSnapshot;
import jp.apple.arad.data.ServerData;
import jp.apple.arad.data.StationSnapshot;
import jp.apple.arad.network.PacketRailData;
import jp.apple.arad.network.PacketStationRouteData;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.station.StationRegistry;
import jp.apple.arad.station.TileEntityStation;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public final class CommonAradEventHandler {

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) return;
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            Chunk chunk = event.getChunk();
            ws.addScheduledTask(() -> ServerData.INSTANCE.onChunkLoad(chunk, ws));
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isPrimaryServerWorld(event.world)) return;
        ServerData.INSTANCE.onServerTick(event.world);
        AutoDriveManager.INSTANCE.tick(event.world);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!isPrimaryServerWorld(world)) return;
        StationRegistry.INSTANCE.loadFromWorld(world);
        AutoDriveManager.INSTANCE.restoreFromRouteManager(world);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!isPrimaryServerWorld(event.getWorld())) return;
        StationRegistry.INSTANCE.clearLoaded();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ServerData.INSTANCE.onPlayerLogin(player, player.world);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;
        Block placed = event.getPlacedBlock().getBlock();
        if (!isRailRelatedBlock(placed)) return;

        WorldServer ws = (WorldServer) world;
        int dim = world.provider.getDimension();
        int cx = event.getPos().getX() >> 4;
        int cz = event.getPos().getZ() >> 4;
        ws.addScheduledTask(() -> refreshRailChunk(ws, dim, cx, cz));
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;

        Block broken = event.getState().getBlock();
        boolean isRailBlock = isRailRelatedBlock(broken);
        boolean isStationBlock = broken instanceof jp.apple.arad.station.BlockStation;
        if (!isRailBlock && !isStationBlock) return;

        WorldServer ws = (WorldServer) world;

        if (isStationBlock) {
            TileEntity te = world.getTileEntity(event.getPos());
            if (te instanceof TileEntityStation) {
                StationRegistry.INSTANCE.removeFromCache(world, ((TileEntityStation) te).getStationId());
            } else {
                StationRegistry.INSTANCE.removeFromCacheByPos(
                        world,
                        world.provider.getDimension(),
                        event.getPos().getX(),
                        event.getPos().getZ()
                );
            }
            List<StationSnapshot> stations = StationRegistry.INSTANCE.toSnapshots();
            List<RouteSnapshot> routes = RouteManager.get(world).toSnapshots();
            AradPacketHandler.CHANNEL.sendToAll(new PacketStationRouteData(stations, routes));
        }

        if (!isRailBlock) return;

        int dim = world.provider.getDimension();
        int cx = event.getPos().getX() >> 4;
        int cz = event.getPos().getZ() >> 4;
        ws.addScheduledTask(() -> refreshRailChunk(ws, dim, cx, cz));
    }

    private static void refreshRailChunk(WorldServer ws, int dim, int cx, int cz) {
        Chunk chunk = ws.getChunkFromChunkCoords(cx, cz);
        String key = RailCacheManager.makeChunkKey(dim, cx, cz);
        List<CachedRail> segs = ServerData.INSTANCE.extractFromChunkPublic(chunk);
        AradPacketHandler.CHANNEL.sendToAll(new PacketRailData(key, segs));
    }

    private static boolean isRailRelatedBlock(Block block) {
        if (block instanceof jp.ngt.rtm.rail.BlockLargeRailBase) return true;
        ResourceLocation key = block.getRegistryName();
        if (key == null) return false;
        String path = key.getResourcePath();
        return path.contains("rail") || path.contains("marker");
    }

    private static boolean isPrimaryServerWorld(World world) {
        return world != null
                && !world.isRemote
                && world.provider != null
                && world.provider.getDimension() == 0;
    }
}
