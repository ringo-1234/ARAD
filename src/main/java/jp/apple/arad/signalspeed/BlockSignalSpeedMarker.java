package jp.apple.arad.signalspeed;

import jp.apple.arad.AradCore;
import jp.apple.arad.handler.AradGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockSignalSpeedMarker extends Block implements ITileEntityProvider {

    public BlockSignalSpeedMarker() {
        super(Material.IRON);
        setUnlocalizedName("arad_signal_speed_marker");
        setRegistryName("arad", "signal_speed_marker");
        setHardness(2.0f);
        setResistance(12.0f);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(AradCore.INSTANCE, AradGuiHandler.GUI_SIGNAL_SPEED_MARKER,
                    world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySignalSpeedMarker();
    }
}