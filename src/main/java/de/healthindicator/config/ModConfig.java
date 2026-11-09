package de.healthindicator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.healthindicator.HealthIndicatorClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Einstellungen. Wird als config/healthindicator.json gespeichert. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("healthindicator.json");
	private static ModConfig instance;

	public boolean enabled = true;
	public boolean showAbsorption = true;
	/** true = Herzen (20 HP = 10 ❤), false = Lebenspunkte. */
	public boolean showAsHearts = true;
	public boolean showDecimals = true;
	public boolean onlyWhenHurt = false;
	/** Wenn der Server echte Werte versteckt: Wert aus der Scoreboard-Anzeige unter dem Namen nehmen. */
	public boolean useScoreboardFallback = true;
	/** Gefälschte Werte erkennen und "?" anzeigen. */
	public boolean detectFake = true;

	public Position position = Position.RIGHT;
	public boolean colorByHealth = true;
	public int healthColor = 0xFF5555;
	public int absorptionColor = 0xFFAA00;
	public boolean background = false;
	public boolean onlyWithLineOfSight = false;
	public int maxDistance = 64;
	public float scale = 1.0f;

	public enum Position {
		RIGHT, ABOVE, BELOW;

		public Position next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public static ModConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		ModConfig cfg = null;
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				cfg = GSON.fromJson(r, ModConfig.class);
			} catch (Exception e) {
				HealthIndicatorClient.LOGGER.warn("Konnte Config nicht laden, nutze Standardwerte", e);
			}
		}
		instance = cfg != null ? cfg : new ModConfig();
		if (instance.position == null) instance.position = Position.RIGHT;
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (Exception e) {
			HealthIndicatorClient.LOGGER.warn("Konnte Config nicht speichern", e);
		}
	}
}
