package com.zzhalex233.twilightforestaddons.map;

import com.zzhalex233.twilightforestaddons.Config;
import com.zzhalex233.twilightforestaddons.item.ItemAdvancedMagicMap;
import com.zzhalex233.twilightforestaddons.item.ModItems;
import com.zzhalex233.twilightforestaddons.network.packet.PacketTeleportToBossFeature;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import twilightforest.TFFeature;
import twilightforest.TFMagicMapData;
import twilightforest.TwilightForestMod;

public final class AdvancedMagicMapTeleportService {
    private AdvancedMagicMapTeleportService() {
    }

    public static void handleTeleportRequest(EntityPlayerMP player, PacketTeleportToBossFeature message) {
        if (player == null || message == null) {
            return;
        }

        World world = player.getServerWorld();
        if (world == null) {
            return;
        }
        if (world.provider.getDimension() != TwilightForestMod.dimType.getId()) {
            player.sendStatusMessage(new TextComponentTranslation("message.twilightforestaddons.teleport_failed_dimension"), false);
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || held.getItem() != ModItems.ADVANCED_MAGIC_MAP || held.getMetadata() != message.getMapId()) {
            player.sendStatusMessage(new TextComponentTranslation("message.twilightforestaddons.teleport_failed_map"), false);
            return;
        }

        TFMagicMapData mapData = ((ItemAdvancedMagicMap) held.getItem()).getMapData(held, world);
        if (mapData == null) {
            player.sendStatusMessage(new TextComponentTranslation("message.twilightforestaddons.teleport_failed_target"), false);
            return;
        }

        TFMagicMapData.TFMapDecoration target = findTargetDecoration(mapData, message);
        int featureId = message.getFeatureId();
        if (target != null) {
            featureId = AdvancedMagicMapDataUtils.getFeatureId(target);
        }

        if (Config.advancedMapEnforceProgression && !BossFeatureRegistry.isFeatureUnlocked(featureId, player)) {
            player.sendStatusMessage(new TextComponentTranslation("message.twilightforestaddons.teleport_blocked_progress", BossFeatureRegistry.getFeatureName(featureId)), false);
            return;
        }

        int targetX = message.getApproxWorldX();
        int targetZ = message.getApproxWorldZ();
        if (target != null) {
            int blocksPerPixel = 1 << mapData.scale;
            targetX = mapData.xCenter + Math.round(target.getX() * blocksPerPixel / 2.0F);
            targetZ = mapData.zCenter + Math.round(target.getY() * blocksPerPixel / 2.0F);
        }

        BlockPos nearestCenter = TFFeature.getNearestCenterXYZ(targetX >> 4, targetZ >> 4, world);
        if (nearestCenter != null) {
            targetX = nearestCenter.getX();
            targetZ = nearestCenter.getZ();
        }

        SafeTeleport.Result safe = SafeTeleport.findSafeTeleportResult(world, targetX, targetZ, player);
        player.dismountRidingEntity();
        player.fallDistance = 0.0F;
        player.connection.setPlayerLocation(safe.x + 0.5D, safe.y, safe.z + 0.5D, player.rotationYaw, player.rotationPitch);
        player.sendStatusMessage(new TextComponentTranslation("message.twilightforestaddons.teleport", safe.x, safe.y, safe.z), false);
    }

    private static TFMagicMapData.TFMapDecoration findTargetDecoration(TFMagicMapData mapData, PacketTeleportToBossFeature message) {
        TFMagicMapData.TFMapDecoration nearest = null;
        int nearestDistanceSq = Integer.MAX_VALUE;

        for (TFMagicMapData.TFMapDecoration decoration : mapData.tfDecorations) {
            if (decoration == null) {
                continue;
            }

            int featureId = AdvancedMagicMapDataUtils.getFeatureId(decoration);
            if (featureId != message.getFeatureId()) {
                continue;
            }

            int dx = decoration.getX() - message.getMapCoordX();
            int dz = decoration.getY() - message.getMapCoordZ();
            int distanceSq = dx * dx + dz * dz;
            if (dx == 0 && dz == 0) {
                return decoration;
            }
            if (distanceSq < nearestDistanceSq) {
                nearest = decoration;
                nearestDistanceSq = distanceSq;
            }
        }

        return nearestDistanceSq <= 144 ? nearest : null;
    }
}