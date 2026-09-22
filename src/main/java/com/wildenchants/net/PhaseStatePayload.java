package com.wildenchants.net;

import com.wildenchants.WildEnchants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent client -> server whenever the player presses or releases the Phase key. */
public record PhaseStatePayload(boolean holding) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PhaseStatePayload> TYPE =
			new CustomPacketPayload.Type<>(WildEnchants.id("phase_state"));

	public static final StreamCodec<ByteBuf, PhaseStatePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.BOOL, PhaseStatePayload::holding, PhaseStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
