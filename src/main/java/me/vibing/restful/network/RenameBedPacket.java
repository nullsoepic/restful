package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RenameBedPacket(int index, String newName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RenameBedPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:rename_bed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameBedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RenameBedPacket::index,
            ByteBufCodecs.STRING_UTF8, RenameBedPacket::newName,
            RenameBedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
