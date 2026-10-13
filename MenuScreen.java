package sharkus.extras;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hauptmenü: Seitenleiste mit Kategorien, Suchleiste, Funktionen mit Schalter und Einstellungen. */
public class MenuScreen extends Screen {
    private static final String[] CATS = {"Alle", "Kampf", "Anzeige", "Inventar", "Sicht"};
    private static final int ACCENT = 0xFF3FA9F5;
    private static final int MUTED = 0xFF9AA3B2;
    private static final int ROW_H = 26;

    private static int cat = 0;
    private static int scroll = 0;
    private static String query = "";

    private final Screen parent;
    private EditBox search;
    private Button masterBtn;
    private final List<Button> catButtons = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private List<Features.F> visible = new ArrayList<>();
    private int px, py, pw, ph, rowsVisible;
    private long lastKeyMs;

    public MenuScreen(Screen parent) {
        super(Component.literal("Sharkus Extras"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------

    private void filter() {
        List<Features.F> all = Features.all();
        visible = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT).trim();
        for (Features.F f : all) {
            if (!q.isEmpty()) {
                if (f.name().toLowerCase(Locale.ROOT).contains(q) || f.desc().toLowerCase(Locale.ROOT).contains(q)) visible.add(f);
            } else if (cat == 0 || f.cat().equals(CATS[cat])) {
                visible.add(f);
            }
        }
        int maxScroll = Math.max(0, visible.size() - rowsVisible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private static Component stateLabel(boolean on) {
        return Component.literal(on ? "AN" : "AUS").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component masterLabel() {
        boolean on = Config.I.enabled;
        return Component.literal("Client: ")
                .append(Component.literal(on ? "AN" : "AUS").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void updateCatLabels() {
        boolean searching = !query.trim().isEmpty();
        for (int i = 0; i < catButtons.size(); i++) {
            boolean sel = !searching && i == cat;
            Component label = Component.literal((sel ? "> " : "   ") + CATS[i]);
            catButtons.get(i).setMessage(sel ? label.copy().withStyle(ChatFormatting.YELLOW) : label);
        }
    }

    private void buildRows() {
        for (AbstractWidget w : rowWidgets) removeWidget(w);
        rowWidgets.clear();

        int rx = px + 104;
        int rowW = pw - 132;
        for (int i = 0; i < rowsVisible; i++) {
            int idx = scroll + i;
            if (idx >= visible.size()) break;
            Features.F f = visible.get(idx);
            int ry = py + 34 + i * ROW_H;
            if (f.soon()) continue;

            Button toggle = Button.builder(stateLabel(f.get().get()), b -> {
                        f.set().accept(!f.get().get());
                        b.setMessage(stateLabel(f.get().get()));
                        Config.save();
                    })
                    .bounds(rx + rowW - (f.opts() != null ? 82 : 56), ry + 3, 52, 20)
                    .tooltip(Tooltip.create(Component.literal(f.desc())))
                    .build();
            addRenderableWidget(toggle);
            rowWidgets.add(toggle);

            if (f.opts() != null) {
                Button gear = Button.builder(Component.literal("..."), b -> minecraft.gui.setScreen(f.opts().apply(this)))
                        .bounds(rx + rowW - 26, ry + 3, 24, 20)
                        .tooltip(Tooltip.create(Component.literal("Einstellungen für " + f.name())))
                        .build();
                addRenderableWidget(gear);
                rowWidgets.add(gear);
            }
        }
    }

    private void refreshAll() {
        filter();
        buildRows();
        updateCatLabels();
    }

    private String fit(String s, int w) {
        if (font.width(s) <= w) return s;
        while (s.length() > 1 && font.width(s + "...") > w) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // ------------------------------------------------------------------

    @Override
    protected void init() {
        pw = Math.min(440, width - 8);
        ph = Math.min(240, height - 8);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        rowsVisible = Math.max(3, (ph - 34 - 10) / ROW_H);

        catButtons.clear();
        rowWidgets.clear();

        // Hauptschalter: schaltet den ganzen Client aus (zum FPS-Vergleichen)
        masterBtn = Button.builder(masterLabel(), b -> {
                    Config.I.enabled = !Config.I.enabled;
                    Config.save();
                    b.setMessage(masterLabel());
                })
                .bounds(px + 108, py + 5, 100, 18)
                .tooltip(Tooltip.create(Component.literal(
                        "Schaltet alle Funktionen des Clients aus und wieder an, damit du FPS vergleichen kannst. Im Spiel geht das auch mit der Taste F8.")))
                .build();
        addRenderableWidget(masterBtn);

        search = new EditBox(font, px + pw - 190, py + 5, 182, 18, Component.literal("Suche"));
        search.setMaxLength(30);
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            scroll = 0;
            refreshAll();
        });
        addRenderableWidget(search);

        for (int i = 0; i < CATS.length; i++) {
            final int ci = i;
            Button b = Button.builder(Component.literal(CATS[i]), x -> {
                cat = ci;
                scroll = 0;
                query = "";
                search.setValue("");
                refreshAll();
            }).bounds(px + 6, py + 34 + i * 22, 92, 20).build();
            catButtons.add(b);
            addRenderableWidget(b);
        }

        int fy = py + ph - 6 - 4 * 22;
        addRenderableWidget(Button.builder(Component.literal("HUD verschieben"),
                        b -> minecraft.gui.setScreen(new HudEditScreen(this)))
                .bounds(px + 6, fy, 92, 20)
                .tooltip(Tooltip.create(Component.literal("Elemente verschieben und ihre Größe ändern")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("FPS Optimizer"),
                        b -> minecraft.gui.setScreen(new FpsScreen(this)))
                .bounds(px + 6, fy + 22, 92, 20)
                .tooltip(Tooltip.create(Component.literal("Mehr FPS durch optimierte Grafik-Einstellungen")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("Mehr..."),
                        b -> minecraft.gui.setScreen(new MoreScreen(this)))
                .bounds(px + 6, fy + 44, 92, 20)
                .tooltip(Tooltip.create(Component.literal("Weitere Einstellungen und Status")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(px + 6, fy + 66, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("^"), b -> {
            scroll = Math.max(0, scroll - 1);
            buildRows();
        }).bounds(px + pw - 22, py + 34, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("v"), b -> {
            scroll = Math.min(Math.max(0, visible.size() - rowsVisible), scroll + 1);
            buildRows();
        }).bounds(px + pw - 22, py + ph - 26, 16, 16).build());

        refreshAll();
    }

    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Hintergrund-Karte
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF0B0E16);
        g.fill(px, py, px + pw, py + ph, 0xF0141824);
        g.fill(px, py, px + pw, py + 28, 0xFF1C2132);
        g.fill(px + 4, py + 32, px + 100, py + ph - 4, 0x30FFFFFF);
        g.fill(px + 102, py + 32, px + pw - 4, py + ph - 4, 0x30000000);

        super.extractRenderState(g, mouseX, mouseY, delta);

        // Bild hoch / runter scrollt die Liste
        long win = minecraft.getWindow().handle();
        long now = System.currentTimeMillis();
        if (now - lastKeyMs > 130) {
            boolean up = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_PAGE_UP) == GLFW.GLFW_PRESS;
            boolean dn = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_PAGE_DOWN) == GLFW.GLFW_PRESS;
            if (up || dn) {
                scroll = Math.max(0, Math.min(Math.max(0, visible.size() - rowsVisible), scroll + (dn ? 1 : -1)));
                buildRows();
                lastKeyMs = now;
            }
        }

        // Kopfzeile
        g.fill(px, py + 28, px + pw, py + 29, ACCENT);
        g.text(font, "SHARKUS", px + 10, py + 10, ACCENT);
        g.text(font, "EXTRAS", px + 10 + font.width("SHARKUS "), py + 10, 0xFFFFFFFF);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            g.text(font, "Suchen...", px + pw - 186, py + 10, 0xFF6B7385);
        }

        // Zeilen
        int rx = px + 104;
        int rowW = pw - 132;
        for (int i = 0; i < rowsVisible; i++) {
            int idx = scroll + i;
            if (idx >= visible.size()) break;
            Features.F f = visible.get(idx);
            int ry = py + 34 + i * ROW_H;
            int textW = rowW - (f.opts() != null ? 96 : 70);

            g.fill(rx, ry + ROW_H - 2, rx + rowW, ry + ROW_H - 1, 0x22FFFFFF);
            boolean on = !f.soon() && f.get().get();
            g.fill(rx + 1, ry + 4, rx + 3, ry + 22, f.soon() ? 0xFF555B6B : (on ? 0xFF3DDC84 : 0xFF555B6B));
            g.text(font, f.name(), rx + 8, ry + 4, f.soon() ? 0xFF7C8496 : 0xFFFFFFFF);
            g.text(font, fit(f.desc(), textW), rx + 8, ry + 15, MUTED);
            if (f.soon()) g.text(font, "bald", rx + rowW - 30, ry + 9, 0xFFFFAA00);
        }

        if (visible.isEmpty()) {
            g.text(font, "Nichts gefunden", rx + 8, py + 44, MUTED);
        }

        // Scroll-Leiste
        if (visible.size() > rowsVisible) {
            int trackTop = py + 54, trackBot = py + ph - 30;
            g.fill(px + pw - 15, trackTop, px + pw - 13, trackBot, 0x33FFFFFF);
            int span = trackBot - trackTop;
            int thumbH = Math.max(12, span * rowsVisible / visible.size());
            int maxScroll = visible.size() - rowsVisible;
            int thumbY = trackTop + (span - thumbH) * scroll / Math.max(1, maxScroll);
            g.fill(px + pw - 16, thumbY, px + pw - 12, thumbY + thumbH, ACCENT);
        }

        // Fußzeile
        int total = 0, active = 0;
        for (Features.F f : Features.all()) {
            if (f.soon()) continue;
            total++;
            if (f.get().get()) active++;
        }
        g.text(font, active + " von " + total + " aktiv", px + 104, py + ph - 14, MUTED);

        if (!Config.I.enabled) {
            String off = "Client ist AUS: alle Funktionen pausiert";
            g.text(font, off, px + pw - 24 - font.width(off), py + ph - 14, 0xFFFF7777);
        }
    }

    @Override
    public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }
}
