package me.vibing.restful.client;

import me.vibing.restful.Restful;
import me.vibing.restful.network.BedSelectionPacket;
import me.vibing.restful.network.SelectBedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BedSelectionScreen extends Screen {

    private static final int TILE_SIZE = 80;
    private static final int TILE_SPACING = 12;
    private static final int COLUMNS = 3;
    private static final int ITEM_SIZE = 40;

    private static final int COLOR_BG_DEFAULT = 0x30FFFFFF;
    private static final int COLOR_BG_HOVER = 0x60FFFFFF;
    private static final int COLOR_BG_FAVORITE = 0x25FFD700;
    private static final int COLOR_BG_FAVORITE_HOVER = 0x45FFD700;
    private static final int COLOR_BORDER = 0xFFAAAAAA;
    private static final int COLOR_BORDER_HOVER = 0xFFFFFFFF;
    private static final int COLOR_BORDER_FAVORITE = 0xFFFFD700;

    private final List<BedSelectionPacket.BedInfo> beds;
    private int gridTop;

    public BedSelectionScreen(List<BedSelectionPacket.BedInfo> beds) {
        super(Component.literal("Choose Respawn Point"));
        this.beds = beds;
    }

    @Override
    protected void init() {
        super.init();
        int rows = (beds.size() + COLUMNS - 1) / COLUMNS;
        int gridHeight = rows * TILE_SIZE + (rows - 1) * TILE_SPACING;
        gridTop = (height - gridHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0000000);

        String titleText = title.getString();
        int titleWidth = font.width(titleText);
        int titleY = gridTop - 40;
        graphics.drawString(font, titleText, (width - titleWidth) / 2 + 1, titleY + 1, 0x333333);
        graphics.drawString(font, titleText, (width - titleWidth) / 2, titleY, 0xFFFFD700);

        String subtitle = beds.size() <= 9 ? "§7Click or press 1-" + beds.size() : "§7Click to respawn";
        graphics.drawCenteredString(font, Component.literal(subtitle), width / 2, gridTop - 20, 0xAAAAAA);

        for (int i = 0; i < beds.size(); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;

            int itemsInRow = Math.min(COLUMNS, beds.size() - row * COLUMNS);
            int rowWidth = itemsInRow * TILE_SIZE + (itemsInRow - 1) * TILE_SPACING;
            int rowStartX = (width - rowWidth) / 2;

            int x = rowStartX + col * (TILE_SIZE + TILE_SPACING);
            int y = gridTop + row * (TILE_SIZE + TILE_SPACING);

            BedSelectionPacket.BedInfo bed = beds.get(i);

            boolean hovered = mouseX >= x && mouseX < x + TILE_SIZE
                    && mouseY >= y && mouseY < y + TILE_SIZE;

            int bgColor;
            if (bed.isFavorite()) {
                bgColor = hovered ? COLOR_BG_FAVORITE_HOVER : COLOR_BG_FAVORITE;
            } else {
                bgColor = hovered ? COLOR_BG_HOVER : COLOR_BG_DEFAULT;
            }
            
            graphics.fill(x + 2, y + 2, x + TILE_SIZE - 2, y + TILE_SIZE - 2, bgColor);

            int borderColor;
            if (bed.isFavorite()) {
                borderColor = COLOR_BORDER_FAVORITE;
            } else {
                borderColor = hovered ? COLOR_BORDER_HOVER : COLOR_BORDER;
            }
            graphics.renderOutline(x, y, TILE_SIZE, TILE_SIZE, borderColor);

            if (bed.isFavorite()) {
                graphics.fill(x + 1, y + 1, x + TILE_SIZE - 1, y + 4, 0xFFFFD700);
            }

            String name = bed.name();
            BlockPos pos = bed.position().pos();
            String defaultName = String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
            boolean hasCustomName = !name.equals(defaultName);
            
            int numberWidth = (i < 9) ? font.width(String.valueOf(i + 1)) + 8 : 0;
            int starWidth = bed.isFavorite() ? 12 : 0;
            int availableWidth = TILE_SIZE - 10 - numberWidth - starWidth;
            
            int currentX = x + 5;
            if (i < 9) {
                graphics.drawString(font, String.valueOf(i + 1), currentX, y + 6, 0xFFFFFF);
                currentX += font.width(String.valueOf(i + 1)) + 4;
            }
            
            if (hasCustomName) {
                String displayName = name;
                if (font.width(name) > availableWidth) {
                    int maxLen = name.length();
                    while (maxLen > 1 && font.width(name.substring(0, maxLen) + "..") > availableWidth) {
                        maxLen--;
                    }
                    displayName = name.substring(0, maxLen) + "..";
                }
                graphics.drawString(font, Component.literal("§e" + displayName), currentX, y + 6, 0xFFFFD700);
            }
            
            if (bed.isFavorite()) {
                graphics.drawString(font, "§6★", x + TILE_SIZE - 12, y + 6, 0xFFD700);
            }

            Item item = getItemFromId(bed.itemId());
            int itemX = x + (TILE_SIZE - ITEM_SIZE) / 2;
            int itemY = y + 20;
            renderLargeItem(graphics, new ItemStack(item), itemX, itemY);

            String coords = String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
            graphics.drawCenteredString(font, coords, x + TILE_SIZE / 2, y + TILE_SIZE - 12, 0xAAAAAA);
        }
    }

    private void renderLargeItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        float scale = ITEM_SIZE / 16.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < beds.size()) {
                selectBed(beds.get(index).index());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rows = (beds.size() + COLUMNS - 1) / COLUMNS;
        for (int i = 0; i < beds.size(); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;

            int itemsInRow = Math.min(COLUMNS, beds.size() - row * COLUMNS);
            int rowWidth = itemsInRow * TILE_SIZE + (itemsInRow - 1) * TILE_SPACING;
            int rowStartX = (width - rowWidth) / 2;

            int x = rowStartX + col * (TILE_SIZE + TILE_SPACING);
            int y = gridTop + row * (TILE_SIZE + TILE_SPACING);

            BedSelectionPacket.BedInfo bed = beds.get(i);

            if (mouseX >= x && mouseX < x + TILE_SIZE
                    && mouseY >= y && mouseY < y + TILE_SIZE) {
                selectBed(bed.index());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectBed(int index) {
        PacketDistributor.sendToServer(new SelectBedPacket(index));
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(null);
        });
    }

    private Item getItemFromId(String itemId) {
        try {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) return item;
        } catch (Exception e) {
            Restful.LOGGER.debug("Failed to parse item id: {}", itemId);
        }
        return Items.RED_BED;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
