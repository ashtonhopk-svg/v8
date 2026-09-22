package com.wildenchants;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Shared tag keys the mod's own data pack files define. */
public final class ModTags {
	private ModTags() {}

	public static final TagKey<Block> PHASEABLE = TagKey.create(Registries.BLOCK, WildEnchants.id("phaseable"));
	public static final TagKey<Block> MAGNETIC = TagKey.create(Registries.BLOCK, WildEnchants.id("magnetic"));
	public static final TagKey<Item> AXES = TagKey.create(Registries.ITEM, WildEnchants.id("axes"));
}
