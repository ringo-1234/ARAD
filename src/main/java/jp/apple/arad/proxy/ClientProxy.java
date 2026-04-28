package jp.apple.arad.proxy;

import jp.apple.arad.AradCore;
import jp.apple.arad.handler.ClientAradEventHandler;
import jp.apple.arad.handler.AradKeyHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Mod.EventBusSubscriber(value = Side.CLIENT , modid = "arad")
    public static class ModelRegistrationHandler {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            net.minecraft.client.renderer.block.model.ModelResourceLocation stationLoc =
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation("arad:station", "inventory");
            net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                    net.minecraft.item.Item.getItemFromBlock(AradCore.blockStation), 0, stationLoc);

            net.minecraft.client.renderer.block.model.ModelResourceLocation speedLimitLoc =
                    new net.minecraft.client.renderer.block.model.ModelResourceLocation("arad:speed_limit_sign", "inventory");
            net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                    net.minecraft.item.Item.getItemFromBlock(AradCore.blockSpeedLimitSign), 0, speedLimitLoc);
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        AradKeyHandler.register();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new ClientAradEventHandler());
        MinecraftForge.EVENT_BUS.register(new AradKeyHandler());
    }
}
