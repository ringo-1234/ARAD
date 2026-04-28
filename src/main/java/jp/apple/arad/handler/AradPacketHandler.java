package jp.apple.arad.handler;

import jp.apple.arad.AradCore;
import jp.apple.arad.network.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class AradPacketHandler {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(AradCore.MOD_ID);

    private AradPacketHandler() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(PacketRailData.Handler.class,          PacketRailData.class,          id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketFormationData.Handler.class,     PacketFormationData.class,     id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketStationRouteData.Handler.class,  PacketStationRouteData.class,  id++, Side.CLIENT);
        CHANNEL.registerMessage(PacketRouteEdit.Handler.class,         PacketRouteEdit.class,         id++, Side.SERVER);
        CHANNEL.registerMessage(PacketStationName.Handler.class,       PacketStationName.class,       id++, Side.SERVER);
        CHANNEL.registerMessage(PacketConfirmRoute.Handler.class,      PacketConfirmRoute.class,      id++, Side.SERVER);
        CHANNEL.registerMessage(PacketStationConfig.Handler.class,     PacketStationConfig.class,     id++, Side.SERVER);
        CHANNEL.registerMessage(PacketSpeedLimitSignConfig.Handler.class, PacketSpeedLimitSignConfig.class, id++, Side.SERVER);
    }
}
