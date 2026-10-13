package sharkus.extras;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * FPS Optimizer: stellt Grafik-Optionen über Presets ein (per Reflection, damit unbekannte Optionen einfach
 * übersprungen werden) und kann die Sichtweite automatisch an eine Ziel-FPS anpassen.
 */
final class FpsOpt {
    static final String[] NAMES = {"Aus (Original)", "Leicht", "Ausgewogen", "Maximum"};

    // Option, Leicht, Ausgewogen, Maximum
    private static final Object[][] PRESET = {
            {"renderDistance", 12, 8, 6},
            {"simulationDistance", 10, 6, 5},
            {"particles", "DECREASED", "MINIMAL", "MINIMAL"},
            {"cloudStatus", "FAST", "OFF", "OFF"},
            {"entityDistanceScaling", 0.75, 0.5, 0.5},
            {"entityShadows", true, false, false},
            {"ambientOcclusion", true, true, false},
            {"biomeBlendRadius", 3, 1, 0},
            {"mipmapLevels", 4, 2, 0},
            {"framerateLimit", 260, 260, 260},
            {"enableVsync", false, false, false}
    };

    static String lastResult = "";

    private static int tickCount;
    private static long fpsSum;
    private static int fpsN;

    private FpsOpt() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object value, Object cur) {
        if (cur instanceof Enum<?> e) return Enum.valueOf((Class) e.getDeclaringClass(), String.valueOf(value));
        if (cur instanceof Integer) return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
        if (cur instanceof Double) return value instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(value));
        if (cur instanceof Boolean) return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
        return value;
    }

    private static Object instance(Options o, String name) throws Exception {
        Method m = o.getClass().getMethod(name);
        return m.invoke(o);
    }

    private static Object get(Object inst) throws Exception {
        return inst.getClass().getMethod("get").invoke(inst);
    }

    static boolean setOpt(Options o, String name, Object value, boolean backup) {
        try {
            Object inst = instance(o, name);
            Object cur = get(inst);
            if (backup && !Config.I.optBackup.containsKey(name)) {
                Config.I.optBackup.put(name, cur instanceof Enum<?> e ? e.name() : String.valueOf(cur));
            }
            inst.getClass().getMethod("set", Object.class).invoke(inst, convert(value, cur));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static Integer getInt(Options o, String name) {
        try {
            Object v = get(instance(o, name));
            return v instanceof Number n ? n.intValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static void apply(int preset) {
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;
        Config c = Config.I;
        int ok = 0, total = 0;
        if (preset == 0) {
            for (Map.Entry<String, String> e : c.optBackup.entrySet()) {
                total++;
                if (setOpt(o, e.getKey(), e.getValue(), false)) ok++;
            }
            c.optBackup.clear();
        } else {
            for (Object[] row : PRESET) {
                total++;
                if (setOpt(o, (String) row[0], row[preset], true)) ok++;
            }
        }
        c.optPreset = preset;
        lastResult = ok + " / " + total;
        Config.save();
    }

    /** Auto-Modus: passt die Sichtweite alle 5 Sekunden an die Ziel-FPS an. */
    static void tick(Minecraft mc) {
        Config c = Config.I;
        if (!c.enabled || !c.fpsAuto || mc.player == null) {
            tickCount = 0;
            fpsSum = 0;
            fpsN = 0;
            return;
        }
        fpsSum += mc.getFps();
        fpsN++;
        if (++tickCount < 100) return;
        double avg = fpsSum / (double) Math.max(1, fpsN);
        tickCount = 0;
        fpsSum = 0;
        fpsN = 0;
        Integer rd = getInt(mc.options, "renderDistance");
        if (rd == null) return;
        int min = Math.max(2, c.fpsMinDist), max = Math.max(min, c.fpsMaxDist);
        if (avg < c.fpsTarget * 0.9 && rd > min) setOpt(mc.options, "renderDistance", rd - 1, true);
        else if (avg > c.fpsTarget * 1.3 && rd < max) setOpt(mc.options, "renderDistance", rd + 1, true);
    }
}
