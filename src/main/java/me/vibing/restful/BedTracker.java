package me.vibing.restful;

import me.vibing.restful.network.BedInfo;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BedTracker {
    private final List<BedData> beds = new ArrayList<>();
    private int selectedBedIndex = -1;

    public List<BedData> getBeds() {
        return Collections.unmodifiableList(beds);
    }

    public int size() {
        return beds.size();
    }

    public boolean isEmpty() {
        return beds.isEmpty();
    }

    public void clear() {
        beds.clear();
    }

    public int getSelectedBedIndex() {
        return selectedBedIndex;
    }

    public void setSelectedBedIndex(int index) {
        this.selectedBedIndex = index;
    }

    public boolean isFavorite(int index) {
        BedData bed = getBed(index);
        return bed != null && bed.favorite();
    }

    public void setFavorite(int index, boolean favorite) {
        BedData old = getBed(index);
        if (old == null) return;
        beds.set(index, new BedData(old.position(), old.name(), old.bedItem(), favorite));
    }

    public boolean addBed(GlobalPos pos, @Nullable String name, Item bedItem, int maxBeds) {
        for (BedData existing : beds) {
            if (existing.position().equals(pos)) {
                return false;
            }
        }

        if (beds.size() >= maxBeds) {
            return false;
        }

        beds.add(new BedData(pos, name, bedItem, false));
        return true;
    }

    public boolean renameBed(int index, String newName) {
        if (index < 0 || index >= beds.size()) {
            return false;
        }
        BedData oldBed = beds.get(index);
        BedData renamedBed = new BedData(oldBed.position(), newName, oldBed.bedItem(), oldBed.favorite());
        beds.set(index, renamedBed);
        return true;
    }

    public boolean removeBed(int index) {
        if (index < 0 || index >= beds.size()) {
            return false;
        }
        beds.remove(index);
        return true;
    }

    public boolean swapBeds(int indexA, int indexB) {
        if (indexA < 0 || indexA >= beds.size() || indexB < 0 || indexB >= beds.size()) {
            return false;
        }
        Collections.swap(beds, indexA, indexB);
        return true;
    }

    @Nullable
    public BedData getBed(int index) {
        if (index < 0 || index >= beds.size()) {
            return null;
        }
        return beds.get(index);
    }

    public int pruneInvalid(Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return 0;
        }

        int removed = 0;
        for (int i = beds.size() - 1; i >= 0; i--) {
            BedData bed = beds.get(i);

            var targetLevel = serverLevel.getServer().getLevel(bed.position().dimension());
            if (targetLevel == null) {
                Restful.LOGGER.debug("Pruned bed at {} - dimension not found", bed.position());
                beds.remove(i);
                removed++;
                continue;
            }

            if (!targetLevel.isLoaded(bed.position().pos())) {
                continue;
            }

            boolean valid = BedValidator.isValidRespawnPoint(player, bed.position());
            if (!valid) {
                Restful.LOGGER.debug("Pruned invalid bed at {}", bed.position());
                beds.remove(i);
                removed++;
            }
        }
        return removed;
    }

    @Nullable
    public BedData findBestValidBed(Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        for (int i = 0; i < beds.size(); i++) {
            int index = beds.size() - 1 - i;
            if (isFavorite(index)) {
                BedData bed = beds.get(index);
                var targetLevel = serverLevel.getServer().getLevel(bed.position().dimension());
                if (targetLevel == null) continue;
                if (!targetLevel.isLoaded(bed.position().pos())) continue;
                if (BedValidator.isValidRespawnPoint(player, bed.position())) {
                    return bed;
                }
            }
        }

        for (int i = beds.size() - 1; i >= 0; i--) {
            if (isFavorite(i)) continue;

            BedData bed = beds.get(i);
            var targetLevel = serverLevel.getServer().getLevel(bed.position().dimension());
            if (targetLevel == null) continue;
            if (!targetLevel.isLoaded(bed.position().pos())) continue;
            if (BedValidator.isValidRespawnPoint(player, bed.position())) {
                return bed;
            }
        }
        return null;
    }

    @Nullable
    public BedData getRespawnBed(Player player) {
        if (selectedBedIndex >= 0 && selectedBedIndex < beds.size()) {
            BedData selected = beds.get(selectedBedIndex);
            if (BedValidator.isValidRespawnPoint(player, selected.position())) {
                return selected;
            }
        }
        return findBestValidBed(player);
    }

    public List<BedInfo> toBedInfoList() {
        List<BedInfo> result = new ArrayList<>();
        for (int i = 0; i < beds.size(); i++) {
            BedData bed = beds.get(i);
            result.add(BedInfo.fromBedData(bed, i, isFavorite(i)));
        }
        return result;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BedData bed : beds) {
            CompoundTag bedTag = bed.toTag();
            if (bedTag != null) {
                list.add(bedTag);
            }
        }
        tag.put("beds", list);
        tag.putInt("selected", selectedBedIndex);
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        beds.clear();
        if (tag.contains("beds")) {
            ListTag list = tag.getList("beds", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag bedTag = list.getCompound(i);
                BedData bed = BedData.fromTag(bedTag);
                if (bed != null) {
                    beds.add(bed);
                }
            }
        }
        selectedBedIndex = tag.getInt("selected");
    }

    public static Item getBedItemFromState(BlockState state) {
        if (state.getBlock() instanceof BedBlock bed) {
            return bed.asItem();
        }
        return Items.RED_BED;
    }
}
