package sharkus.extras;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Items auswählen: für den Highlighter (mit Farbe) oder für den Item Counter. */
public class ItemPickerScreen extends Screen {
    public enum Mode { HIGHLIGHT, COUNTER }

    private static final int[] PALETTE = {
            0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00CCCC, 0x5599FF, 0xAA55FF, 0xFF55FF, 0xFFFFFF, 0xFFD700
    };
    private static final int ROWS = 8;

    private static List<Item> ALL;
    private static List<String> NAMES;

    private final Screen parent;
    private final Mode mode;
    private EditBox search;
    private String filter = "";
    private int page;
    private final Button[] rowBtn = new Button[ROWS];
    private final Button[] colBtn = new Button[ROWS];
    private final Item[] rowItem = new Item[ROWS];
    private List<Integer> shown = new ArrayList<>();
    private Button pageBtn;

    public ItemPickerScreen(Screen parent, Mode mode) {
        super(Component.literal(mode == Mode.HIGHLIGHT ? "Highlighter: Items wählen" : "Item Counter: Items wählen"));
        this.parent = parent;
        this.mode = mode;
    }

    private static void loadAll() {
        if (ALL != null) return;
        ALL = new ArrayList<>();
        NAMES = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) {
            if (it == Items.AIR) continue;
            ALL.add(it);
            String id = BuiltInRegistries.ITEM.getKey(it).getPath();
            String name;
            try {
                name = new ItemStack(it).getHoverName().getString();
            } catch (Throwable t) {
                name = id;
            }
            NAMES.add((name + " " + id).toLowerCase(Locale.ROOT));
        }
    }

    private static String idOf(Item it) {
        return BuiltInRegistries.ITEM.getKey(it).toString();
    }

    private boolean isOn(String id) {
        Config c = Config.I;
        if (mode == Mode.HIGHLIGHT) return c.hlItems.containsKey(id);
        for (String s : c.itemCounterIds.split(",")) {
            String t = s.trim();
            if (!t.contains(":") && !t.isEmpty()) t = "minecraft:" + t;
            if (t.equals(id)) return true;
        }
        return false;
    }

    private void toggle(String id) {
        Config c = Config.I;
        if (mode == Mode.HIGHLIGHT) {
            if (c.hlItems.containsKey(id)) c.hlItems.remove(id);
            else c.hlItems.put(id, PALETTE[0]);
        } else {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String s : c.itemCounterIds.split(",")) {
                String t = s.trim();
                if (t.isEmpty()) continue;
                if (!t.contains(":")) t = "minecraft:" + t;
                set.add(t);
            }
            if (!set.remove(id)) set.add(id);
            c.itemCounterIds = String.join(",", set);
        }
        Config.save();
    }

    private void cycleColor(String id) {
        Integer cur = Config.I.hlItems.get(id);
        int idx = 0;
        if (cur != null) {
            for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == cur) idx = (i + 1) % PALETTE.length;
        }
        Config.I.hlItems.put(id, PALETTE[idx]);
        Config.save();
    }

    private void applyFilter() {
        String f = filter.toLowerCase(Locale.ROOT).trim();
        shown = new ArrayList<>();
        for (int i = 0; i < ALL.size(); i++) {
            if (f.isEmpty() || NAMES.get(i).contains(f)) shown.add(i);
        }
        int pages = Math.max(1, (shown.size() + ROWS - 1) / ROWS);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
    }

    private void refresh() {
        int pages = Math.max(1, (shown.size() + ROWS - 1) / ROWS);
        if (pageBtn != null) pageBtn.setMessage(Component.literal((page + 1) + " / " + pages));
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx < shown.size()) {
                Item it = ALL.get(shown.get(idx));
                rowItem[i] = it;
                String id = idOf(it);
                boolean on = isOn(id);
                String name = new ItemStack(it).getHoverName().getString();
                if (name.length() > 26) name = name.substring(0, 26);
                rowBtn[i].setMessage(Component.literal((on ? "[AN] " : "[  ] ") + name));
                rowBtn[i].visible = true;
                colBtn[i].visible = mode == Mode.HIGHLIGHT && on;
            } else {
                rowItem[i] = null;
                rowBtn[i].visible = false;
                colBtn[i].visible = false;
            }
        }
    }

    @Override
    protected void init() {
        loadAll();
        int cx = width / 2;

        search = new EditBox(font, cx - 130, 28, 200, 20, Component.literal(L.t("Suche")));
        search.setMaxLength(40);
        search.setResponder(v -> {
            filter = v;
            page = 0;
            applyFilter();
            refresh();
        });
        addRenderableWidget(search);

        if (mode == Mode.HIGHLIGHT) {
            addRenderableWidget(Button.builder(Component.literal("Standard: " + (Config.I.hlCategories ? "AN" : "AUS")), b -> {
                Config.I.hlCategories = !Config.I.hlCategories;
                b.setMessage(Component.literal("Standard: " + (Config.I.hlCategories ? "AN" : "AUS")));
                Config.save();
            }).bounds(cx + 74, 28, 76, 20).build());
        }

        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            int y = 54 + i * 22;
            rowBtn[i] = Button.builder(Component.literal(""), b -> {
                if (rowItem[row] != null) {
                    toggle(idOf(rowItem[row]));
                    refresh();
                }
            }).bounds(cx - 130, y, 200, 20).build();
            addRenderableWidget(rowBtn[i]);

            colBtn[i] = Button.builder(Component.literal(L.t("Farbe")), b -> {
                if (rowItem[row] != null) cycleColor(idOf(rowItem[row]));
            }).bounds(cx + 74, y, 76, 20).build();
            addRenderableWidget(colBtn[i]);
        }

        int by = 54 + ROWS * 22 + 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            applyFilter();
            refresh();
        }).bounds(cx - 130, by, 40, 20).build());
        pageBtn = Button.builder(Component.literal("1"), b -> { }).bounds(cx - 86, by, 60, 20).build();
        pageBtn.active = false;
        addRenderableWidget(pageBtn);
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            applyFilter();
            refresh();
        }).bounds(cx - 22, by, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal(L.t("Fertig")), b -> onClose())
                .bounds(cx + 30, by, 120, 20).build());

        applyFilter();
        refresh();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = width / 2;
        g.centeredText(font, title, cx, 10, 0xFFFFFFFF);
        for (int i = 0; i < ROWS; i++) {
            if (rowItem[i] == null) continue;
            int y = 54 + i * 22;
            g.item(new ItemStack(rowItem[i]), cx - 150, y + 2);
            if (mode == Mode.HIGHLIGHT && colBtn[i].visible) {
                Integer col = Config.I.hlItems.get(idOf(rowItem[i]));
                if (col != null) g.fill(cx + 78, y + 5, cx + 90, y + 15, 0xFF000000 | (col & 0xFFFFFF));
            }
        }
    }

    @Override
    public void onClose() {
        Config.save();
        minecraft.gui.setScreen(parent);
    }
}
