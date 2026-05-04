package me.vibing.restful;

import net.minecraft.core.GlobalPos;

public class BedIdUtil {
    
    public static String generateId(GlobalPos pos) {
        int hash = pos.hashCode();
        int unsignedHash = hash & 0xFFFF;
        return String.format("%04x", unsignedHash);
    }

    public static int findBedById(BedTracker tracker, String id) {
        var beds = tracker.getBeds();
        for (int i = 0; i < beds.size(); i++) {
            String bedId = generateId(beds.get(i).position());
            if (bedId.equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }
    
}
