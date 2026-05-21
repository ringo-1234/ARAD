package jp.apple.arad.signalspeed;

import jp.apple.arad.controller.AutoDriveController;
import jp.apple.arad.controller.AutoDriveManager;
import jp.apple.arad.section.SectionSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class TileEntitySignalSpeedMarker extends TileEntity implements ITickable {

    private static final double TRIGGER_RADIUS = 3.0;
    private final Map<Long, Boolean> triggerState = new HashMap<>();
    private int[] speedMap = SectionSlot.defaultSpeedMap();

    private static int[] build(int[] src) {
        int[] map = SectionSlot.defaultSpeedMap();
        if (src == null)
            return map;
        for (int i = 0; i < SectionSlot.MAP_SIZE && i < src.length; i++)
            map[i] = (src[i] < 0) ? SectionSlot.SPEED_FREE : Math.min(src[i], 9999);
        return map;
    }

    public int[] getSpeedMap() {
        return speedMap.clone();
    }

    public void setSpeedMap(int[] src) {
        this.speedMap = build(src);
        triggerState.clear();
        markDirty();
        if (world != null && !world.isRemote)
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
    }

    @Override
    public void update() {
        World world = getWorld();
        if (world == null || world.isRemote)
            return;

        double bx = pos.getX() + 0.5;
        double bz = pos.getZ() + 0.5;
        double r2 = TRIGGER_RADIUS * TRIGGER_RADIUS;

        for (AutoDriveController ctrl : AutoDriveManager.INSTANCE.getAllControllers()) {
            long fid = ctrl.getFormationId();
            double tx = ctrl.getLeadX(world);
            double tz = ctrl.getLeadZ(world);
            if (tx == Double.MIN_VALUE)
                continue;

            double dx = tx - bx, dz = tz - bz;
            boolean nowOn = (dx * dx + dz * dz) <= r2;
            Boolean lastOn = triggerState.getOrDefault(fid, false);

            if (nowOn && !lastOn) {
                ctrl.onSignalSpeedMapReceived(speedMap.clone());
            }
            triggerState.put(fid, nowOn);
        }

        triggerState.entrySet().removeIf(e -> AutoDriveManager.INSTANCE.getController(e.getKey()) == null);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setIntArray("speedMap", speedMap);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("speedMap"))
            speedMap = build(nbt.getIntArray("speedMap"));
        triggerState.clear();
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.getNbtCompound();
        if (nbt.hasKey("speedMap"))
            speedMap = build(nbt.getIntArray("speedMap"));
        triggerState.clear();
    }
}