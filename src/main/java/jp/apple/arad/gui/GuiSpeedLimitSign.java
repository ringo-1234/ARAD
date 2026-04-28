package jp.apple.arad.gui;

import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.network.PacketSpeedLimitSignConfig;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiSpeedLimitSign extends GuiContainer {

    private static final int GUI_W = 196;
    private static final int GUI_H = 86;

    private final ContainerSpeedLimitSign container;
    private final BlockPos pos;

    private GuiTextField limitField;
    private GuiTextField offsetField;

    public GuiSpeedLimitSign(ContainerSpeedLimitSign container, BlockPos pos) {
        super(container);
        this.container = container;
        this.pos = pos;
        this.xSize = GUI_W;
        this.ySize = GUI_H;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        limitField = new GuiTextField(0, fontRenderer, guiLeft + 100, guiTop + 20, 56, 16);
        limitField.setMaxStringLength(4);
        limitField.setText(String.valueOf(container.speedLimitSign.getSpeedLimitKmh()));
        limitField.setCanLoseFocus(true);
        limitField.setFocused(true);

        offsetField = new GuiTextField(1, fontRenderer, guiLeft + 100, guiTop + 45, 56, 16);
        offsetField.setMaxStringLength(5);
        offsetField.setText(String.valueOf(container.speedLimitSign.getStartOffsetBlocks()));
        offsetField.setCanLoseFocus(true);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        sendIfChanged();
    }

    private void sendIfChanged() {
        int newLimit = parseLimit();
        int newOffset = parseOffset();
        if (newLimit == container.speedLimitSign.getSpeedLimitKmh()
                && newOffset == container.speedLimitSign.getStartOffsetBlocks()) {
            return;
        }

        container.speedLimitSign.setConfig(newLimit, newOffset);
        AradPacketHandler.CHANNEL.sendToServer(
                new PacketSpeedLimitSignConfig(pos, newLimit, newOffset));
    }

    private int parseLimit() {
        try {
            int v = Integer.parseInt(limitField.getText().trim());
            return Math.max(1, Math.min(360, v));
        } catch (Exception e) {
            return container.speedLimitSign.getSpeedLimitKmh();
        }
    }

    private int parseOffset() {
        try {
            int v = Integer.parseInt(offsetField.getText().trim());
            return Math.max(0, Math.min(5000, v));
        } catch (Exception e) {
            return container.speedLimitSign.getStartOffsetBlocks();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (limitField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                limitField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(null);
                return;
            }
            limitField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (offsetField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                offsetField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(null);
                return;
            }
            offsetField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        limitField.mouseClicked(mouseX, mouseY, mouseButton);
        offsetField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        limitField.updateCursorCounter();
        offsetField.updateCursorCounter();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1f, 1f, 1f, 1f);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF1A2A55);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF0D1B3E);

        fontRenderer.drawString("§f制限ブロック", guiLeft + 8, guiTop + 7, 0xFFFFFF);

        fontRenderer.drawString("§7制限速度:", guiLeft + 8, guiTop + 24, 0xCCDDFF);
        fontRenderer.drawString("§7km/h", guiLeft + 160, guiTop + 24, 0xCCDDFF);
        drawRect(guiLeft + 99, guiTop + 19, guiLeft + 157, guiTop + 37, 0xFF000000);
        limitField.drawTextBox();

        fontRenderer.drawString("§7開始地点:", guiLeft + 8, guiTop + 49, 0xCCDDFF);
        fontRenderer.drawString("§7blocks", guiLeft + 160, guiTop + 49, 0xCCDDFF);
        drawRect(guiLeft + 99, guiTop + 44, guiLeft + 157, guiTop + 62, 0xFF000000);
        offsetField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}
}
