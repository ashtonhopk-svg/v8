package com.wildenchants;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Surge: bonus damage = (fraction of durability lost) * (1 + (level - 1) / 6)
 *
 * 50% durability left:  Surge I = +50%, II = +58%, III = +67%, IV = +75%
 * almost broken:        Surge I = +100%, IV = +150%
 */
public final class SurgeMath {
	private static final float EXTRA_PER_LEVEL_ABOVE_FIRST = 1.0F / 6.0F;

	private SurgeMath() {}

	public static float damageMultiplier(int surgeLevel, float durabilityLostFraction) {
		float scaling = 1.0F + (surgeLevel - 1) * EXTRA_PER_LEVEL_ABOVE_FIRST;
		return 1.0F + durabilityLostFraction * scaling;
	}

	/** Multiplier to apply to the damage dealt with this stack (1.0 = no change). */
	public static float multiplierFor(Level level, ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem()) {
			return 1.0F;
		}
		int surge = ModEnchantments.levelOn(level, ModEnchantments.SURGE, stack);
		if (surge <= 0) {
			return 1.0F;
		}
		int max = stack.getMaxDamage();
		if (max <= 0) {
			return 1.0F;
		}
		float lost = Math.min(1.0F, Math.max(0.0F, stack.getDamageValue() / (float) max));
		return damageMultiplier(surge, lost);
	}
}
