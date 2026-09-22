package com.wildenchants.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wildenchants.FallingBlockLanding;
import com.wildenchants.ModEnchantments;
import com.wildenchants.RadiusTracker;
import com.wildenchants.Tuning;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tumble: every solid block in a sphere around the hit entity becomes a falling block with the same
 * block state. Radius = base_radius (5) + radius_per_wind_burst_level (2) * Wind Burst level on the mace.
 * Only happens on a real mace smash attack. With Wind Burst the blocks are launched upwards and outwards
 * (upward speed = launch_speed_per_wind_burst_level * Wind Burst level).
 */
public record TumbleEffect(float baseRadius, float radiusPerWindBurstLevel, float launchSpeedPerWindBurstLevel) implements EnchantmentEntityEffect {
	public static final MapCodec<TumbleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.FLOAT.fieldOf("base_radius").forGetter(TumbleEffect::baseRadius),
			Codec.FLOAT.fieldOf("radius_per_wind_burst_level").forGetter(TumbleEffect::radiusPerWindBurstLevel),
			Codec.FLOAT.fieldOf("launch_speed_per_wind_burst_level").forGetter(TumbleEffect::launchSpeedPerWindBurstLevel)
	).apply(instance, TumbleEffect::new));

	// blocks converted so far in the current game tick (shared by all Tumble hits, incl. those repeated by Radius)
	private static long budgetTick = Long.MIN_VALUE;
	private static int budgetUsed = 0;

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		if (Tuning.TUMBLE_REQUIRES_SMASH) {
			// only a real mace smash (falling attack) counts - it has its own damage type
			LivingEntity wielder = item.owner();
			if (wielder == null || !(target instanceof LivingEntity victim)) {
				return;
			}
			RadiusTracker.Hit hit = RadiusTracker.getHit(victim, wielder, level.getGameTime());
			if (hit == null || !hit.source().is(DamageTypes.MACE_SMASH)) {
				return;
			}
		}

		long now = level.getGameTime();
		if (now != budgetTick) {
			budgetTick = now;
			budgetUsed = 0;
		}
		int allowed = Tuning.TUMBLE_MAX_BLOCKS - budgetUsed;
		if (allowed <= 0) {
			return;
		}

		int windBurst = ModEnchantments.levelOn(level, Enchantments.WIND_BURST, item.itemStack());
		float radius = this.baseRadius + this.radiusPerWindBurstLevel * windBurst;
		if (radius <= 0.0F) {
			return;
		}

		BlockPos center = target.blockPosition();
		int reach = (int) Math.ceil(radius);
		double radiusSq = (double) radius * radius;

		record Candidate(BlockPos pos, int distSq) {}
		List<Candidate> candidates = new ArrayList<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int distSq = dx * dx + dy * dy + dz * dz;
					if (distSq > radiusSq) {
						continue;
					}
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (level.isOutsideBuildHeight(cursor) || !level.hasChunkAt(cursor)) {
						continue;
					}
					if (canTumble(level, cursor, level.getBlockState(cursor))) {
						candidates.add(new Candidate(cursor.immutable(), distSq));
					}
				}
			}
		}

		if (candidates.size() > allowed) {
			candidates.sort(Comparator.comparingInt(Candidate::distSq));
			candidates = candidates.subList(0, allowed);
		}

		RandomSource random = level.getRandom();
		double upSpeed = (double) this.launchSpeedPerWindBurstLevel * windBurst;
		for (Candidate candidate : candidates) {
			// re-read: earlier conversions may have popped/changed neighbouring blocks
			BlockState state = level.getBlockState(candidate.pos());
			if (canTumble(level, candidate.pos(), state)) {
				FallingBlockEntity block = FallingBlockEntity.fall(level, candidate.pos(), state);
				budgetUsed++;
				FallingBlockLanding.track(level, block);
				if (upSpeed > 0.0D) {
					launch(block, candidate.pos(), target, upSpeed, random);
				}
			}
		}
	}

	/** Wind Burst: throw the block up and away from the entity that was hit. */
	private static void launch(FallingBlockEntity block, BlockPos from, Entity target, double upSpeed, RandomSource random) {
		double dx = from.getX() + 0.5D - target.getX();
		double dz = from.getZ() + 0.5D - target.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		double outX = length > 1.0E-4D ? dx / length : random.nextDouble() - 0.5D;
		double outZ = length > 1.0E-4D ? dz / length : random.nextDouble() - 0.5D;
		double jitter = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * Tuning.TUMBLE_LAUNCH_RANDOMNESS;
		double sideSpeed = upSpeed * Tuning.TUMBLE_LAUNCH_HORIZONTAL_RATIO;
		block.setDeltaMovement(outX * sideSpeed * jitter, upSpeed * jitter, outZ * sideSpeed * jitter);
	}

	private static boolean canTumble(ServerLevel level, BlockPos pos, BlockState state) {
		return !state.isAir()
				&& !(state.getBlock() instanceof LiquidBlock)
				&& !state.hasBlockEntity()               // chests, spawners, ... would lose their contents
				&& state.getDestroySpeed(level, pos) >= 0; // bedrock, barriers, portal frames, ...
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
