package de.healthindicator;

import de.healthindicator.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Zeichnet "❤ 14.5 +4" neben/über/unter dem Namensschild jedes Spielers. */
public final class HealthOverlay {
	private static final int BG_COLOR = 0x60000000;
	private static final double WORLD_PIXEL = 0.025;
	/** Oberkante des Namensschilds über der Hitbox. */
	private static final double NAME_TAG_TOP = 0.5;
	private static final String HEART = "❤";

	private record Target(double depth, float sx, float sy, float scale, int nameWidth,
						  String main, int mainColor, String extra) {}

	private HealthOverlay() {}

	public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.enabled) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.options.hideGui) return;
		Entity camEntity = mc.getCameraEntity();
		if (camEntity == null) return;

		float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
		long now = System.currentTimeMillis();

		Vec3 cam = CameraCapture.pos;
		if (cam == null || now - CameraCapture.posTimeMillis > 250) cam = camEntity.getEyePosition(pt);
		float yaw = camEntity.getViewYRot(pt);
		float pitch = camEntity.getViewXRot(pt);
		if (mc.options.getCameraType().isMirrored()) {
			yaw += 180f;
			pitch = -pitch;
		}
		float fov = (CameraCapture.fov > 0 && now - CameraCapture.fovTimeMillis < 250)
				? CameraCapture.fov
				: mc.options.fov().get().floatValue();
		Matrix4f bob = (CameraCapture.bob != null && now - CameraCapture.bobTimeMillis < 250) ? CameraCapture.bob : null;

		double yawR = Math.toRadians(yaw), pitchR = Math.toRadians(pitch);
		double cy = Math.cos(yawR), sy = Math.sin(yawR), cp = Math.cos(pitchR), sp = Math.sin(pitchR);
		double fx = -sy * cp, fy = -sp, fz = cy * cp;
		double rx = -cy, ry = 0, rz = -sy;
		double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

		int winW = mc.getWindow().getWidth(), winH = mc.getWindow().getHeight();
		if (winW <= 0 || winH <= 0) return;
		double aspect = (double) winW / winH;
		double focal = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
		int guiW = g.guiWidth(), guiH = g.guiHeight();
		Vector4f v = new Vector4f();
		Font font = mc.font;

		List<Target> targets = new ArrayList<>();
		for (Player p : mc.level.players()) {
			if (p == mc.player || p == camEntity) continue;
			if (p.isSpectator() || p.isInvisibleTo(mc.player) || !p.isAlive()) continue;
			if (p.distanceTo(camEntity) > cfg.maxDistance) continue;
			if (cfg.onlyWithLineOfSight && !mc.player.hasLineOfSight(p)) continue;

			HealthTracker.Reading r = HealthTracker.read(p, cfg);
			if (cfg.onlyWhenHurt && r.source() == HealthTracker.Source.REAL
					&& r.health() >= r.max() && r.absorption() <= 0) continue;

			String main;
			int mainColor;
			String extra = null;
			if (r.source() == HealthTracker.Source.UNKNOWN) {
				main = HEART + " ?";
				mainColor = 0xFFAAAAAA;
			} else {
				main = HEART + " " + fmt(r.health(), cfg);
				if (cfg.colorByHealth) {
					float frac = r.max() > 0 ? r.health() / r.max() : 1f;
					mainColor = frac > 0.6f ? 0xFF55FF55 : frac > 0.3f ? 0xFFFFDD55 : 0xFFFF5555;
				} else {
					mainColor = 0xFF000000 | cfg.healthColor;
				}
				if (cfg.showAbsorption && r.absorption() > 0) extra = " +" + fmt(r.absorption(), cfg);
			}

			Vec3 anchor = p.getPosition(pt).add(0, p.getBbHeight() + NAME_TAG_TOP, 0);
			double dx = anchor.x - cam.x, dy = anchor.y - cam.y, dz = anchor.z - cam.z;
			v.set((float) (dx * rx + dy * ry + dz * rz),
					(float) (dx * ux + dy * uy + dz * uz),
					(float) -(dx * fx + dy * fy + dz * fz),
					1f);
			if (bob != null) bob.transform(v);
			double depth = -v.z;
			if (depth < 0.1) continue;
			double ndcX = (v.x * focal) / (aspect * depth);
			double ndcY = (v.y * focal) / depth;
			if (Math.abs(ndcX) > 1.3 || Math.abs(ndcY) > 1.3) continue;

			float screenX = (float) ((ndcX + 1.0) * 0.5 * guiW);
			float screenY = (float) ((1.0 - ndcY) * 0.5 * guiH);
			float scale = (float) (WORLD_PIXEL * guiH * 0.5 * focal / depth) * cfg.scale;
			scale = Math.max(0.3f, Math.min(4f, scale));
			int nameWidth = font.width(p.getDisplayName());
			targets.add(new Target(depth, screenX, screenY, scale, nameWidth, main, mainColor, extra));
		}
		if (targets.isEmpty()) return;

		targets.sort(Comparator.comparingDouble(Target::depth).reversed());
		for (Target t : targets) draw(g, font, cfg, t);
	}

	private static String fmt(float hp, ModConfig cfg) {
		float value = cfg.showAsHearts ? hp / 2f : hp;
		if (cfg.showDecimals) {
			float rounded = Math.round(value * 2f) / 2f; // halbe Herzen
			return rounded == (int) rounded
					? Integer.toString((int) rounded)
					: String.format(Locale.ROOT, "%.1f", rounded);
		}
		return Integer.toString(Math.round(value));
	}

	private static void draw(GuiGraphics g, Font font, ModConfig cfg, Target t) {
		int mainW = font.width(t.main());
		int extraW = t.extra() != null ? font.width(t.extra()) : 0;
		int totalW = mainW + extraW;

		int x, y;
		switch (cfg.position) {
			case RIGHT -> { x = t.nameWidth() / 2 + 4; y = 0; }
			case ABOVE -> { x = -totalW / 2; y = -10; }
			default -> { x = -totalW / 2; y = 10; } // BELOW
		}

		g.pose().pushMatrix();
		g.pose().translate(t.sx(), t.sy());
		g.pose().scale(t.scale(), t.scale());
		if (cfg.background) g.fill(x - 1, y - 1, x + totalW + 1, y + 9, BG_COLOR);
		g.drawString(font, t.main(), x, y, t.mainColor(), true);
		if (t.extra() != null) {
			g.drawString(font, t.extra(), x + mainW, y, 0xFF000000 | cfg.absorptionColor, true);
		}
		g.pose().popMatrix();
	}
}
