package jp.apple.arad.gui;

import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.network.PacketSectionMarkerConfig;
import jp.apple.arad.section.SectionSlot;
import jp.apple.arad.section.TileEntitySectionMarker;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiSectionMarker extends GuiScreen {

    private static final int GUI_W = 460;
    private static final int GUI_H = 210;
    private static final int LEFT_W = 140;
    private static final int RIGHT_X = LEFT_W + 1;
    private static final int RIGHT_W = GUI_W - RIGHT_X;
    private static final int FOOTER_H = 28;

    private static final int C_BG = 0xFF0D1B3E;
    private static final int C_PANEL = 0xFF0A1428;
    private static final int C_BORDER = 0xFF2A3F70;
    private static final int C_ACCENT = 0xFF44DDFF;
    private static final int C_SEL_BG = 0xFF1A3A6A;
    private static final int C_TEXT = 0xFFCCDDFF;
    private static final int C_DIM = 0xFF8899CC;
    private static final int C_FIELD_BG = 0xFF060E22;
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int C_YELLOW = 0xFFFFDD44;

    private static final int BTN_ADD = 10;
    private static final int BTN_SAVE = 11;
    private static final int BTN_CANCEL = 12;
    private static final int BTN_PASTE_SIG = 20;
    private static final int BTN_PASTE_PASS = 21;
    private static final int BTN_DEL_BASE = 100;

    private static final int LIST_TOP = 22;
    private static final int ROW_H = 18;
    private static final int MAX_ROWS = (GUI_H - LIST_TOP - FOOTER_H) / ROW_H;
    private static final int DLH = 20;
    private static final int LBL_W = 58;
    private static final int FW = 44;
    private static final int FG = 4;
    private static final int AX_W = 12;
    private static final int VBW = 18;

    private final TileEntitySectionMarker te;
    private final BlockPos pos;
    private final List<SectionSlot> editSlots = new ArrayList<>();
    private int selectedSlot = -1;

    private GuiTextField fieldName;
    private GuiTextField fieldSigX, fieldSigY, fieldSigZ;
    private GuiTextField fieldPassX, fieldPassZ;

    private int gx, gy;

    public GuiSectionMarker(TileEntitySectionMarker te, BlockPos pos) {
        this.te = te;
        this.pos = pos;
        for (SectionSlot s : te.getSlots())
            editSlots.add(s.copy());
    }

    @Override
    public void initGui() {
        gx = (width - GUI_W) / 2;
        gy = (height - GUI_H) / 2;
        buttonList.clear();

        if (selectedSlot < 0 && !editSlots.isEmpty()) {
            selectedSlot = 0;
        }

        int fy = gy + GUI_H - FOOTER_H + 4;
        buttonList.add(new GuiButton(BTN_ADD, gx + 6, fy, 100, 20, "＋ スロット追加"));
        buttonList.add(new GuiButton(BTN_SAVE, gx + GUI_W - 200, fy, 94, 20, "保存"));
        buttonList.add(new GuiButton(BTN_CANCEL, gx + GUI_W - 100, fy, 94, 20, "キャンセル"));

        for (int i = 0; i < editSlots.size() && i < MAX_ROWS; i++) {
            int ry = gy + LIST_TOP + i * ROW_H;
            buttonList.add(new GuiButton(BTN_DEL_BASE + i,
                    gx + LEFT_W - 22, ry + 2, 20, 14, "×"));
        }

        buildDetailFields();
    }

    private void buildDetailFields() {

        int rx = gx + RIGHT_X + 8;

        int row0Y = gy + 20 + DLH;
        int row1Y = row0Y + DLH + 2;
        int row2Y = row1Y + DLH + 2;

        int nameFx = rx + LBL_W;
        int nameFw = GUI_W - RIGHT_X - LBL_W - 16;
        fieldName = tf(nameFx, row0Y, nameFw, "");

        int sigX0 = rx + LBL_W + AX_W;
        fieldSigX = tf(sigX0, row1Y, FW, "0");
        fieldSigY = tf(sigX0 + FW + AX_W + FG, row1Y, FW, "64");
        fieldSigZ = tf(sigX0 + (FW + AX_W + FG) * 2, row1Y, FW, "0");

        int pasX0 = rx + LBL_W + AX_W;
        fieldPassX = tf(pasX0, row2Y, FW, "0");
        fieldPassZ = tf(pasX0 + FW + AX_W + FG, row2Y, FW, "0");

        if (selectedSlot >= 0 && selectedSlot < editSlots.size()) {
            int sigVx = fieldSigZ.x + FW + FG;
            buttonList.add(new GuiButton(BTN_PASTE_SIG, sigVx, row1Y - 1, VBW, 16, "V"));
            int pasVx = fieldPassZ.x + FW + FG;
            buttonList.add(new GuiButton(BTN_PASTE_PASS, pasVx, row2Y - 1, VBW, 16, "V"));
            loadToFields(editSlots.get(selectedSlot));
        }
    }

    private GuiTextField tf(int x, int y, int w, String def) {
        GuiTextField f = new GuiTextField(0, fontRenderer, x, y, w, 14);
        f.setMaxStringLength(10);
        f.setText(def);
        f.setCanLoseFocus(true);
        return f;
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawBackground(0);
        drawFrame();
        drawLeftPane();
        drawRightPane();
        super.drawScreen(mx, my, partial);
        drawFields();
    }

    private void drawFrame() {
        drawRect(gx, gy, gx + GUI_W, gy + GUI_H, C_BG);
        drawRect(gx, gy, gx + GUI_W, gy + 1, C_ACCENT);
        drawRect(gx, gy + GUI_H - 1, gx + GUI_W, gy + GUI_H, C_BORDER);
        drawRect(gx, gy, gx + 1, gy + GUI_H, C_BORDER);
        drawRect(gx + GUI_W - 1, gy, gx + GUI_W, gy + GUI_H, C_BORDER);
        drawString(fontRenderer, "§l閉塞設定", gx + 8, gy + 6, C_WHITE);

        drawRect(gx + LEFT_W, gy + 16, gx + LEFT_W + 1, gy + GUI_H - FOOTER_H, C_BORDER);

        drawRect(gx + 1, gy + GUI_H - FOOTER_H, gx + GUI_W - 1, gy + GUI_H - FOOTER_H + 1, C_BORDER);

        drawRect(gx + 1, gy + 16, gx + LEFT_W, gy + 22, C_PANEL);
        drawString(fontRenderer, "§7スロット", gx + 6, gy + 14, C_DIM);
    }

    private void drawLeftPane() {
        for (int i = 0; i < editSlots.size() && i < MAX_ROWS; i++) {
            SectionSlot s = editSlots.get(i);
            int ry = gy + LIST_TOP + i * ROW_H;
            boolean sel = (i == selectedSlot);
            if (sel) {
                drawRect(gx + 1, ry, gx + LEFT_W, ry + ROW_H, C_SEL_BG);
                drawRect(gx + 1, ry, gx + 3, ry + ROW_H, C_ACCENT);
            }
            String label = (s.name != null && !s.name.isEmpty())
                    ? String.format("%d. %s", i + 1, s.name)
                    : String.format("%d. (%d,%d)", i + 1, s.passX, s.passZ);
            label = fontRenderer.trimStringToWidth(label, LEFT_W - 30);
            drawString(fontRenderer, label, gx + 6, ry + 4, sel ? C_YELLOW : C_TEXT);
        }
        if (editSlots.isEmpty()) {
            drawString(fontRenderer, "§7（スロットなし）", gx + 6, gy + LIST_TOP + 4, C_DIM);
        }
    }

    private void drawRightPane() {
        int rx = gx + RIGHT_X + 8;

        if (selectedSlot < 0 || selectedSlot >= editSlots.size()) {
            drawCenteredString(fontRenderer, "§7スロットを選択してください",
                    gx + RIGHT_X + RIGHT_W / 2, gy + GUI_H / 2 - 10, C_DIM);
            return;
        }

        int row0Y = gy + 20 + DLH;
        int row1Y = row0Y + DLH + 2;
        int row2Y = row1Y + DLH + 2;

        drawString(fontRenderer, "§l詳細設定  §7スロット " + (selectedSlot + 1),
                rx, gy + 22, C_ACCENT);

        drawString(fontRenderer, "名前", rx, row0Y + 1, C_TEXT);
        if (fieldName != null)
            drawFieldBg(fieldName);

        drawString(fontRenderer, "信号座標", rx, row1Y + 1, C_TEXT);
        if (fieldSigX != null) {
            drawString(fontRenderer, "X:", fieldSigX.x - AX_W, row1Y + 1, C_DIM);
            drawFieldBg(fieldSigX);
        }
        if (fieldSigY != null) {
            drawString(fontRenderer, "Y:", fieldSigY.x - AX_W, row1Y + 1, C_DIM);
            drawFieldBg(fieldSigY);
        }
        if (fieldSigZ != null) {
            drawString(fontRenderer, "Z:", fieldSigZ.x - AX_W, row1Y + 1, C_DIM);
            drawFieldBg(fieldSigZ);
        }

        drawString(fontRenderer, "通過座標", rx, row2Y + 1, C_TEXT);
        if (fieldPassX != null) {
            drawString(fontRenderer, "X:", fieldPassX.x - AX_W, row2Y + 1, C_DIM);
            drawFieldBg(fieldPassX);
        }
        if (fieldPassZ != null) {
            drawString(fontRenderer, "Z:", fieldPassZ.x - AX_W, row2Y + 1, C_DIM);
            drawFieldBg(fieldPassZ);
        }
    }

    private void drawFieldBg(GuiTextField f) {
        drawRect(f.x - 1, f.y - 1, f.x + f.width + 1, f.y + f.height + 1, C_BORDER);
        drawRect(f.x, f.y, f.x + f.width, f.y + f.height, C_FIELD_BG);
    }

    private void drawFields() {
        if (selectedSlot < 0 || selectedSlot >= editSlots.size())
            return;
        if (fieldName != null)
            fieldName.drawTextBox();
        if (fieldSigX != null)
            fieldSigX.drawTextBox();
        if (fieldSigY != null)
            fieldSigY.drawTextBox();
        if (fieldSigZ != null)
            fieldSigZ.drawTextBox();
        if (fieldPassX != null)
            fieldPassX.drawTextBox();
        if (fieldPassZ != null)
            fieldPassZ.drawTextBox();
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        int id = btn.id;

        if (id == BTN_ADD) {
            flushFields();
            editSlots.add(new SectionSlot());
            selectedSlot = editSlots.size() - 1;
            initGui();

        } else if (id == BTN_SAVE) {
            flushFields();
            AradPacketHandler.CHANNEL.sendToServer(
                    new PacketSectionMarkerConfig(pos, editSlots));
            mc.displayGuiScreen(null);

        } else if (id == BTN_CANCEL) {
            mc.displayGuiScreen(null);

        } else if (id == BTN_PASTE_SIG) {
            pasteSig();

        } else if (id == BTN_PASTE_PASS) {
            pastePass();

        } else if (id >= BTN_DEL_BASE) {
            int idx = id - BTN_DEL_BASE;
            if (idx < editSlots.size()) {
                if (selectedSlot != idx)
                    flushFields();
                editSlots.remove(idx);
                if (selectedSlot >= editSlots.size())
                    selectedSlot = editSlots.size() - 1;
                initGui();
            }
        }
    }

    private int[] clip() {
        try {
            String raw = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (raw == null)
                return null;
            String[] p = raw.trim().split("[\\s,]+");
            if (p.length >= 3)
                return new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]) };
            if (p.length == 2)
                return new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };
        } catch (Exception ignored) {
        }
        return null;
    }

    private void pasteSig() {
        int[] v = clip();
        if (v == null)
            return;
        if (v.length >= 3) {
            if (fieldSigX != null)
                fieldSigX.setText(String.valueOf(v[0]));
            if (fieldSigY != null)
                fieldSigY.setText(String.valueOf(v[1]));
            if (fieldSigZ != null)
                fieldSigZ.setText(String.valueOf(v[2]));
        } else {
            if (fieldSigX != null)
                fieldSigX.setText(String.valueOf(v[0]));
            if (fieldSigZ != null)
                fieldSigZ.setText(String.valueOf(v[1]));
        }
    }

    private void pastePass() {
        int[] v = clip();
        if (v == null)
            return;
        if (v.length >= 3) {
            if (fieldPassX != null)
                fieldPassX.setText(String.valueOf(v[0]));
            if (fieldPassZ != null)
                fieldPassZ.setText(String.valueOf(v[2]));
        } else {
            if (fieldPassX != null)
                fieldPassX.setText(String.valueOf(v[0]));
            if (fieldPassZ != null)
                fieldPassZ.setText(String.valueOf(v[1]));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {

        fwdMouse(mx, my, btn);

        for (int i = 0; i < editSlots.size() && i < MAX_ROWS; i++) {
            int ry = gy + LIST_TOP + i * ROW_H;
            if (mx >= gx + 1 && mx < gx + LEFT_W - 22 && my >= ry && my < ry + ROW_H) {
                if (i != selectedSlot) {
                    flushFields();
                    selectedSlot = i;
                    loadToFields(editSlots.get(i));
                }
                return;
            }
        }

        super.mouseClicked(mx, my, btn);
    }

    private void fwdMouse(int mx, int my, int btn) {
        if (fieldName != null)
            fieldName.mouseClicked(mx, my, btn);
        if (fieldSigX != null)
            fieldSigX.mouseClicked(mx, my, btn);
        if (fieldSigY != null)
            fieldSigY.mouseClicked(mx, my, btn);
        if (fieldSigZ != null)
            fieldSigZ.mouseClicked(mx, my, btn);
        if (fieldPassX != null)
            fieldPassX.mouseClicked(mx, my, btn);
        if (fieldPassZ != null)
            fieldPassZ.mouseClicked(mx, my, btn);
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        passKey(fieldName, ch, key);
        passKey(fieldSigX, ch, key);
        passKey(fieldSigY, ch, key);
        passKey(fieldSigZ, ch, key);
        passKey(fieldPassX, ch, key);
        passKey(fieldPassZ, ch, key);
    }

    private void passKey(GuiTextField f, char ch, int key) {
        if (f != null && f.isFocused())
            f.textboxKeyTyped(ch, key);
    }

    @Override
    public void updateScreen() {
        if (fieldName != null)
            fieldName.updateCursorCounter();
        if (fieldSigX != null)
            fieldSigX.updateCursorCounter();
        if (fieldSigY != null)
            fieldSigY.updateCursorCounter();
        if (fieldSigZ != null)
            fieldSigZ.updateCursorCounter();
        if (fieldPassX != null)
            fieldPassX.updateCursorCounter();
        if (fieldPassZ != null)
            fieldPassZ.updateCursorCounter();
    }

    private void loadToFields(SectionSlot s) {
        if (fieldName != null)
            fieldName.setText(s.name != null ? s.name : "");
        if (fieldSigX != null)
            fieldSigX.setText(String.valueOf(s.signalPos.getX()));
        if (fieldSigY != null)
            fieldSigY.setText(String.valueOf(s.signalPos.getY()));
        if (fieldSigZ != null)
            fieldSigZ.setText(String.valueOf(s.signalPos.getZ()));
        if (fieldPassX != null)
            fieldPassX.setText(String.valueOf(s.passX));
        if (fieldPassZ != null)
            fieldPassZ.setText(String.valueOf(s.passZ));
    }

    private void flushFields() {
        if (selectedSlot < 0 || selectedSlot >= editSlots.size())
            return;
        SectionSlot s = editSlots.get(selectedSlot);
        if (fieldName != null)
            s.name = fieldName.getText();
        int sx = pi(fieldSigX, s.signalPos.getX());
        int sy = pi(fieldSigY, s.signalPos.getY());
        int sz = pi(fieldSigZ, s.signalPos.getZ());
        s.signalPos = new BlockPos(sx, sy, sz);
        s.passX = pi(fieldPassX, s.passX);
        s.passZ = pi(fieldPassZ, s.passZ);
    }

    private int pi(GuiTextField f, int fallback) {
        if (f == null)
            return fallback;
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}