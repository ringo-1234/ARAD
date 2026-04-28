package jp.apple.arad.handler;

import jp.apple.arad.gui.GuiRailMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class AradKeyHandler {

    public static final KeyBinding KEY_OPEN_MAP = new KeyBinding(
            "key.arad.open_map",
            Keyboard.KEY_M,
            "key.categories.arad"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(KEY_OPEN_MAP);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!KEY_OPEN_MAP.isPressed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null && mc.world != null) {
            mc.displayGuiScreen(new GuiRailMap());
        }
    }
}