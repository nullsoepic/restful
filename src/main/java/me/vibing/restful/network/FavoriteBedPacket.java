package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// client -> server: toggle favorite status
public record FavoriteBedPacket(int index) implements CustomPacketPayload {
    
    public static final Type<FavoriteBedPacket> TYPE = 
            new Type<>(ResourceLocation.parse("restful:favorite_bed"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FavoriteBedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FavoriteBedPacket::index,
            FavoriteBedPacket::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
