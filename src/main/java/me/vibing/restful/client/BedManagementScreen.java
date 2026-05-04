package me.vibing.restful.client;

import me.vibing.restful.network.BedInfo;
import me.vibing.restful.network.C2SBedActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BedManagementScreen extends Screen {

    private static final int ROW_HEIGHT = 32;
    private static final int LIST_WIDTH = 340;
    private static final int LIST_HEIGHT = 240;

    private final List<ManagedBed> beds;
    private int scrollOffset = 0;
    private int listTop;
    private int listLeft;
    private EditBox activeEditBox = null;
    private int editingIndex = -1;
    private int pendingRemoveIndex = -1;

    public BedManagementScreen(List<BedInfo> beds) {
        super(Component.literal("Manage Respawn Points"));
        this.beds = new ArrayList<>();
        for (int i = 0; i < beds.size(); i++) {
            this.beds.add(new ManagedBed(beds.get(i), i));
        }
    }

    @Override
    protected void init() {
        super.init();
        listLeft = (width - LIST_WIDTH) / 2;
        listTop = (height - LIST_HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> closeAndSync())
                .pos(listLeft + LIST_WIDTH - 50, listTop + LIST_HEIGHT + 8)
                .size(50, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0000000);

        String titleText = title.getString();
        graphics.drawString(font, titleText, (width - font.width(titleText)) / 2, listTop - 22, 0xFFFFD700);

        graphics.fill(listLeft - 4, listTop - 4, listLeft + LIST_WIDTH + 4, listTop + LIST_HEIGHT + 4, 0x30FFFFFF);
        graphics.renderOutline(listLeft - 4, listTop - 4, LIST_WIDTH + 8, LIST_HEIGHT + 8, 0xFFAAAAAA);

        int contentHeight = beds.size() * ROW_HEIGHT;
        if (contentHeight > LIST_HEIGHT) {
            drawScrollbar(graphics, contentHeight);
        }

        for (int i = 0; i < beds.size(); i++) {
            int y = listTop + i * ROW_HEIGHT - scrollOffset;
            if (y + ROW_HEIGHT < listTop || y > listTop + LIST_HEIGHT) continue;

            ManagedBed bed = beds.get(i);
            drawRow(graphics, mouseX, mouseY, partialTick, i, bed, y);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics graphics, int contentHeight) {
        int scrollbarHeight = Math.max(30, LIST_HEIGHT * LIST_HEIGHT / contentHeight);
        int maxScroll = contentHeight - LIST_HEIGHT;
        int scrollbarY = listTop + (scrollOffset * (LIST_HEIGHT - scrollbarHeight) / maxScroll);

        graphics.fill(listLeft + LIST_WIDTH + 6, listTop, listLeft + LIST_WIDTH + 10, listTop + LIST_HEIGHT, 0x20FFFFFF);
        graphics.fill(listLeft + LIST_WIDTH + 6, scrollbarY, listLeft + LIST_WIDTH + 10, scrollbarY + scrollbarHeight, 0x80AAAAAA);
    }

    private void drawRow(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int i, ManagedBed bed, int y) {
        boolean isPendingRemove = pendingRemoveIndex == i;

        int bgColor = isPendingRemove ? 0x40FF4444 : (bed.favorite ? 0x25FFD700 : 0x20FFFFFF);
        graphics.fill(listLeft, y, listLeft + LIST_WIDTH, y + ROW_HEIGHT, bgColor);

        graphics.renderItem(new ItemStack(parseBedItem(bed.info.itemId())), listLeft + 4, y + 6);

        if (editingIndex == i && activeEditBox != null) {
            activeEditBox.setX(listLeft + 28);
            activeEditBox.setY(y + 6);
            activeEditBox.setWidth(LIST_WIDTH - 160);
            activeEditBox.render(graphics, mouseX, mouseY, partialTick);
        } else {
            String name = bed.name;
            int maxWidth = LIST_WIDTH - 160;
            if (font.width(name) > maxWidth) {
                name = font.plainSubstrByWidth(name, maxWidth - 6) + "..";
            }
            graphics.drawString(font, name, listLeft + 28, y + 6, bed.favorite ? 0xFFD700 : 0xFFFFFF);

            String coords = String.format("§7%d %d %d", bed.info.position().pos().getX(), bed.info.position().pos().getY(), bed.info.position().pos().getZ());
            String dim = getDimAbbreviation(bed.info.position().dimension().location().toString());
            graphics.drawString(font, coords + " §7" + dim, listLeft + 28, y + 18, 0xAAAAAA);
        }

        int rightX = listLeft + LIST_WIDTH - 8;

        String removeText = isPendingRemove ? "§csure?" : "§c×";
        int removeWidth = font.width(removeText);
        graphics.drawString(font, removeText, rightX - removeWidth, y + 11, 0xFFFFFF);

        String star = bed.favorite ? "§6★" : "§7☆";
        graphics.drawString(font, star, rightX - 50, y + 11, 0xFFFFFF);

        if (i > 0) {
            String up = isHovered(mouseX, mouseY, rightX - 80, y + 2, 20, 14) ? "§f▲" : "§7▲";
            graphics.drawString(font, up, rightX - 78, y + 4, 0xFFFFFF);
        }
        if (i < beds.size() - 1) {
            String down = isHovered(mouseX, mouseY, rightX - 80, y + 16, 20, 14) ? "§f▼" : "§7▼";
            graphics.drawString(font, down, rightX - 78, y + 18, 0xFFFFFF);
        }
    }

    private String getDimAbbreviation(String dimId) {
        return switch (dimId) {
            case "minecraft:overworld" -> "ow";
            case "minecraft:the_nether" -> "n";
            case "minecraft:the_end" -> "e";
            default -> "?";
        };
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeEditBox != null && !activeEditBox.isMouseOver(mouseX, mouseY)) {
            finishEditing();
        }

        for (int i = 0; i < beds.size(); i++) {
            int y = listTop + i * ROW_HEIGHT - scrollOffset;
            if (y + ROW_HEIGHT < listTop || y > listTop + LIST_HEIGHT) continue;

            if (!isHovered((int) mouseX, (int) mouseY, listLeft, y, LIST_WIDTH, ROW_HEIGHT)) continue;

            ManagedBed bed = beds.get(i);
            int rightX = listLeft + LIST_WIDTH - 8;

            if (isHovered((int) mouseX, (int) mouseY, listLeft + 28, y, LIST_WIDTH - 160, ROW_HEIGHT)) {
                startEditing(i);
                return true;
            }

            if (isHovered((int) mouseX, (int) mouseY, rightX - 52, y, 24, ROW_HEIGHT)) {
                bed.favorite = !bed.favorite;
                PacketDistributor.sendToServer(C2SBedActionPacket.favorite(i));
                playClick();
                return true;
            }

            if (i > 0 && isHovered((int) mouseX, (int) mouseY, rightX - 82, y, 22, 16)) {
                swapBeds(i, i - 1);
                return true;
            }
            if (i < beds.size() - 1 && isHovered((int) mouseX, (int) mouseY, rightX - 82, y + 16, 22, 16)) {
                swapBeds(i, i + 1);
                return true;
            }

            int removeWidth = pendingRemoveIndex == i ? font.width("sure?") + 4 : 12;
            if (isHovered((int) mouseX, (int) mouseY, rightX - removeWidth, y, removeWidth + 4, ROW_HEIGHT)) {
                if (pendingRemoveIndex == i) {
                    removeBed(i);
                } else {
                    pendingRemoveIndex = i;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int contentHeight = beds.size() * ROW_HEIGHT;
        if (contentHeight <= LIST_HEIGHT) return true;

        int maxScroll = contentHeight - LIST_HEIGHT;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * 25)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeEditBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                finishEditing();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelEditing();
                return true;
            }
            return activeEditBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAndSync();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeEditBox != null) {
            return activeEditBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void startEditing(int index) {
        editingIndex = index;
        activeEditBox = new EditBox(font, 0, 0, LIST_WIDTH - 160, 18, Component.empty());
        activeEditBox.setValue(beds.get(index).name);
        activeEditBox.setFocused(true);
        activeEditBox.setTextColor(0xFFFFD700);
        addWidget(activeEditBox);
    }

    private void finishEditing() {
        if (activeEditBox != null && editingIndex >= 0) {
            String newName = activeEditBox.getValue().trim();
            beds.get(editingIndex).name = newName;
            PacketDistributor.sendToServer(C2SBedActionPacket.rename(editingIndex, newName));
        }
        cancelEditing();
    }

    private void cancelEditing() {
        if (activeEditBox != null) {
            removeWidget(activeEditBox);
        }
        activeEditBox = null;
        editingIndex = -1;
    }

    private void swapBeds(int a, int b) {
        ManagedBed temp = beds.get(a);
        beds.set(a, beds.get(b));
        beds.set(b, temp);
        playClick();
    }

    private void removeBed(int index) {
        PacketDistributor.sendToServer(C2SBedActionPacket.remove(index));
        beds.remove(index);
        pendingRemoveIndex = -1;
        playClick();

        int contentHeight = beds.size() * ROW_HEIGHT;
        if (contentHeight > LIST_HEIGHT) {
            scrollOffset = Math.min(scrollOffset, contentHeight - LIST_HEIGHT);
        } else {
            scrollOffset = 0;
        }
    }

    private void closeAndSync() {
        if (activeEditBox != null) finishEditing();

        List<Integer> newOrder = new ArrayList<>();
        for (ManagedBed bed : beds) {
            newOrder.add(bed.originalIndex);
        }
        PacketDistributor.sendToServer(C2SBedActionPacket.reorder(newOrder));

        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

    private static class ManagedBed {
        BedInfo info;
        String name;
        boolean favorite;
        int originalIndex;

        ManagedBed(BedInfo info, int originalIndex) {
            this.info = info;
            this.name = info.name();
            this.favorite = info.isFavorite();
            this.originalIndex = originalIndex;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
