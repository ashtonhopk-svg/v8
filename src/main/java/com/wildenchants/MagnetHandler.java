package com.wildenchants;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * Magnet (armor): each equipped piece pulls you toward the nearest magnetic block (iron block, raw iron
 * block, lodestone - see the wild_enchants:magnetic block tag) in the direction that piece covers:
 * helmet = up, boots = down, leggings/chestplate = sideways. Wear pieces on opposite sides and the pulls
 * can cancel out into a hover.
 */
public final class MagnetHandler {
	private MagnetHandler() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MagnetHandler::onEndTick);
	}

	private static void onEndTick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.getAbilities().flying) {
				continue; // don't fight creative/Flight-enchant flying
			}
			Vec3 pull = Vec3.ZERO;
			pull = pull.add(pullFor(player, EquipmentSlot.HEAD, new Direction[] { Direction.UP }));
			pull = pull.add(pullFor(player, EquipmentSlot.FEET, new Direction[] { Direction.DOWN }));
			Direction[] sideways = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
			pull = pull.add(pullFor(player, EquipmentSlot.LEGS, sideways));
			pull = pull.add(pullFor(player, EquipmentSlot.CHEST, sideways));

			if (pull.lengthSqr() > 1.0E-6D) {
				player.setDeltaMovement(player.getDeltaMovement().add(pull));
				player.fallDistance = 0.0F;
			}
		}
	}

	private static Vec3 pullFor(ServerPlayer player, EquipmentSlot slot, Direction[] directions) {
		int level = ModEnchantments.levelOn(player.level(), ModEnchantments.MAGNET, player.getItemBySlot(slot));
		if (level <= 0) {
			return Vec3.ZERO;
		}
		Vec3 total = Vec3.ZERO;
		for (Direction dir : directions) {
			double distance = nearestMagneticDistance(player, dir);
			if (distance > 0) {
				double strength = Tuning.MAGNET_STRENGTH_PER_LEVEL * level / distance;
				total = total.add(dir.getStepX() * strength, dir.getStepY() * strength, dir.getStepZ() * strength);
			}
		}
		return total;
	}

	/** Distance (blocks, >= 1) to the nearest magnetic block along this direction, or -1 if none within range. */
	private static double nearestMagneticDistance(ServerPlayer player, Direction dir) {
		BlockPos origin = player.blockPosition();
		int max = (int) Tuning.MAGNET_MAX_RANGE;
		for (int step = 1; step <= max; step++) {
			BlockPos pos = origin.relative(dir, step);
			if (player.level().getBlockState(pos).is(ModTags.MAGNETIC)) {
				return step;
			}
		}
		return -1;
	}
}
