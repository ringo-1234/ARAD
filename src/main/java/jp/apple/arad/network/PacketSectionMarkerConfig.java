package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.section.SectionSlot;
import jp.apple.arad.section.TileEntitySectionMarker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketSectionMarkerConfig implements IMessage {

    private BlockPos markerPos;
    private List<SectionSlot> slots;

    public PacketSectionMarkerConfig() {
    }

    public PacketSectionMarkerConfig(BlockPos markerPos, List<SectionSlot> slots) {
        this.markerPos = markerPos;
        this.slots = new ArrayList<>(slots);
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
        buf.writeLong(markerPos.toLong());
        buf.writeShort(slots.size());
        for (SectionSlot s : slots) {
            writeStr(buf, s.name);
            buf.writeLong(s.signalPos.toLong());
            buf.writeInt(s.passX);
            buf.writeInt(s.passZ);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        markerPos = BlockPos.fromLong(buf.readLong());
        int count = buf.readShort() & 0xFFFF;
        slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = readStr(buf);
            BlockPos sPos = BlockPos.fromLong(buf.readLong());
            int passX = buf.readInt();
            int passZ = buf.readInt();
            slots.add(new SectionSlot(name, sPos, passX, passZ));
        }
    }

    public static final class Handler implements IMessageHandler<PacketSectionMarkerConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketSectionMarkerConfig msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(msg.markerPos);
                if (!(te instanceof TileEntitySectionMarker))
                    return;
                ((TileEntitySectionMarker) te).setSlots(msg.slots);
            });
            return null;
        }
    }
}