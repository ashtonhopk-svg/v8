package com.wildenchants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Frostbite: counts recent hits per victim; each stack expires on its own after a while. */
public final class FrostbiteTracker {
	private record Stacks(int count, long expiresAtTick) {}

	private static final Map<UUID, Stacks> STACKS = new HashMap<>();

	private FrostbiteTracker() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FrostbiteTracker::onEndTick);
	}

	/** Adds a stack for this victim and returns the new stack count. */
	public static int addStack(LivingEntity victim) {
		long now = victim.level().getGameTime();
		Stacks current = STACKS.get(victim.getUUID());
		int count = (current != null && current.expiresAtTick() > now ? current.count() : 0) + 1;
		STACKS.put(victim.getUUID(), new Stacks(count, now + Tuning.FROSTBITE_STACK_DURATION_TICKS));

		int amplifier = Math.min(count, Tuning.FROSTBITE_STACKS_TO_FREEZE) - 1;
		int duration = count >= Tuning.FROSTBITE_STACKS_TO_FREEZE
				? Tuning.FROSTBITE_FREEZE_DURATION_TICKS
				: Tuning.FROSTBITE_STACK_DURATION_TICKS;
		victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, Math.min(amplifier, 6), false, true));
		if (count >= Tuning.FROSTBITE_STACKS_TO_FREEZE) {
			victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 5, false, true));
			STACKS.remove(victim.getUUID()); // reset after a freeze so it has to build up again
		}
		return count;
	}

	private static void onEndTick(MinecraftServer server) {
		if (STACKS.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		STACKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= now);
	}
}
