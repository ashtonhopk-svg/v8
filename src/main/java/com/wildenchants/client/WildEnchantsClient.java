package com.wildenchants.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Client side of the mod: key-press based enchantments and effects that need local input/state. */
@Environment(EnvType.CLIENT)
public class WildEnchantsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MovementHandler.register();
		PhaseClientHandler.register();
		GrappleClientHandler.register();
		ClientButtonSync.register();
		GhostPhaseClientHandler.register();
	}
}
