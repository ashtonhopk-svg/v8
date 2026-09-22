package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wildenchants.RadiusTracker;
import com.wildenchants.Tuning;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Radius: repeats the hit on every living entity within the radius of the entity that was hit.
 * "Repeat" means the same damage source and the same damage amount, followed by the weapon's other
 * on-hit enchantment effects (Fire Aspect, Exploding, Thunderstrike, ...), exactly as for the original target.
 * The radius (blocks) comes from the JSON (default: level + 1, so 2..6).
 */
public record RadiusEffect(LevelBasedValue radius) implements EnchantmentEntityEffect {
	public static final MapCodec<RadiusEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			LevelBasedValue.CODEC.fieldOf("radius").forGetter(RadiusEffect::radius)
	).apply(instance, RadiusEffect::new));

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		if (RadiusTracker.isSplashing()) {
			return; // this is one of our own repeated hits - don't repeat it again
		}
		LivingEntity wielder = item.owner();
		if (wielder == null || !(target instanceof LivingEntity victim)) {
			return;
		}
		RadiusTracker.Hit hit = RadiusTracker.getHit(victim, wielder, level.getGameTime());
		if (hit == null) {
			return;
		}
		float reach = this.radius.calculate(enchantmentLevel);
		if (reach <= 0.0F) {
			return;
		}

		double reachSq = (double) reach * reach;
		List<LivingEntity> neighbours = level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(reach),
				e -> e != victim && e != wielder && e.isAlive() && !e.isSpectator()
						&& !(e instanceof ArmorStand)
						&& !(Tuning.RADIUS_SPARES_OWN_PETS && e instanceof TamableAnimal pet && pet.isOwnedBy(wielder))
						&& victim.distanceToSqr(e) <= reachSq);

		RadiusTracker.setSplashing(true);
		try {
			for (LivingEntity neighbour : neighbours) {
				if (neighbour.hurtServer(level, hit.source(), hit.amount())) {
					EnchantmentHelper.doPostAttackEffects(level, neighbour, hit.source());
				}
			}
		} finally {
			RadiusTracker.setSplashing(false);
		}
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
