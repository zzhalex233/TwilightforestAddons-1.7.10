package com.twilightforestaddons;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Twilight Forest Addons loaded.";
    public static int advancedMapRecenterChunks = 3;
    public static boolean advancedMapEnforceProgression = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");
        advancedMapRecenterChunks = configuration.getInt(
            "advancedMapRecenterChunks",
            Configuration.CATEGORY_GENERAL,
            advancedMapRecenterChunks,
            1,
            64,
            "Recenter the Advanced Magic Map when player is farther than this many chunks from map center.");
        advancedMapEnforceProgression = configuration.getBoolean(
            "advancedMapEnforceProgression",
            Configuration.CATEGORY_GENERAL,
            advancedMapEnforceProgression,
            "If true, Advanced Magic Map teleportation respects Twilight Forest progression requirements.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
