package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ReorderBedPacket(List<Integer> newOrder) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReorderBedPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:reorder_bed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReorderBedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ReorderBedPacket::newOrder,
            ReorderBedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
