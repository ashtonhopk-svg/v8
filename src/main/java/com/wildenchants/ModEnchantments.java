package com.wildenchants;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class ModEnchantments {
	public static final ResourceKey<Enchantment> EXPLODING = key("exploding");
	public static final ResourceKey<Enchantment> SURGE = key("surge");
	public static final ResourceKey<Enchantment> THUNDERSTRIKE = key("thunderstrike");
	public static final ResourceKey<Enchantment> LIGHTNING = key("lightning");
	public static final ResourceKey<Enchantment> CARROT = key("carrot");
	public static final ResourceKey<Enchantment> TUMBLE = key("tumble");
	public static final ResourceKey<Enchantment> CLONING = key("cloning");
	public static final ResourceKey<Enchantment> FLIGHT = key("flight");
	public static final ResourceKey<Enchantment> RADIUS = key("radius");
	public static final ResourceKey<Enchantment> AIR_JUMP = key("air_jump");
	public static final ResourceKey<Enchantment> DASH = key("dash");
	public static final ResourceKey<Enchantment> FALLBACK = key("fallback");
	public static final ResourceKey<Enchantment> PHASE = key("phase");
	public static final ResourceKey<Enchantment> GRAPPLE = key("grapple");
	public static final ResourceKey<Enchantment> FROSTBITE = key("frostbite");
	public static final ResourceKey<Enchantment> BACKSTAB = key("backstab");
	public static final ResourceKey<Enchantment> TIMBER = key("timber");
	public static final ResourceKey<Enchantment> TELEKINESIS = key("telekinesis");
	public static final ResourceKey<Enchantment> REACH = key("reach");
	public static final ResourceKey<Enchantment> MAGNET = key("magnet");
	public static final ResourceKey<Enchantment> AFTERLIFE = key("afterlife");

	private ModEnchantments() {}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, WildEnchants.id(path));
	}

	/** Level of the given enchantment on the stack (0 if absent or the enchantment is not registered). */
	public static int levelOn(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		Optional<Holder.Reference<Enchantment>> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
		return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}
}
