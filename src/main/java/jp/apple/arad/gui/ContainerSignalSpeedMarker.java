package jp.apple.arad.gui;

import jp.apple.arad.signalspeed.TileEntitySignalSpeedMarker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerSignalSpeedMarker extends Container {

    public final TileEntitySignalSpeedMarker te;

    public ContainerSignalSpeedMarker(TileEntitySignalSpeedMarker te) {
        this.te = te;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}