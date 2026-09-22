package com.wildenchants.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wildenchants.ModEnchantments;
import com.wildenchants.ModTags;
import com.wildenchants.Tuning;
import com.wildenchants.WildEnchants;
import com.wildenchants.net.PhaseStatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Phase (boots): while the key is held, you can walk through a curated set of thin/decorative blocks
 * (leaves, cobwebs, vines, ferns, tall grass, ...) - see the {@code wild_enchants:phaseable} block tag.
 *
 * This nudges the LOCAL player's own position each tick, the same way Dash/Fallback do, rather than
 * disabling collision for the whole game - it only lets you slip through blocks in that tag, nothing else.
 */
@Environment(EnvType.CLIENT)
public final class PhaseClientHandler {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(WildEnchants.id("controls"));
	public static KeyMapping KEY;
	private static boolean lastHeld = false;

	private PhaseClientHandler() {}

	public static void register() {
		KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.wild_enchants.phase", InputConstants.Type.KEYSYM, InputConstants.KEY_V, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(PhaseClientHandler::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			lastHeld = false;
			return;
		}
		boolean held = client.screen == null && KEY.isDown();
		if (held != lastHeld) {
			lastHeld = held;
			ClientPlayNetworking.send(new PhaseStatePayload(held));
		}
		if (!held) {
			return;
		}
		int level = ModEnchantments.levelOn(player.level(), ModEnchantments.PHASE, player.getItemBySlot(EquipmentSlot.FEET));
		if (level <= 0) {
			return;
		}
		ClientPhasing.tryPhaseThrough(player, Tuning.PHASE_SPEED, state -> state.is(ModTags.PHASEABLE));
	}
}
