package com.sussyclient.ui;

import com.sussyclient.Config;
import com.sussyclient.SussyClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** KR-client style dropdown: header -> module -> settings. */
public class SussyScreen extends Screen {
    private static boolean panelOpen = true;
    private static boolean settingsOpen = false;

    private static final int X = 30, Y = 30, W = 170, HEAD = 20, ROW = 17;
    private static final int HEADC = 0xFFB01E1E, ROWC = 0xE0202020, HOVER = 0xE0303030,
            ON = 0xFFB01E1E, TEXT = 0xFFFFFFFF, DIM = 0xFFAAAAAA;

    private final List<Row> rows = new ArrayList<>();
    private Row dragging = null;

    public SussyScreen() {
        super(Component.literal(SussyClient.NAME));
        Config c = Config.I;
        rows.add(new Slider("Sensitivity", 1, 30, 1, () -> (double) c.sensitivity, v -> c.sensitivity = v.intValue(), ""));
        rows.add(new Slider("Sim Distance", 2, 32, 1, () -> (double) c.simDistance, v -> c.simDistance = v.intValue(), " ch"));
        rows.add(new Slider("Below Y", -60, 64, 1, () -> (double) c.maxY, v -> c.maxY = v.intValue(), ""));
        rows.add(new Slider("Scan Speed", 1, 16, 1, () -> (double) c.scanSpeed, v -> c.scanSpeed = v.intValue(), "/tick"));
        rows.add(new Slider("Box Height", 1, 384, 1, () -> (double) c.boxHeight, v -> c.boxHeight = v.intValue(), ""));
        rows.add(new Slider("Chunk Size", 10, 100, 1, () -> (double) c.chunkSize, v -> c.chunkSize = v.intValue(), "%"));
        rows.add(new Slider("Thickness", 0.05, 1.0, 0.05, () -> (double) c.outlineThickness, v -> c.outlineThickness = v.floatValue(), ""));
        rows.add(new Slider("Fill Opacity", 0, 100, 1, () -> (double) c.fillOpacity, v -> c.fillOpacity = v.intValue(), "%"));
        rows.add(new Slider("Red", 0, 255, 1, () -> (double) c.red, v -> c.red = v.intValue(), ""));
        rows.add(new Slider("Green", 0, 255, 1, () -> (double) c.green, v -> c.green = v.intValue(), ""));
        rows.add(new Slider("Blue", 0, 255, 1, () -> (double) c.blue, v -> c.blue = v.intValue(), ""));
        rows.add(new Toggle("Outline", () -> c.outline, v -> c.outline = v));
        rows.add(new Toggle("Fill", () -> c.fill, v -> c.fill = v));
        rows.add(new Toggle("Through Walls", () -> c.throughWalls, v -> c.throughWalls = v));
        rows.add(new Toggle("Chat Notify", () -> c.chatNotify, v -> c.chatNotify = v));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // header
        g.fill(X, Y, X + W, Y + HEAD, HEADC);
        g.drawString(font, SussyClient.NAME, X + 6, Y + 6, TEXT, true);
        g.drawString(font, panelOpen ? "v" : ">", X + W - 12, Y + 6, TEXT, true);
        if (!panelOpen) return;

        // module row
        int my0 = Y + HEAD;
        boolean hov = inside(mx, my, X, my0, W, ROW);
        g.fill(X, my0, X + W, my0 + ROW, hov ? HOVER : ROWC);
        if (Config.I.enabled) g.fill(X, my0, X + 2, my0 + ROW, ON);
        g.drawString(font, "Sus Chunk Finder", X + 6, my0 + 5, Config.I.enabled ? TEXT : DIM, true);
        g.drawString(font, settingsOpen ? "-" : "+", X + W - 10, my0 + 5, DIM, true);

        int ry = my0 + ROW;
        if (settingsOpen) {
            for (Row r : rows) {
                r.draw(g, ry, mx, my);
                ry += ROW;
            }
        }
        g.fill(X, ry, X + W, ry + 1, HEADC);
        g.drawString(font, "L-click: toggle  R-click: settings", X, ry + 5, DIM, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();

        if (inside(mx, my, X, Y, W, HEAD)) { panelOpen = !panelOpen; return true; }
        if (!panelOpen) return super.mouseClicked(event, doubleClick);

        int my0 = Y + HEAD;
        if (inside(mx, my, X, my0, W, ROW)) {
            if (button == 0) { Config.I.enabled = !Config.I.enabled; Config.save(); }
            else if (button == 1) settingsOpen = !settingsOpen;
            return true;
        }
        if (settingsOpen) {
            int ry = my0 + ROW;
            for (Row r : rows) {
                if (inside(mx, my, X, ry, W, ROW)) {
                    r.click(mx);
                    if (r instanceof Slider) dragging = r;
                    Config.save();
                    return true;
                }
                ry += ROW;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) { dragging.click(event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) { dragging = null; Config.save(); }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; } // same bind closes it
        return super.keyPressed(event);
    }

    @Override
    public void removed() { Config.save(); }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------- rows ----------
    private abstract class Row {
        abstract void draw(GuiGraphics g, int y, int mx, int my);
        abstract void click(double mx);
    }

    private class Slider extends Row {
        final String name, suffix; final double min, max, step;
        final Supplier<Double> get; final Consumer<Double> set;
        Slider(String name, double min, double max, double step, Supplier<Double> get, Consumer<Double> set, String suffix) {
            this.name = name; this.min = min; this.max = max; this.step = step; this.get = get; this.set = set; this.suffix = suffix;
        }
        void draw(GuiGraphics g, int y, int mx, int my) {
            g.fill(X, y, X + W, y + ROW, inside(mx, my, X, y, W, ROW) ? HOVER : ROWC);
            double f = (get.get() - min) / (max - min);
            g.fill(X, y, X + (int) (W * Math.max(0, Math.min(1, f))), y + ROW, 0x80B01E1E);
            double val = get.get();
            String shown = step >= 1 ? String.valueOf((int) val) : String.format("%.2f", val);
            g.drawString(font, name + ": " + shown + suffix, X + 6, y + 5, TEXT, true);
        }
        void click(double mx) {
            double f = Math.max(0, Math.min(1, (mx - X) / W));
            double v = min + f * (max - min);
            v = Math.round(v / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }
    }

    private class Toggle extends Row {
        final String name; final Supplier<Boolean> get; final Consumer<Boolean> set;
        Toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set) { this.name = name; this.get = get; this.set = set; }
        void draw(GuiGraphics g, int y, int mx, int my) {
            g.fill(X, y, X + W, y + ROW, inside(mx, my, X, y, W, ROW) ? HOVER : ROWC);
            g.drawString(font, name, X + 6, y + 5, TEXT, true);
            g.drawString(font, get.get() ? "ON" : "OFF", X + W - 26, y + 5, get.get() ? 0xFF55FF55 : 0xFFFF5555, true);
        }
        void click(double mx) { set.accept(!get.get()); }
    }
}
