package com.wildenchants.client;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared "walk through blocks matching a predicate" nudge, used by both Phase and Afterlife's ghost phasing. */
final class ClientPhasing {
	private ClientPhasing() {}

	/** If the player's own hitbox is currently blocked ONLY by blocks the predicate accepts, nudges them through. */
	static void tryPhaseThrough(LocalPlayer player, double speed, Predicate<BlockState> allowed) {
		Vec3 motion = player.getDeltaMovement();
		Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
		if (horizontal.lengthSqr() < 1.0E-6D) {
			// not moving into anything - use the direction they're facing so holding the key alone still helps
			horizontal = player.getLookAngle();
		}
		Vec3 step = horizontal.normalize().scale(speed);

		if (!isBlockedOnlyBy(player, allowed)) {
			return; // nothing to phase through right now
		}
		player.setPos(player.getX() + step.x, player.getY(), player.getZ() + step.z);
	}

	private static boolean isBlockedOnlyBy(LocalPlayer player, Predicate<BlockState> allowed) {
		AABB box = player.getBoundingBox();
		boolean touchedAllowed = false;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		int minX = (int) Math.floor(box.minX), maxX = (int) Math.floor(box.maxX);
		int minY = (int) Math.floor(box.minY), maxY = (int) Math.floor(box.maxY);
		int minZ = (int) Math.floor(box.minZ), maxZ = (int) Math.floor(box.maxZ);
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					BlockState state = player.level().getBlockState(cursor);
					if (state.isAir()) {
						continue;
					}
					if (allowed.test(state)) {
						touchedAllowed = true;
					} else if (!state.getCollisionShape(player.level(), cursor).isEmpty()) {
						return false; // something not allowed nearby too - play it safe, don't clip through it
					}
				}
			}
		}
		return touchedAllowed;
	}
}
