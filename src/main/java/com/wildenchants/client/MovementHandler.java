package com.wildenchants.client;

import com.wildenchants.ModEnchantments;
import com.wildenchants.Tuning;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Air Jump (boots), Dash and Fallback (leggings).
 *
 * Keys are only known on the client, and the local player's movement is decided by the client
 * (the server just follows the position the client reports, as it does for a normal jump), so all of
 * this happens here and the server needs no extra packets.
 *
 *  - Air Jump: pressing jump while already in the air gives a fresh jump. Level = number of air jumps
 *    per time in the air (reset when you touch the ground, water, a ladder, ...).
 *  - Dash: double-tap forward in the air = horizontal push forward (once per time in the air).
 *  - Fallback: double-tap back = horizontal push backward (once per time in the air).
 */
@Environment(EnvType.CLIENT)
public final class MovementHandler {
	private static int ticks = 0;
	private static int airJumpsUsed = 0;
	private static boolean dashUsed = false;
	private static boolean fallbackUsed = false;
	private static boolean wasAirborne = false;
	private static boolean jumpWasDown = false;
	private static final DoubleTap FORWARD_TAP = new DoubleTap();
	private static final DoubleTap BACKWARD_TAP = new DoubleTap();

	private MovementHandler() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(MovementHandler::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			resetAll();
			return;
		}
		ticks++;

		boolean inputActive = client.screen == null; // no key presses while a menu/inventory is open
		boolean jumpDown = inputActive && client.options.keyJump.isDown();
		boolean jumpPressed = jumpDown && !jumpWasDown;
		jumpWasDown = jumpDown;
		boolean forwardTapped = FORWARD_TAP.update(inputActive && client.options.keyUp.isDown(), ticks);
		boolean backwardTapped = BACKWARD_TAP.update(inputActive && client.options.keyDown.isDown(), ticks);

		boolean supported = player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()
				|| player.getAbilities().flying || player.isPassenger();
		if (supported) {
			airJumpsUsed = 0;
			dashUsed = false;
			fallbackUsed = false;
		}
		boolean airborne = !supported && !player.isFallFlying();
		// must already have been in the air on the previous tick, otherwise the press that starts a normal
		// jump from the ground would immediately count as an air jump too
		boolean inAir = airborne && wasAirborne;
		wasAirborne = airborne;

		boolean groundedBack = Tuning.FALLBACK_WORKS_ON_GROUND && !airborne && player.onGround();

		if (inAir && jumpPressed) {
			tryAirJump(player);
		}
		if (inAir && forwardTapped) {
			tryPush(player, ModEnchantments.DASH, EquipmentSlot.LEGS, 1.0D, Tuning.DASH_BASE_STRENGTH, Tuning.DASH_STRENGTH_PER_LEVEL, true);
		}
		if ((inAir || groundedBack) && backwardTapped) {
			tryPush(player, ModEnchantments.FALLBACK, EquipmentSlot.LEGS, -1.0D, Tuning.FALLBACK_BASE_STRENGTH, Tuning.FALLBACK_STRENGTH_PER_LEVEL, false);
		}
	}

	private static void tryAirJump(LocalPlayer player) {
		int allowed = ModEnchantments.levelOn(player.level(), ModEnchantments.AIR_JUMP, player.getItemBySlot(EquipmentSlot.FEET));
		if (airJumpsUsed >= allowed) {
			return;
		}
		airJumpsUsed++;
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, Tuning.AIR_JUMP_VELOCITY, motion.z);
	}

	private static void tryPush(LocalPlayer player, ResourceKey<Enchantment> key, EquipmentSlot slot, double direction,
			double base, double perLevel, boolean isDash) {
		if ((isDash && dashUsed) || (!isDash && fallbackUsed && !player.onGround())) {
			return;
		}
		int level = ModEnchantments.levelOn(player.level(), key, player.getItemBySlot(slot));
		if (level <= 0) {
			return;
		}
		if (isDash) {
			dashUsed = true;
		} else if (!player.onGround()) {
			fallbackUsed = true;
		}
		double strength = base + perLevel * level;
		float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
		double pushX = -Mth.sin(yaw) * strength * direction;
		double pushZ = Mth.cos(yaw) * strength * direction;
		player.setDeltaMovement(player.getDeltaMovement().add(pushX, 0.0D, pushZ));
	}

	private static void resetAll() {
		airJumpsUsed = 0;
		dashUsed = false;
		fallbackUsed = false;
		wasAirborne = false;
		jumpWasDown = false;
		FORWARD_TAP.reset();
		BACKWARD_TAP.reset();
	}

	/** Detects "pressed twice within a few ticks". */
	private static final class DoubleTap {
		private boolean wasDown = false;
		private int lastPressTick = -1000;

		boolean update(boolean down, int now) {
			boolean pressed = down && !wasDown;
			wasDown = down;
			if (!pressed) {
				return false;
			}
			boolean doubleTap = now - lastPressTick <= Tuning.DOUBLE_TAP_WINDOW_TICKS;
			lastPressTick = doubleTap ? -1000 : now;
			return doubleTap;
		}

		void reset() {
			wasDown = false;
			lastPressTick = -1000;
		}
	}
}
