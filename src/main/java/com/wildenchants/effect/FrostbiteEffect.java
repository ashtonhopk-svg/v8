package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.wildenchants.FrostbiteTracker;
import com.wildenchants.ModTags;
import com.wildenchants.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Frostbite: stacking slow that caps out in a short freeze, and permanent (non-melting) ice grows around the victim. */
public record FrostbiteEffect() implements EnchantmentEntityEffect {
	public static final FrostbiteEffect INSTANCE = new FrostbiteEffect();
	public static final MapCodec<FrostbiteEffect> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		if (!(target instanceof LivingEntity victim) || !victim.isAlive()) {
			return;
		}
		for (int i = 0; i < enchantmentLevel; i++) {
			FrostbiteTracker.addStack(victim);
		}
		spawnIce(level, victim);
	}

	private static void spawnIce(ServerLevel level, LivingEntity victim) {
		int r = (int) Math.round(Tuning.FROSTBITE_ICE_RADIUS);
		BlockPos center = victim.blockPosition();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) {
					continue;
				}
				BlockPos ground = center.offset(dx, -1, dz);
				if (level.getBlockState(ground).isAir() || level.getBlockState(ground).getFluidState().isSource()) {
					level.setBlock(ground, Blocks.PACKED_ICE.defaultBlockState(), Block.UPDATE_ALL); // never melts
				}
			}
		}
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
