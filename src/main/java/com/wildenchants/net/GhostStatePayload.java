package com.wildenchants.net;

import com.wildenchants.WildEnchants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent server -> client to tell the local player whether they're currently an Afterlife ghost. */
public record GhostStatePayload(boolean isGhost) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GhostStatePayload> TYPE =
			new CustomPacketPayload.Type<>(WildEnchants.id("ghost_state"));

	public static final StreamCodec<ByteBuf, GhostStatePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.BOOL, GhostStatePayload::isGhost, GhostStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
