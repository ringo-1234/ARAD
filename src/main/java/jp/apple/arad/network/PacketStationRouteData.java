package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.data.MapData;
import jp.apple.arad.data.RouteSnapshot;
import jp.apple.arad.data.StationSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketStationRouteData implements IMessage {

    private List<StationSnapshot> stations;
    private List<RouteSnapshot> routes;

    public PacketStationRouteData() {
    }

    public PacketStationRouteData(List<StationSnapshot> stations, List<RouteSnapshot> routes) {
        this.stations = stations;
        this.routes = routes;
    }

    private static void writeStr(ByteBuf buf, String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
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
        buf.writeShort(stations.size());
        for (StationSnapshot s : stations) {
            writeStr(buf, s.id);
            writeStr(buf, s.name);
            buf.writeFloat(s.x);
            buf.writeFloat(s.z);
            buf.writeInt(s.dim);
            buf.writeBoolean(s.doorLeft);
            buf.writeBoolean(s.doorRight);
            buf.writeBoolean(s.spawnReversed);
            buf.writeInt(s.dwellTicks);
        }
        buf.writeShort(routes.size());
        for (RouteSnapshot r : routes) {
            writeStr(buf, r.id);
            writeStr(buf, r.name);
            buf.writeInt(r.trainCount);
            buf.writeShort(r.stationIds.size());
            for (String sid : r.stationIds) writeStr(buf, sid);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int sc = buf.readShort();
        stations = new ArrayList<>(sc);
        for (int i = 0; i < sc; i++) {
            stations.add(new StationSnapshot(
                    readStr(buf), readStr(buf),
                    buf.readFloat(), buf.readFloat(),
                    buf.readInt(),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readInt()));
        }
        int rc = buf.readShort();
        routes = new ArrayList<>(rc);
        for (int i = 0; i < rc; i++) {
            String id = readStr(buf);
            String name = readStr(buf);
            int trainCount = buf.readInt();
            int nSt = buf.readShort();
            List<String> sids = new ArrayList<>(nSt);
            for (int j = 0; j < nSt; j++) sids.add(readStr(buf));
            routes.add(new RouteSnapshot(id, name, sids, trainCount));
        }
    }

    public static final class Handler
            implements IMessageHandler<PacketStationRouteData, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketStationRouteData msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                MapData.INSTANCE.onStationDataReceived(msg.stations);
                MapData.INSTANCE.onRouteDataReceived(msg.routes);
            });
            return null;
        }
    }
}
