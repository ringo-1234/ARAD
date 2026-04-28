package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.speed.ClientSpeedLimitCache;
import jp.apple.arad.speed.RailSpeedEntry;
import jp.apple.arad.speed.RailSpeedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketSpeedLimit implements IMessage {

    private byte mode;
    private String key;
    private int kmh;
    private List<RailSpeedEntry> entries;

    public PacketSpeedLimit() {
    }

    public static PacketSpeedLimit change(String key, int kmh) {
        PacketSpeedLimit p = new PacketSpeedLimit();
        p.mode = 0;
        p.key = key;
        p.kmh = kmh;
        return p;
    }

    public static PacketSpeedLimit syncAll(List<RailSpeedEntry> entries) {
        PacketSpeedLimit p = new PacketSpeedLimit();
        p.mode = 1;
        p.entries = entries;
        return p;
    }

    private static void writeStr(ByteBuf buf, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, 511);
        buf.writeShort(len);
        buf.writeBytes(b, 0, len);
    }

    private static String readStr(ByteBuf buf) {
        int len = buf.readShort() & 0xFFFF;
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        if (mode == 0) {
            writeStr(buf, key == null ? "" : key);
            buf.writeInt(kmh);
        } else {
            int size = entries == null ? 0 : entries.size();
            buf.writeShort(size);
            if (entries != null) {
                for (RailSpeedEntry e : entries) {
                    writeStr(buf, e.key);
                    buf.writeInt(e.speedLimitKmh);
                }
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readByte();
        if (mode == 0) {
            key = readStr(buf);
            kmh = buf.readInt();
        } else {
            int size = buf.readShort() & 0xFFFF;
            entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String k = readStr(buf);
                int kmhv = buf.readInt();
                entries.add(new RailSpeedEntry(k, kmhv));
            }
        }
    }

    public static final class Handler implements IMessageHandler<PacketSpeedLimit, IMessage> {
        @Override
        public IMessage onMessage(PacketSpeedLimit msg, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {

                EntityPlayerMP player = ctx.getServerHandler().player;
                WorldServer world = player.getServerWorld();
                world.addScheduledTask(() -> {
                    RailSpeedManager rsm = RailSpeedManager.get(world);
                    rsm.setSpeedLimit(msg.key, msg.kmh);

                    AradPacketHandler.CHANNEL.sendToAll(
                            PacketSpeedLimit.syncAll(rsm.toEntries()));
                });
            } else {

                Minecraft.getMinecraft().addScheduledTask(() ->
                        ClientSpeedLimitCache.INSTANCE.onDataReceived(msg.entries));
            }
            return null;
        }
    }
}