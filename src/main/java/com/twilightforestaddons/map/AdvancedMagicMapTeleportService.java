package com.twilightforestaddons.map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData.MapCoord;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import twilightforest.TFFeature;
import twilightforest.TFMagicMapData;
import twilightforest.TwilightForestMod;
import twilightforest.item.ItemTFMagicMap;

public final class AdvancedMagicMapTeleportService {

    private AdvancedMagicMapTeleportService() {}

    public static void handleTeleportRequest(EntityPlayerMP player, PacketTeleportToBossFeature message) {
        if (player == null || message == null) {
            return;
        }

        World world = player.worldObj;
        if (world == null || world.provider.dimensionId != TwilightForestMod.dimensionID) {
            return;
        }

        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || held.getItem() != ModItems.advancedMagicMap || held.getItemDamage() != message.getMapId()) {
            return;
        }

        TFMagicMapData mapData = ItemTFMagicMap.getMPMapData(message.getMapId(), world);
        if (mapData == null) {
            return;
        }

        MapCoord target = findTargetFeature(mapData, message);
        if (target == null) {
            return;
        }

        if (Config.advancedMapEnforceProgression && !BossFeatureRegistry.isFeatureUnlocked(target.iconSize, player)) {
            player.addChatMessage(
                new ChatComponentTranslation(
                    "message.twilightforestaddons.teleport_blocked_progress",
                    BossFeatureRegistry.getFeatureName(target.iconSize)));
            return;
        }

        int scale = Math.max(0, mapData.scale);
        int blocksPerPixel = 1 << scale;
        int relativeX = target.centerX >> 1;
        int relativeZ = target.centerZ >> 1;
        int approxX = clampToWorldCoordinate((long) mapData.xCenter + (long) relativeX * blocksPerPixel);
        int approxZ = clampToWorldCoordinate((long) mapData.zCenter + (long) relativeZ * blocksPerPixel);

        // TFFeature#getNearestCenterXYZ expects chunk coordinates, not block coordinates.
        ChunkCoordinates nearestCenter = TFFeature.getNearestCenterXYZ(approxX >> 4, approxZ >> 4, world);
        int targetX = nearestCenter != null ? nearestCenter.posX : approxX;
        int targetZ = nearestCenter != null ? nearestCenter.posZ : approxZ;
        int[] safePos = SafeTeleport.findSafeTeleport(world, targetX, targetZ, player);
        int safeX = clampToWorldCoordinate(safePos[0]);
        int safeY = clampY(world, safePos[1]);
        int safeZ = clampToWorldCoordinate(safePos[2]);

        player.mountEntity(null);
        player.fallDistance = 0.0F;
        player.playerNetServerHandler
            .setPlayerLocation(safeX + 0.5D, safeY, safeZ + 0.5D, player.rotationYaw, player.rotationPitch);
        player
            .addChatMessage(new ChatComponentTranslation("message.twilightforestaddons.teleport", safeX, safeY, safeZ));
    }

    private static MapCoord findTargetFeature(TFMagicMapData mapData, PacketTeleportToBossFeature message) {
        for (MapCoord coord : AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData)) {
            if (!BossFeatureRegistry.isBossFeature(coord.iconSize)) {
                continue;
            }
            if (coord.iconSize == message.getFeatureId() && coord.centerX == message.getCenterX()
                && coord.centerZ == message.getCenterZ()) {
                return coord;
            }
        }
        return null;
    }

    private static int clampToWorldCoordinate(long value) {
        if (value > 29999984L) {
            return 29999984;
        }
        if (value < -29999984L) {
            return -29999984;
        }
        return (int) value;
    }

    private static int clampY(World world, int y) {
        if (world == null) {
            return 64;
        }
        int maxY = Math.max(2, world.getActualHeight() - 2);
        if (y < 2) {
            return 2;
        }
        if (y > maxY) {
            return maxY;
        }
        return y;
    }
}
