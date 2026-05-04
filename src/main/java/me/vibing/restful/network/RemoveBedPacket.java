package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveBedPacket(int index) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RemoveBedPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:remove_bed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveBedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoveBedPacket::index,
            RemoveBedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
