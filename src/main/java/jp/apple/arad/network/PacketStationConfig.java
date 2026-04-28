package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.data.RouteSnapshot;
import jp.apple.arad.data.StationSnapshot;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.station.StationRegistry;
import jp.apple.arad.station.TileEntityStation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class PacketStationConfig implements IMessage {

    private int x, y, z;
    private String name;
    private boolean doorLeft;
    private boolean doorRight;
    private boolean spawnReversed;
    private int dwellTicks;

    public PacketStationConfig() {
    }

    public PacketStationConfig(BlockPos pos, String name,
                               boolean doorLeft, boolean doorRight, boolean spawnReversed, int dwellTicks) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.name = name;
        this.doorLeft = doorLeft;
        this.doorRight = doorRight;
        this.spawnReversed = spawnReversed;
        this.dwellTicks = dwellTicks;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        byte[] b = (name == null ? "" : name).getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, 255);
        buf.writeByte(len);
        buf.writeBytes(b, 0, len);
        buf.writeBoolean(doorLeft);
        buf.writeBoolean(doorRight);
        buf.writeBoolean(spawnReversed);
        buf.writeInt(dwellTicks);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        int len = buf.readByte() & 0xFF;
        byte[] b = new byte[len];
        buf.readBytes(b);
        name = new String(b, StandardCharsets.UTF_8);
        doorLeft = buf.readBoolean();
        doorRight = buf.readBoolean();
        spawnReversed = buf.readBoolean();
        dwellTicks = buf.readInt();
    }

    public static final class Handler implements IMessageHandler<PacketStationConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketStationConfig msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(new BlockPos(msg.x, msg.y, msg.z));
                if (!(te instanceof TileEntityStation)) return;

                TileEntityStation station = (TileEntityStation) te;
                station.setStationName(msg.name.isEmpty() ? "駅" : msg.name);
                station.setDoorLeft(msg.doorLeft);
                station.setDoorRight(msg.doorRight);
                station.setSpawnReversed(msg.spawnReversed);
                station.setDwellTimeTicks(msg.dwellTicks);
                StationRegistry.INSTANCE.register(station);

                List<StationSnapshot> stations = StationRegistry.INSTANCE.toSnapshots();
                List<RouteSnapshot> routes = RouteManager.get(world).toSnapshots();
                AradPacketHandler.CHANNEL.sendToAll(
                        new PacketStationRouteData(stations, routes));
            });
            return null;
        }
    }
}
