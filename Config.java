package sharkus.extras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sharkus-extras.json");
    public static Config I = new Config();

    // Schalter
    public boolean armorDisplay, healthIndicator, hitColor, nametags, blockOutlines, teamTracker,
            itemPhysics, cps, fps = true, ping, crosshairIndicator, shieldStatus, motionBlur,
            highlighter, keystrokes, toggleSprint, invHud, crosshairStudio,
            hitboxColors, preInv, totemCounter, jumpReset, combo, reach, durWarn, itemCounter,
            horseStats, tps, coords, serverInfo, zoom = true;

    // Nametags
    public String ownNameOverride = "";
    public Map<String, String> nameReplacements = new HashMap<>();

    // Farben
    public int hitRgb = 0xFF0000;
    public int hitAlpha = 50;          // 0-100 Prozent
    public int outlineRgb = 0x00FFFF;
    public int outlineThickness = 2;   // 1-4
    public int highlightRgb = 0xFFFF00; // alt, nicht mehr benutzt
    public String teamNames = "";

    // Hitbox-Farben
    public int hbPlayerRgb = 0xFF5555;
    public int hbTeamRgb = 0x55FF55;
    public int hbHostileRgb = 0xFFAA00;
    public int hbPassiveRgb = 0x55FFFF;
    public int hbItemRgb = 0xFFFF55;
    public int hbProjectileRgb = 0xFFFFFF;

    // Highlighter (Inventar)
    public int hlWeaponRgb = 0xFF5555;
    public int hlArmorRgb = 0x5599FF;
    public int hlTotemRgb = 0xFFD700;
    public int hlPearlRgb = 0x00CCCC;
    public int hlAppleRgb = 0xFFAA00;
    public int hlPotionRgb = 0xFF55FF;
    public int hlEnchantedRgb = 0xAA55FF;

    // Stile
    public int healthStyle = 0;        // 0 Herzen, 1 Kompakt, 2 Leiste
    public int crosshairStyle = 0;     // Indicator
    public boolean crosshairBar = true;

    // Crosshair Studio
    public int csShape = 0;
    public int csSize = 5;
    public int csThick = 1;
    public int csGap = 2;
    public int csRgb = 0xFFFFFF;
    public boolean csOutline = true;
    public boolean csDot = false;
    public boolean csHideVanilla = true;

    // Pre Inventory (gespeichertes Inventar: Slot -> Item-ID)
    public Map<Integer, String> kit = new HashMap<>();

    // Allgemein
    public boolean enabled = true;     // Hauptschalter: aus = Client komplett inaktiv
    public boolean fullbright = false;
    public boolean lowImpact = false;  // Sparmodus für die Mod selbst
    public String lang = "de";
    public int accent = 0;

    // Jump Reset
    public int jrStyle = 1;            // 0 Pille, 1 Zeitleiste
    public int jrDurationMs = 2500;

    // Shield Status (wie die Mod "Shield Statuses": Farben + Übergang)
    public int shDisabledRgb = 0xFF0000;
    public int shEnabledRgb = 0x00FF00;
    public boolean shInterpolate = true;
    public boolean shOthers = true;

    // FPS Optimizer
    public int optPreset = 0;          // 0 Aus, 1 Leicht, 2 Ausgewogen, 3 Maximum
    public Map<String, String> optBackup = new HashMap<>();
    public boolean fpsAuto = false;
    public int fpsTarget = 120;
    public int fpsMinDist = 6;
    public int fpsMaxDist = 16;

    // Zoom
    public int zoomFov = 30;           // 30-110, kleiner = mehr Zoom

    // Highlighter: gewählte Items (Item-ID -> Farbe) und Standard-Kategorien
    public Map<String, Integer> hlItems = new HashMap<>();
    public boolean hlCategories = true;

    // Item Counter (PvP-Presets: an/aus und Warnschwelle)
    public Map<String, Boolean> counterOn = new HashMap<>();
    public Map<String, Integer> counterWarn = new HashMap<>();
    public String itemCounterIds = "";

    // HUD-Positionen und Größen (Schlüssel siehe Hud.ELS)
    public Map<String, int[]> hudPos = new HashMap<>();
    public Map<String, Float> hudScale = new HashMap<>();

    public float sc(String key) {
        Float f = hudScale.get(key);
        return f == null ? 1.0f : f;
    }

    public void resetPositions() {
        hudPos.clear();
        hudScale.clear();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config loaded = GSON.fromJson(Files.readString(FILE), Config.class);
                if (loaded != null) I = loaded;
            }
        } catch (Exception e) { e.printStackTrace(); }
        if (I.nameReplacements == null) I.nameReplacements = new HashMap<>();
        if (I.teamNames == null) I.teamNames = "";
        if (I.ownNameOverride == null) I.ownNameOverride = "";
        if (I.kit == null) I.kit = new HashMap<>();
        if (I.hudPos == null) I.hudPos = new HashMap<>();
        if (I.hudScale == null) I.hudScale = new HashMap<>();
        if (I.itemCounterIds == null) I.itemCounterIds = "";
        if (I.hlItems == null) I.hlItems = new HashMap<>();
        if (I.counterOn == null) I.counterOn = new HashMap<>();
        if (I.optBackup == null) I.optBackup = new HashMap<>();
        if (I.lang == null) I.lang = "de";
        if (I.counterWarn == null) I.counterWarn = new HashMap<>();
        if ("minecraft:ender_pearl,minecraft:golden_apple,minecraft:arrow".equals(I.itemCounterIds)) I.itemCounterIds = "";
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(I)); } catch (Exception e) { e.printStackTrace(); }
    }
}
