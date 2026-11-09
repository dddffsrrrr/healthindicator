package de.healthindicator.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Farbe wählen: Farbpalette oder eigener Hex-Code (z.B. FF5555). */
public class ColorScreen extends Screen {
	private static final int[] PALETTE = {
			0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000,
			0xFF5555, 0xAA0000, 0xFF55AA, 0xFF55FF,
			0xAA00AA, 0x5555FF, 0x0000AA, 0x55FFFF,
			0x00AAAA, 0x55FF55, 0x00AA00, 0xFFFF55,
			0xFFAA00, 0xFF7F00, 0xFFC0CB, 0x8B4513
	};
	private static final int COLS = 5;

	private final Screen parent;
	private final IntSupplier getter;
	private final IntConsumer setter;
	private final String previewText;
	private EditBox hexBox;

	public ColorScreen(Screen parent, Component title, String previewText, IntSupplier getter, IntConsumer setter) {
		super(title);
		this.parent = parent;
		this.previewText = previewText;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	protected void init() {
		int cellW = 40, cellH = 20, gap = 4;
		int gridW = COLS * cellW + (COLS - 1) * gap;
		int left = this.width / 2 - gridW / 2;
		int top = 56;

		for (int i = 0; i < PALETTE.length; i++) {
			int rgb = PALETTE[i];
			int x = left + (i % COLS) * (cellW + gap);
			int y = top + (i / COLS) * (cellH + gap);
			Component swatch = Component.literal("████").withStyle(s -> s.withColor(rgb));
			addRenderableWidget(Button.builder(swatch, b -> {
				setter.accept(rgb);
				ModConfig.save();
				hexBox.setValue(hex(rgb));
			}).bounds(x, y, cellW, cellH).build());
		}

		int rows = (PALETTE.length + COLS - 1) / COLS;
		int y = top + rows * (cellH + gap) + 14;
		hexBox = new EditBox(this.font, this.width / 2 - 50, y, 100, 20, Component.literal("Hex"));
		hexBox.setMaxLength(7);
		hexBox.setValue(hex(getter.getAsInt()));
		hexBox.setResponder(text -> {
			String s = text.trim();
			if (s.startsWith("#")) s = s.substring(1);
			if (s.length() == 6) {
				try {
					setter.accept(Integer.parseInt(s, 16) & 0xFFFFFF);
					ModConfig.save();
				} catch (NumberFormatException ignored) {
				}
			}
		});
		addRenderableWidget(hexBox);

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(this.width / 2 - 75, this.height - 28, 150, 20).build());
	}

	private static String hex(int rgb) {
		return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		g.drawCenteredString(this.font, previewText, this.width / 2, 32, 0xFF000000 | getter.getAsInt());
		Component label = Component.translatable("healthindicator.color.hex");
		g.drawString(this.font, label, hexBox.getX() - 6 - this.font.width(label), hexBox.getY() + 6, 0xFFAAAAAA, true);
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreen(parent);
	}
}
