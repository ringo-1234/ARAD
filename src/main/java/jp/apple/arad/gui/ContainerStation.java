package jp.apple.arad.gui;

import jp.apple.arad.station.TileEntityStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerStation extends Container {

    public final TileEntityStation station;

    public ContainerStation(InventoryPlayer playerInv, TileEntityStation station) {
        this.station = station;

        addSlotToContainer(new Slot(station, 0, 80, 94) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return station.isItemValidForSlot(0, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        118 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 176));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return result;

        ItemStack stack = slot.getStack();
        result = stack.copy();

        if (index == 0) {
            if (!mergeItemStack(stack, 1, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (station.isItemValidForSlot(0, stack) && !inventorySlots.get(0).getHasStack()) {
                if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();

        return result;
    }
}