package com.wildenchants.client;

import com.wildenchants.Tuning;
import com.wildenchants.net.GhostStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Afterlife ghosts can walk through any solid block, but only while actually flying - land on the ground
 * and normal collision applies. Unlike Phase, the server tells us when we're a ghost (see GhostStatePayload),
 * since that state isn't otherwise known to the client.
 */
@Environment(EnvType.CLIENT)
public final class GhostPhaseClientHandler {
	private static boolean isGhost = false;

	private GhostPhaseClientHandler() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(GhostStatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> isGhost = payload.isGhost()));
		ClientTickEvents.END_CLIENT_TICK.register(GhostPhaseClientHandler::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		if (!isGhost || !player.getAbilities().flying) {
			return; // only while actually flying - on the ground, normal collision applies
		}
		ClientPhasing.tryPhaseThrough(player, Tuning.AFTERLIFE_GHOST_PHASE_SPEED, state -> true);
	}
}
