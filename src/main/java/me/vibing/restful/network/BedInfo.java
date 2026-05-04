package me.vibing.restful.network;

import me.vibing.restful.BedData;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record BedInfo(GlobalPos position, String name, String itemId, int index, boolean isFavorite) {

    public static final StreamCodec<RegistryFriendlyByteBuf, BedInfo> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, BedInfo::position,
            ByteBufCodecs.STRING_UTF8, BedInfo::name,
            ByteBufCodecs.STRING_UTF8, BedInfo::itemId,
            ByteBufCodecs.VAR_INT, BedInfo::index,
            ByteBufCodecs.BOOL, BedInfo::isFavorite,
            BedInfo::new
    );

    public static BedInfo fromBedData(BedData data, int index, boolean isFavorite) {
        // bedItem can theoretically be null if constructed directly with null
        Item item = data.bedItem();
        String itemId = item != null ? item.toString() : "minecraft:red_bed";
        
        return new BedInfo(
                data.position(),
                data.name(),
                itemId,
                index,
                isFavorite
        );
    }
}
