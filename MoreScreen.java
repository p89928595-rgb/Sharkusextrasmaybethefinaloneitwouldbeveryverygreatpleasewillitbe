package sharkus.extras;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MoreScreen extends Screen {
    private final Screen parent;

    public MoreScreen(Screen parent) {
        super(Component.literal(L.t("Einstellungen")));
        this.parent = parent;
    }

    private void button(String text, int col, int row, Runnable r) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> r.run())
                .bounds(width / 2 - 152 + col * 154, 32 + row * 22, 150, 20).build());
    }

    @Override
    protected void init() {
        Config c = Config.I;
        button("Nametag-Einstellungen...", 0, 0, () -> minecraft.gui.setScreen(new NametagScreen(this)));
        button("Farben, Stile & Team...", 1, 0, () -> minecraft.gui.setScreen(new SettingsScreen(this)));
        button("Hitbox-/Highlighter-Farben", 0, 1, () -> minecraft.gui.setScreen(new ColorsScreen(this)));
        button("Highlighter: Items wählen", 1, 1,
                () -> minecraft.gui.setScreen(new ItemPickerScreen(this, ItemPickerScreen.Mode.HIGHLIGHT)));
        button("Item Counter: Items wählen", 0, 2,
                () -> minecraft.gui.setScreen(new ItemPickerScreen(this, ItemPickerScreen.Mode.COUNTER)));
        button("Crosshair Studio...", 1, 2, () -> minecraft.gui.setScreen(new CrosshairStudioScreen(this)));
        button("Pre Inventory...", 0, 3, () -> minecraft.gui.setScreen(new KitScreen(this)));
        button("HUD verschieben & Größe...", 1, 3, () -> minecraft.gui.setScreen(new HudEditScreen(this)));

        EditBox zoom = new EditBox(font, width / 2 + 96, 32 + 4 * 22 + 12, 54, 20, Component.literal(L.t("Zoom FOV")));
        zoom.setMaxLength(3);
        zoom.setValue(String.valueOf(c.zoomFov));
        zoom.setResponder(v -> {
            try {
                c.zoomFov = Math.max(30, Math.min(110, Integer.parseInt(v.trim())));
                Config.save();
            } catch (NumberFormatException ignored) { }
        });
        addRenderableWidget(zoom);

        addRenderableWidget(Button.builder(Component.literal(L.t("Fertig")), b -> onClose())
                .bounds(width / 2 - 50, 32 + 6 * 22 + 4, 100, 20).build());
    }

    private void status(GuiGraphicsExtractor g, String text, boolean ok, int y) {
        g.text(font, text, width / 2 - 150, y, ok ? 0xFF55FF55 : 0xFFFF5555);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        g.text(font, L.t("Zoom-FOV (30 = max. Zoom, 110 = aus):"), width / 2 - 150, 32 + 4 * 22 + 18, 0xFFAAAAAA);

        int y = 32 + 8 * 22 + 4;
        status(g, "Text-Hook (Nametags): " + (Names.hookCalls > 0 ? "aktiv" : "NICHT aktiv"), Names.hookCalls > 0, y);
        status(g, "Hit-Color-Hook: " + HitColor.status, HitColor.hooked, y + 11);
        status(g, "Inventar-Hook: " + (ItemFx.screenHook ? "aktiv" : "noch nicht gesehen (Inventar öffnen)"),
                ItemFx.screenHook, y + 22);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
