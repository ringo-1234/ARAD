package jp.apple.arad.handler;

import jp.apple.arad.gui.ContainerSpeedLimitSign;
import jp.apple.arad.gui.ContainerStation;
import jp.apple.arad.gui.GuiSpeedLimitSign;
import jp.apple.arad.gui.GuiStation;
import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import jp.apple.arad.station.TileEntityStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class AradGuiHandler implements IGuiHandler {

    public static final int GUI_STATION = 0;
    public static final int GUI_SPEED_LIMIT = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == GUI_STATION && te instanceof TileEntityStation) {
            return new ContainerStation(player.inventory, (TileEntityStation) te);
        }
        if (id == GUI_SPEED_LIMIT && te instanceof TileEntitySpeedLimitSign) {
            return new ContainerSpeedLimitSign((TileEntitySpeedLimitSign) te);
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
        return null;
    }
}
