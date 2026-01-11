package io.github.itskillerluc.familiarfaces.server.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder commonConfigBuilder = new ForgeConfigSpec.Builder();
        Common.setupCommonConfig(commonConfigBuilder);
        COMMON_SPEC = commonConfigBuilder.build();
    }

    public static class Common {
        public static ForgeConfigSpec.IntValue brushingCooldown;

        private static void setupCommonConfig(ForgeConfigSpec.Builder builder) {
            brushingCooldown = builder.comment("The cooldown for brushing armadillos in seconds.")
                    .defineInRange("config.familiar_faces.armadillo_brush_cooldown", 5, 0, Integer.MAX_VALUE);
        }
    }
}