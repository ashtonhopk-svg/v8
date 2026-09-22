package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wildenchants.Tuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exploding: blows up the entity that was hit. Radius comes from the JSON (default: level + 1). */
public record ExplodingEffect(LevelBasedValue radius) implements EnchantmentEntityEffect {
	public static final MapCodec<ExplodingEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			LevelBasedValue.CODEC.fieldOf("radius").forGetter(ExplodingEffect::radius)
	).apply(instance, ExplodingEffect::new));

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		float blastRadius = this.radius.calculate(enchantmentLevel);
		if (blastRadius <= 0.0F) {
			return;
		}

		blast(level, item.owner(), target, blastRadius, Tuning.EXPLOSION_BREAKS_BLOCKS);
	}

	/** An explosion centred on {@code target}; the wielder (if any) is not hurt by it when EXPLOSION_SPARES_WIELDER is on. */
	public static void blast(ServerLevel level, LivingEntity wielder, Entity target, float blastRadius, boolean breakBlocks) {
		boolean shield = Tuning.EXPLOSION_SPARES_WIELDER && wielder != null && wielder != target;
		boolean wasInvulnerable = false;
		if (shield) {
			wasInvulnerable = wielder.isInvulnerable();
			wielder.setInvulnerable(true);
		}
		try {
			Level.ExplosionInteraction interaction = breakBlocks
					? Level.ExplosionInteraction.TNT
					: Level.ExplosionInteraction.NONE;
			level.explode(wielder, target.getX(), target.getY(0.5D), target.getZ(), blastRadius, interaction);
		} finally {
			if (shield) {
				wielder.setInvulnerable(wasInvulnerable);
			}
		}
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
