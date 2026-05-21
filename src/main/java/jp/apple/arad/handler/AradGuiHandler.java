package jp.apple.arad.handler;

import jp.apple.arad.gui.*;
import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import jp.apple.arad.section.TileEntitySectionMarker;
import jp.apple.arad.signalspeed.TileEntitySignalSpeedMarker;
import jp.apple.arad.station.TileEntityStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class AradGuiHandler implements IGuiHandler {

    public static final int GUI_STATION = 0;
    public static final int GUI_SPEED_LIMIT = 1;
    public static final int GUI_SECTION_MARKER = 2;
    public static final int GUI_SIGNAL_SPEED_MARKER = 3;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == GUI_STATION && te instanceof TileEntityStation) {
            return new ContainerStation(player.inventory, (TileEntityStation) te);
        }
        if (id == GUI_SPEED_LIMIT && te instanceof TileEntitySpeedLimitSign) {
            return new ContainerSpeedLimitSign((TileEntitySpeedLimitSign) te);
        }
        if (id == GUI_SECTION_MARKER) {
            return new ContainerSectionMarker();
        }
        if (id == GUI_SIGNAL_SPEED_MARKER && te instanceof TileEntitySignalSpeedMarker) {
            return new ContainerSignalSpeedMarker((TileEntitySignalSpeedMarker) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == GUI_STATION && te instanceof TileEntityStation) {
            TileEntityStation station = (TileEntityStation) te;
            ContainerStation container = new ContainerStation(player.inventory, station);
            return new GuiStation(container, new BlockPos(x, y, z));
        }
        if (id == GUI_SPEED_LIMIT && te instanceof TileEntitySpeedLimitSign) {
            TileEntitySpeedLimitSign sign = (TileEntitySpeedLimitSign) te;
            ContainerSpeedLimitSign container = new ContainerSpeedLimitSign(sign);
            return new GuiSpeedLimitSign(container, new BlockPos(x, y, z));
        }
        if (id == GUI_SECTION_MARKER && te instanceof TileEntitySectionMarker) {
            return new GuiSectionMarker((TileEntitySectionMarker) te, new BlockPos(x, y, z));
        }
        if (id == GUI_SIGNAL_SPEED_MARKER && te instanceof TileEntitySignalSpeedMarker) {
            TileEntitySignalSpeedMarker marker = (TileEntitySignalSpeedMarker) te;
            return new GuiSignalSpeedMarker(marker, new BlockPos(x, y, z));
        }
        return null;
    }
}
