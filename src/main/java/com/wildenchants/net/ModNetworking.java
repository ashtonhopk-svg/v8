package com.wildenchants.net;

import com.wildenchants.TelekinesisHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Registers our tiny "is this button held" packets, and the "you are a ghost" packet, and keeps their current state. */
public final class ModNetworking {
	private static final Map<UUID, Boolean> TELEKINESIS_HOLDING = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> PHASE_HOLDING = new ConcurrentHashMap<>();

	private ModNetworking() {}

	public static void registerCommon() {
		PayloadTypeRegistry.serverboundPlay().register(TelekinesisStatePayload.TYPE, TelekinesisStatePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PhaseStatePayload.TYPE, PhaseStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(GhostStatePayload.TYPE, GhostStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TelekinesisStatePayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			if (payload.holding()) {
				TELEKINESIS_HOLDING.put(id, true);
			} else {
				TELEKINESIS_HOLDING.remove(id);
				context.server().execute(() -> TelekinesisHandler.release(context.player()));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(PhaseStatePayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			if (payload.holding()) {
				PHASE_HOLDING.put(id, true);
			} else {
				PHASE_HOLDING.remove(id);
			}
		});
	}

	public static boolean isHoldingTelekinesis(UUID playerId) {
		return TELEKINESIS_HOLDING.getOrDefault(playerId, false);
	}

	public static boolean isHoldingPhase(UUID playerId) {
		return PHASE_HOLDING.getOrDefault(playerId, false);
	}

	public static void sendGhostState(ServerPlayer player, boolean isGhost) {
		ServerPlayNetworking.send(player, new GhostStatePayload(isGhost));
	}

	public static void clear(UUID playerId) {
		TELEKINESIS_HOLDING.remove(playerId);
		PHASE_HOLDING.remove(playerId);
	}
}
