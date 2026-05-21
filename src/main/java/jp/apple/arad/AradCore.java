package jp.apple.arad;

import jp.apple.arad.handler.AradGuiHandler;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.limit.BlockSpeedLimitSign;
import jp.apple.arad.limit.TileEntitySpeedLimitSign;
import jp.apple.arad.proxy.CommonProxy;
import jp.apple.arad.section.BlockSectionMarker;
import jp.apple.arad.section.TileEntitySectionMarker;
import jp.apple.arad.signalspeed.BlockSignalSpeedMarker;
import jp.apple.arad.signalspeed.TileEntitySignalSpeedMarker;
import jp.apple.arad.station.BlockStation;
import jp.apple.arad.station.TileEntityStation;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static jp.apple.AppleLib.tabAppleLib;

@Mod(modid = "arad", name = "ARAD", version = "1.0.0", dependencies = "required-after:rtm")

public class AradCore {

    public static final String MOD_ID = "arad";
    public static final String MOD_NAME = "ARAD";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static BlockStation blockStation;
    public static BlockSpeedLimitSign blockSpeedLimitSign;
    public static BlockSectionMarker blockSectionMarker;
    public static BlockSignalSpeedMarker blockSignalSpeedMarker;

    @Instance(MOD_ID)
    public static AradCore INSTANCE;

    @SidedProxy(clientSide = "jp.apple.arad.proxy.ClientProxy", serverSide = "jp.apple.arad.proxy.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[Arad] preInit");

        blockStation = new BlockStation();
        blockStation.setCreativeTab(tabAppleLib);
        blockSpeedLimitSign = new BlockSpeedLimitSign();
        blockSpeedLimitSign.setCreativeTab(tabAppleLib);
        blockSectionMarker = new BlockSectionMarker();
        blockSectionMarker.setCreativeTab(tabAppleLib);
        blockSignalSpeedMarker = new BlockSignalSpeedMarker();
        blockSignalSpeedMarker.setCreativeTab(tabAppleLib);

        GameRegistry.registerTileEntity(TileEntityStation.class, "arad:station");
        GameRegistry.registerTileEntity(TileEntitySpeedLimitSign.class, "arad:speed_limit_sign");
        GameRegistry.registerTileEntity(TileEntitySectionMarker.class, "arad:section_marker");
        GameRegistry.registerTileEntity(TileEntitySignalSpeedMarker.class, "arad:signal_speed_marker");

        AradPacketHandler.register();
        proxy.preInit(event);
        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new AradGuiHandler());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[Arad] init");
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventBusSubscriber
    public static class RegistrationHandler {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public static void registerBlocks(
                net.minecraftforge.event.RegistryEvent.Register<net.minecraft.block.Block> event) {
            event.getRegistry().register(blockStation);
            event.getRegistry().register(blockSpeedLimitSign);
            event.getRegistry().register(blockSectionMarker);
            event.getRegistry().register(blockSignalSpeedMarker);
        }

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public static void registerItems(
                net.minecraftforge.event.RegistryEvent.Register<net.minecraft.item.Item> event) {
            event.getRegistry().register(new ItemBlock(blockStation).setRegistryName(blockStation.getRegistryName()));
            event.getRegistry().register(
                    new ItemBlock(blockSpeedLimitSign).setRegistryName(blockSpeedLimitSign.getRegistryName()));
            event.getRegistry()
                    .register(new ItemBlock(blockSectionMarker).setRegistryName(blockSectionMarker.getRegistryName()));
            event.getRegistry().register(
                    new ItemBlock(blockSignalSpeedMarker).setRegistryName(blockSignalSpeedMarker.getRegistryName()));
        }
    }
}
