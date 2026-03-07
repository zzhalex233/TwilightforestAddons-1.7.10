package com.twilightforestaddons.map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.item.ItemAdvancedMagicMap;
import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketFullAdvancedMapRefresh;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;

import twilightforest.TFMagicMapData;

public final class AdvancedMagicMapRefreshService {

    private static final int MANUAL_REFRESH_PASSES = 16;

    private AdvancedMagicMapRefreshService() {}

    public static void handleRefreshRequest(EntityPlayerMP player, int mapId) {
        if (player == null) {
            return;
        }

        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || held.getItem() != ModItems.advancedMagicMap || held.getItemDamage() != mapId) {
            return;
        }

        World world = player.worldObj;
        if (world == null) {
            return;
        }

        ItemAdvancedMagicMap item = (ItemAdvancedMagicMap) held.getItem();
        TFMagicMapData mapData = item.getMapData(held, world);
        if (mapData == null) {
            return;
        }

        thisRecenterAroundPlayer(held, world, player, mapData);
        mapData.updateVisiblePlayers(player, held);
        mapData.checkExistingFeatures(world);
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);

        for (int i = 0; i < MANUAL_REFRESH_PASSES; i++) {
            item.updateMapData(world, player, mapData);
        }

        for (int x = 0; x < 128; x++) {
            mapData.setColumnDirty(x, 0, 127);
        }
        mapData.markDirty();

        ModNetwork.CHANNEL.sendTo(
            new PacketSyncAdvancedMapCenter(
                held.getItemDamage(),
                mapData.xCenter,
                mapData.zCenter,
                mapData.dimension,
                mapData.scale),
            player);
        ModNetwork.CHANNEL.sendTo(new PacketFullAdvancedMapRefresh(held.getItemDamage(), mapData), player);
    }

    private static void thisRecenterAroundPlayer(ItemStack itemStack, World world, EntityPlayerMP player,
        TFMagicMapData mapData) {
        int blocksPerPixel = 1 << mapData.scale;
        int thresholdBlocks = Math.max(blocksPerPixel, Config.advancedMapRecenterChunks * 16);
        int playerX = MathHelper.floor_double(player.posX);
        int playerZ = MathHelper.floor_double(player.posZ);

        if (Math.abs(playerX - mapData.xCenter) < thresholdBlocks
            && Math.abs(playerZ - mapData.zCenter) < thresholdBlocks) {
            return;
        }

        int desiredCenterX = snapToPixel(player.posX, blocksPerPixel);
        int desiredCenterZ = snapToPixel(player.posZ, blocksPerPixel);
        int shiftXPixels = (desiredCenterX - mapData.xCenter) >> mapData.scale;
        int shiftZPixels = (desiredCenterZ - mapData.zCenter) >> mapData.scale;
        if (shiftXPixels == 0 && shiftZPixels == 0) {
            return;
        }

        AdvancedMagicMapDataUtils.shiftMapContent(mapData, shiftXPixels, shiftZPixels, true);
        mapData.xCenter = desiredCenterX;
        mapData.zCenter = desiredCenterZ;
        mapData.dimension = world.provider.dimensionId;
        mapData.markDirty();
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);

        if (itemStack.getTagCompound() != null) {
            itemStack.getTagCompound()
                .setInteger("advCenterX", mapData.xCenter);
            itemStack.getTagCompound()
                .setInteger("advCenterZ", mapData.zCenter);
            itemStack.getTagCompound()
                .setInteger("advDimension", mapData.dimension);
            itemStack.getTagCompound()
                .setByte("advScale", mapData.scale);
        }
    }

    private static int snapToPixel(double coord, int blocksPerPixel) {
        return MathHelper.floor_double(coord / blocksPerPixel) * blocksPerPixel;
    }
}
