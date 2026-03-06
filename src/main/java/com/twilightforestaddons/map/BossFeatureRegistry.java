package com.twilightforestaddons.map;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatisticsFile;

import twilightforest.TFAchievementPage;
import twilightforest.TFFeature;

public final class BossFeatureRegistry {

    private static final Set<Byte> BOSS_FEATURE_IDS = new HashSet<Byte>();
    private static final Map<Byte, Achievement> REQUIRED_ACHIEVEMENTS = new HashMap<Byte, Achievement>();
    private static final Map<Byte, Achievement> COMPLETION_ACHIEVEMENTS = new HashMap<Byte, Achievement>();

    static {
        addFeature(TFFeature.nagaCourtyard, null, TFAchievementPage.twilightKillNaga);
        addFeature(TFFeature.lichTower, TFAchievementPage.twilightKillNaga, TFAchievementPage.twilightKillLich);
        addFeature(TFFeature.hydraLair, TFAchievementPage.twilightProgressLabyrinth, TFAchievementPage.twilightKillHydra);
        addFeature(TFFeature.labyrinth, TFAchievementPage.twilightKillLich, TFAchievementPage.twilightProgressLabyrinth);
        addFeature(TFFeature.darkTower, TFAchievementPage.twilightProgressKnights, TFAchievementPage.twilightProgressUrghast);
        addFeature(
            TFFeature.tfStronghold,
            TFAchievementPage.twilightProgressTrophyPedestal,
            TFAchievementPage.twilightProgressKnights);
        addFeature(TFFeature.yetiCave, TFAchievementPage.twilightProgressUrghast, TFAchievementPage.twilightProgressYeti);
        addFeature(TFFeature.iceTower, TFAchievementPage.twilightProgressYeti, TFAchievementPage.twilightProgressGlacier);
        addFeature(TFFeature.trollCave, TFAchievementPage.twilightProgressGlacier, TFAchievementPage.twilightProgressTroll);
        addFeature(TFFeature.finalCastle, null, TFAchievementPage.twilightProgressCastle);
    }

    private BossFeatureRegistry() {}

    private static void addFeature(TFFeature feature, Achievement requiredAchievement, Achievement completionAchievement) {
        if (feature != null) {
            byte featureId = (byte) feature.featureID;
            BOSS_FEATURE_IDS.add(featureId);
            if (requiredAchievement != null) {
                REQUIRED_ACHIEVEMENTS.put(featureId, requiredAchievement);
            }
            if (completionAchievement != null) {
                COMPLETION_ACHIEVEMENTS.put(featureId, completionAchievement);
            }
        }
    }

    public static boolean isBossFeature(byte featureId) {
        return BOSS_FEATURE_IDS.contains(featureId);
    }

    public static TFFeature getFeature(byte featureId) {
        int index = featureId & 0xFF;
        if (index < 0 || index >= TFFeature.featureList.length) {
            return null;
        }
        return TFFeature.featureList[index];
    }

    public static boolean isFeatureUnlocked(byte featureId, EntityPlayer player) {
        Achievement requiredAchievement = REQUIRED_ACHIEVEMENTS.get(featureId);
        return requiredAchievement == null || hasAchievement(player, requiredAchievement);
    }

    public static boolean isBossDefeated(byte featureId, EntityPlayer player) {
        Achievement completionAchievement = COMPLETION_ACHIEVEMENTS.get(featureId);
        return completionAchievement != null && hasAchievement(player, completionAchievement);
    }

    public static String getFeatureName(byte featureId) {
        TFFeature feature = getFeature(featureId);
        if (feature == null || feature.name == null || feature.name.isEmpty()) {
            return "Unknown";
        }
        return feature.name;
    }

    private static boolean hasAchievement(EntityPlayer player, Achievement achievement) {
        if (player == null || achievement == null) {
            return false;
        }

        if (player instanceof EntityPlayerMP) {
            StatisticsFile stats = ((EntityPlayerMP) player).func_147099_x();
            return stats != null && stats.hasAchievementUnlocked(achievement);
        }

        try {
            Method getStatFileWriter = player.getClass()
                .getMethod("getStatFileWriter");
            Object stats = getStatFileWriter.invoke(player);
            return stats instanceof StatFileWriter && ((StatFileWriter) stats).hasAchievementUnlocked(achievement);
        } catch (Exception ignored) {
            return false;
        }
    }
}
