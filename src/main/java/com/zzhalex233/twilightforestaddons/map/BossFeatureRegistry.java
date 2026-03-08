package com.zzhalex233.twilightforestaddons.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import twilightforest.TFFeature;

public final class BossFeatureRegistry {
    private static final Set<Integer> BOSS_FEATURE_IDS = new HashSet<>(Arrays.asList(
        TFFeature.NAGA_COURTYARD.ordinal(),
        TFFeature.LICH_TOWER.ordinal(),
        TFFeature.HYDRA_LAIR.ordinal(),
        TFFeature.LABYRINTH.ordinal(),
        TFFeature.DARK_TOWER.ordinal(),
        TFFeature.KNIGHT_STRONGHOLD.ordinal(),
        TFFeature.YETI_CAVE.ordinal(),
        TFFeature.ICE_TOWER.ordinal(),
        TFFeature.TROLL_CAVE.ordinal(),
        TFFeature.FINAL_CASTLE.ordinal()
    ));

    private BossFeatureRegistry() {
    }

    public static boolean isBossFeature(int featureId) {
        return BOSS_FEATURE_IDS.contains(featureId);
    }

    public static boolean isFeatureUnlocked(int featureId, EntityPlayer player) {
        TFFeature feature = TFFeature.getFeatureByID(featureId);
        return feature == null || player == null || feature.doesPlayerHaveRequiredAdvancements(player);
    }

    public static String getFeatureName(int featureId) {
        TFFeature feature = TFFeature.getFeatureByID(featureId);
        if (feature == null || feature.name == null || feature.name.isEmpty()) {
            return "Unknown";
        }

        String translationKey = "feature.twilightforestaddons." + feature.name;
        String localized = I18n.format(translationKey);
        if (!translationKey.equals(localized)) {
            return localized;
        }
        return feature.name;
    }
}