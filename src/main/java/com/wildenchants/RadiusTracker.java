package com.wildenchants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Remembers the exact damage + damage source of each melee hit so the Radius enchantment can repeat
 * the same hit on nearby entities.
 *
 * ALLOW_DAMAGE runs at the start of every hit (also killing blows, unlike AFTER_DAMAGE); we only
 * record it and always allow the damage. The enchantment effect runs right after the hit and
 * picks the record up with {@link #getHit}. (Radius and Tumble both read it.)
 */
public final class RadiusTracker {
	public record Hit(LivingEntity victim, DamageSource source, float amount, long gameTime) {}

	private static final Map<UUID, Hit> HITS = new HashMap<>();
	private static long hitsTick = Long.MIN_VALUE;
	private static boolean splashing = false;

	private RadiusTracker() {}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(RadiusTracker::onAllowDamage);
	}

	private static boolean onAllowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (isWeaponHit(source)) {
			long now = victim.level().getGameTime();
			if (now != hitsTick) {
				HITS.clear();
				hitsTick = now;
			}
			HITS.put(victim.getUUID(), new Hit(victim, source, amount, now));
		}
		return true; // never cancel anything
	}

	/** A hit made directly by a living attacker (not arrows, explosions, ...). */
	private static boolean isWeaponHit(DamageSource source) {
		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity
				&& source.getDirectEntity() == attacker
				&& !source.is(DamageTypeTags.IS_EXPLOSION);
	}

	/** The recorded hit of {@code wielder} on {@code victim} from this tick, or null. */
	public static Hit getHit(LivingEntity victim, LivingEntity wielder, long gameTime) {
		Hit hit = HITS.get(victim.getUUID());
		if (hit == null || hit.gameTime() != gameTime || hit.source().getEntity() != wielder) {
			return null;
		}
		return hit;
	}

	/** True while Radius is repeating a hit on nearby entities (stops Radius from triggering itself). */
	public static boolean isSplashing() {
		return splashing;
	}

	public static void setSplashing(boolean value) {
		splashing = value;
	}
}
