package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record S2COpenManagementPacket(List<BedInfo> beds) implements CustomPacketPayload {

    public static final Type<S2COpenManagementPacket> TYPE =
            new Type<>(ResourceLocation.parse("restful:open_management"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenManagementPacket> STREAM_CODEC = StreamCodec.composite(
            BedInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), S2COpenManagementPacket::beds,
            S2COpenManagementPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
