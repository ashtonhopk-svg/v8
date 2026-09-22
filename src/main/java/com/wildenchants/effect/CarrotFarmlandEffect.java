package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Carrot: when the hit kills the target, dirt-like ground around it becomes watered farmland with
 * carrots growing on it. The radius (blocks) comes from the JSON (default: level + 1, so 2..8).
 */
public record CarrotFarmlandEffect(LevelBasedValue radius) implements EnchantmentEntityEffect {
	public static final MapCodec<CarrotFarmlandEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			LevelBasedValue.CODEC.fieldOf("radius").forGetter(CarrotFarmlandEffect::radius)
	).apply(instance, CarrotFarmlandEffect::new));

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		if (!(target instanceof LivingEntity victim) || !victim.isDeadOrDying()) {
			return; // only on kills
		}
		int r = Math.max(0, Math.round(this.radius.calculate(enchantmentLevel)));
		BlockPos center = victim.blockPosition();
		RandomSource random = level.getRandom();

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r + r) {
					continue; // roughly circular
				}
				plantColumn(level, center.getX() + dx, center.getY(), center.getZ() + dz, random);
			}
		}
	}

	private static void plantColumn(ServerLevel level, int x, int centerY, int z, RandomSource random) {
		for (int y = centerY + 1; y >= centerY - 3; y--) {
			BlockPos ground = new BlockPos(x, y, z);
			BlockState groundState = level.getBlockState(ground);
			if (!isFarmable(groundState)) {
				continue;
			}
			BlockPos above = ground.above();
			BlockState aboveState = level.getBlockState(above);
			boolean roomForCrop = aboveState.isAir() || (aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty());
			if (!roomForCrop) {
				continue;
			}
			if (!groundState.is(Blocks.FARMLAND)) {
				level.setBlock(ground, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), Block.UPDATE_ALL);
			}
			level.setBlock(above, Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, random.nextInt(8)), Block.UPDATE_ALL);
			return; // topmost valid block of this column only
		}
	}

	/** dirt, rooted dirt, coarse dirt, dirt path, grass block, mycelium, podzol + other dirt-like ground. */
	private static boolean isFarmable(BlockState state) {
		return state.is(Blocks.DIRT)
				|| state.is(Blocks.ROOTED_DIRT)
				|| state.is(Blocks.COARSE_DIRT)
				|| state.is(Blocks.DIRT_PATH)
				|| state.is(Blocks.GRASS_BLOCK)
				|| state.is(Blocks.MYCELIUM)
				|| state.is(Blocks.PODZOL)
				|| state.is(Blocks.MUD)
				|| state.is(Blocks.MUDDY_MANGROVE_ROOTS)
				|| state.is(Blocks.MOSS_BLOCK)
				|| state.is(Blocks.PALE_MOSS_BLOCK)
				|| state.is(Blocks.FARMLAND) // already farmland: just plant on it
				|| state.is(BlockTags.DIRT);
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
