package me.vibing.restful.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ItemUtil {

    private ItemUtil() {}

    public static Item parseBedItem(String itemId) {
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
