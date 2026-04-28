package jp.apple.arad.handler;

import jp.apple.arad.data.MapData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientAradEventHandler {
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) return;
        MapData.INSTANCE.onWorldJoin();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) return;
        MapData.INSTANCE.onWorldLeave();
    }
}