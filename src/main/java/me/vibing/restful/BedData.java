package me.vibing.restful;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

public record BedData(GlobalPos position, String name, Item bedItem, boolean favorite) {
    
    public BedData {
        if (name == null) name = formatCoords(position);
    }
    
    private static String formatCoords(GlobalPos pos) {
        return String.format("%d, %d, %d", pos.pos().getX(), pos.pos().getY(), pos.pos().getZ());
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, position)
                .resultOrPartial(Restful.LOGGER::error)
                .ifPresent(posTag -> tag.put("pos", posTag));
        tag.putString("name", name);
        tag.putString("item", BuiltInRegistries.ITEM.getKey(bedItem).toString());

        tag.putBoolean("favorite", favorite);
        return tag;
    }
    
    public static BedData fromTag(CompoundTag tag) {
        GlobalPos pos = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("pos"))
                .resultOrPartial(Restful.LOGGER::error)
                .orElse(null);
        if (pos == null) return null;
        
        String name = tag.getString("name");
        
        Item item = parseBedItem(tag.getString("item"));
        
        return new BedData(pos, name, item, tag.getBoolean("favorite"));
    }
    
    private static Item parseBedItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Items.RED_BED;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);
            return item != Items.AIR ? item : Items.RED_BED;
        } catch (Exception e) {
            return Items.RED_BED;
        }
    }
}
