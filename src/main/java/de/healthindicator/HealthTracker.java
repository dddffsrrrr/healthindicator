package de.healthindicator;

import de.healthindicator.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Liest die Lebenspunkte, die der Server an den Client schickt, und erkennt, ob der Server sie fälscht.
 * Erkennung: Wird ein Spieler mehrmals getroffen, ohne dass sich seine Herzen je ändern, sind die Werte gefälscht.
 */
public final class HealthTracker {
	public enum Source { REAL, SCOREBOARD, UNKNOWN }

	public record Reading(float health, float absorption, float max, Source source) {}

	private static final int FAKE_AFTER_HITS = 3;
	private static final int CHECK_DELAY_TICKS = 12;

	private static final class State {
		float lastTotal = -1;
		boolean everChanged;
		int hitsWithoutChange;
		int lastHurtTime;
		int pendingCheck = -1;
		float totalAtHit;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	private static int pruneCounter;

	private HealthTracker() {}

	public static void clear() {
		STATES.clear();
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		for (Player p : mc.level.players()) {
			if (p == mc.player) continue;
			State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
			float total = p.getHealth() + p.getAbsorptionAmount();

			if (s.lastTotal >= 0 && Math.abs(total - s.lastTotal) > 0.01f) {
				s.everChanged = true;
				s.hitsWithoutChange = 0;
				s.pendingCheck = -1;
			}
			s.lastTotal = total;

			// Neuer Treffer (Schadens-Animation startet)
			if (p.hurtTime > s.lastHurtTime && p.hurtTime >= 8) {
				s.pendingCheck = CHECK_DELAY_TICKS;
				s.totalAtHit = total;
			}
			s.lastHurtTime = p.hurtTime;

			if (s.pendingCheck > 0 && --s.pendingCheck == 0) {
				if (Math.abs(total - s.totalAtHit) < 0.01f && !s.everChanged) s.hitsWithoutChange++;
				s.pendingCheck = -1;
			}
		}

		if (++pruneCounter >= 200) {
			pruneCounter = 0;
			Set<UUID> present = new HashSet<>();
			for (Player p : mc.level.players()) present.add(p.getUUID());
			STATES.keySet().retainAll(present);
		}
	}

	public static boolean looksFake(Player p) {
		if (p.getHealth() <= 0 && p.isAlive() && p.deathTime == 0) return true;
		State s = STATES.get(p.getUUID());
		return s != null && s.hitsWithoutChange >= FAKE_AFTER_HITS;
	}

	public static Reading read(Player p, ModConfig cfg) {
		boolean fake = cfg.detectFake && looksFake(p);
		if (!fake) {
			return new Reading(p.getHealth(), p.getAbsorptionAmount(), p.getMaxHealth(), Source.REAL);
		}
		if (cfg.useScoreboardFallback) {
			Integer sb = scoreboardHealth(p);
			if (sb != null) return new Reading(sb, 0, p.getMaxHealth(), Source.SCOREBOARD);
		}
		return new Reading(0, 0, p.getMaxHealth(), Source.UNKNOWN);
	}

	/** Wert der Scoreboard-Anzeige unter dem Namen (viele Server zeigen dort die HP). */
	private static Integer scoreboardHealth(Player p) {
		Scoreboard sb = p.level().getScoreboard();
		Objective obj = sb.getDisplayObjective(DisplaySlot.BELOW_NAME);
		if (obj == null) return null;
		ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(p, obj);
		return info != null ? info.value() : null;
	}
}
