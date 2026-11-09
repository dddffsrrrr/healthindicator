package de.healthindicator.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Einstellungsmenü – erreichbar über Mod Menu (oder eine eigene Taste). */
public class ConfigScreen extends Screen {
	private static final int ROW = 22;
	private static final int TOP = 28;
	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("healthindicator.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		int cx = this.width / 2;
		int leftX = cx - 155;
		int rightX = cx + 5;

		toggle(leftX, TOP, 310, "enabled", () -> c.enabled, v -> c.enabled = v);

		int startY = TOP + ROW + 12;

		// ---------- Links: Werte
		int y = startY;
		toggle(leftX, y, 150, "absorption", () -> c.showAbsorption, v -> c.showAbsorption = v);
		y += ROW;
		addRenderableWidget(Button.builder(Component.translatable("healthindicator.config.unit",
								Component.translatable(c.showAsHearts ? "healthindicator.unit.hearts" : "healthindicator.unit.hp")),
						b -> { c.showAsHearts = !c.showAsHearts; changed(); })
				.bounds(leftX, y, 150, 20).build());
		y += ROW;
		toggle(leftX, y, 150, "decimals", () -> c.showDecimals, v -> c.showDecimals = v);
		y += ROW;
		toggle(leftX, y, 150, "only_hurt", () -> c.onlyWhenHurt, v -> c.onlyWhenHurt = v);
		y += ROW;
		toggle(leftX, y, 150, "detect_fake", () -> c.detectFake, v -> c.detectFake = v);
		y += ROW;
		toggle(leftX, y, 150, "scoreboard", () -> c.useScoreboardFallback, v -> c.useScoreboardFallback = v);

		// ---------- Rechts: Anzeige
		y = startY;
		addRenderableWidget(Button.builder(Component.translatable("healthindicator.config.position",
								Component.translatable("healthindicator.position." + c.position.name().toLowerCase(Locale.ROOT))),
						b -> { c.position = c.position.next(); changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;
		toggle(rightX, y, 150, "color_by_health", () -> c.colorByHealth, v -> c.colorByHealth = v);
		y += ROW;
		int hc = c.healthColor, ac = c.absorptionColor;
		addRenderableWidget(Button.builder(Component.translatable("healthindicator.config.color_health")
								.append(" ").append(Component.literal("█").withStyle(s -> s.withColor(hc))),
						b -> this.minecraft.setScreen(new ColorScreen(this,
								Component.translatable("healthindicator.config.color_health"), "❤ 7.5",
								() -> c.healthColor, v -> c.healthColor = v)))
				.bounds(rightX, y, 74, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("healthindicator.config.color_absorption")
								.append(" ").append(Component.literal("█").withStyle(s -> s.withColor(ac))),
						b -> this.minecraft.setScreen(new ColorScreen(this,
								Component.translatable("healthindicator.config.color_absorption"), "+2",
								() -> c.absorptionColor, v -> c.absorptionColor = v)))
				.bounds(rightX + 76, y, 74, 20).build());
		y += ROW;
		toggle(rightX, y, 150, "background", () -> c.background, v -> c.background = v);
		y += ROW;
		toggle(rightX, y, 150, "line_of_sight", () -> c.onlyWithLineOfSight, v -> c.onlyWithLineOfSight = v);
		y += ROW;

		addRenderableWidget(Button.builder(
						Component.translatable("healthindicator.config.scale", String.format(Locale.ROOT, "%.1f", c.scale)),
						b -> { c.scale = 1.0f; changed(); })
				.bounds(rightX, y, 106, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-"),
						b -> { c.scale = Math.max(0.3f, Math.round((c.scale - 0.1f) * 10) / 10f); changed(); })
				.bounds(rightX + 108, y, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+"),
						b -> { c.scale = Math.min(3.0f, Math.round((c.scale + 0.1f) * 10) / 10f); changed(); })
				.bounds(rightX + 130, y, 20, 20).build());
		y += ROW;

		addRenderableWidget(Button.builder(
						Component.translatable("healthindicator.config.distance", c.maxDistance),
						b -> { c.maxDistance = 64; changed(); })
				.bounds(rightX, y, 106, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-"),
						b -> { c.maxDistance = Math.max(8, c.maxDistance - 8); changed(); })
				.bounds(rightX + 108, y, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+"),
						b -> { c.maxDistance = Math.min(256, c.maxDistance + 8); changed(); })
				.bounds(rightX + 130, y, 20, 20).build());

		int bottom = this.height - 26;
		addRenderableWidget(Button.builder(Component.translatable("healthindicator.config.keybinds"),
						b -> this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options)))
				.bounds(leftX, bottom, 150, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(rightX, bottom, 150, 20).build());
	}

	private void toggle(int x, int y, int w, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
		boolean on = get.get();
		Component text = Component.empty()
				.append(Component.translatable("healthindicator.config." + key)).append(": ")
				.append(on ? Component.translatable("healthindicator.on").withStyle(ChatFormatting.GREEN)
						: Component.translatable("healthindicator.off").withStyle(ChatFormatting.RED));
		addRenderableWidget(Button.builder(text, b -> { set.accept(!get.get()); changed(); })
				.bounds(x, y, w, 20).build());
	}

	private void changed() {
		ModConfig.save();
		rebuildWidgets();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
		int labelY = TOP + ROW + 1;
		g.drawString(this.font, Component.translatable("healthindicator.config.section.values"),
				this.width / 2 - 155, labelY, 0xFFFFDD55, true);
		g.drawString(this.font, Component.translatable("healthindicator.config.section.display"),
				this.width / 2 + 5, labelY, 0xFFFFDD55, true);
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreen(parent);
	}
}
