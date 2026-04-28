package jp.apple.arad.station;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

public class TileEntityStation extends TileEntity implements ITickable, IInventory {

    private String    stationId   = UUID.randomUUID().toString();
    private String    stationName = "新しい駅";
    private boolean   registered  = false;

    private ItemStack formationItem = ItemStack.EMPTY;

    private boolean doorLeft  = true;
    private boolean doorRight = true;
    private boolean spawnReversed = false;
    private int dwellTimeTicks = 400;

    public String getStationId()   { return stationId; }
    public String getStationName() { return stationName; }

    public void setStationName(String name) {
        this.stationName = (name == null || name.isEmpty()) ? "駅" : name;
        markDirty();
        registered = false;
    }

    public boolean isDoorLeft()  { return doorLeft; }
    public boolean isDoorRight() { return doorRight; }
    public boolean isSpawnReversed() { return spawnReversed; }
    public int     getDwellTimeTicks() { return dwellTimeTicks; }

    public void setDoorLeft(boolean v) {
        this.doorLeft = v;
        markDirty();
        registered = false;
    }

    public void setDoorRight(boolean v) {
        this.doorRight = v;
        markDirty();
        registered = false;
    }

    public void setSpawnReversed(boolean v) {
        this.spawnReversed = v;
        markDirty();
        registered = false;
    }

    public void setDwellTimeTicks(int ticks) {
        this.dwellTimeTicks = Math.max(20, ticks);
        markDirty();
    }

    public byte getDoorData() {
        if (doorLeft && doorRight) return 3;
        if (doorRight) return 1;
        if (doorLeft)  return 2;
        return 0;
    }

    public ItemStack getFormationItem() { return formationItem; }



    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        if (!registered) {
            StationRegistry.INSTANCE.register(this);
            registered = true;
        }
    }



    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && !world.isRemote) StationRegistry.INSTANCE.unregister(stationId);
    }

    @Override
    public void onChunkUnload() {
        if (world != null && !world.isRemote) StationRegistry.INSTANCE.unregister(stationId);
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
        nbt.setString("StationId",    stationId);
        nbt.setString("StationName",  stationName);
        nbt.setBoolean("DoorLeft",    doorLeft);
        nbt.setBoolean("DoorRight",   doorRight);
        nbt.setBoolean("SpawnReversed", spawnReversed);
        nbt.setInteger("DwellTicks",  dwellTimeTicks);
        if (!formationItem.isEmpty()) {
            nbt.setTag("FormationItem", formationItem.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("StationId"))   stationId   = nbt.getString("StationId");
        if (nbt.hasKey("StationName")) stationName = nbt.getString("StationName");
        if (nbt.hasKey("DoorLeft"))    doorLeft    = nbt.getBoolean("DoorLeft");
        if (nbt.hasKey("DoorRight"))   doorRight   = nbt.getBoolean("DoorRight");
        if (nbt.hasKey("SpawnReversed")) spawnReversed = nbt.getBoolean("SpawnReversed");
        if (nbt.hasKey("DwellTicks"))  dwellTimeTicks = Math.max(20, nbt.getInteger("DwellTicks"));
        if (nbt.hasKey("FormationItem"))
            formationItem = new ItemStack(nbt.getCompoundTag("FormationItem"));
        registered = false;
    }

    @Override public int getSizeInventory()    { return 1; }
    @Override public boolean isEmpty()         { return formationItem.isEmpty(); }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index == 0 ? formationItem : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index != 0 || formationItem.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = formationItem.splitStack(count);
        if (formationItem.isEmpty()) formationItem = ItemStack.EMPTY;
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index != 0) return ItemStack.EMPTY;
        ItemStack old = formationItem;
        formationItem = ItemStack.EMPTY;
        markDirty();
        return old;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index != 0) return;
        formationItem = stack;
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        markDirty();
    }

    @Override public int  getInventoryStackLimit()                    { return 1; }
    @Override public boolean isUsableByPlayer(EntityPlayer player)    { return true; }
    @Override public void openInventory(EntityPlayer player)          {}
    @Override public void closeInventory(EntityPlayer player)         {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.getItem().getRegistryName() == null) return false;
        return "artpe".equals(stack.getItem().getRegistryName().getResourceDomain())
                && "artpe_train".equals(stack.getItem().getRegistryName().getResourcePath());
    }

    @Override public int  getField(int id)             { return 0; }
    @Override public void setField(int id, int value)  {}
    @Override public int  getFieldCount()              { return 0; }
    @Override public void clear()                      { formationItem = ItemStack.EMPTY; markDirty(); }
    @Override public String getName()                  { return stationName; }
    @Override public boolean hasCustomName()           { return true; }
    @Override public ITextComponent getDisplayName()   { return new TextComponentString(stationName); }
}
