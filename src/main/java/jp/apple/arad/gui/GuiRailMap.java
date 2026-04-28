package jp.apple.arad.gui;

import jp.apple.arad.cache.CachedRail;
import jp.apple.arad.data.*;
import jp.apple.arad.handler.AradKeyHandler;
import jp.apple.arad.handler.AradPacketHandler;
import jp.apple.arad.network.PacketConfirmRoute;
import jp.apple.arad.network.PacketRouteEdit;
import jp.apple.arad.network.PacketSpeedLimit;
import jp.apple.arad.speed.ClientSpeedLimitCache;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class GuiRailMap extends GuiScreen {

    private static final int BG = 0xFF0D1B3E;
    private static final int GRID = 0xFF1A2A55;
    private static final int TEXT = 0xFFCCDDFF;
    private static final int TEXT_DIM = 0xFF8899CC;
    private static final int PANEL_BG = 0xCC0A1530;
    private static final int BORDER = 0xFF2A3F70;
    private static final int PANEL_W = 170;
    private static final int BTN_ROUTE_MODE = 100;
    private static final int BTN_CANCEL = 101;
    private static final int BTN_CONFIRM = 102;
    private static final int BTN_DEL_BASE = 200;
    private static final int BTN_EDIT_BASE = 300;
    private static final int BTN_EDIT_SAVE = 400;
    private static final int BTN_EDIT_CANCEL = 401;
    private static final int BTN_SPEED_MODE = 500;
    private static final int BTN_SPEED_SAVE = 501;
    private static final int BTN_SPEED_CANCEL = 502;
    private static final int ROUTE_LIST_TOP = 68;
    private static final int ROUTE_ROW_STEP = 40;
    private static final int ROUTE_ROW_H = 36;
    private static final int ROUTE_LIST_BOTTOM_MARGIN = 56;
    private static final int STATION_HIT_R = 12;
    private static final int STATION_ICON = 7;
    private static final double RAIL_HIT_PX = 8.0;
    private final List<String> pendingIds = new ArrayList<>();
    private double offsetX = 0, offsetZ = 0;
    private double scale = 2.0;
    private boolean dragging = false;
    private int lastMX, lastMY;
    private int lastRouteCount = -1;
    private boolean routeCreateMode = false;
    private GuiTextField routeNameField;
    private int routeListScroll = 0;
    private boolean editDialogOpen = false;
    private String editRouteId = null;
    private String editRouteName = "";
    private GuiTextField editCountField;
    private boolean speedEditMode = false;
    private boolean speedInputOpen = false;
    private String speedInputKey = null;
    private GuiTextField speedInputField;
    private String hoveredCoreKey = null;

    private static double pointToSegmentDist(double px, double py,
                                             double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-6) return Math.hypot(px - x0, py - y0);
        double t = ((px - x0) * dx + (py - y0) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x0 + t * dx), py - (y0 + t * dy));
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h / 60f) % 6;
        switch (sector) {
            case 0:
                r = c;
                g = x;
                b = 0;
                break;
            case 1:
                r = x;
                g = c;
                b = 0;
                break;
            case 2:
                r = 0;
                g = c;
                b = x;
                break;
            case 3:
                r = 0;
                g = x;
                b = c;
                break;
            case 4:
                r = x;
                g = 0;
                b = c;
                break;
            default:
                r = c;
                g = 0;
                b = x;
                break;
        }
        return new float[]{r + m, g + m, b + m};
    }

    @Override
    public void initGui() {
        if (mc.player != null) {
            offsetX = mc.player.posX;
            offsetZ = mc.player.posZ;
        }
        routeListScroll = 0;
        lastRouteCount = MapData.INSTANCE.getRoutes().size();
        clampRouteListScroll();
        rebuildButtons();
    }

    private void rebuildButtons() {
        buttonList.clear();

        if (editDialogOpen) {
            int dx = width - PANEL_W + 5;
            int dy = 160;
            buttonList.add(new GuiButton(BTN_EDIT_SAVE, dx, dy, (PANEL_W - 15) / 2, 20, "保存"));
            buttonList.add(new GuiButton(BTN_EDIT_CANCEL, dx + (PANEL_W - 15) / 2 + 5, dy, (PANEL_W - 15) / 2, 20, "閉じる"));
            if (editCountField == null) {
                editCountField = new GuiTextField(10, fontRenderer,
                        width - PANEL_W + 5, 135, PANEL_W - 10, 18);
                editCountField.setMaxStringLength(3);
                editCountField.setText("1");
                editCountField.setFocused(true);
            }
            return;
        }

        if (speedInputOpen) {
            int dx = width / 2 - 60;
            int dy = height / 2 + 10;
            buttonList.add(new GuiButton(BTN_SPEED_SAVE, dx, dy, 55, 20, "設定"));
            buttonList.add(new GuiButton(BTN_SPEED_CANCEL, dx + 60, dy, 55, 20, "キャンセル"));
            if (speedInputField == null) {
                speedInputField = new GuiTextField(20, fontRenderer,
                        width / 2 - 40, height / 2 - 5, 80, 18);
                speedInputField.setMaxStringLength(4);
                speedInputField.setText("120");
                speedInputField.setFocused(true);
            }
            return;
        }

        if (!routeCreateMode && !speedEditMode) {
            buttonList.add(new GuiButton(BTN_ROUTE_MODE,
                    width - PANEL_W + 5, height - 44, PANEL_W - 10, 20, "＋ 路線作成"));

            List<RouteSnapshot> routes = MapData.INSTANCE.getRoutes();
            clampRouteListScroll();
            int rows = getVisibleRouteRows();
            int drawCount = Math.min(rows, Math.max(0, routes.size() - routeListScroll));
            for (int row = 0; row < drawCount; row++) {
                int itemY = ROUTE_LIST_TOP + row * ROUTE_ROW_STEP;
                buttonList.add(new GuiButton(BTN_DEL_BASE + row, width - 38, itemY, 32, 14, "§c×"));
                buttonList.add(new GuiButton(BTN_EDIT_BASE + row, width - 38, itemY + 15, 32, 14, "編集"));
            }

        } else if (routeCreateMode) {
            buttonList.add(new GuiButton(BTN_CANCEL,
                    width - PANEL_W + 5, height - 44, (PANEL_W - 15) / 2, 20, "キャンセル"));
            buttonList.add(new GuiButton(BTN_CONFIRM,
                    width - PANEL_W + 5 + (PANEL_W - 15) / 2 + 5, height - 44,
                    (PANEL_W - 15) / 2, 20, "確定 ✓"));

        } else {

            buttonList.add(new GuiButton(BTN_CANCEL,
                    width - PANEL_W + 5, height - 30, PANEL_W - 10, 20, "制限設定 終了"));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        List<CachedRail> rails = MapData.INSTANCE.getRails();
        List<FormationSnapshot> formations = MapData.INSTANCE.getFormations();
        List<PlayerSnapshot> players = MapData.INSTANCE.getPlayers();
        List<StationSnapshot> stations = MapData.INSTANCE.getStations();
        List<RouteSnapshot> routes = MapData.INSTANCE.getRoutes();

        int mapW = width - PANEL_W;


        if (speedEditMode && !speedInputOpen) {
            updateHoveredRail(mouseX, mouseY, mapW, rails);
        } else {
            hoveredCoreKey = null;
        }

        drawRect(0, 0, mapW, height, BG);

        drawGrid(mapW);
        drawRouteLines(routes, stations, mapW);
        drawPendingRoutePreview(stations, mapW);
        drawRails(rails, mapW);
        drawFormations(formations, mapW);
        drawPlayers(players, mapW);
        drawStations(stations, routes, mouseX, mouseY, mapW);

        drawRightPanel(routes, stations, mouseX, mouseY, mapW);
        drawInfoPanel(formations, stations, routes);

        if (speedInputOpen) {
            drawSpeedInputDialog();
        }

        String coord = String.format("X %.1f  Z %.1f", toWorldX(mouseX), toWorldZ(mouseY));
        drawString(fontRenderer, coord, 6, height - 11, TEXT_DIM);
        drawScaleBar(mapW);

        if (routeCreateMode && routeNameField != null) routeNameField.drawTextBox();
        if (editDialogOpen && editCountField != null) editCountField.drawTextBox();
        if (speedInputOpen && speedInputField != null) speedInputField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partial);
    }

    private void updateHoveredRail(int mx, int my, int mapW, List<CachedRail> rails) {
        if (mx >= mapW) {
            hoveredCoreKey = null;
            return;
        }
        double bestDist = RAIL_HIT_PX;
        CachedRail bestSeg = null;
        for (CachedRail seg : rails) {
            for (int i = 0; i < seg.size() - 1; i++) {
                int sx0 = toSX(seg.xPoints[i]), sz0 = toSZ(seg.zPoints[i]);
                int sx1 = toSX(seg.xPoints[i + 1]), sz1 = toSZ(seg.zPoints[i + 1]);
                double d = pointToSegmentDist(mx, my, sx0, sz0, sx1, sz1);
                if (d < bestDist) {
                    bestDist = d;
                    bestSeg = seg;
                }
            }
        }
        hoveredCoreKey = bestSeg != null
                ? (bestSeg.coreX + ":" + bestSeg.coreY + ":" + bestSeg.coreZ)
                : null;
    }

    private void drawSpeedLimitSigns(int mapW) {
        int dim = mc.world != null ? mc.world.provider.getDimension() : 0;

        for (Map.Entry<String, Integer> entry : ClientSpeedLimitCache.INSTANCE.getAllEntries()) {
            String key = entry.getKey();
            int kmh = entry.getValue();

            String[] parts = key.split(":");
            if (parts.length != 4) continue;
            try {
                int kDim = Integer.parseInt(parts[0]);
                if (kDim != dim) continue;
                double wx = Double.parseDouble(parts[1]) + 0.5;
                double wz = Double.parseDouble(parts[3]) + 0.5;

                int sx = toSX(wx);
                int sz = toSZ(wz);
                if (sx < 0 || sx >= mapW || sz < 0 || sz >= height) continue;

                drawSpeedSign(sx, sz - 14, kmh);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void drawSpeedSign(int sx, int sy, int kmh) {
        String label = String.valueOf(kmh);
        int tw = fontRenderer.getStringWidth(label);
        int pw = tw + 4;
        int ph = 11;

        drawRect(sx - pw / 2, sy, sx + pw / 2 + 1, sy + ph, 0xFFFFFFFF);
        drawRect(sx - pw / 2, sy, sx + pw / 2 + 1, sy + 1, 0xFFCC0000);
        drawRect(sx - pw / 2, sy + ph - 1, sx + pw / 2 + 1, sy + ph, 0xFFCC0000);
        drawRect(sx - pw / 2, sy, sx - pw / 2 + 1, sy + ph, 0xFFCC0000);
        drawRect(sx + pw / 2, sy, sx + pw / 2 + 1, sy + ph, 0xFFCC0000);
        fontRenderer.drawString(label, sx - tw / 2, sy + 2, 0xFF000000);
    }

    private void drawSpeedInputDialog() {
        int dw = 200, dh = 80;
        int dx = (width - dw) / 2;
        int dy = (height - dh) / 2;

        drawRect(dx, dy, dx + dw, dy + dh, 0xEE0A1530);
        drawRect(dx, dy, dx + dw, dy + 1, BORDER);
        drawRect(dx, dy + dh - 1, dx + dw, dy + dh, BORDER);
        drawRect(dx, dy, dx + 1, dy + dh, BORDER);
        drawRect(dx + dw - 1, dy, dx + dw, dy + dh, BORDER);

        drawCenteredString(fontRenderer, "§l制限速度を設定 (km/h)", width / 2, dy + 8, 0xFFFFFFFF);
        drawCenteredString(fontRenderer, "§70 で制限解除", width / 2, dy + 20, TEXT_DIM);

        if (speedInputField != null) speedInputField.drawTextBox();
    }

    private void drawGrid(int mapW) {
        double gs = 16.0;
        double gx0 = Math.floor(toWorldX(0) / gs) * gs;
        double gz0 = Math.floor(toWorldZ(0) / gs) * gs;
        for (double gx = gx0; gx <= toWorldX(mapW); gx += gs) {
            int sx = toSX(gx);
            if (sx >= mapW) continue;
            drawVerticalLine(sx, 0, height, GRID);
            if (gx % 64 == 0 && sx > 4 && sx < mapW - 24)
                drawString(fontRenderer, (int) gx + "", sx + 2, 2, 0x338899CC);
        }
        for (double gz = gz0; gz <= toWorldZ(height); gz += gs) {
            int sz = toSZ(gz);
            drawHorizontalLine(0, mapW, sz, GRID);
            if (gz % 64 == 0 && sz > 12 && sz < height - 4)
                drawString(fontRenderer, (int) gz + "", 2, sz - 4, 0x338899CC);
        }
    }

    private void drawRails(List<CachedRail> rails, int mapW) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);


        GL11.glLineWidth(speedEditMode ? 2.5f : 1.8f);
        GL11.glBegin(GL11.GL_LINES);
        if (speedEditMode)
            GL11.glColor4f(1f, 0.85f, 0.2f, 0.9f);
        else
            GL11.glColor4f(1f, 1f, 1f, 0.85f);
        for (CachedRail seg : rails) {

            if (speedEditMode && hoveredCoreKey != null) {
                String ck = seg.coreX + ":" + seg.coreY + ":" + seg.coreZ;
                if (hoveredCoreKey.equals(ck)) continue;
            }
            for (int i = 0; i < seg.size() - 1; i++) {
                int sx0 = toSX(seg.xPoints[i]), sz0 = toSZ(seg.zPoints[i]);
                int sx1 = toSX(seg.xPoints[i + 1]), sz1 = toSZ(seg.zPoints[i + 1]);
                if (offScreen(sx0, sz0, mapW) && offScreen(sx1, sz1, mapW)) continue;
                GL11.glVertex2i(sx0, sz0);
                GL11.glVertex2i(sx1, sz1);
            }
        }
        GL11.glEnd();


        if (speedEditMode && hoveredCoreKey != null) {
            GL11.glLineWidth(5.0f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            for (CachedRail seg : rails) {
                String ck = seg.coreX + ":" + seg.coreY + ":" + seg.coreZ;
                if (!hoveredCoreKey.equals(ck)) continue;
                for (int i = 0; i < seg.size() - 1; i++) {
                    int sx0 = toSX(seg.xPoints[i]), sz0 = toSZ(seg.zPoints[i]);
                    int sx1 = toSX(seg.xPoints[i + 1]), sz1 = toSZ(seg.zPoints[i + 1]);
                    if (offScreen(sx0, sz0, mapW) && offScreen(sx1, sz1, mapW)) continue;
                    GL11.glVertex2i(sx0, sz0);
                    GL11.glVertex2i(sx1, sz1);
                }
            }
            GL11.glEnd();
        }

        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
    }

    private void drawRouteLines(List<RouteSnapshot> routes,
                                List<StationSnapshot> stations, int mapW) {
        if (routes.isEmpty()) return;
        GlStateManager.disableTexture2D();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.2f);
        for (int ri = 0; ri < routes.size(); ri++) {
            RouteSnapshot route = routes.get(ri);
            float hue = (ri * 137.5f) % 360f;
            float[] rgb = hsvToRgb(hue, 0.8f, 1.0f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(rgb[0], rgb[1], rgb[2], 0.7f);
            for (String sid : route.stationIds) {
                StationSnapshot st = findStation(sid, stations);
                if (st == null) continue;
                int sx = toSX(st.x), sz = toSZ(st.z);
                if (sx > mapW) continue;
                GL11.glVertex2i(sx, sz);
            }
            GL11.glEnd();
        }
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
    }

    private void drawPendingRoutePreview(List<StationSnapshot> stations, int mapW) {
        if (!routeCreateMode || pendingIds.size() < 2) return;
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(2.5f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(1f, 0.85f, 0.2f, 0.9f);
        for (String sid : pendingIds) {
            StationSnapshot st = findStation(sid, stations);
            if (st == null) continue;
            GL11.glVertex2i(toSX(st.x), toSZ(st.z));
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
    }

    private void drawStations(List<StationSnapshot> stations,
                              List<RouteSnapshot> routes,
                              int mouseX, int mouseY, int mapW) {
        for (StationSnapshot st : stations) {
            int sx = toSX(st.x), sz = toSZ(st.z);
            if (sx >= mapW || offScreen(sx, sz, mapW)) continue;

            boolean isPending = pendingIds.contains(st.id);
            boolean isHovered = routeCreateMode
                    && Math.abs(mouseX - sx) <= STATION_HIT_R
                    && Math.abs(mouseY - sz) <= STATION_HIT_R;
            boolean hasRoute = routes.stream().anyMatch(r -> r.stationIds.contains(st.id));

            int fillColor, borderColor;
            if (isPending) {
                fillColor = 0xFFFFCC33;
                borderColor = 0xFFFFFFAA;
            } else if (isHovered) {
                fillColor = 0xFFFFFF66;
                borderColor = 0xFFFFFFFF;
            } else if (hasRoute) {
                fillColor = 0xFF33DD88;
                borderColor = 0xFF88FFCC;
            } else {
                fillColor = 0xFF2299CC;
                borderColor = 0xFF66DDFF;
            }

            int r = STATION_ICON;
            drawRect(sx - r - 1, sz - r - 1, sx + r + 2, sz + r + 2, borderColor);
            drawRect(sx - r, sz - r, sx + r + 1, sz + r + 1, fillColor);
            drawRect(sx - r, sz - r, sx + r + 1, sz - r + 1, 0x44FFFFFF);
            drawRect(sx - r, sz - r, sx - r + 1, sz + r + 1, 0x44FFFFFF);

            if (isPending) {
                int num = pendingIds.indexOf(st.id) + 1;
                String ns = String.valueOf(num);
                drawString(fontRenderer, ns, sx - fontRenderer.getStringWidth(ns) / 2, sz - 4, 0xFF000000);
            }

            String label = st.name;
            int lw = fontRenderer.getStringWidth(label);
            drawRect(sx - lw / 2 - 1, sz + r + 2, sx + lw / 2 + 2, sz + r + 12, 0xAA000000);
            drawString(fontRenderer, label, sx - lw / 2, sz + r + 3,
                    isPending ? 0xFFFFDD44 : (hasRoute ? 0xFF88FFCC : 0xFF66DDFF));
        }
    }

    private void drawRightPanel(List<RouteSnapshot> routes,
                                List<StationSnapshot> stations,
                                int mouseX, int mouseY, int mapW) {
        drawRect(mapW, 0, width, height, 0xFF0A1225);
        drawRect(mapW, 0, mapW + 1, height, 0xFF2A3F70);

        int px = mapW + 6;
        int py = 6;

        drawString(fontRenderer, "§lArad", px, py, 0xFFFFFFFF);
        py += 14;

        if (editDialogOpen) {
            drawRect(mapW + 2, py - 2, width - 2, py + 22, 0xFF1A2A55);
            drawRect(mapW + 2, py - 2, width - 2, py - 1, 0xFF44DDFF);
            drawString(fontRenderer, "§e路線編集: " + editRouteName, px, py, 0xFFFFDD44);
            py += 12;
            drawString(fontRenderer, "§7同時運行本数:", px, py, TEXT_DIM);

        } else if (routeCreateMode) {
            drawRect(mapW + 2, py - 2, width - 2, py + 22, 0xFF1A2A55);
            drawRect(mapW + 2, py - 2, width - 2, py - 1, 0xFF44DDFF);
            drawString(fontRenderer, "§e路線作成モード", px, py + 2, 0xFFFFDD44);

        } else if (speedEditMode) {
            drawRect(mapW + 2, py - 2, width - 2, py + 22, 0xFF1A2A55);
            drawRect(mapW + 2, py - 2, width - 2, py - 1, 0xFFFFAA00);
            drawString(fontRenderer, "§e⚡ 制限速度設定モード", px, py, 0xFFFFDD44);
            py += 12;
            drawString(fontRenderer, "§7レールをクリックして", px, py, TEXT_DIM);
            py += 12;
            drawString(fontRenderer, "§7制限速度を設定", px, py, TEXT_DIM);
            py += 14;
            drawString(fontRenderer, "§7右クリック: 制限解除", px, py, TEXT_DIM);
        } else {
            drawString(fontRenderer, "§7路線リスト", px, py, TEXT_DIM);
            py += 14;
            drawHorizontalLine(mapW + 2, width - 2, py, BORDER);
            py += 6;

            if (routes.isEmpty()) {
                drawString(fontRenderer, "§7（路線なし）", px, py, TEXT_DIM);
            } else {
                clampRouteListScroll();
                int rows = getVisibleRouteRows();
                int drawCount = Math.min(rows, Math.max(0, routes.size() - routeListScroll));
                int start = routeListScroll;
                for (int row = 0; row < drawCount; row++) {
                    int routeIdx = start + row;
                    RouteSnapshot r = routes.get(routeIdx);
                    int itemY = ROUTE_LIST_TOP + row * ROUTE_ROW_STEP;

                    float hue = (routeIdx * 137.5f) % 360f;
                    float[] rgb = hsvToRgb(hue, 0.8f, 1.0f);
                    int lineColor = 0xFF000000
                            | ((int) (rgb[0] * 255) << 16)
                            | ((int) (rgb[1] * 255) << 8)
                            | (int) (rgb[2] * 255);

                    drawRect(px, itemY, px + 3, itemY + 32, lineColor);
                    String trimmedName = fontRenderer.trimStringToWidth(r.name, PANEL_W - 55);
                    drawString(fontRenderer, "§l" + trimmedName, px + 8, itemY + 2, 0xFFFFFFFF);
                    String status = "§7" + r.stationIds.size() + "駅  "
                            + (r.trainCount > 0 ? "§a●" + r.trainCount + "本運行" : "§7○停止");
                    drawString(fontRenderer, status, px + 8, itemY + 14, TEXT_DIM);
                    drawHorizontalLine(mapW + 2, width - 2, itemY + 36, 0xFF1A2A55);
                }
                if (routes.size() > rows && rows > 0) {
                    int from = start + 1;
                    int to = start + drawCount;
                    String page = "§7" + from + "-" + to + "/" + routes.size();
                    drawString(fontRenderer, page, px, ROUTE_LIST_TOP - 10, TEXT_DIM);
                }
            }
        }
    }

    private void drawInfoPanel(List<FormationSnapshot> formations,
                               List<StationSnapshot> stations,
                               List<RouteSnapshot> routes) {
        drawRect(6, 6, 190, 58, PANEL_BG);
        drawRect(6, 6, 190, 7, BORDER);
        long alive = formations.stream().filter(f -> !f.cars.isEmpty()).count();
        int cars = formations.stream().mapToInt(FormationSnapshot::carCount).sum();
        drawString(fontRenderer, "§lArad", 12, 12, 0xFFFFFFFF);
        drawString(fontRenderer, "編成: " + alive + " 本", 12, 26, TEXT);
        drawString(fontRenderer, "車両: " + cars + " 両", 12, 38, TEXT);
        drawString(fontRenderer, String.format("縮尺 1:%.0f", 16.0 / scale), 12, 50, TEXT_DIM);
    }

    private void drawFormations(List<FormationSnapshot> formations, int mapW) {
        for (FormationSnapshot f : formations) {
            if (f.cars.isEmpty()) continue;
            float[] col = speedColor(f.speed);
            if (f.cars.size() > 1) {
                GlStateManager.disableTexture2D();
                GL11.glLineWidth(2.5f);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glColor4f(col[0], col[1], col[2], 0.5f);
                for (float[] car : f.cars) GL11.glVertex2i(toSX(car[0]), toSZ(car[1]));
                GL11.glEnd();
                GL11.glLineWidth(1f);
                GlStateManager.enableTexture2D();
            }
            for (int i = 0; i < f.cars.size(); i++) {
                float[] car = f.cars.get(i);
                int sx = toSX(car[0]), sz = toSZ(car[1]);
                if (offScreen(sx, sz, mapW)) continue;
                drawCarMarker(sx, sz, car[2], col, i == 0);
            }
            float[] front = f.cars.get(0);
            int sx = toSX(front[0]), sz = toSZ(front[1]);
            if (!offScreen(sx, sz, mapW)) {
                String idStr = f.idLabel();
                String spdStr = f.speedLabel();
                int lw = Math.max(fontRenderer.getStringWidth(idStr), fontRenderer.getStringWidth(spdStr));
                int lx = sx - lw / 2;
                drawString(fontRenderer, idStr, lx, sz - 22, 0xFFFFFFFF);
                drawString(fontRenderer, spdStr, lx, sz - 13, speedTextColor(f.speed));
                drawString(fontRenderer, f.carCount() + "両", lx, sz - 4, TEXT_DIM);
            }
        }
    }

    private void drawCarMarker(int sx, int sz, float yaw, float[] col, boolean front) {
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();
        GlStateManager.translate(sx, sz, 0);
        GlStateManager.rotate(yaw, 0, 0, 1);
        GL11.glBegin(GL11.GL_TRIANGLES);
        if (front) {
            GL11.glColor4f(col[0], col[1], col[2], 1f);
            GL11.glVertex2f(0, -8);
            GL11.glVertex2f(-5, 5);
            GL11.glVertex2f(5, 5);
        } else {
            GL11.glColor4f(col[0], col[1], col[2], 0.65f);
            GL11.glVertex2f(0, -5);
            GL11.glVertex2f(-3, 3);
            GL11.glVertex2f(3, 3);
        }
        GL11.glEnd();
        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
    }

    private void drawPlayers(List<PlayerSnapshot> players, int mapW) {
        String myName = (mc.player != null) ? mc.player.getName() : null;
        for (PlayerSnapshot p : players) {
            int sx = toSX(p.x), sz = toSZ(p.z);
            if (offScreen(sx, sz, mapW)) continue;
            boolean isMe = p.name.equals(myName);
            GlStateManager.disableTexture2D();
            GL11.glPointSize(isMe ? 10f : 6f);
            GL11.glBegin(GL11.GL_POINTS);
            if (isMe) GL11.glColor4f(0.25f, 0.85f, 1f, 1f);
            else GL11.glColor4f(1f, 1f, 0.4f, 0.9f);
            GL11.glVertex2i(sx, sz);
            GL11.glEnd();
            GL11.glPointSize(1f);
            GlStateManager.enableTexture2D();
            String label = isMe ? "§b" + p.name : p.name;
            int lw = fontRenderer.getStringWidth(p.name);
            drawString(fontRenderer, label, sx - lw / 2, sz + 7, isMe ? 0xFF7BC8FF : 0xFFFFFF66);
        }
    }

    private void drawScaleBar(int mapW) {
        int blen = 60, bx = mapW - 80, by = height - 24;
        double blocks = blen / scale;
        String label = blocks >= 1000 ? String.format("%.1fkm", blocks / 1000) : String.format("%.0fm", blocks);
        drawRect(bx, by + 9, bx + blen, by + 11, 0xFFFFFFFF);
        drawVerticalLine(bx, by + 7, by + 13, 0xFFFFFFFF);
        drawVerticalLine(bx + blen, by + 7, by + 13, 0xFFFFFFFF);
        drawString(fontRenderer, label, bx + blen / 2 - fontRenderer.getStringWidth(label) / 2, by, TEXT);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;

        if (id == BTN_ROUTE_MODE) {
            routeCreateMode = true;
            speedEditMode = false;
            pendingIds.clear();
            routeNameField = new GuiTextField(0, fontRenderer, width - PANEL_W + 5, 92, PANEL_W - 10, 18);
            routeNameField.setMaxStringLength(32);
            routeNameField.setText("新路線");
            rebuildButtons();

        } else if (id == BTN_CANCEL) {
            routeCreateMode = false;
            speedEditMode = false;
            editDialogOpen = false;
            editCountField = null;
            hoveredCoreKey = null;
            pendingIds.clear();
            routeNameField = null;
            rebuildButtons();

        } else if (id == BTN_CONFIRM) {
            confirmRoute();

        } else if (id == BTN_EDIT_SAVE) {
            saveEditDialog();

        } else if (id == BTN_EDIT_CANCEL) {
            editDialogOpen = false;
            editCountField = null;
            editRouteId = null;
            rebuildButtons();

        } else if (id >= BTN_DEL_BASE && id < BTN_EDIT_BASE) {
            int row = id - BTN_DEL_BASE;
            int idx = routeListScroll + row;
            List<RouteSnapshot> routes = MapData.INSTANCE.getRoutes();
            if (idx < routes.size()) {
                AradPacketHandler.CHANNEL.sendToServer(
                        PacketRouteEdit.deleteRoute(routes.get(idx).id));
            }

        } else if (id >= BTN_EDIT_BASE) {
            int row = id - BTN_EDIT_BASE;
            int idx = routeListScroll + row;
            List<RouteSnapshot> routes = MapData.INSTANCE.getRoutes();
            if (idx < routes.size()) {
                openEditDialog(routes.get(idx));
            }
        }
    }

    private void openEditDialog(RouteSnapshot route) {
        editDialogOpen = true;
        editRouteId = route.id;
        editRouteName = route.name;
        editCountField = null;
        rebuildButtons();
        if (editCountField != null) editCountField.setText(String.valueOf(route.trainCount));
    }

    private void saveEditDialog() {
        if (editRouteId == null || editCountField == null) return;
        try {
            int count = Integer.parseInt(editCountField.getText().trim());
            AradPacketHandler.CHANNEL.sendToServer(
                    PacketRouteEdit.setTrainCount(editRouteId, count));
        } catch (NumberFormatException ignored) {
        }
        editDialogOpen = false;
        editCountField = null;
        editRouteId = null;
        rebuildButtons();
    }

    private void saveSpeedInput() {
        if (speedInputKey == null || speedInputField == null) return;
        try {
            int kmh = Integer.parseInt(speedInputField.getText().trim());
            AradPacketHandler.CHANNEL.sendToServer(PacketSpeedLimit.change(speedInputKey, kmh));
        } catch (NumberFormatException ignored) {
        }
        speedInputOpen = false;
        speedInputKey = null;
        speedInputField = null;
        rebuildButtons();
    }

    private void confirmRoute() {
        if (pendingIds.size() < 2) return;
        String name = (routeNameField != null && !routeNameField.getText().trim().isEmpty())
                ? routeNameField.getText().trim() : "新路線";
        AradPacketHandler.CHANNEL.sendToServer(
                new PacketConfirmRoute(name, new ArrayList<>(pendingIds)));
        routeCreateMode = false;
        pendingIds.clear();
        routeNameField = null;
        rebuildButtons();
    }

    private void handleSpeedClick(int mx, int my, boolean rightClick, int mapW) {
        if (mx >= mapW) return;
        if (speedInputOpen) return;


        if (hoveredCoreKey == null) return;

        int dim = mc.world != null ? mc.world.provider.getDimension() : 0;

        String key = dim + ":" + hoveredCoreKey;

        if (rightClick) {
            AradPacketHandler.CHANNEL.sendToServer(PacketSpeedLimit.change(key, 0));
        } else {
            speedInputKey = key;
            speedInputOpen = true;
            speedInputField = null;
            rebuildButtons();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            boolean onRightPanel = mx >= width - PANEL_W;
            if (onRightPanel && !routeCreateMode && !speedEditMode && !editDialogOpen && !speedInputOpen) {
                int old = routeListScroll;
                routeListScroll += (scroll > 0) ? -1 : 1;
                clampRouteListScroll();
                if (old != routeListScroll) rebuildButtons();
                return;
            }
            scale = Math.max(0.15, Math.min(20.0, scale * (scroll > 0 ? 1.25 : 0.8)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (routeCreateMode && routeNameField != null) routeNameField.mouseClicked(mx, my, btn);
        if (editDialogOpen && editCountField != null) editCountField.mouseClicked(mx, my, btn);
        if (speedInputOpen && speedInputField != null) speedInputField.mouseClicked(mx, my, btn);

        int mapW = width - PANEL_W;

        if (speedEditMode && !speedInputOpen && mx < mapW) {
            handleSpeedClick(mx, my, btn == 1, mapW);
            return;
        }

        if (routeCreateMode && mx < mapW) {
            List<StationSnapshot> stations = MapData.INSTANCE.getStations();
            for (StationSnapshot st : stations) {
                int sx = toSX(st.x), sz = toSZ(st.z);
                if (Math.abs(mx - sx) <= STATION_HIT_R && Math.abs(my - sz) <= STATION_HIT_R) {
                    if (btn == 0 && !pendingIds.contains(st.id)) pendingIds.add(st.id);
                    else if (btn == 1) pendingIds.remove(st.id);
                    return;
                }
            }
        }

        super.mouseClicked(mx, my, btn);
        if (btn == 0 && mx < mapW && !speedInputOpen && !editDialogOpen) {
            dragging = true;
            lastMX = mx;
            lastMY = my;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (state == 0) dragging = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (dragging) {
            offsetX -= (mx - lastMX) / scale;
            offsetZ -= (my - lastMY) / scale;
            lastMX = mx;
            lastMY = my;
        }
    }

    @Override
    protected void keyTyped(char ch, int keyCode) throws IOException {
        if (speedInputOpen && speedInputField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                saveSpeedInput();
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                speedInputOpen = false;
                speedInputKey = null;
                speedInputField = null;
                rebuildButtons();
                return;
            }
            speedInputField.textboxKeyTyped(ch, keyCode);
            return;
        }
        if (editDialogOpen && editCountField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                saveEditDialog();
                return;
            }
            editCountField.textboxKeyTyped(ch, keyCode);
            return;
        }
        if (routeCreateMode && routeNameField != null && routeNameField.isFocused()) {
            routeNameField.textboxKeyTyped(ch, keyCode);
            return;
        }
        if (keyCode == AradKeyHandler.KEY_OPEN_MAP.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(ch, keyCode);
    }

    @Override
    public void updateScreen() {
        if (routeCreateMode && routeNameField != null) routeNameField.updateCursorCounter();
        if (editDialogOpen && editCountField != null) editCountField.updateCursorCounter();
        if (speedInputOpen && speedInputField != null) speedInputField.updateCursorCounter();

        int currentCount = MapData.INSTANCE.getRoutes().size();
        if (currentCount != lastRouteCount) {
            lastRouteCount = currentCount;
            clampRouteListScroll();
            if (!editDialogOpen && !speedInputOpen) rebuildButtons();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int toSX(double wx) {
        return (int) ((wx - offsetX) * scale + (width - PANEL_W) / 2.0);
    }

    private int toSZ(double wz) {
        return (int) ((wz - offsetZ) * scale + height / 2.0);
    }

    private double toWorldX(int sx) {
        return (sx - (width - PANEL_W) / 2.0) / scale + offsetX;
    }

    private double toWorldZ(int sz) {
        return (sz - height / 2.0) / scale + offsetZ;
    }

    private boolean offScreen(int sx, int sz, int mapW) {
        return sx < -64 || sx > mapW + 64 || sz < -64 || sz > height + 64;
    }

    private float[] speedColor(float spd) {
        float kmh = Math.abs(spd) * 72f;
        if (kmh < 1f) return new float[]{0.6f, 0.6f, 0.6f};
        if (kmh < 75f) return new float[]{1f, 1f, 0f};
        return new float[]{1f, 0.2f, 0.2f};
    }

    private int speedTextColor(float spd) {
        float kmh = Math.abs(spd) * 72f;
        if (kmh < 1f) return 0xFFAAAAAA;
        if (kmh < 75f) return 0xFFFFDD44;
        return 0xFFFF5555;
    }

    private StationSnapshot findStation(String id, List<StationSnapshot> list) {
        for (StationSnapshot s : list) if (id.equals(s.id)) return s;
        return null;
    }

    private int getVisibleRouteRows() {
        int rows = 0;
        for (int y = ROUTE_LIST_TOP; y + ROUTE_ROW_H <= height - ROUTE_LIST_BOTTOM_MARGIN; y += ROUTE_ROW_STEP) {
            rows++;
        }
        return Math.max(0, rows);
    }

    private void clampRouteListScroll() {
        int total = MapData.INSTANCE.getRoutes().size();
        int maxScroll = Math.max(0, total - getVisibleRouteRows());
        if (routeListScroll < 0) routeListScroll = 0;
        if (routeListScroll > maxScroll) routeListScroll = maxScroll;
    }
}
