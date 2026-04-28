package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.data.RouteSnapshot;
import jp.apple.arad.data.StationSnapshot;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.route.RouteManager;
import jp.apple.arad.station.StationRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketConfirmRoute implements IMessage {

    private String routeName;
    private List<String> stationIds;

    public PacketConfirmRoute() {
    }

    public PacketConfirmRoute(String routeName, List<String> stationIds) {
        this.routeName = routeName;
        this.stationIds = stationIds;
    }

    private static void writeStr(ByteBuf buf, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, 255);
        buf.writeByte(len);
        buf.writeBytes(b, 0, len);
    }

    private static String readStr(ByteBuf buf) {
        int len = buf.readByte() & 0xFF;
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeStr(buf, routeName);
        buf.writeShort(stationIds.size());
        for (String id : stationIds) writeStr(buf, id);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        routeName = readStr(buf);
        int count = buf.readShort();
        stationIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) stationIds.add(readStr(buf));
    }

    public static final class Handler implements IMessageHandler<PacketConfirmRoute, IMessage> {
        @Override
        public IMessage onMessage(PacketConfirmRoute msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                if (msg.stationIds.size() < 2) return;

                String name = msg.routeName.isEmpty() ? "新路線" : msg.routeName;

                RouteManager rm = RouteManager.get(world);
                String routeId = rm.createRoute(name);
                for (String sid : msg.stationIds) rm.addStation(routeId, sid);

                List<StationSnapshot> stations = StationRegistry.INSTANCE.toSnapshots();
                List<RouteSnapshot> routes = rm.toSnapshots();
                AradPacketHandler.CHANNEL.sendToAll(
                        new PacketStationRouteData(stations, routes));
            });
            return null;
        }
    }
}