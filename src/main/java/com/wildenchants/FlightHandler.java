package com.wildenchants;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

/**
 * Flight: wearing a chestplate with the Flight enchantment turns on the player's "may fly" ability
 * (player data abilities.mayfly - the same flag creative mode uses).
 * We remember that WE granted it with an entity tag, so flight from other sources (creative mode, other
 * mods, commands) is never taken away, and the tag survives log-out.
 */
public final class FlightHandler {
	private static final String GRANTED_TAG = WildEnchants.MOD_ID + "_flight_granted";

	private FlightHandler() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FlightHandler::onEndTick);
	}

	private static void onEndTick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			update(player);
		}
	}

	private static void update(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
		boolean hasFlight = ModEnchantments.levelOn(player.level(), ModEnchantments.FLIGHT, chestplate) > 0;

		if (hasFlight) {
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				player.addTag(GRANTED_TAG);
				player.onUpdateAbilities();
			}
			// flying wears the chestplate down (not in creative/spectator); when it breaks, flight ends on the next tick
			if (abilities.flying && !player.isCreative() && !player.isSpectator()
					&& player.tickCount % Tuning.FLIGHT_DURABILITY_INTERVAL_TICKS == 0) {
				chestplate.hurtAndBreak(Tuning.FLIGHT_DURABILITY_COST, player, EquipmentSlot.CHEST);
			}
		} else if (player.removeTag(GRANTED_TAG)) { // true = we had granted flight earlier
			if (!player.isCreative() && !player.isSpectator()) {
				abilities.mayfly = false;
				abilities.flying = false;
				player.onUpdateAbilities();
			}
		}
	}
}
