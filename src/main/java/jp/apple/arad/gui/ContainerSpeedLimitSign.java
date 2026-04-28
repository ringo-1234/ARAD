package jp.apple.arad.gui;

import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerSpeedLimitSign extends Container {

    public final TileEntitySpeedLimitSign speedLimitSign;

    public ContainerSpeedLimitSign(TileEntitySpeedLimitSign speedLimitSign) {
        this.speedLimitSign = speedLimitSign;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
