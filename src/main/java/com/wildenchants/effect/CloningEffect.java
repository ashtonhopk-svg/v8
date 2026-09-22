package com.wildenchants.effect;

import com.mojang.serialization.MapCodec;
import com.wildenchants.Tuning;
import com.wildenchants.WildEnchants;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Cloning: on a kill, spawn 2 half-size copies (Attributes.SCALE) of the victim.
 *  - Players turn into Mannequins with the player's name tag and skin.
 *  - Every clone remembers its generation (entity tag). A victim of generation g is cloned again only
 *    if g < enchantment level, so Cloning I clones originals only, Cloning II also clones the clones once
 *    more (like slimes splitting).
 */
public record CloningEffect() implements EnchantmentEntityEffect {
	public static final CloningEffect INSTANCE = new CloningEffect();
	public static final MapCodec<CloningEffect> CODEC = MapCodec.unit(INSTANCE);

	private static final String GENERATION_PREFIX = WildEnchants.MOD_ID + "_clone_gen_";
	private static final String ALREADY_CLONED_TAG = WildEnchants.MOD_ID + "_cloned";

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
		if (!(target instanceof LivingEntity victim) || !victim.isDeadOrDying()) {
			return; // only on kills
		}
		if (!isCloneable(victim) || hasTag(victim, ALREADY_CLONED_TAG)) {
			return;
		}
		int generation = generationOf(victim);
		if (generation >= enchantmentLevel) {
			return; // clones of this generation are not cloned again at this enchantment level
		}
		victim.addTag(ALREADY_CLONED_TAG);

		RandomSource random = level.getRandom();
		for (int i = 0; i < Tuning.CLONES_PER_KILL; i++) {
			double x = victim.getX() + (random.nextDouble() - 0.5D);
			double z = victim.getZ() + (random.nextDouble() - 0.5D);
			if (victim instanceof ServerPlayer player) {
				summonMannequin(level, player, generation + 1, x, victim.getY(), z);
			} else {
				spawnCopy(level, victim, generation + 1, x, victim.getY(), z);
			}
		}
	}

	private static boolean isCloneable(LivingEntity victim) {
		return !(victim instanceof EnderDragon) && !(victim instanceof WitherBoss) && !(victim instanceof ArmorStand);
	}

	/** Highest generation number that is looked for (Cloning only goes up to level 2; this leaves room for datapack changes). */
	private static final int MAX_GENERATION = 8;

	/** Entity tags can only be added/removed here, so "has tag" = try to add it (false = it was already there). */
	private static boolean hasTag(Entity entity, String tag) {
		if (entity.addTag(tag)) {
			entity.removeTag(tag);
			return false;
		}
		return true;
	}

	private static int generationOf(Entity entity) {
		for (int generation = MAX_GENERATION; generation >= 1; generation--) {
			if (hasTag(entity, GENERATION_PREFIX + generation)) {
				return generation;
			}
		}
		return 0;
	}

	/** Generic entity: copy all data of the victim, then shrink it. */
	private static void spawnCopy(ServerLevel level, LivingEntity victim, int generation, double x, double y, double z) {
		EntityType<?> type = victim.getType();
		Entity copy = type.create(level, EntitySpawnReason.TRIGGERED);
		if (copy == null) {
			return;
		}
		copy.restoreFrom(victim);            // same data (NBT) as the victim
		copy.setUUID(UUID.randomUUID());     // must be unique
		for (int old = 1; old <= MAX_GENERATION; old++) {
			copy.removeTag(GENERATION_PREFIX + old);
		}
		copy.removeTag(ALREADY_CLONED_TAG);
		copy.addTag(GENERATION_PREFIX + generation);

		if (copy instanceof LivingEntity living) {
			living.setHealth(living.getMaxHealth()); // the copied data still says "dead"
			AttributeInstance scale = living.getAttribute(Attributes.SCALE);
			if (scale != null) {
				scale.setBaseValue(scale.getBaseValue() * Tuning.CLONE_SCALE_FACTOR);
			}
		}
		copy.setPos(x, y, z);
		copy.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(copy);
	}

	/** Player: a Mannequin with the same name tag + skin, half size. Spawned through /summon so the NBT format stays version-proof. */
	private static void summonMannequin(ServerLevel level, ServerPlayer player, int generation, double x, double y, double z) {
		CompoundTag data = new CompoundTag();

		ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(player.getGameProfile()))
				.result()
				.ifPresent(profileTag -> data.put("profile", profileTag));

		CompoundTag name = new CompoundTag();
		name.putString("text", player.getName().getString());
		data.put("CustomName", name);
		data.putBoolean("CustomNameVisible", true);
		data.putBoolean("hide_description", true); // no "Mannequin" line under the name

		CompoundTag scale = new CompoundTag();
		scale.putString("id", "minecraft:scale");
		scale.putDouble("base", player.getAttributeBaseValue(Attributes.SCALE) * Tuning.CLONE_SCALE_FACTOR);
		ListTag attributes = new ListTag();
		attributes.add(scale);
		data.put("attributes", attributes);

		ListTag tags = new ListTag();
		tags.add(StringTag.valueOf(GENERATION_PREFIX + generation));
		data.put("Tags", tags);

		MinecraftServer server = level.getServer();
		CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
		String command = String.format(Locale.ROOT, "summon minecraft:mannequin %.3f %.3f %.3f %s", x, y, z, data);
		try {
			server.getCommands().performPrefixedCommand(source, command);
		} catch (RuntimeException e) {
			WildEnchants.LOGGER.warn("Cloning: could not summon mannequin for {}", player.getName().getString(), e);
		}
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
