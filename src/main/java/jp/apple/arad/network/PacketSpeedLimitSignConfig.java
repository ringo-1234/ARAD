package jp.apple.arad.network;

import io.netty.buffer.ByteBuf;
import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketSpeedLimitSignConfig implements IMessage {

    private int x;
    private int y;
    private int z;
    private int speedLimitKmh;
    private int startOffsetBlocks;

    public PacketSpeedLimitSignConfig() {}

    public PacketSpeedLimitSignConfig(BlockPos pos, int speedLimitKmh, int startOffsetBlocks) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.speedLimitKmh = speedLimitKmh;
        this.startOffsetBlocks = startOffsetBlocks;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(speedLimitKmh);
        buf.writeInt(startOffsetBlocks);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        speedLimitKmh = buf.readInt();
        startOffsetBlocks = buf.readInt();
    }

    public static final class Handler implements IMessageHandler<PacketSpeedLimitSignConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketSpeedLimitSignConfig msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            WorldServer world = player.getServerWorld();

            world.addScheduledTask(() -> {
                TileEntity te = world.getTileEntity(new BlockPos(msg.x, msg.y, msg.z));
                if (!(te instanceof TileEntitySpeedLimitSign)) return;

                TileEntitySpeedLimitSign sign = (TileEntitySpeedLimitSign) te;
                sign.setConfig(msg.speedLimitKmh, msg.startOffsetBlocks);

                if (world != null) {
                    world.notifyBlockUpdate(sign.getPos(), world.getBlockState(sign.getPos()),
                            world.getBlockState(sign.getPos()), 3);
                }
            });
            return null;
        }
    }
}
