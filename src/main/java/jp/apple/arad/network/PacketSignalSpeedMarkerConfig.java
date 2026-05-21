package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.section.SectionSlot;
import jp.apple.arad.signalspeed.TileEntitySignalSpeedMarker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketSignalSpeedMarkerConfig implements IMessage {

    private BlockPos markerPos;
    private int[] speedMap;

    public PacketSignalSpeedMarkerConfig() {
    }

    public PacketSignalSpeedMarkerConfig(BlockPos markerPos, int[] speedMap) {
        this.markerPos = markerPos;
        this.speedMap = speedMap;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(markerPos.toLong());
        buf.writeByte(SectionSlot.MAP_SIZE);
        for (int i = 0; i < SectionSlot.MAP_SIZE; i++) {
            buf.writeInt(speedMap != null && i < speedMap.length ? speedMap[i] : SectionSlot.SPEED_FREE);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        markerPos = BlockPos.fromLong(buf.readLong());
        int size = buf.readByte() & 0xFF;
        speedMap = new int[SectionSlot.MAP_SIZE];
        for (int i = 0; i < SectionSlot.MAP_SIZE; i++) {
            speedMap[i] = (i < size) ? buf.readInt() : SectionSlot.SPEED_FREE;
        }
    }

    public static final class Handler
            implements IMessageHandler<PacketSignalSpeedMarkerConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketSignalSpeedMarkerConfig msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();
            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(msg.markerPos);
                if (!(te instanceof TileEntitySignalSpeedMarker))
                    return;
                ((TileEntitySignalSpeedMarker) te).setSpeedMap(msg.speedMap);
            });
            return null;
        }
    }
}