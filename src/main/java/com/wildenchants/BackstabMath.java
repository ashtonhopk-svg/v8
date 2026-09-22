package com.wildenchants;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Backstab: bonus damage when the victim isn't facing the attacker. */
public final class BackstabMath {
	private BackstabMath() {}

	public static float multiplierFor(Level level, ItemStack weapon, DamageSource source, Entity victim) {
		Entity attacker = source.getEntity();
		if (weapon.isEmpty() || attacker == null) {
			return 1.0F;
		}
		int lvl = ModEnchantments.levelOn(level, ModEnchantments.BACKSTAB, weapon);
		if (lvl <= 0) {
			return 1.0F;
		}
		Vec3 victimToAttacker = attacker.position().subtract(victim.position());
		if (victimToAttacker.horizontalDistanceSqr() < 1.0E-4D) {
			return 1.0F;
		}
		Vec3 victimFacing = Vec3.directionFromRotation(0.0F, victim.getYRot());
		double angleToAttacker = Math.toDegrees(Math.acos(
				Mth.clamp(victimFacing.normalize().dot(victimToAttacker.normalize()), -1.0D, 1.0D)));
		if (angleToAttacker < Tuning.BACKSTAB_ANGLE_THRESHOLD_DEGREES) {
			return 1.0F; // victim is facing roughly toward the attacker
		}
		return 1.0F + Tuning.BACKSTAB_BONUS_BASE + Tuning.BACKSTAB_BONUS_PER_LEVEL_ABOVE_FIRST * (lvl - 1);
	}
}
