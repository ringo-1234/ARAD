package jp.apple.arad.station;

import jp.apple.arad.AradCore;
import jp.apple.arad.handler.AradGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockStation extends Block implements ITileEntityProvider {

    public BlockStation() {
        super(Material.GLASS);
        setUnlocalizedName("arad_station");
        setRegistryName("arad", "station");
        setHardness(1.5f);
        setResistance(10f);
        setLightLevel(0.5f);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(AradCore.INSTANCE, AradGuiHandler.GUI_STATION,
                    world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityStation();
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
