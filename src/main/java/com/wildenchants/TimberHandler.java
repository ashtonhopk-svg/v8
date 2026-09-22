package com.wildenchants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Timber (axes): breaking one log breaks every connected log of the same tree with it. */
public final class TimberHandler {
	private TimberHandler() {}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(TimberHandler::onBlockBroken);
	}

	private static void onBlockBroken(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(player instanceof ServerPlayer serverPlayer) || !state.is(BlockTags.LOGS)) {
			return;
		}
		ItemStack axe = player.getMainHandItem();
		int lvl = ModEnchantments.levelOn(level, ModEnchantments.TIMBER, axe);
		if (lvl <= 0) {
			return;
		}

		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		visited.add(pos);
		for (Direction dir : Direction.values()) {
			queue.add(pos.relative(dir));
		}

		List<BlockPos> toBreak = new ArrayList<>();
		while (!queue.isEmpty() && toBreak.size() < Tuning.TIMBER_MAX_LOGS) {
			BlockPos current = queue.poll();
			if (!visited.add(current)) {
				continue;
			}
			if (!level.getBlockState(current).is(BlockTags.LOGS)) {
				continue;
			}
			toBreak.add(current);
			for (Direction dir : Direction.values()) {
				queue.add(current.relative(dir));
			}
		}

		for (BlockPos logPos : toBreak) {
			// breaks it exactly like a normal survival break: tool check, drops, durability, stats
			serverPlayer.gameMode.destroyBlock(logPos);
		}
	}
}
