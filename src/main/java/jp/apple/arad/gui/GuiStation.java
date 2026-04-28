package jp.apple.arad.gui;

import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.network.PacketStationConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiStation extends GuiContainer {

    private static final int GUI_W = 176;
    private static final int GUI_H = 200;

    private static final int BTN_DOOR_LEFT = 10;
    private static final int BTN_DOOR_RIGHT = 11;
    private static final int BTN_SPAWN_REVERSE = 12;

    private final ContainerStation container;
    private final BlockPos pos;

    private GuiTextField nameField;
    private GuiTextField dwellField;

    private boolean doorLeft;
    private boolean doorRight;
    private boolean spawnReversed;

    private GuiButton btnDoorLeft;
    private GuiButton btnDoorRight;
    private GuiButton btnSpawnReverse;

    public GuiStation(ContainerStation container, BlockPos pos) {
        super(container);
        this.container = container;
        this.pos = pos;
        this.xSize = GUI_W;
        this.ySize = GUI_H;

        this.doorLeft = container.station.isDoorLeft();
        this.doorRight = container.station.isDoorRight();
        this.spawnReversed = container.station.isSpawnReversed();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);


        int fx = guiLeft + 8;
        nameField = new GuiTextField(0, fontRenderer, fx, guiTop + 6, GUI_W - 16, 16);
        nameField.setMaxStringLength(32);
        nameField.setText(container.station.getStationName());
        nameField.setFocused(true);
        nameField.setCanLoseFocus(true);


        int dwellSec = container.station.getDwellTimeTicks() / 20;
        dwellField = new GuiTextField(1, fontRenderer, guiLeft + 80, guiTop + 66, 40, 16);
        dwellField.setMaxStringLength(4);
        dwellField.setText(String.valueOf(dwellSec));
        dwellField.setCanLoseFocus(true);


        buttonList.clear();
        int bw = 70, bh = 18;
        int by = guiTop + 40;
        btnDoorLeft = new GuiButton(BTN_DOOR_LEFT, guiLeft + 8, by, bw, bh, doorLeft ? "§a◀ 左ドア" : "§7◀ 左ドア");
        btnDoorRight = new GuiButton(BTN_DOOR_RIGHT, guiLeft + 8 + bw + 2, by, bw, bh, doorRight ? "§a右ドア ▶" : "§7右ドア ▶");
        btnSpawnReverse = new GuiButton(BTN_SPAWN_REVERSE, guiLeft + 133, guiTop + 66, 35, 16,
                spawnReversed ? "§a反転" : "§7反転");
        buttonList.add(btnDoorLeft);
        buttonList.add(btnDoorRight);
        buttonList.add(btnSpawnReverse);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        sendConfigIfChanged();
    }

    private void sendConfigIfChanged() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) newName = "駅";

        int dwellSec = parseDwellSec();
        int newTicks = dwellSec * 20;

        boolean changed = !newName.equals(container.station.getStationName())
                || doorLeft != container.station.isDoorLeft()
                || doorRight != container.station.isDoorRight()
                || spawnReversed != container.station.isSpawnReversed()
                || newTicks != container.station.getDwellTimeTicks();

        if (changed) {
            container.station.setStationName(newName);
            container.station.setDoorLeft(doorLeft);
            container.station.setDoorRight(doorRight);
            container.station.setSpawnReversed(spawnReversed);
            container.station.setDwellTimeTicks(newTicks);
            AradPacketHandler.CHANNEL.sendToServer(
                    new PacketStationConfig(pos, newName, doorLeft, doorRight, spawnReversed, newTicks));
        }
    }

    private int parseDwellSec() {
        try {
            int v = Integer.parseInt(dwellField.getText().trim());
            return Math.max(1, v);
        } catch (NumberFormatException e) {
            return container.station.getDwellTimeTicks() / 20;
        }
    }


    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DOOR_LEFT) {
            doorLeft = !doorLeft;
            btnDoorLeft.displayString = doorLeft ? "§a◀ 左ドア" : "§7◀ 左ドア";
        } else if (button.id == BTN_DOOR_RIGHT) {
            doorRight = !doorRight;
            btnDoorRight.displayString = doorRight ? "§a右ドア ▶" : "§7右ドア ▶";
        } else if (button.id == BTN_SPAWN_REVERSE) {
            spawnReversed = !spawnReversed;
            btnSpawnReverse.displayString = spawnReversed ? "§a反転" : "§7反転";
        }
    }


    @Override
    protected void keyTyped(char ch, int keyCode) throws IOException {
        if (nameField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                nameField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(null);
                return;
            }
            nameField.textboxKeyTyped(ch, keyCode);
        } else if (dwellField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                dwellField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(null);
                return;
            }
            dwellField.textboxKeyTyped(ch, keyCode);
        } else {
            super.keyTyped(ch, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        nameField.mouseClicked(mx, my, btn);
        dwellField.mouseClicked(mx, my, btn);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        nameField.updateCursorCounter();
        dwellField.updateCursorCounter();
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        GlStateManager.color(1f, 1f, 1f, 1f);


        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF1A2A55);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFF0D1B3E);


        drawRect(guiLeft + 7, guiTop + 5, guiLeft + xSize - 7, guiTop + 23, 0xFF000000);
        nameField.drawTextBox();


        fontRenderer.drawString("§7ドア向き:", guiLeft + 8, guiTop + 26, 0xCCDDFF);


        fontRenderer.drawString("§7停車時間:", guiLeft + 8, guiTop + 70, 0xCCDDFF);
        fontRenderer.drawString("§7秒", guiLeft + 123, guiTop + 70, 0xCCDDFF);
        drawRect(guiLeft + 79, guiTop + 66, guiLeft + 121, guiTop + 84, 0xFF000000);
        dwellField.drawTextBox();


        fontRenderer.drawString("§7編成:", guiLeft + 8, guiTop + 92, 0xCCDDFF);
        int sx = guiLeft + 79, sy = guiTop + 93;
        drawRect(sx - 1, sy - 1, sx + 19, sy + 19, 0xFF555555);
        drawRect(sx, sy, sx + 18, sy + 18, 0xFF2A2A2A);


        drawHorizontalLine(guiLeft + 8, guiLeft + xSize - 8, guiTop + 112, 0xFF2A3F70);
        fontRenderer.drawString("§7インベントリ", guiLeft + 8, guiTop + 115, 0x8899CC);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
    }
}
