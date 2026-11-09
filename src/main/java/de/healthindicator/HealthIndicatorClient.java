package de.healthindicator;

import com.mojang.blaze3d.platform.InputConstants;
import de.healthindicator.config.ConfigScreen;
import de.healthindicator.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthIndicatorClient implements ClientModInitializer {
	public static final String MOD_ID = "healthindicator";
	public static final Logger LOGGER = LoggerFactory.getLogger("Health Indicator");

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static KeyMapping toggleKey;
	private static KeyMapping toggleAbsorptionKey;
	private static KeyMapping openConfigKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.healthindicator.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
		toggleAbsorptionKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.healthindicator.toggle_absorption", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.healthindicator.open_config", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			HealthTracker.tick(client);
			handleKeys(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HealthTracker.clear());

		WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
			CameraCapture.pos = context.worldState().cameraRenderState.pos;
			CameraCapture.posTimeMillis = System.currentTimeMillis();
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "player_health"), HealthOverlay::render);
	}

	private static void handleKeys(Minecraft client) {
		ModConfig cfg = ModConfig.get();
		boolean changed = false;
		while (toggleKey.consumeClick()) {
			cfg.enabled = !cfg.enabled;
			changed = true;
			notify(client, Component.translatable("healthindicator.title"), cfg.enabled);
		}
		while (toggleAbsorptionKey.consumeClick()) {
			cfg.showAbsorption = !cfg.showAbsorption;
			changed = true;
			notify(client, Component.translatable("healthindicator.config.absorption"), cfg.showAbsorption);
		}
		while (openConfigKey.consumeClick()) {
			if (client.screen == null) client.setScreen(new ConfigScreen(null));
		}
		if (changed) ModConfig.save();
	}

	private static void notify(Minecraft client, Component what, boolean on) {
		Component state = on
				? Component.translatable("healthindicator.on").withStyle(ChatFormatting.GREEN)
				: Component.translatable("healthindicator.off").withStyle(ChatFormatting.RED);
		client.gui.setOverlayMessage(Component.empty().append(what).append(": ").append(state), false);
	}
}
