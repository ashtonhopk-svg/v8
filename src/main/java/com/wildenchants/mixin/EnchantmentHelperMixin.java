package com.wildenchants.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.wildenchants.SurgeMath;
import com.wildenchants.BackstabMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hooks the place where vanilla applies weapon-enchantment damage (Sharpness, Smite, ...).
 * Vanilla only uses (result - baseDamage) as the "enchantment bonus", so scaling the result by
 * the Surge multiplier scales the base hit and the other enchantment bonuses.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@ModifyReturnValue(method = "modifyDamage", at = @At("RETURN"))
	private static float wildEnchants$surge(float original, ServerLevel level, ItemStack weapon, Entity victim,
			DamageSource source, float baseDamage) {
		float result = original * SurgeMath.multiplierFor(level, weapon);
		return result * BackstabMath.multiplierFor(level, weapon, source, victim);
	}
}
