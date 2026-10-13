package sharkus.extras;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Pferde-Werte wie in den Horse-Stats-Mods: Speed (Blöcke/s), Sprunghöhe (Blöcke), Leben. */
final class Horses {
    // Grenzen bei der Spawn-Generierung (Wiki)
    static final double SPEED_MIN = 4.86, SPEED_MAX = 14.57;
    static final double JUMP_MIN = 1.09, JUMP_MAX = 5.3;
    static final double HP_MIN = 15, HP_MAX = 30;

    private Horses() { }

    static boolean isHorse(Entity e) {
        String p = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return p.equals("horse") || p.equals("donkey") || p.equals("mule") || p.equals("llama")
                || p.equals("trader_llama") || p.equals("camel") || p.equals("skeleton_horse")
                || p.equals("zombie_horse");
    }

    static double jumpBlocks(double x) {
        return -0.1817584952 * x * x * x + 3.689713992 * x * x + 2.128599134 * x - 0.343930367;
    }

    /** {speed b/s, jump Blöcke (-1 = unbekannt), health, maxHealth} */
    static double[] stats(LivingEntity e) {
        double speed = 0, jump = -1;
        try {
            speed = e.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17;
        } catch (Throwable ignored) { }
        try {
            jump = jumpBlocks(e.getAttributeValue(Attributes.JUMP_STRENGTH));
        } catch (Throwable ignored) { }
        return new double[]{speed, jump, e.getHealth(), e.getMaxHealth()};
    }

    static double ratio(double v, double min, double max) {
        return Math.max(0, Math.min(1, (v - min) / (max - min)));
    }

    static double rating(LivingEntity e) {
        double[] s = stats(e);
        double r = ratio(s[0], SPEED_MIN, SPEED_MAX) + ratio(s[3], HP_MIN, HP_MAX);
        int n = 2;
        if (s[1] >= 0) {
            r += ratio(s[1], JUMP_MIN, JUMP_MAX);
            n++;
        }
        return r / n;
    }

    static int color(double r) {
        if (r >= 0.95) return 0xFFFFD700;
        if (r >= 0.66) return 0xFF55FF55;
        if (r >= 0.33) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static void bar(GuiGraphicsExtractor g, Font font, int y, String label, String value, double r) {
        int col = color(r);
        g.text(font, label, 0, y, 0xFFFFFFFF);
        g.text(font, value, 48, y, col);
        g.fill(88, y + 1, 128, y + 7, 0xFF000000);
        g.fill(89, y + 2, 89 + (int) (38 * r), y + 6, col);
    }

    /** Kleines Panel mit den Werten (Position/Größe im HUD-Editor einstellbar). */
    static void panel(GuiGraphicsExtractor g, Font font, LivingEntity e) {
        double[] s = stats(e);
        g.fill(-3, -3, 133, 41, 0x66000000);
        g.text(font, e.getName().getString(), 0, 0, 0xFFFFFFFF);
        bar(g, font, 10, "Speed", String.format("%.1f", s[0]), ratio(s[0], SPEED_MIN, SPEED_MAX));
        if (s[1] >= 0) bar(g, font, 20, "Jump", String.format("%.2f", s[1]), ratio(s[1], JUMP_MIN, JUMP_MAX));
        bar(g, font, 30, "Leben", String.format("%.0f", s[3]), ratio(s[3], HP_MIN, HP_MAX));
    }
}
