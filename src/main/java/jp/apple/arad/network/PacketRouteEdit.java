package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.controller.AutoDriveManager;
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
import java.util.List;

public final class PacketRouteEdit implements IMessage {

    public static final byte OP_CREATE_ROUTE = 1;
    public static final byte OP_DELETE_ROUTE = 2;
    public static final byte OP_ADD_STATION = 3;
    public static final byte OP_REMOVE_STATION = 4;
    public static final byte OP_SET_TRAIN_COUNT = 7;

    private byte op;
    private String routeId = "";
    private String stationId = "";
    private String name = "";
    private int trainCount;

    public PacketRouteEdit() {
    }

    public static PacketRouteEdit createRoute(String routeName) {
        PacketRouteEdit p = new PacketRouteEdit();
        p.op = OP_CREATE_ROUTE;
        p.name = routeName;
        return p;
    }

    public static PacketRouteEdit deleteRoute(String routeId) {
        PacketRouteEdit p = new PacketRouteEdit();
        p.op = OP_DELETE_ROUTE;
        p.routeId = routeId;
        return p;
    }

    public static PacketRouteEdit addStation(String routeId, String stationId) {
        PacketRouteEdit p = new PacketRouteEdit();
        p.op = OP_ADD_STATION;
        p.routeId = routeId;
        p.stationId = stationId;
        return p;
    }

    public static PacketRouteEdit removeStation(String routeId, String stationId) {
        PacketRouteEdit p = new PacketRouteEdit();
        p.op = OP_REMOVE_STATION;
        p.routeId = routeId;
        p.stationId = stationId;
        return p;
    }

    public static PacketRouteEdit setTrainCount(String routeId, int count) {
        PacketRouteEdit p = new PacketRouteEdit();
        p.op = OP_SET_TRAIN_COUNT;
        p.routeId = routeId;
        p.trainCount = count;
        return p;
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
        buf.writeByte(op);
        writeStr(buf, routeId);
        writeStr(buf, stationId);
        writeStr(buf, name);
        buf.writeInt(trainCount);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        op = buf.readByte();
        routeId = readStr(buf);
        stationId = readStr(buf);
        name = readStr(buf);
        trainCount = buf.readInt();
    }

    public static final class Handler implements IMessageHandler<PacketRouteEdit, IMessage> {
        @Override
        public IMessage onMessage(PacketRouteEdit msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                RouteManager rm = RouteManager.get(world);

                switch (msg.op) {
                    case OP_CREATE_ROUTE:
                        rm.createRoute(msg.name.isEmpty() ? "新路線" : msg.name);
                        break;
                    case OP_DELETE_ROUTE:
                        AutoDriveManager.INSTANCE.stopRoute(world, msg.routeId);
                        rm.deleteRoute(msg.routeId);
                        break;
                    case OP_ADD_STATION:
                        rm.addStation(msg.routeId, msg.stationId);
                        break;
                    case OP_REMOVE_STATION:
                        rm.removeStation(msg.routeId, msg.stationId);
                        break;
                    case OP_SET_TRAIN_COUNT:
                        rm.setTrainCount(msg.routeId, msg.trainCount);
                        AutoDriveManager.INSTANCE.startSchedule(world, msg.routeId, msg.trainCount);
                        break;
                }

                List<StationSnapshot> stations = StationRegistry.INSTANCE.toSnapshots();
                List<RouteSnapshot> routes = rm.toSnapshots();
                AradPacketHandler.CHANNEL.sendToAll(
                        new PacketStationRouteData(stations, routes));
            });
            return null;
        }
    }
}