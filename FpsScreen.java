package sharkus.extras;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** FPS Optimizer: Presets für Grafik-Einstellungen und Auto-Modus für die Sichtweite. */
public class FpsScreen extends Screen {
    private final Screen parent;

    public FpsScreen(Screen parent) {
        super(Component.literal(L.t("FPS Optimizer")));
        this.parent = parent;
    }

    private static int num(String v, int old, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
        } catch (NumberFormatException ignored) { }
        return old;
    }

    private static String preset(int i) {
        return L.t(FpsOpt.NAMES[i]);
    }

    @Override
    protected void init() {
        Config c = Config.I;
        int cx = width / 2;
        String[] tips = {
                "Stellt alle Optionen auf deine ursprünglichen Werte zurück.",
                "Etwas weniger Sichtweite und Partikel, Optik bleibt gut.",
                "Deutlich mehr FPS: Sichtweite 8, Wolken aus, Schatten aus.",
                "Maximale FPS: Sichtweite 6, minimale Partikel, kein Mipmapping."
        };
        for (int i = 0; i < 4; i++) {
            final int pi = i;
            int x = cx - 154 + (i % 2) * 156;
            int y = 48 + (i / 2) * 24;
            addRenderableWidget(Button.builder(Component.literal(preset(i)), b -> {
                        FpsOpt.apply(pi);
                    })
                    .bounds(x, y, 152, 20)
                    .tooltip(Tooltip.create(Component.literal(L.t(tips[i]))))
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal(L.t("Auto-Modus") + ": " + (c.fpsAuto ? L.t("AN") : L.t("AUS"))), b -> {
            c.fpsAuto = !c.fpsAuto;
            b.setMessage(Component.literal(L.t("Auto-Modus") + ": " + (c.fpsAuto ? L.t("AN") : L.t("AUS"))));
            Config.save();
        }).bounds(cx - 154, 106, 308, 20).build());

        EditBox target = new EditBox(font, cx - 154, 150, 96, 20, Component.literal("Ziel"));
        target.setMaxLength(3);
        target.setValue(String.valueOf(c.fpsTarget));
        target.setResponder(v -> { c.fpsTarget = num(v, c.fpsTarget, 20, 999); Config.save(); });
        addRenderableWidget(target);

        EditBox min = new EditBox(font, cx - 50, 150, 96, 20, Component.literal("Min"));
        min.setMaxLength(2);
        min.setValue(String.valueOf(c.fpsMinDist));
        min.setResponder(v -> { c.fpsMinDist = num(v, c.fpsMinDist, 2, 32); Config.save(); });
        addRenderableWidget(min);

        EditBox max = new EditBox(font, cx + 58, 150, 96, 20, Component.literal("Max"));
        max.setMaxLength(2);
        max.setValue(String.valueOf(c.fpsMaxDist));
        max.setResponder(v -> { c.fpsMaxDist = num(v, c.fpsMaxDist, 2, 32); Config.save(); });
        addRenderableWidget(max);

        addRenderableWidget(Button.builder(Component.literal(L.t("Fertig")), b -> onClose())
                .bounds(cx - 50, 214, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Ui.backdrop(g, width, height);
        super.extractRenderState(g, mouseX, mouseY, delta);
        Config c = Config.I;
        int cx = width / 2;
        String title = L.t("FPS Optimizer");
        g.text(font, title, cx - font.width(title) / 2, 14, 0xFFFFFFFF);
        g.text(font, L.t("Preset") + ": " + preset(c.optPreset)
                + (FpsOpt.lastResult.isEmpty() ? "" : "   (" + L.t("angewendet") + " " + FpsOpt.lastResult + ")"),
                cx - 154, 32, Ui.accent());
        g.text(font, L.t("Ziel-FPS"), cx - 154, 139, 0xFFAAAAAA);
        g.text(font, L.t("Min. Sichtweite"), cx - 50, 139, 0xFFAAAAAA);
        g.text(font, L.t("Max. Sichtweite"), cx + 58, 139, 0xFFAAAAAA);
        g.text(font, L.t("Auto-Modus prüft alle 5 Sekunden deine FPS und ändert die Sichtweite."), cx - 154, 130 + 52, 0xFF9AA3B2);
        g.text(font, L.t("Tipp: Für noch mehr FPS hilft der Mod Sodium (Chunk-Rendering)."), cx - 154, 130 + 64, 0xFF9AA3B2);
        g.text(font, "FPS: " + Minecraft_fps(), cx - 154, 130 + 78, 0xFFFFFFFF);
    }

    private static int Minecraft_fps() {
        return net.minecraft.client.Minecraft.getInstance().getFps();
    }

    @Override
    public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }
}
