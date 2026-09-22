package com.wildenchants;

import com.mojang.serialization.MapCodec;
import com.wildenchants.effect.CarrotFarmlandEffect;
import com.wildenchants.effect.CloningEffect;
import com.wildenchants.effect.ExplodingEffect;
import com.wildenchants.effect.LightningStrikeEffect;
import com.wildenchants.effect.FrostbiteEffect;
import com.wildenchants.effect.RadiusEffect;
import com.wildenchants.effect.TumbleEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;

/** Registers the custom "enchantment entity effect" types that the enchantment JSON files refer to. */
public final class ModEffects {
	private ModEffects() {}

	public static void register() {
		register("exploding", ExplodingEffect.CODEC);
		register("lightning_strike", LightningStrikeEffect.CODEC);
		register("carrot_farmland", CarrotFarmlandEffect.CODEC);
		register("tumble", TumbleEffect.CODEC);
		register("cloning", CloningEffect.CODEC);
		register("radius", RadiusEffect.CODEC);
		register("frostbite", FrostbiteEffect.CODEC);
	}

	private static <T extends EnchantmentEntityEffect> void register(String name, MapCodec<T> codec) {
		Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, WildEnchants.id(name), codec);
	}
}
