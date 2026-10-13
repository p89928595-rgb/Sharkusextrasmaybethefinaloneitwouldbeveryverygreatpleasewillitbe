package sharkus.extras;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Hud {
    /** Verschiebbare HUD-Elemente: Schlüssel, Name, Standard-Position, Größe bei Maßstab 1. */
    public record El(String key, String label, int dx, int dy, int w, int h) { }

    public static final List<El> ELS = List.of(
            new El("fps", "FPS", 4, 4, 50, 10),
            new El("cps", "CPS", 4, 14, 50, 10),
            new El("ping", "Ping", 4, 24, 70, 10),
            new El("sprint", "Sprint", 4, 34, 90, 10),
            new El("armor", "Armor", 4, 48, 50, 72),
            new El("totem", "Totems", 60, 4, 60, 16),
            new El("combo", "Combo", 60, 22, 60, 10),
            new El("reach", "Reach", 60, 32, 70, 10),
            new El("jr", "Jump Reset", 60, 42, 130, 34),
            new El("items", "Item Counter", 60, 70, 80, 90),
            new El("inv", "Inventar", 190, 4, 166, 58),
            new El("keys", "Keystrokes", 4, 130, 64, 74),
            new El("tps", "TPS", 4, 200, 60, 10),
            new El("coords", "Koordinaten", 4, 212, 170, 10),
            new El("server", "Server", 4, 224, 150, 26),
            new El("horse", "Pferd", 200, 70, 130, 40),
            new El("shield", "Schild", -1, -1, 40, 30));

    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasDown;

    // Schild-Cooldown
    private static boolean shieldCdPrev;
    private static long shieldCdStart;
    private static final long SHIELD_CD_MS = 5000;

    private static boolean sprintWasOn;

    // Zähler
    private static long tickN;
    private static LivingEntity pendTarget;
    private static long pendTick;
    private static int pendHurt;
    private static double pendReach;
    private static int comboN;
    private static long lastHitMs;
    private static double lastReach = -1;
    private static long lastReachMs;
    private static int prevPlayerHurt;
    private static int jrHits, jrPerfect, jrGood;
    private static long jrLastJumpMs = -10000, jrHitMs;
    private static boolean jrAwaiting, prevJkJr;
    private static int prevHurtJr;
    private static int jrGrade = -1;       // 0 Perfect, 1 Gut, 2 Knapp, 3 zu früh, 4 zu spät, 5 kein Sprung
    private static int jrDt;
    private static long jrFeedbackMs;
    private static final String[] JR_NAMES = {"PERFECT", "GOOD", "CLOSE", "ZU FRÜH", "ZU SPÄT", "KEIN SPRUNG"};
    private static final int[] JR_COLORS = {0xFF55FF55, 0xFF55AAFF, 0xFFFFFF55, 0xFFFF5555, 0xFFFF5555, 0xFFAAAAAA};
    private static final Map<java.util.UUID, Long> shieldOff = new java.util.HashMap<>();
    private static long lastJumpTick = -100;
    private static double prevVy;
    private static boolean prevJumpKey;

    // TPS-Schätzung
    private static long lastGt = Long.MIN_VALUE;
    private static long lastSyncMs, lastSyncGt, tpsMs;
    private static double tpsValue = 20.0;

    // Zoom
    private static boolean zooming;
    private static int savedFov;

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // ------------------------------------------------------------------
    // Positionen
    // ------------------------------------------------------------------

    public static int X(String key, int sw) {
        int[] p = Config.I.hudPos.get(key);
        if (p != null) return p[0];
        for (El e : ELS) if (e.key().equals(key)) return e.dx() == -1 ? sw / 2 - 20 : e.dx();
        return 4;
    }

    public static int Y(String key, int sh) {
        int[] p = Config.I.hudPos.get(key);
        if (p != null) return p[1];
        for (El e : ELS) if (e.key().equals(key)) return e.dy() == -1 ? sh - 90 : e.dy();
        return 4;
    }

    private static void at(GuiGraphicsExtractor g, String key, int sw, int sh, Runnable r) {
        float s = Config.I.sc(key);
        Matrix3x2fStack m = g.pose();
        m.pushMatrix();
        m.translate((float) X(key, sw), (float) Y(key, sh));
        m.scale(s, s);
        r.run();
        m.popMatrix();
    }

    // ------------------------------------------------------------------
    // Tick: Klicks, Sprint, Zähler
    // ------------------------------------------------------------------

    public static void tick(Minecraft mc) {
        if (mc.getWindow() == null) return;
        Config c = Config.I;
        if (!c.enabled) {
            cleanup(mc, c);
            return;
        }
        boolean was = wasDown;
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !was) clicks.add(System.currentTimeMillis());
        wasDown = down;
        boolean rising = down && !was;
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peek() > 1000) clicks.poll();

        if (mc.player != null && mc.level != null) {
            tickN++;

            boolean cd = shieldOnCd(mc);
            if (cd && !shieldCdPrev) shieldCdStart = now;
            shieldCdPrev = cd;

            // Toggle Sprint
            if (c.toggleSprint) {
                if (mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()) {
                    mc.options.keySprint.setDown(true);
                }
                sprintWasOn = true;
            } else if (sprintWasOn) {
                mc.options.keySprint.setDown(false);
                sprintWasOn = false;
            }

            // --- Angriff erkennen (Combo, Reach, Haltbarkeits-Warnung) ---
            if (rising) {
                LivingEntity t = target(mc);
                if (t instanceof Player tp && tp.isBlocking() && ItemFx.key(mc.player.getMainHandItem()).endsWith("_axe")
                        && mc.player.getAttackStrengthScale(0f) >= 0.9f) {
                    shieldOff.put(tp.getUUID(), now + SHIELD_CD_MS);
                }
                if (t != null) {
                    pendTarget = t;
                    pendTick = tickN;
                    pendHurt = t.hurtTime;
                    if (mc.hitResult instanceof EntityHitResult ehr) {
                        pendReach = ehr.getLocation().distanceTo(mc.player.getEyePosition());
                    }
                }
            }
            if (pendTarget != null) {
                if (pendTarget.hurtTime > pendHurt) {
                    comboN++;
                    lastHitMs = now;
                    lastReach = pendReach;
                    lastReachMs = now;
                    if (c.durWarn) warnTool(mc);
                    pendTarget = null;
                } else if (tickN - pendTick > 4) {
                    pendTarget = null;
                }
            }
            if (comboN > 0 && now - lastHitMs > 3000) comboN = 0;

            // --- Selbst getroffen (Combo-Ende, Jump Reset, Rüstungs-Warnung) ---
            int ph = mc.player.hurtTime;
            if (ph > prevPlayerHurt && ph > 0) {
                comboN = 0;
                if (c.durWarn) warnArmor(mc);
            }
            prevPlayerHurt = ph;

            pollJr(mc);

            // --- TPS schätzen: Server-Zeitpakete zeigen sich als Sprung der Spielzeit ---
            long gt = mc.level.getGameTime();
            if (lastGt != Long.MIN_VALUE && gt - lastGt != 1) {
                if (lastSyncMs > 0) {
                    double sec = (now - lastSyncMs) / 1000.0;
                    long dT = gt - lastSyncGt;
                    if (sec > 0.5 && dT >= 10 && dT <= 40) {
                        tpsValue = Math.max(0, Math.min(20, dT / sec));
                        tpsMs = now;
                    }
                }
                lastSyncMs = now;
                lastSyncGt = gt;
            }
            lastGt = gt;
            if (now - tpsMs > 6000) tpsValue = 20.0;
        }
        zoomTick(mc, c);
        FpsOpt.tick(mc);
        HitColor.tick();
    }

    // ------------------------------------------------------------------
    // Haltbarkeits-Warnung (Break-Geräusch ab 30 Haltbarkeit)
    // ------------------------------------------------------------------

    private static boolean low(ItemStack st) {
        if (st.isEmpty() || !st.isDamageableItem()) return false;
        int left = st.getMaxDamage() - st.getDamageValue();
        return left > 0 && left <= 30;
    }

    private static void warnTool(Minecraft mc) {
        if (low(mc.player.getMainHandItem())) breakSound(mc);
    }

    private static void warnArmor(Minecraft mc) {
        for (EquipmentSlot s : ARMOR) {
            if (low(mc.player.getItemBySlot(s))) {
                breakSound(mc);
                return;
            }
        }
    }

    private static void breakSound(Minecraft mc) {
        try {
            Object o = SoundEvents.class.getField("ITEM_BREAK").get(null);
            SoundEvent se = o instanceof Holder<?> h ? (SoundEvent) h.value() : (SoundEvent) o;
            mc.player.playSound(se, 1.0f, 1.0f);
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Jump Reset: Zeitabstand zwischen Sprung und Treffer in Millisekunden
    // ------------------------------------------------------------------

    /** Wird pro Tick UND pro Bild aufgerufen, damit Sprung und Treffer möglichst genau erfasst werden. */
    private static void pollJr(Minecraft mc) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        boolean jk = mc.options.keyJump.isDown();
        if (jk && !prevJkJr) {
            jrLastJumpMs = now;
            if (jrAwaiting && now - jrHitMs <= 400) jrEvaluate((int) (now - jrHitMs), now);
        }
        prevJkJr = jk;

        int ph = mc.player.hurtTime;
        if (ph > prevHurtJr && ph > 0) {
            jrHits++;
            jrHitMs = now;
            if (now - jrLastJumpMs <= 400) {
                jrEvaluate((int) (jrLastJumpMs - now), now);
            } else {
                jrAwaiting = true;
            }
        }
        prevHurtJr = ph;

        if (jrAwaiting && now - jrHitMs > 400) {
            jrAwaiting = false;
            jrGrade = 5;
            jrDt = 0;
            jrFeedbackMs = now;
        }
    }

    private static void jrEvaluate(int dt, long now) {
        jrAwaiting = false;
        int ad = Math.abs(dt);
        if (ad <= 75) {
            jrGrade = 0;
            jrPerfect++;
        } else if (ad <= 150) {
            jrGrade = 1;
            jrGood++;
        } else if (ad <= 250) {
            jrGrade = 2;
        } else {
            jrGrade = dt < 0 ? 3 : 4;
        }
        jrDt = dt;
        jrFeedbackMs = now;
    }

    /** Hauptschalter aus: alles Aktive zurücksetzen (Sprint, Zoom, Hit Color). */
    private static void cleanup(Minecraft mc, Config c) {
        if (sprintWasOn) {
            mc.options.keySprint.setDown(false);
            sprintWasOn = false;
        }
        zoomTick(mc, c);
        HitColor.tick();
    }

    public static void resetJr() {
        jrHits = 0;
        jrPerfect = 0;
        jrGood = 0;
        jrGrade = -1;
    }

    private static boolean shieldOnCd(Minecraft mc) {
        try {
            var cds = mc.player.getCooldowns();
            if (cds.isOnCooldown(new ItemStack(Items.SHIELD))) return true;
            ItemStack m = mc.player.getMainHandItem(), o = mc.player.getOffhandItem();
            return (m.is(Items.SHIELD) && cds.isOnCooldown(m)) || (o.is(Items.SHIELD) && cds.isOnCooldown(o));
        } catch (Throwable t) {
            return false;
        }
    }

    /** Zoom: solange die Zoom-Taste gehalten wird, wird das Sichtfeld (FOV) verkleinert. */
    private static void zoomTick(Minecraft mc, Config c) {
        boolean want = c.enabled && c.zoom && SharkusExtras.ZOOM != null && mc.player != null && SharkusExtras.ZOOM.isDown();
        try {
            if (want) {
                if (!zooming) {
                    savedFov = mc.options.fov().get();
                    zooming = true;
                }
                mc.options.fov().set(Math.max(30, Math.min(110, c.zoomFov)));
            } else if (zooming) {
                mc.options.fov().set(savedFov);
                zooming = false;
            }
        } catch (Throwable ignored) {
            zooming = false;
        }
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("sharkus-extras", "hud"), Hud::render);
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR,
                original -> (g, d) -> renderCrosshair(original, g, d));
    }

    public static boolean isTeammate(Minecraft mc, Player p, Config c) {
        if (p.getTeam() != null && mc.player.getTeam() != null && p.isAlliedTo(mc.player)) return true;
        String n = p.getName().getString();
        for (String s : c.teamNames.split(",")) {
            if (!s.isBlank() && s.trim().equalsIgnoreCase(n)) return true;
        }
        return false;
    }

    private static LivingEntity target(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    // ------------------------------------------------------------------
    // Fadenkreuz (Studio)
    // ------------------------------------------------------------------

    private static Method samCache;

    private static void callOriginal(HudElement original, GuiGraphicsExtractor g, DeltaTracker d) {
        try {
            if (samCache == null) {
                for (Method m : HudElement.class.getMethods()) {
                    if (Modifier.isAbstract(m.getModifiers())) {
                        samCache = m;
                        break;
                    }
                }
            }
            if (samCache != null) samCache.invoke(original, g, d);
        } catch (Throwable ignored) { }
    }

    private static void renderCrosshair(HudElement original, GuiGraphicsExtractor g, DeltaTracker d) {
        Config c = Config.I;
        boolean studio = c.enabled && c.crosshairStudio;
        if (!(studio && c.csHideVanilla)) callOriginal(original, g, d);
        if (!studio) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = (sw - 15) / 2 + 7, cy = (sh - 15) / 2 + 7;
        int color = 0xFF000000 | (c.csRgb & 0xFFFFFF);
        if (c.crosshairIndicator && target(mc) != null) {
            color = mc.player.getAttackStrengthScale(0f) >= 1.0f ? 0xFF30FF30 : 0xFFFF3030;
        }
        CrosshairDraw.draw(g, c.csShape, c.csSize, c.csThick, c.csGap, color, c.csOutline, c.csDot, cx, cy);
    }

    // ------------------------------------------------------------------

    private static void key(GuiGraphicsExtractor g, Font font, String label, int x, int y, int w, int h, boolean on) {
        g.fill(x, y, x + w, y + h, on ? 0xCCFFFFFF : 0x80000000);
        g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, on ? 0xFF000000 : 0xFFFFFFFF);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Config c = Config.I;
        if (!c.enabled) return;
        if (c.jumpReset) pollJr(mc);
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        LivingEntity target = target(mc);
        Inventory inv = mc.player.getInventory();
        long nowMs = System.currentTimeMillis();

        // ---------- FPS / CPS / Ping ----------
        if (c.fps) at(g, "fps", sw, sh, () -> g.text(font, "FPS: " + mc.getFps(), 0, 0, 0xFFFFFFFF));
        if (c.cps) at(g, "cps", sw, sh, () -> g.text(font, "CPS: " + clicks.size(), 0, 0, 0xFFFFFFFF));
        if (c.ping && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            String s = "Ping: " + (info == null ? "?" : info.getLatency()) + " ms";
            at(g, "ping", sw, sh, () -> g.text(font, s, 0, 0, 0xFFFFFFFF));
        }

        // ---------- Toggle Sprint ----------
        if (c.toggleSprint) {
            boolean sp = mc.player.isSprinting();
            at(g, "sprint", sw, sh, () -> g.text(font, sp ? "Sprint: AN (Toggle)" : "Sprint: Toggle", 0, 0,
                    sp ? 0xFF55FF55 : 0xFFAAAAAA));
        }

        // ---------- Armor Display ----------
        if (c.armorDisplay) {
            at(g, "armor", sw, sh, () -> {
                int y = 0;
                for (EquipmentSlot slot : ARMOR) {
                    ItemStack stack = mc.player.getItemBySlot(slot);
                    if (stack.isEmpty()) continue;
                    g.item(stack, 0, y);
                    if (stack.isDamageableItem()) {
                        int left = stack.getMaxDamage() - stack.getDamageValue();
                        g.text(font, String.valueOf(left), 20, y + 4, 0xFFFFFFFF);
                    }
                    y += 18;
                }
            });
        }

        // ---------- Totem Counter / Item Counter ----------
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, ItemStack> samples = new LinkedHashMap<>();
        if (c.totemCounter || c.itemCounter) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (st.isEmpty()) continue;
                String k = ItemFx.key(st);
                counts.merge(k, st.getCount(), Integer::sum);
                samples.putIfAbsent(k, st.copyWithCount(1));
            }
        }
        if (c.totemCounter) {
            int n = counts.getOrDefault("minecraft:totem_of_undying", 0);
            at(g, "totem", sw, sh, () -> {
                g.item(new ItemStack(Items.TOTEM_OF_UNDYING), 0, 0);
                g.text(font, "x" + n, 18, 4, n > 0 ? 0xFFFFFFFF : 0xFFFF5555);
            });
        }
        if (c.itemCounter) {
            at(g, "items", sw, sh, () -> {
                int y = 0;
                for (ItemCounters.Preset p : ItemCounters.PRESETS) {
                    if (!ItemCounters.enabled(p)) continue;
                    int n = counts.getOrDefault(p.id(), 0);
                    int warn = ItemCounters.warn(p);
                    ItemStack icon = ItemFx.stackFor(p.id());
                    if (icon != null) g.item(icon, 0, y);
                    String num = "x" + n;
                    g.text(font, num, 20, y + 4, 0xFF000000 | p.rgb());
                    if (warn > 0 && n <= warn) g.text(font, "!", 24 + font.width(num), y + 4, 0xFFFF3030);
                    y += 18;
                }
                for (String raw : c.itemCounterIds.split(",")) {
                    String id = raw.trim();
                    if (id.isEmpty()) continue;
                    if (!id.contains(":")) id = "minecraft:" + id;
                    int n = counts.getOrDefault(id, 0);
                    ItemStack smp = ItemFx.stackFor(id);
                    if (smp != null) g.item(smp, 0, y);
                    g.text(font, "x" + n, 20, y + 4, n > 0 ? 0xFFFFFFFF : 0xFFFF5555);
                    y += 18;
                }
            });
        }

        // ---------- Combo / Reach / Jump Reset ----------
        if (c.combo) {
            at(g, "combo", sw, sh, () -> g.text(font, "Combo: " + comboN, 0, 0, comboN > 0 ? 0xFFFFAA00 : 0xFFAAAAAA));
        }
        if (c.reach) {
            boolean fresh = nowMs - lastReachMs < 3000;
            String s = lastReach < 0 ? "Reach: -" : String.format("Reach: %.2f", lastReach);
            at(g, "reach", sw, sh, () -> g.text(font, s, 0, 0, fresh ? 0xFF55FFFF : 0xFFAAAAAA));
        }
        if (c.jumpReset) {
            long age = nowMs - jrFeedbackMs;
            boolean fresh = jrGrade >= 0 && age < c.jrDurationMs;
            double sl = Math.min(1.0, age / 180.0);
            int slideOff = (int) ((1.0 - sl * sl * (3 - 2 * sl)) * -8);
            int tries = jrHits;
            int hitRate = tries > 0 ? (jrPerfect + jrGood) * 100 / tries : 0;
            String stats = "Perfect " + jrPerfect + "  " + L.t("Gut") + " " + jrGood + "  " + L.t("Quote") + " " + hitRate + "%";
            int grade = Math.max(0, jrGrade);
            String head = L.t(JR_NAMES[grade]) + (grade == 5 ? "" : String.format("  %+d ms", jrDt));
            int col = JR_COLORS[grade];
            at(g, "jr", sw, sh, () -> {
                if (!fresh) {
                    g.text(font, "Jump Reset", 0, 0, 0xFFAAAAAA);
                } else if (c.jrStyle == 0) {
                    int w = font.width(head) + 12;
                    Ui.round(g, 0, slideOff, w, 14, 7, 0xCC000000);
                    g.text(font, head, 6, slideOff + 3, col);
                } else {
                    g.text(font, head, 0, slideOff, col);
                    int bw = 120, by = slideOff + 11, mid = bw / 2;
                    g.fill(0, by, bw, by + 6, 0xAA000000);
                    g.fill(mid - 50, by, mid + 50, by + 6, 0x66FFFF55);
                    g.fill(mid - 30, by, mid + 30, by + 6, 0x6655AAFF);
                    g.fill(mid - 15, by, mid + 15, by + 6, 0xAA55FF55);
                    if (jrGrade != 5) {
                        int mx = mid + (int) Math.max(-60, Math.min(59, jrDt * 0.2));
                        g.fill(mx, by - 2, mx + 2, by + 8, 0xFFFFFFFF);
                    }
                }
                g.text(font, stats, 0, 24, 0xFFFFFFFF);
            });
        }

        // ---------- Inventar-Vorschau ----------
        if (c.invHud) {
            at(g, "inv", sw, sh, () -> {
                g.fill(-2, -2, 164, 56, 0x66000000);
                for (int r = 0; r < 3; r++) {
                    for (int col = 0; col < 9; col++) {
                        g.fill(col * 18, r * 18, col * 18 + 17, r * 18 + 17, 0x40FFFFFF);
                        ItemStack st = inv.getItem(9 + r * 9 + col);
                        if (st.isEmpty()) continue;
                        g.item(st, col * 18 + 1, r * 18 + 1);
                        if (st.getCount() > 1) {
                            String n = String.valueOf(st.getCount());
                            g.text(font, n, col * 18 + 17 - font.width(n), r * 18 + 9, 0xFFFFFFFF);
                        }
                    }
                }
            });
        }

        // ---------- Keystrokes ----------
        if (c.keystrokes) {
            long win = mc.getWindow().handle();
            boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            at(g, "keys", sw, sh, () -> {
                key(g, font, "W", 22, 0, 20, 20, mc.options.keyUp.isDown());
                key(g, font, "A", 0, 22, 20, 20, mc.options.keyLeft.isDown());
                key(g, font, "S", 22, 22, 20, 20, mc.options.keyDown.isDown());
                key(g, font, "D", 44, 22, 20, 20, mc.options.keyRight.isDown());
                key(g, font, "SPACE", 0, 44, 64, 14, mc.options.keyJump.isDown());
                key(g, font, "LMB", 0, 60, 31, 14, lmb);
                key(g, font, "RMB", 33, 60, 31, 14, rmb);
            });
        }

        // ---------- Highlighter + Pre Inventory auf der Hotbar ----------
        if (c.highlighter || c.preInv) {
            for (int i = 0; i < 9; i++) {
                int x = sw / 2 - 90 + i * 20 + 2, y = sh - 19;
                ItemStack st = inv.getItem(i);
                ItemFx.highlight(g, st, x, y);
                ItemFx.ghost(g, i, st, x, y);
            }
        }

        // ---------- TPS / Koordinaten / Server ----------
        if (c.tps) {
            String s = String.format("TPS: %.1f", tpsValue);
            int col = tpsValue >= 19 ? 0xFF55FF55 : (tpsValue >= 15 ? 0xFFFFFF55 : 0xFFFF5555);
            at(g, "tps", sw, sh, () -> g.text(font, s, 0, 0, col));
        }
        if (c.coords) {
            String s = String.format("XYZ: %.1f / %.1f / %.1f  %s", mc.player.getX(), mc.player.getY(),
                    mc.player.getZ(), mc.player.getDirection().getName());
            at(g, "coords", sw, sh, () -> g.text(font, s, 0, 0, 0xFFFFFFFF));
        }
        if (c.serverInfo) {
            ServerData sd = mc.getCurrentServer();
            String l1 = sd == null ? "Einzelspieler" : (sd.name == null || sd.name.isEmpty() ? "Server" : sd.name);
            String l2 = sd == null ? "Lokale Welt" : sd.ip;
            PlayerInfo pi = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
            String ms = (sd != null && pi != null) ? "  " + pi.getLatency() + " ms" : "";
            int pcol = pi == null ? 0xFFAAAAAA : (pi.getLatency() < 60 ? 0xFF55FF55 : (pi.getLatency() < 150 ? 0xFFFFFF55 : 0xFFFF5555));
            at(g, "server", sw, sh, () -> {
                int w = Math.max(font.width(l1), font.width(l2 + ms)) + 14;
                g.fill(0, 0, w, 26, 0x99000000);
                g.fill(0, 0, 2, 26, pcol);
                g.text(font, l1, 8, 3, 0xFFFFFFFF);
                g.text(font, l2, 8, 14, 0xFFAAAAAA);
                if (!ms.isEmpty()) g.text(font, ms, 8 + font.width(l2), 14, pcol);
            });
        }

        // ---------- Pferde-Werte ----------
        if (c.horseStats) renderHorses(g, font, mc, sw, sh, pt, target);

        // ---------- Shield Status (wie die Mods: rot = gesperrt, Übergang zu grün) ----------
        if (c.shieldStatus) {
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();
            ItemStack held = main.is(Items.SHIELD) ? main : (off.is(Items.SHIELD) ? off : ItemStack.EMPTY);
            boolean cd = shieldOnCd(mc);
            if (!held.isEmpty() || cd) {
                ItemStack icon = held.isEmpty() ? new ItemStack(Items.SHIELD) : held;
                long rem = cd ? Math.max(0, SHIELD_CD_MS - (nowMs - shieldCdStart)) : 0;
                double ratio = cd ? 1.0 - rem / (double) SHIELD_CD_MS : 1.0;
                int rgb = java.awt.Color.HSBtoRGB((float) (ratio * 0.33), 1f, 1f) & 0xFFFFFF;
                String txt = cd ? String.format("%.1fs", rem / 1000.0) : (mc.player.isBlocking() ? "BLOCKT" : "BEREIT");
                at(g, "shield", sw, sh, () -> {
                    g.fill(6, -3, 34, 17, 0x66000000);
                    g.item(icon, 12, 0);
                    g.fill(12, 0, 28, 16, (0x80 << 24) | rgb);
                    g.text(font, txt, 20 - font.width(txt) / 2, 19, 0xFF000000 | rgb);
                });
            }

            // Gegner: nach einem Axt-Treffer auf einen blockenden Spieler läuft sein Timer über dem Spieler
            if (!shieldOff.isEmpty()) {
                shieldOff.entrySet().removeIf(e -> e.getValue() < nowMs);
                Cam cam = new Cam(mc, pt, sw, sh);
                for (Player p : mc.level.players()) {
                    Long until = shieldOff.get(p.getUUID());
                    if (until == null) continue;
                    long rem = until - nowMs;
                    double ratio = 1.0 - rem / (double) SHIELD_CD_MS;
                    int rgb = java.awt.Color.HSBtoRGB((float) (ratio * 0.33), 1f, 1f) & 0xFFFFFF;
                    double ex = Cam.lerp(p.xo, p.getX(), pt), ey = Cam.lerp(p.yo, p.getY(), pt), ez = Cam.lerp(p.zo, p.getZ(), pt);
                    double[] pr = cam.proj(ex, ey + p.getBbHeight() * 0.65, ez);
                    if (pr == null) continue;
                    String t = String.format("%.1fs", rem / 1000.0);
                    int x = (int) pr[0], y = (int) pr[1];
                    g.fill(x - 14, y - 10, x + 14, y + 20, 0x77000000);
                    g.item(new ItemStack(Items.SHIELD), x - 8, y - 8);
                    g.fill(x - 8, y - 8, x + 8, y + 8, (0x90 << 24) | rgb);
                    g.text(font, t, x - font.width(t) / 2, y + 10, 0xFF000000 | rgb);
                }
            }
        }

        // ---------- Crosshair Indicator ----------
        if (c.crosshairIndicator) {
            float strength = mc.player.getAttackStrengthScale(0f);
            int cx = (sw - 15) / 2 + 7, cy = (sh - 15) / 2 + 7;
            if (target != null && !c.crosshairStudio) {
                int color = strength >= 1.0f ? 0xFF30FF30 : 0xFFFF3030;
                drawIndicator(g, c.crosshairStyle, cx, cy, color);
            }
            if (c.crosshairBar && strength < 1.0f) {
                g.fill(cx - 8, cy + 10, cx + 9, cy + 12, 0x80000000);
                g.fill(cx - 7, cy + 10, cx - 7 + (int) (15 * strength), cy + 12, 0xFFFFFFFF);
            }
        }

        // ---------- Team Tracker ----------
        if (c.teamTracker) {
            Cam cam = new Cam(mc, pt, sw, sh);
            int ty = 4;
            for (Player p : mc.level.players()) {
                if (p == mc.player || !isTeammate(mc, p, c)) continue;
                double dist = mc.player.distanceTo(p);
                String name = Names.apply(p.getName().getString());

                String s = name + "  " + (int) dist + "m  " + (int) p.getHealth() + " HP";
                g.text(font, s, sw - font.width(s) - 4, ty, 0xFF55FF55);
                ty += 10;

                double ex = Cam.lerp(p.xo, p.getX(), pt), ey = Cam.lerp(p.yo, p.getY(), pt), ez = Cam.lerp(p.zo, p.getZ(), pt);
                String label = name + " " + (int) dist + "m";
                double[] pr = cam.proj(ex, ey + p.getBbHeight() + 1.9, ez);
                int half = font.width(label) / 2;
                int sx, sy;
                String shown = label;
                if (pr == null) {
                    boolean right = cam.side(ex, ez) >= 0;
                    sx = right ? sw - 14 - half : 14 + half;
                    sy = sh / 2;
                    shown = right ? label + " >" : "< " + label;
                } else {
                    sx = (int) Math.max(14 + half, Math.min(sw - 14 - half, pr[0]));
                    sy = (int) Math.max(14, Math.min(sh - 14, pr[1]));
                }
                g.text(font, shown, sx - font.width(shown) / 2, sy, 0xFF55FF55);
            }
        }
    }

    private static void renderHorses(GuiGraphicsExtractor g, Font font, Minecraft mc, int sw, int sh,
                                     float pt, LivingEntity target) {
        LivingEntity focus = null;
        if (mc.player.getVehicle() instanceof LivingEntity v && Horses.isHorse(v)) focus = v;
        else if (target != null && Horses.isHorse(target)) focus = target;
        if (focus != null) {
            LivingEntity f = focus;
            at(g, "horse", sw, sh, () -> Horses.panel(g, font, f));
        }

        List<LivingEntity> hs = new ArrayList<>();
        for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(32.0), x -> true)) {
            if (e instanceof LivingEntity le && Horses.isHorse(le)) hs.add(le);
        }
        if (hs.isEmpty()) return;

        LivingEntity best = null;
        double bestR = -1;
        for (LivingEntity h : hs) {
            double r = Horses.rating(h);
            if (r > bestR) {
                bestR = r;
                best = h;
            }
        }

        Cam cam = new Cam(mc, pt, sw, sh);
        for (LivingEntity h : hs) {
            double ex = Cam.lerp(h.xo, h.getX(), pt), ey = Cam.lerp(h.yo, h.getY(), pt), ez = Cam.lerp(h.zo, h.getZ(), pt);
            double[] pr = cam.proj(ex, ey + h.getBbHeight() + 0.7, ez);
            if (pr == null) continue;
            double[] st = Horses.stats(h);
            String a = (h == best ? "\u2605 " : "") + "S " + String.format("%.1f", st[0]);
            String b = st[1] >= 0 ? "  J " + String.format("%.1f", st[1]) : "";
            String c2 = "  HP " + String.format("%.0f", st[3]);
            int total = font.width(a) + font.width(b) + font.width(c2);
            int x = (int) pr[0] - total / 2, y = (int) pr[1];
            g.fill(x - 2, y - 2, x + total + 2, y + 10, 0x66000000);
            g.text(font, a, x, y, Horses.color(Horses.ratio(st[0], Horses.SPEED_MIN, Horses.SPEED_MAX)));
            x += font.width(a);
            g.text(font, b, x, y, Horses.color(Horses.ratio(st[1], Horses.JUMP_MIN, Horses.JUMP_MAX)));
            x += font.width(b);
            g.text(font, c2, x, y, Horses.color(Horses.ratio(st[3], Horses.HP_MIN, Horses.HP_MAX)));
        }
    }

    private static void drawIndicator(GuiGraphicsExtractor g, int style, int cx, int cy, int color) {
        switch (style) {
            case 1 -> CrosshairDraw.draw(g, 2, 3, 1, 0, color, false, false, cx, cy);
            case 2 -> CrosshairDraw.draw(g, 3, 5, 1, 2, color, false, false, cx, cy);
            case 3 -> CrosshairDraw.draw(g, 4, 4, 1, 3, color, false, false, cx, cy);
            case 4 -> CrosshairDraw.draw(g, 5, 4, 1, 2, color, false, false, cx, cy);
            default -> CrosshairDraw.draw(g, 0, 6, 1, 0, color, false, false, cx, cy);
        }
    }
}
