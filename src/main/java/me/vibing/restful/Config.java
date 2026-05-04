package me.vibing.restful;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_POINTS = BUILDER
            .comment("Maximum number of respawn points each player can have")
            .translation("config.restful.max_points")
            .defineInRange("maxPoints", 5, 0, 100);



    static final ModConfigSpec SPEC = BUILDER.build();
}
