package me.vibing.restful;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BedEventHandler {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof BedBlock) {
            ItemStack heldItem = player.getItemInHand(event.getHand());
            if (heldItem.is(Items.NAME_TAG)) {
                return;
            }

            BlockPos headPos = pos;
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                headPos = pos.relative(state.getValue(BedBlock.FACING));
            }

            var bedItem = BedTracker.getBedItemFromState(state);
            handleRespawnBlock(serverPlayer, GlobalPos.of(level.dimension(), headPos), null, bedItem);
        }

        if (state.getBlock() instanceof RespawnAnchorBlock) {
            if (state.getValue(RespawnAnchorBlock.CHARGE) <= 0) {
                return;
            }
            if (!RespawnAnchorBlock.canSetSpawn(level)) {
                return;
            }

            handleRespawnBlock(serverPlayer, GlobalPos.of(level.dimension(), pos), null, Items.RESPAWN_ANCHOR);
        }
    }

    private void handleRespawnBlock(ServerPlayer player, GlobalPos pos, String name, Item item) {
        BedTracker tracker = player.getData(Restful.BED_TRACKER);

        int existingIndex = -1;
        var beds = tracker.getBeds();
        for (int i = 0; i < beds.size(); i++) {
            if (beds.get(i).position().equals(pos)) {
                existingIndex = i;
                break;
            }
        }

        int maxPoints = Config.MAX_POINTS.get();

        if (existingIndex == -1 && tracker.size() < maxPoints) {
            boolean added = tracker.addBed(pos, name, item, maxPoints);

            if (added) {
                player.sendSystemMessage(Component.translatable("message.restful.added_point"));
            }
        } else if (existingIndex != -1) {
            player.sendSystemMessage(Component.translatable("message.restful.already_have_point"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.restful.reached_limit"));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        int selectedIndex = tracker.getSelectedBedIndex();

        if (selectedIndex < 0) {
            tracker.setSelectedBedIndex(-1);
            return;
        }

        BedData selectedBed = tracker.getBed(selectedIndex);
        if (selectedBed == null) {
            tracker.setSelectedBedIndex(-1);
            return;
        }

        boolean respawnedAtSelectedBed = player.level().dimension().equals(selectedBed.position().dimension())
                && player.blockPosition().closerThan(selectedBed.position().pos(), 10);

        if (!respawnedAtSelectedBed) {
            tracker.removeBed(selectedIndex);
            player.sendSystemMessage(Component.translatable(
                    "message.restful.point_was_destroyed", selectedBed.name()));
        }

        tracker.setSelectedBedIndex(-1);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        BedTracker oldTracker = oldPlayer.getData(Restful.BED_TRACKER);
        BedTracker newTracker = newPlayer.getData(Restful.BED_TRACKER);

        for (BedData bed : oldTracker.getBeds()) {
            newTracker.addBed(bed.position(), bed.name(), bed.bedItem(), 100);
        }
    }

}
