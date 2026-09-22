package com.wildenchants.net;

import com.wildenchants.WildEnchants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent client -> server whenever the player presses or releases the "use item" (right click) button. */
public record TelekinesisStatePayload(boolean holding) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TelekinesisStatePayload> TYPE =
			new CustomPacketPayload.Type<>(WildEnchants.id("telekinesis_state"));

	public static final StreamCodec<ByteBuf, TelekinesisStatePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.BOOL, TelekinesisStatePayload::holding, TelekinesisStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
