package com.wildenchants;

import com.mojang.brigadier.context.CommandContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import com.wildenchants.net.ModNetworking;

/**
 * Afterlife (any armor piece): if you're wearing it when you die, you come back from respawn as a ghost -
 * invisible (gear included, via a permanent Invisibility effect), invulnerable, and able to fly, but still
 * able to interact with the world. Run /respawn to become a normal living player again.
 * In hardcore, there is no /respawn - you stay a ghost for good, instead of vanilla's usual permanent spectator lock.
 */
public final class AfterlifeHandler {
	// Tracks, per tick, whether the player currently has Afterlife equipped - read at the moment of death.
	private static final Map<UUID, Boolean> HAD_AFTERLIFE = new HashMap<>();
	private static final Map<UUID, Boolean> IS_GHOST = new HashMap<>();
	private static final Map<UUID, Boolean> PERMANENT_GHOST = new HashMap<>();

	private AfterlifeHandler() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(AfterlifeHandler::trackEquipped);
		ServerLivingEntityEvents.AFTER_DEATH.register(AfterlifeHandler::onDeath);
		ServerPlayerEvents.AFTER_RESPAWN.register(AfterlifeHandler::onRespawn);
		ServerPlayerEvents.JOIN.register(player -> ModNetworking.sendGhostState(player, IS_GHOST.getOrDefault(player.getUUID(), false)));
		ServerTickEvents.END_SERVER_TICK.register(AfterlifeHandler::maintainGhosts);
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
				dispatcher.register(Commands.literal("respawn").executes(AfterlifeHandler::runRespawnCommand)));
	}

	private static void trackEquipped(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean has = false;
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
						&& ModEnchantments.levelOn(player.level(), ModEnchantments.AFTERLIFE, player.getItemBySlot(slot)) > 0) {
					has = true;
					break;
				}
			}
			HAD_AFTERLIFE.put(player.getUUID(), has);
		}
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof ServerPlayer player && HAD_AFTERLIFE.getOrDefault(player.getUUID(), false)) {
			IS_GHOST.put(player.getUUID(), true);
			if (player.level().getServer().isHardcore()) {
				PERMANENT_GHOST.put(player.getUUID(), true);
			}
		}
	}

	private static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		UUID id = oldPlayer.getUUID();
		if (IS_GHOST.getOrDefault(id, false)) {
			becomeGhost(newPlayer);
		}
	}

	private static void becomeGhost(ServerPlayer player) {
		player.setInvulnerable(true);
		// only grant the ABILITY to fly here - actual phasing only kicks in once the player is really
		// flying (see GhostPhaseClientHandler), so we don't force it on and fight a landed player.
		player.getAbilities().mayfly = true;
		player.onUpdateAbilities();
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Tuning.AFTERLIFE_EFFECT_REFRESH_TICKS + 40, 0, true, false));
		ModNetworking.sendGhostState(player, true);
	}

	private static void maintainGhosts(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!IS_GHOST.getOrDefault(player.getUUID(), false)) {
				continue;
			}
			MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
			if (current == null || current.getDuration() < Tuning.AFTERLIFE_EFFECT_REFRESH_TICKS) {
				player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Tuning.AFTERLIFE_EFFECT_REFRESH_TICKS + 40, 0, true, false));
			}
			if (!player.getAbilities().mayfly) {
				player.getAbilities().mayfly = true;
				player.onUpdateAbilities();
			}
			if (!player.isInvulnerable()) {
				player.setInvulnerable(true);
			}
		}
	}

	private static int runRespawnCommand(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("Only a player can use this."));
			return 0;
		}
		UUID id = player.getUUID();
		if (!IS_GHOST.getOrDefault(id, false)) {
			source.sendFailure(Component.literal("You aren't a ghost."));
			return 0;
		}
		if (PERMANENT_GHOST.getOrDefault(id, false)) {
			source.sendFailure(Component.literal("This is a hardcore world - you are a ghost for good."));
			return 0;
		}
		IS_GHOST.remove(id);
		player.removeEffect(MobEffects.INVISIBILITY);
		player.setInvulnerable(false);
		ModNetworking.sendGhostState(player, false);
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
		}
		player.onUpdateAbilities();
		source.sendSuccess(() -> Component.literal("Welcome back to the world of the living."), false);
		return 1;
	}
}
