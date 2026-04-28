package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.cache.CachedRail;
import jp.apple.arad.data.MapData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketRailData implements IMessage {

    private String chunkKey;
    private List<CachedRail> segments;

    public PacketRailData() {
    }

    public PacketRailData(String chunkKey, List<CachedRail> segments) {
        this.chunkKey = chunkKey;
        this.segments = segments;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] keyBytes = chunkKey.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(keyBytes.length);
        buf.writeBytes(keyBytes);

        buf.writeShort(segments.size());
        for (CachedRail seg : segments) {
            buf.writeShort(seg.size());
            buf.writeInt(seg.coreX);
            buf.writeInt(seg.coreY);
            buf.writeInt(seg.coreZ);
            for (int i = 0; i < seg.size(); i++) {
                buf.writeFloat(seg.xPoints[i]);
                buf.writeFloat(seg.zPoints[i]);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int keyLen = buf.readShort();
        byte[] keyBytes = new byte[keyLen];
        buf.readBytes(keyBytes);
        chunkKey = new String(keyBytes, StandardCharsets.UTF_8);

        int segCount = buf.readShort();
        segments = new ArrayList<>(segCount);
        for (int s = 0; s < segCount; s++) {
            int ptCount = buf.readShort();
            int cx = buf.readInt();
            int cy = buf.readInt();
            int cz = buf.readInt();
            float[] x = new float[ptCount];
            float[] z = new float[ptCount];
            for (int i = 0; i < ptCount; i++) {
                x[i] = buf.readFloat();
                z[i] = buf.readFloat();
            }
            segments.add(new CachedRail(x, z, cx, cy, cz));
        }
    }

    public static final class Handler implements IMessageHandler<PacketRailData, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRailData msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                    MapData.INSTANCE.onRailDataReceived(msg.chunkKey, msg.segments)
            );
            return null;
        }
    }
}