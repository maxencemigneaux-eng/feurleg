package fr.goat.feurleg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FeurlegClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("feurleg");

    private static KeyBinding toggleKey;
    private static KeyBinding dumpKey;

    @Override
    public void onInitializeClient() {
        FeurlegConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.feurleg.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.feurleg"));
        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.feurleg.dump", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.feurleg"));

        // Touche de debug utilisable pendant qu'un menu est ouvert.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scancode, modifiers) -> {
                if (dumpKey.matchesKey(key, scancode)) TextCapture.requestDump();
            }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo info = client.getCurrentServerEntry();
            String key = info != null ? info.address.toLowerCase() : "solo";
            client.execute(() -> {
                TimerStore.get().onJoin(key);
                ZoneClock.onJoin(key);
                ZonePeriods.onJoin(key);
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                TimerStore.get().onLeave();
                ZoneClock.onLeave();
                ZonePeriods.onLeave();
            }));

        ClientTickEvents.END_CLIENT_TICK.register(FeurlegClient::tick);
        HudRenderCallback.EVENT.register(TimerHud::render);
    }

    private static void tick(MinecraftClient client) {
        FeurlegConfig cfg = FeurlegConfig.get();
        while (toggleKey.wasPressed()) {
            cfg.enabled = !cfg.enabled;
            FeurlegConfig.save();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(cfg.enabled
                    ? "§6[Feurleg]§r Timers affichés" : "§6[Feurleg]§r Timers cachés"), true);
            }
        }
        if (client.player == null) return;
        ZoneClock.tick(client);
        ZonePeriods.tickSave();

        for (TimerStore.Timer t : TimerStore.get().list()) {
            long left = t.remainingMillis();
            if (cfg.warnSeconds > 0 && !t.warned && left <= cfg.warnSeconds * 1000L && left > 0) {
                t.warned = true;
                alert(client, "§e" + t.name, "§fLégendaire dans " + TimerHud.format(left) + " !", 1.0f);
            }
            if (!t.alerted && left <= 0) {
                t.alerted = true;
                t.warned = true;
                alert(client, "§a" + t.name, "§fTentative de spawn légendaire !", 1.5f);
            }
        }
    }

    /** Gros message au milieu de l'écran (titre + sous-titre). */
    private static void alert(MinecraftClient client, String title, String subtitle, float pitch) {
        FeurlegConfig cfg = FeurlegConfig.get();
        if (cfg.alertMessage) {
            client.inGameHud.setTitleTicks(5, 50, 15);
            client.inGameHud.setSubtitle(Text.literal(subtitle));
            client.inGameHud.setTitle(Text.literal(title));
        }
        if (cfg.alertSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, pitch));
        }
    }
}
