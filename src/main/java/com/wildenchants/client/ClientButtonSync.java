package com.wildenchants.client;

import com.wildenchants.net.TelekinesisStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Tells the server whenever the player starts or stops holding right-click, for Telekinesis. */
@Environment(EnvType.CLIENT)
public final class ClientButtonSync {
	private static boolean lastHeld = false;

	private ClientButtonSync() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ClientButtonSync::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		if (client.player == null) {
			lastHeld = false;
			return;
		}
		boolean held = client.screen == null && client.options.keyUse.isDown();
		if (held != lastHeld) {
			lastHeld = held;
			ClientPlayNetworking.send(new TelekinesisStatePayload(held));
		}
	}
}
