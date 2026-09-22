package com.wildenchants.client;

import com.wildenchants.ModEnchantments;
import com.wildenchants.Tuning;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Grapple (any holdable item): right-click a block beyond your normal reach to get yanked toward it,
 * like a hookshot. A single press, not held - self movement only, like Dash/Fallback/Air Jump.
 */
@Environment(EnvType.CLIENT)
public final class GrappleClientHandler {
	private static boolean lastUseDown = false;

	private GrappleClientHandler() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(GrappleClientHandler::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			lastUseDown = false;
			return;
		}
		boolean useDown = client.screen == null && client.options.keyUse.isDown();
		boolean pressed = useDown && !lastUseDown;
		lastUseDown = useDown;
		if (!pressed) {
			return;
		}

		ItemStack held = player.getItemBySlot(EquipmentSlot.MAINHAND);
		int level = ModEnchantments.levelOn(player.level(), ModEnchantments.GRAPPLE, held);
		if (level <= 0) {
			return;
		}

		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getLookAngle().scale(Tuning.GRAPPLE_MAX_RANGE));
		BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		double distance = from.distanceTo(hit.getLocation());
		double normalReach = player.entityInteractionRange(); // close enough to normal reach; used only as a sanity minimum
		if (distance < Math.max(normalReach, 3.0D)) {
			return; // too close to bother grappling
		}

		Vec3 direction = hit.getLocation().subtract(from).normalize();
		double strength = Tuning.GRAPPLE_PULL_STRENGTH * (1.0D + 0.15D * (level - 1));
		player.setDeltaMovement(direction.scale(strength));
		player.fallDistance = 0.0F;
	}
}
