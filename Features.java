package sharkus.extras;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Alle Funktionen des Clients mit Kategorie, Beschreibung und (optionalen) Einstellungen. */
final class Features {
    record F(String name, String desc, String cat, Supplier<Boolean> get, Consumer<Boolean> set,
             Function<Screen, Screen> opts, boolean soon) { }

    private Features() { }

    static List<F> all() {
        Config c = Config.I;
        List<F> l = new ArrayList<>();

        // ---------------- Kampf ----------------
        l.add(new F("Jump Reset",
                "Hilft Wettkampf-Spielern mit visuellem Feedback zum perfekten Timing für den Jump Reset und verringert so den Rückstoß, den du bekommst.",
                "Kampf", () -> c.jumpReset, v -> c.jumpReset = v, null, false));
        l.add(new F("Shield Status",
                "Zeigt deinen Schild-Cooldown: rot = gesperrt, danach fließender Übergang zu grün = bereit. Bei Gegnern läuft nach einem Axt-Treffer ein Timer über dem Spieler.",
                "Kampf", () -> c.shieldStatus, v -> c.shieldStatus = v, null, false));
        l.add(new F("Health Indicator",
                "Zeigt die Leben von Gegnern als Herzen, Herz mit Zahl oder Leiste über dem Kopf.",
                "Kampf", () -> c.healthIndicator, v -> c.healthIndicator = v, SettingsScreen::new, false));
        l.add(new F("Hit Color",
                "Färbt den roten Treffer-Blink von Gegnern in deiner Wunschfarbe und Stärke.",
                "Kampf", () -> c.hitColor, v -> c.hitColor = v, SettingsScreen::new, false));
        l.add(new F("Hitbox Colors",
                "Zeigt Hitboxen mit eigenen Farben für Spieler, Team, Monster, Tiere, Items und Geschosse.",
                "Kampf", () -> c.hitboxColors, v -> c.hitboxColors = v, ColorsScreen::new, false));
        l.add(new F("Combo Counter",
                "Zählt, wie viele Treffer du hintereinander landest, bis du selbst getroffen wirst.",
                "Kampf", () -> c.combo, v -> c.combo = v, null, false));
        l.add(new F("Reach Display",
                "Zeigt die Reichweite deines letzten Treffers in Blöcken.",
                "Kampf", () -> c.reach, v -> c.reach = v, null, false));
        l.add(new F("Crosshair Indicator",
                "Das Fadenkreuz färbt sich auf Gegnern (grün = Schlag bereit, rot = lädt) und zeigt einen Ladebalken.",
                "Kampf", () -> c.crosshairIndicator, v -> c.crosshairIndicator = v, SettingsScreen::new, false));
        l.add(new F("Team Tracker",
                "Zeigt Teammitglieder mit Name, Entfernung und Pfeil am Bildrand, auch durch Wände.",
                "Kampf", () -> c.teamTracker, v -> c.teamTracker = v, SettingsScreen::new, false));
        l.add(new F("Durability Warnung",
                "Spielt bei jedem Treffer das Break-Geräusch, wenn Waffe oder Rüstung unter 30 Haltbarkeit haben.",
                "Kampf", () -> c.durWarn, v -> c.durWarn = v, null, false));

        // ---------------- Anzeige ----------------
        l.add(new F("FPS", "Zeigt deine Bilder pro Sekunde.",
                "Anzeige", () -> c.fps, v -> c.fps = v, null, false));
        l.add(new F("CPS", "Zeigt deine Klicks pro Sekunde.",
                "Anzeige", () -> c.cps, v -> c.cps = v, null, false));
        l.add(new F("Ping", "Zeigt deinen Ping zum Server in Millisekunden.",
                "Anzeige", () -> c.ping, v -> c.ping = v, null, false));
        l.add(new F("TPS", "Zeigt die geschätzte Server-Geschwindigkeit (20 = perfekt).",
                "Anzeige", () -> c.tps, v -> c.tps = v, null, false));
        l.add(new F("Koordinaten", "Zeigt deine Position (XYZ) und Blickrichtung.",
                "Anzeige", () -> c.coords, v -> c.coords = v, null, false));
        l.add(new F("Server", "Zeigt Servername, Adresse und Ping in einer kleinen Karte.",
                "Anzeige", () -> c.serverInfo, v -> c.serverInfo = v, null, false));
        l.add(new F("Keystrokes", "Zeigt WASD, Leertaste und die Maustasten, gedrückte Tasten leuchten auf.",
                "Anzeige", () -> c.keystrokes, v -> c.keystrokes = v, null, false));
        l.add(new F("Armor Display", "Zeigt deine Rüstungsteile mit der übrigen Haltbarkeit.",
                "Anzeige", () -> c.armorDisplay, v -> c.armorDisplay = v, null, false));
        l.add(new F("Toggle Sprint", "Sprint bleibt an, solange du vorwärts läufst.",
                "Anzeige", () -> c.toggleSprint, v -> c.toggleSprint = v, null, false));
        l.add(new F("Pferde-Werte", "Zeigt Speed, Sprunghöhe und Leben von Pferden, Eseln, Lamas und Kamelen.",
                "Anzeige", () -> c.horseStats, v -> c.horseStats = v, null, false));

        // ---------------- Inventar ----------------
        l.add(new F("Item Counter",
                "Zählt PvP-Items wie Totems, Gapples, Crystals und Perlen. Unter der Warnschwelle erscheint ein rotes Ausrufezeichen.",
                "Inventar", () -> c.itemCounter, v -> c.itemCounter = v, ItemCounterScreen::new, false));
        l.add(new F("Highlighter",
                "Färbt ausgewählte Items im Inventar und in der Hotbar ein. Du suchst die Items selbst aus.",
                "Inventar", () -> c.highlighter, v -> c.highlighter = v,
                p -> new ItemPickerScreen(p, ItemPickerScreen.Mode.HIGHLIGHT), false));
        l.add(new F("Pre Inventory",
                "Speichere dein Inventar. Fehlt später etwas, siehst du es abgedunkelt am alten Platz.",
                "Inventar", () -> c.preInv, v -> c.preInv = v, KitScreen::new, false));
        l.add(new F("Inventar-Vorschau", "Zeigt dein Inventar als kleines Raster auf dem Bildschirm.",
                "Inventar", () -> c.invHud, v -> c.invHud = v, null, false));

        // ---------------- Sicht ----------------
        l.add(new F("Block Outlines", "Farbige, dickere Umrandung des Blocks, auf den du zielst.",
                "Sicht", () -> c.blockOutlines, v -> c.blockOutlines = v, SettingsScreen::new, false));
        l.add(new F("Crosshair Studio", "Baue dein eigenes Fadenkreuz: Form, Größe, Dicke, Lücke, Farbe.",
                "Sicht", () -> c.crosshairStudio, v -> c.crosshairStudio = v, CrosshairStudioScreen::new, false));
        l.add(new F("Zoom", "Halte die Taste C, um heranzuzoomen. Stärke unter Einstellungen.",
                "Sicht", () -> c.zoom, v -> c.zoom = v, MoreScreen::new, false));
        l.add(new F("Nametags", "Ändert Spielernamen überall (Namensschild, Chat, Tab-Liste).",
                "Sicht", () -> c.nametags, v -> c.nametags = v, NametagScreen::new, false));
        l.add(new F("Big Items", "Größere Items in der Hand. Braucht den Minecraft-Quellcode.",
                "Sicht", null, null, null, true));
        l.add(new F("Item Physics", "Items liegen flach am Boden. Braucht den Minecraft-Quellcode.",
                "Sicht", null, null, null, true));
        l.add(new F("Motion Blur", "Bewegungsunschärfe. Braucht den Minecraft-Quellcode.",
                "Sicht", null, null, null, true));
        l.add(new F("Low Fire", "Niedrigeres Feuer auf dem Bildschirm. Braucht den Minecraft-Quellcode.",
                "Sicht", null, null, null, true));
        l.add(new F("Low Shield", "Schild tiefer in der Hand. Braucht den Minecraft-Quellcode.",
                "Sicht", null, null, null, true));

        return l;
    }
}
