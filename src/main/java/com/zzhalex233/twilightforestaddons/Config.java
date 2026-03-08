package com.zzhalex233.twilightforestaddons;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class Config {
    public static int advancedMapRecenterChunks = 1;
    public static boolean advancedMapEnforceProgression = true;
    public static boolean advancedMapAutoRefreshAfterTeleport = true;

    private Config() {
    }

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        advancedMapRecenterChunks = configuration.getInt(
            "advancedMapRecenterChunks",
            Configuration.CATEGORY_GENERAL,
            advancedMapRecenterChunks,
            1,
            64,
            "Recenter the Advanced Magic Map when the player is farther than this many chunks from map center."
        );
        advancedMapEnforceProgression = configuration.getBoolean(
            "advancedMapEnforceProgression",
            Configuration.CATEGORY_GENERAL,
            advancedMapEnforceProgression,
            "If true, Advanced Magic Map teleportation respects Twilight Forest progression requirements."
        );
        advancedMapAutoRefreshAfterTeleport = configuration.getBoolean(
            "advancedMapAutoRefreshAfterTeleport",
            Configuration.CATEGORY_GENERAL,
            advancedMapAutoRefreshAfterTeleport,
            "If true, teleporting with the Advanced Magic Map schedules one automatic map refresh shortly after arrival."
        );

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}