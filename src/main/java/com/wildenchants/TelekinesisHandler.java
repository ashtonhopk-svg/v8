package com.wildenchants;

import com.wildenchants.net.ModNetworking;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Telekinesis (any holdable item): hold right-click on a block or entity beyond your normal interaction
 * range to drag it around - it follows your crosshair at the distance you first grabbed it, and drops /
 * lets go when you release the button.
 */
public final class TelekinesisHandler {
	private sealed interface Held permits HeldBlock, HeldEntity {}
	private record HeldBlock(FallingBlockEntity entity) implements Held {}
	private record HeldEntity(Entity entity) implements Held {}

	private record Lock(Held held, double distance) {}

	private static final Map<UUID, Lock> LOCKS = new HashMap<>();

	private TelekinesisHandler() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TelekinesisHandler::onEndTick);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> release(oldPlayer));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> LOCKS.clear());
	}

	public static void release(ServerPlayer player) {
		Lock lock = LOCKS.remove(player.getUUID());
		if (lock != null && lock.held() instanceof HeldBlock heldBlock) {
			placeBackDown(heldBlock.entity());
		}
	}

	/** Telekinesis only (not a real fall): the held block turns back into a real block right where it is,
	 *  instead of dropping and falling like a normal falling block. */
	private static void placeBackDown(FallingBlockEntity entity) {
		if (entity.isRemoved() || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		BlockState state = entity.getBlockState();
		BlockPos origin = BlockPos.containing(entity.position());
		BlockPos spot = FallingBlockLanding.findSpot(level, origin, state);
		entity.discard();
		if (spot != null) {
			level.setBlock(spot, state, Block.UPDATE_ALL);
		} else {
			// nowhere valid nearby (mid-air, wedged, ...) - drop it as an item rather than lose it silently
			Containers.dropItemStack(level, origin.getX(), origin.getY(), origin.getZ(),
					new ItemStack(state.getBlock().asItem()));
		}
	}

	private static void onEndTick(MinecraftServer server) {
		if (LOCKS.isEmpty() && server.getPlayerList().getPlayers().stream()
				.noneMatch(p -> ModNetworking.isHoldingTelekinesis(p.getUUID()))) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean wantsToHold = ModNetworking.isHoldingTelekinesis(player.getUUID());
			ItemStack held = player.getItemBySlot(EquipmentSlot.MAINHAND);
			int level = ModEnchantments.levelOn(player.level(), ModEnchantments.TELEKINESIS, held);
			if (!wantsToHold || level <= 0) {
				release(player);
				continue;
			}
			Lock lock = LOCKS.get(player.getUUID());
			if (lock == null) {
				tryLockOn(player);
			} else {
				follow(player, lock);
			}
		}
	}

	private static void tryLockOn(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getLookAngle().scale(Tuning.TELEKINESIS_MAX_RANGE));

		EntityHitResult entityHit = pickEntity(player, from, to);
		BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		double entityDist = entityHit != null ? from.distanceTo(entityHit.getLocation()) : Double.MAX_VALUE;
		double blockDist = blockHit.getType() == HitResult.Type.BLOCK ? from.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;
		if (entityDist == Double.MAX_VALUE && blockDist == Double.MAX_VALUE) {
			return;
		}

		if (entityDist <= blockDist) {
			if (entityDist <= player.entityInteractionRange()) {
				return; // within normal reach - nothing special to do
			}
			LOCKS.put(player.getUUID(), new Lock(new HeldEntity(entityHit.getEntity()), entityDist));
		} else {
			if (blockDist <= player.blockInteractionRange()) {
				return;
			}
			BlockPos pos = blockHit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
				return; // unbreakable or has data (chest, ...) - too risky to lift
			}
			level.removeBlock(pos, false);
			FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
			falling.setNoGravity(true);
			falling.setDeltaMovement(Vec3.ZERO);
			LOCKS.put(player.getUUID(), new Lock(new HeldBlock(falling), blockDist));
		}
	}

	private static void follow(ServerPlayer player, Lock lock) {
		Entity entity = switch (lock.held()) {
			case HeldBlock heldBlock -> heldBlock.entity();
			case HeldEntity heldEntity -> heldEntity.entity();
		};
		if (!entity.isAlive() || entity.isRemoved()) {
			LOCKS.remove(player.getUUID());
			return;
		}
		Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(lock.distance()));
		Vec3 current = entity.position();
		Vec3 next = current.lerp(target, Tuning.TELEKINESIS_FOLLOW_SPEED);
		entity.setPos(next.x, next.y, next.z);
		entity.setDeltaMovement(Vec3.ZERO);
	}

	private static EntityHitResult pickEntity(ServerPlayer player, Vec3 from, Vec3 to) {
		AABB searchBox = player.getBoundingBox().expandTowards(player.getLookAngle().scale(Tuning.TELEKINESIS_MAX_RANGE)).inflate(1.0D);
		double bestDistSq = Double.MAX_VALUE;
		EntityHitResult best = null;
		for (Entity candidate : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
			var clip = box.clip(from, to);
			if (clip.isPresent()) {
				double distSq = from.distanceToSqr(clip.get());
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					best = new EntityHitResult(candidate, clip.get());
				}
			}
		}
		return best;
	}
}
