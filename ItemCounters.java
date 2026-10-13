package sharkus.extras;

import java.util.List;

/** PvP-Item-Counter: Totems, Gapples, Crystals usw. mit eigener Warnschwelle. */
final class ItemCounters {
    record Preset(String id, String label, int rgb, int defWarn, boolean defOn) { }

    static final List<Preset> PRESETS = List.of(
            new Preset("minecraft:totem_of_undying", "Totems", 0xFFD700, 2, true),
            new Preset("minecraft:golden_apple", "Gapples", 0xFFAA00, 8, true),
            new Preset("minecraft:end_crystal", "Crystals", 0xFF55FF, 16, false),
            new Preset("minecraft:respawn_anchor", "Anchors", 0xAA55FF, 4, false),
            new Preset("minecraft:glowstone", "Glowstone", 0xFFFF55, 16, false),
            new Preset("minecraft:obsidian", "Obsidian", 0x9966FF, 16, false),
            new Preset("minecraft:cobweb", "Cobwebs", 0xDDDDDD, 4, false),
            new Preset("minecraft:ender_pearl", "Perlen", 0x00CCCC, 4, true),
            new Preset("minecraft:experience_bottle", "XP", 0x55FF55, 8, false),
            new Preset("minecraft:arrow", "Pfeile", 0xBBBBBB, 16, false));

    private ItemCounters() { }

    static boolean enabled(Preset p) {
        Boolean b = Config.I.counterOn.get(p.id());
        return b == null ? p.defOn() : b;
    }

    static int warn(Preset p) {
        Integer w = Config.I.counterWarn.get(p.id());
        return w == null ? p.defWarn() : w;
    }
}
