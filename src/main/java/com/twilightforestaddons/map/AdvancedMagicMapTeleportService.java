package com.twilightforestaddons.map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData.MapCoord;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.item.ItemAdvancedMagicMap;
import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import twilightforest.TFMagicMapData;
import twilightforest.TFFeature;
import twilightforest.TwilightForestMod;

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

        TFMagicMapData mapData = twilightforest.item.ItemTFMagicMap.getMPMapData(message.getMapId(), world);
        if (mapData == null) {
            return;
        }

        MapCoord targetCoord = findTargetCoord(mapData, message.getFeatureId(), message.getMapCoordX(), message.getMapCoordZ());
        if (targetCoord == null) {
            return;
        }

        byte featureId = targetCoord.iconSize;

        if (Config.advancedMapEnforceProgression && !BossFeatureRegistry.isFeatureUnlocked(featureId, player)) {
            player.addChatMessage(
                new ChatComponentTranslation(
                    "message.twilightforestaddons.teleport_blocked_progress",
                    BossFeatureRegistry.getFeatureName(featureId)));
            return;
        }

        int approxX = mapData.xCenter + MathHelper.floor_double(targetCoord.centerX * (1 << mapData.scale) / 2.0D);
        int approxZ = mapData.zCenter + MathHelper.floor_double(targetCoord.centerZ * (1 << mapData.scale) / 2.0D);
        ChunkCoordinates nearestCenter = TFFeature.getNearestCenterXYZ(approxX >> 4, approxZ >> 4, world);
        int targetX = nearestCenter != null ? nearestCenter.posX : approxX;
        int targetZ = nearestCenter != null ? nearestCenter.posZ : approxZ;

        SafeTeleport.Result safePos = SafeTeleport.findSafeTeleportResult(world, targetX, targetZ, player);
        int safeX = clampToWorldCoordinate(safePos.x);
        int safeY = clampY(world, safePos.y);
        int safeZ = clampToWorldCoordinate(safePos.z);

        player.mountEntity(null);
        player.fallDistance = 0.0F;
        player.playerNetServerHandler
            .setPlayerLocation(safeX + 0.5D, safeY, safeZ + 0.5D, player.rotationYaw, player.rotationPitch);
        SafeTeleport.schedulePostTeleportCorrection(player);
        if (Config.advancedMapAutoRefreshAfterTeleport && held.getItem() instanceof ItemAdvancedMagicMap) {
            ((ItemAdvancedMagicMap) held.getItem()).scheduleAutoRefresh(held);
        }
        player.addChatMessage(new ChatComponentTranslation("message.twilightforestaddons.teleport", safeX, safeY, safeZ));
    }

    private static MapCoord findTargetCoord(TFMagicMapData mapData, byte featureId, byte mapCoordX, byte mapCoordZ) {
        MapCoord exact = null;
        MapCoord nearest = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (MapCoord coord : AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData)) {
            if (coord.iconSize != featureId) {
                continue;
            }

            if (coord.centerX == mapCoordX && coord.centerZ == mapCoordZ) {
                exact = coord;
                break;
            }

            int dx = coord.centerX - mapCoordX;
            int dz = coord.centerZ - mapCoordZ;
            int distanceSq = dx * dx + dz * dz;
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = coord;
            }
        }

        return exact != null ? exact : nearestDistanceSq <= 36 ? nearest : null;
    }

    private static int clampToWorldCoordinate(int value) {
        if (value > 29999984) {
            return 29999984;
        }
        if (value < -29999984) {
            return -29999984;
        }
        return value;
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
