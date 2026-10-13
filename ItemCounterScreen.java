package sharkus.extras;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Item Counter: pro PvP-Item an/aus und eine Warnschwelle (rotes Ausrufezeichen). */
public class ItemCounterScreen extends Screen {
    private final Screen parent;

    public ItemCounterScreen(Screen parent) {
        super(Component.literal(L.t("Item Counter")));
        this.parent = parent;
    }

    private static Component state(ItemCounters.Preset p) {
        boolean on = ItemCounters.enabled(p);
        return Component.literal(p.label() + ": ")
                .append(Component.literal(on ? "AN" : "AUS").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private int x0(int i) {
        return width / 2 - 156 + (i / 5) * 160;
    }

    private int y0(int i) {
        return 58 + (i % 5) * 24;
    }

    @Override
    protected void init() {
        Config c = Config.I;
        int cx = width / 2;
        for (int i = 0; i < ItemCounters.PRESETS.size(); i++) {
            ItemCounters.Preset p = ItemCounters.PRESETS.get(i);
            int x = x0(i), y = y0(i);

            addRenderableWidget(Button.builder(state(p), b -> {
                c.counterOn.put(p.id(), !ItemCounters.enabled(p));
                b.setMessage(state(p));
                Config.save();
            }).bounds(x + 20, y, 90, 20).build());

            EditBox warn = new EditBox(font, x + 114, y, 36, 20, Component.literal(L.t("Warnung")));
            warn.setMaxLength(3);
            warn.setValue(String.valueOf(ItemCounters.warn(p)));
            warn.setResponder(v -> {
                try {
                    c.counterWarn.put(p.id(), Math.max(0, Math.min(999, Integer.parseInt(v.trim()))));
                    Config.save();
                } catch (NumberFormatException ignored) { }
            });
            addRenderableWidget(warn);
        }

        addRenderableWidget(Button.builder(Component.literal(L.t("Weitere Items wählen...")),
                b -> minecraft.gui.setScreen(new ItemPickerScreen(this, ItemPickerScreen.Mode.COUNTER)))
                .bounds(cx - 156, 190, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal(L.t("Fertig")), b -> onClose())
                .bounds(cx + 6, 190, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        g.centeredText(font, title, cx, 12, 0xFFFFFFFF);
        g.centeredText(font, Component.literal(L.t("Unter der Warnschwelle erscheint ein rotes Ausrufezeichen (0 = keine Warnung)")),
                cx, 28, 0xFFAAAAAA);
        g.text(font, L.t("Warnung ab"), x0(0) + 112, 46, 0xFFAAAAAA);
        g.text(font, L.t("Warnung ab"), x0(5) + 112, 46, 0xFFAAAAAA);
        for (int i = 0; i < ItemCounters.PRESETS.size(); i++) {
            ItemStack icon = ItemFx.stackFor(ItemCounters.PRESETS.get(i).id());
            if (icon != null) g.item(icon, x0(i), y0(i) + 2);
        }
    }

    @Override
    public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }
}
