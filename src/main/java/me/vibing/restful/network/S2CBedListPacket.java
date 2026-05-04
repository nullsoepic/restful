package me.vibing.restful.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// server -> client: send list of available beds for selection
public record S2CBedListPacket(List<BedInfo> beds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CBedListPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.parse("restful:bed_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBedListPacket> STREAM_CODEC = StreamCodec.composite(
            BedInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), S2CBedListPacket::beds,
            S2CBedListPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
