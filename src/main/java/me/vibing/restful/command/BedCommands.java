package me.vibing.restful.command;

import me.vibing.restful.BedData;
import me.vibing.restful.BedIdUtil;
import me.vibing.restful.BedTracker;
import me.vibing.restful.Config;
import me.vibing.restful.Restful;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class BedCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("restful")
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("list")
                    .executes(context -> listBeds(context.getSource())))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> removeBed(
                            context.getSource(),
                            StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("rename")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(context -> renameBed(
                                context.getSource(),
                                StringArgumentType.getString(context, "id"),
                                StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("favorite")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> toggleFavorite(
                            context.getSource(),
                            StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("clear")
                    .executes(context -> clearBeds(context.getSource())))
        );
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.restful.help.title"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.commands_header"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.cmd_list"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.cmd_remove"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.cmd_rename"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.cmd_clear"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.favorite"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.ids_header"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.ids_desc1"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.ids_desc2"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.shortcuts"), false);
        source.sendSuccess(() -> Component.translatable("command.restful.help.max", Config.MAX_POINTS.get()), false);
        
        return 1;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.restful.usage"));
            return null;
        }
        return player;
    }

    private static int listBeds(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        tracker.pruneInvalid(player);
        var beds = tracker.getBeds();

        if (beds.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.restful.list.none"), false);
            source.sendSuccess(() -> Component.translatable("command.restful.list.empty_hint"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.restful.list.header"), false);

        for (int i = 0; i < beds.size(); i++) {
            BedData bed = beds.get(i);
            final String id = BedIdUtil.generateId(bed.position());
            final String dim = bed.dimensionDisplay();
            final int x = bed.position().pos().getX();
            final int y = bed.position().pos().getY();
            final int z = bed.position().pos().getZ();

            String name = bed.name();
            boolean hasCustomName = !name.equals(String.format("%d, %d, %d", x, y, z));

            String coordColor = getDimensionColor(dim);
            String coords = coordColor + x + " " + y + " " + z;
            
            MutableComponent line;
            if (hasCustomName) {
                line = Component.literal(" " + "§e" + name + " " + coords + " §8" + id);
            } else {
                line = Component.literal(" " + coords + " §8" + id);
            }
            
            source.sendSuccess(() -> line, false);
        }

        final int count = beds.size();
        final int max = Config.MAX_POINTS.get();
        source.sendSuccess(() -> Component.translatable("command.restful.list.footer", count, max), false);

        return beds.size();
    }

    private static String getDimensionColor(String dim) {
        return switch (dim) {
            case "Overworld" -> "§b";
            case "Nether" -> "§c";
            case "End" -> "§d";
            default -> "§f";
        };
    }

    private static int removeBed(CommandSourceStack source, String idOrIndex) {
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        
        int index = findBedIndex(tracker, idOrIndex);
        if (index < 0) {
            source.sendFailure(Component.translatable("command.restful.remove.invalid", idOrIndex));
            return 0;
        }

        BedData removed = tracker.getBed(index);
        tracker.removeBed(index);

        source.sendSuccess(() -> Component.translatable("command.restful.remove.success", BedIdUtil.generateId(removed.position()), removed.name()), true);

        return 1;
    }

    private static int renameBed(CommandSourceStack source, String idOrIndex, String newName) {
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        
        int index = findBedIndex(tracker, idOrIndex);
        if (index < 0) {
            source.sendFailure(Component.translatable("command.restful.rename.invalid", idOrIndex));
            return 0;
        }

        String trimmedName = newName.length() > 32 ? newName.substring(0, 32) : newName;

        BedData bed = tracker.getBed(index);
        
        boolean renamed = tracker.renameBed(index, trimmedName);

        if (!renamed) {
            source.sendFailure(Component.translatable("command.restful.rename.failed"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.restful.rename.success", BedIdUtil.generateId(bed.position()), trimmedName), true);

        return 1;
    }

    private static int toggleFavorite(CommandSourceStack source, String idOrIndex) {
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        
        int index = findBedIndex(tracker, idOrIndex);
        if (index < 0) {
            source.sendFailure(Component.translatable("command.restful.rename.invalid", idOrIndex));
            return 0;
        }

        BedData bed = tracker.getBed(index);

        boolean isFavorite = tracker.isFavorite(index);
        tracker.setFavorite(index, !isFavorite);

        if (!isFavorite) {
            source.sendSuccess(() -> Component.translatable("command.restful.favorite.success", BedIdUtil.generateId(bed.position())), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.restful.favorite.removed", BedIdUtil.generateId(bed.position())), true);
        }

        return 1;
    }

    private static int clearBeds(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        BedTracker tracker = player.getData(Restful.BED_TRACKER);
        int count = tracker.size();

        tracker.clear();

        source.sendSuccess(() -> Component.translatable("command.restful.clear.success", count), true);

        return count;
    }

    private static int findBedIndex(BedTracker tracker, String idOrIndex) {
        // try hex ID first
        int idx = BedIdUtil.findBedById(tracker, idOrIndex);
        if (idx >= 0) return idx;
        
        // try numeric index
        try {
            int index = Integer.parseInt(idOrIndex) - 1;
            if (index >= 0 && index < tracker.size()) {
                return index;
            }
        } catch (NumberFormatException ignored) {
        }
        
        return -1;
    }
}
