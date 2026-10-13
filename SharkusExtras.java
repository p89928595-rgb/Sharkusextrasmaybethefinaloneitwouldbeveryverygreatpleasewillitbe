package sharkus.extras;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SharkusExtras implements ClientModInitializer {
    public static KeyMapping OPEN_MENU;
    public static KeyMapping ZOOM;
    public static KeyMapping TOGGLE;

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sharkus-extras", "main"));

        // GLFW kennt kein "Ü" - die Taste liegt auf deutschem Layout auf LEFT_BRACKET
        OPEN_MENU = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.sharkus-extras.menu", GLFW.GLFW_KEY_LEFT_BRACKET, cat));
        ZOOM = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.sharkus-extras.zoom", GLFW.GLFW_KEY_C, cat));

        TOGGLE = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.sharkus-extras.toggle", GLFW.GLFW_KEY_F8, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (TOGGLE.consumeClick()) {
                Config.I.enabled = !Config.I.enabled;
                Config.save();
                if (mc.player != null) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Sharkus Extras: " + (Config.I.enabled ? "AN" : "AUS")), true);
                }
            }
            while (OPEN_MENU.consumeClick()) mc.gui.setScreen(new MenuScreen(null));
            Hud.tick(mc);
        });

        Hud.register();
        WorldFx.register();
    }
}
