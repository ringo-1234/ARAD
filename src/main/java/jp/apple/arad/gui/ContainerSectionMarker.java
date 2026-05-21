package jp.apple.arad.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerSectionMarker extends Container {

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}