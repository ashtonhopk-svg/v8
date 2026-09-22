package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.wildenchants.Tuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Thunderstrike (melee) and Lightning (bow): a lightning bolt on whatever was hit. */
public record LightningStrikeEffect() implements EnchantmentEntityEffect {
	public static final LightningStrikeEffect INSTANCE = new LightningStrikeEffect();
	public static final MapCodec<LightningStrikeEffect> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		LivingEntity wielder = item.owner();

		if (!Tuning.LIGHTNING_SPARES_WIELDER) {
			// plain vanilla lightning (shocks anybody close, including the wielder)
			LightningBolt vanilla = EntityType.LIGHTNING_BOLT.spawn(level, target.blockPosition(), EntitySpawnReason.TRIGGERED);
			if (vanilla != null && wielder instanceof ServerPlayer player) {
				vanilla.setCause(player);
			}
			return;
		}

		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			return;
		}
		double x = target.getX();
		double y = target.getY();
		double z = target.getZ();
		bolt.setPos(x, y, z);
		bolt.setVisualOnly(true); // no automatic damage - we shock everything except the wielder ourselves
		if (wielder instanceof ServerPlayer player) {
			bolt.setCause(player);
		}
		level.addFreshEntity(bolt);

		// same area vanilla lightning uses
		AABB area = new AABB(x - 3.0D, y - 3.0D, z - 3.0D, x + 3.0D, y + 9.0D, z + 3.0D);
		for (Entity struck : level.getEntities(bolt, area, Entity::isAlive)) {
			if (struck != wielder) {
				struck.thunderHit(level, bolt);
			}
		}
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
