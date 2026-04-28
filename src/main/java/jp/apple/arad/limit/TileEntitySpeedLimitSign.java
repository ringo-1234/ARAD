package jp.apple.arad.limit;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySpeedLimitSign extends TileEntity {

    private int speedLimitKmh = 120;
    private int startOffsetBlocks = 0;

    public int getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    public int getStartOffsetBlocks() {
        return startOffsetBlocks;
    }

    public void setSpeedLimitKmh(int kmh) {
        this.speedLimitKmh = clampSpeed(kmh);
        markDirty();
    }

    public void setStartOffsetBlocks(int blocks) {
        this.startOffsetBlocks = clampOffset(blocks);
        markDirty();
    }

    public void setConfig(int kmh, int offsetBlocks) {
        this.speedLimitKmh = clampSpeed(kmh);
        this.startOffsetBlocks = clampOffset(offsetBlocks);
        markDirty();
    }

    private static int clampSpeed(int kmh) {
        return Math.max(1, Math.min(360, kmh));
    }

    private static int clampOffset(int blocks) {
        return Math.max(0, Math.min(5000, blocks));
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("SpeedLimitKmh", speedLimitKmh);
        nbt.setInteger("StartOffsetBlocks", startOffsetBlocks);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("SpeedLimitKmh")) speedLimitKmh = clampSpeed(nbt.getInteger("SpeedLimitKmh"));
        if (nbt.hasKey("StartOffsetBlocks")) startOffsetBlocks = clampOffset(nbt.getInteger("StartOffsetBlocks"));
    }
}
