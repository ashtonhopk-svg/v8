package com.wildenchants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Vanilla: a falling block that lands somewhere it can't be placed (on a slab or stair, in an occupied
 * cell, ...) breaks and drops as an item. Here: when that happens, the drop is removed and the block is
 * placed on the nearest valid spot instead (free cell, solid floor below, block can survive there).
 *
 * Done after the fact (the vanilla landing code can't be intercepted without a fragile mixin): we watch
 * every falling block; when one has disappeared without leaving its block behind and a fresh item of that
 * block is lying next to it, we swap the item for a placed block.
 */
public final class FallingBlockLanding {
	private record Tracked(ServerLevel level, BlockState state) {}

	private record Landed(FallingBlockEntity entity, Tracked tracked) {}

	private static final Map<FallingBlockEntity, Tracked> TRACKED = new IdentityHashMap<>();
	private static final List<BlockPos> OFFSETS = buildOffsets();

	private FallingBlockLanding() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FallingBlockLanding::onEndTick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TRACKED.clear());
		if (Tuning.LANDING_FIX_ALL_FALLING_BLOCKS) {
			ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
				if (entity instanceof FallingBlockEntity block) {
					track(level, block);
				}
			});
		}
	}

	public static void track(ServerLevel level, FallingBlockEntity block) {
		TRACKED.putIfAbsent(block, new Tracked(level, block.getBlockState()));
	}

	private static void onEndTick(MinecraftServer server) {
		if (TRACKED.isEmpty()) {
			return;
		}
		List<Landed> finished = new ArrayList<>();
		TRACKED.entrySet().removeIf(entry -> {
			if (entry.getKey().isRemoved()) {
				finished.add(new Landed(entry.getKey(), entry.getValue()));
				return true;
			}
			return false;
		});
		for (Landed landed : finished) {
			recover(landed);
		}
	}

	private static void recover(Landed landed) {
		FallingBlockEntity entity = landed.entity();
		ServerLevel level = landed.tracked().level();
		BlockState state = landed.tracked().state();

		if (entity.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
			return; // unloaded / moved dimension: not a landing
		}
		BlockPos at = entity.blockPosition();
		if (level.isOutsideBuildHeight(at) || !level.hasChunkAt(at)) {
			return; // fell out of the world
		}
		if (level.getBlockState(at).is(state.getBlock())) {
			return; // landed normally
		}

		Item dropped = state.getBlock().asItem();
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(1.5D),
				drop -> drop.tickCount <= 1 && drop.getItem().is(dropped));
		if (drops.isEmpty()) {
			return; // it did not break (concrete powder turning solid, drops disabled, ...)
		}

		BlockPos spot = findSpot(level, at, state);
		if (spot == null) {
			return; // nowhere to put it: leave the vanilla drop
		}
		drops.get(0).discard();
		level.setBlock(spot, state, Block.UPDATE_ALL);
	}

	/** Nearest spot around {@code origin} (including {@code origin} itself) where {@code state} could be placed, or null. */
	public static BlockPos findSpot(ServerLevel level, BlockPos origin, BlockState state) {
		for (BlockPos offset : OFFSETS) {
			BlockPos candidate = origin.offset(offset.getX(), offset.getY(), offset.getZ());
			if (canLandAt(level, candidate, state)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean canLandAt(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) {
			return false;
		}
		BlockState here = level.getBlockState(pos);
		boolean free = here.isAir() || (here.canBeReplaced() && here.getFluidState().isEmpty());
		if (!free) {
			return false;
		}
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
			return false; // would just fall again / hang in the air
		}
		return state.canSurvive(level, pos);
	}

	/** All offsets within the search radius, nearest first. */
	private static List<BlockPos> buildOffsets() {
		int r = Tuning.LANDING_SEARCH_RADIUS;
		List<BlockPos> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					list.add(new BlockPos(dx, dy, dz));
				}
			}
		}
		list.sort(Comparator
				.comparingInt((BlockPos p) -> p.getX() * p.getX() + p.getY() * p.getY() + p.getZ() * p.getZ())
				.thenComparingInt(BlockPos::getY));
		return list;
	}
}
