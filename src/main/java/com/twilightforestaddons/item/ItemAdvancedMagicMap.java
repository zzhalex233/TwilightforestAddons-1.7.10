package com.twilightforestaddons.item;

import java.util.Arrays;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.TwilightForestAddons;
import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.twilightforestaddons.map.AdvancedMagicMapRefreshService;
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;
import twilightforest.item.ItemTFMagicMap;

public class ItemAdvancedMagicMap extends ItemTFMagicMap {

    private static final String TAG_LAST_PLAYER_X = "advLastPlayerX";
    private static final String TAG_LAST_PLAYER_Z = "advLastPlayerZ";
    private static final String TAG_PENDING_REFRESH_PASSES = "advPendingRefreshPasses";
    private static final String TAG_DELAYED_REFRESH_PASSES = "advDelayedRefreshPasses";
    private static final String TAG_DELAYED_REFRESH_TICKS = "advDelayedRefreshTicks";
    private static final String TAG_AUTO_REFRESH_TICKS = "advAutoRefreshTicks";
    private static final String TAG_AUTO_REFRESH_WARMUP_TICKS = "advAutoRefreshWarmupTicks";

    private static final int RECENTER_NONE = 0;
    private static final int RECENTER_SHIFT = 1;
    private static final int RECENTER_CLEAR = 2;

    private static final int TELEPORT_DETECT_BLOCKS = 96;
    private static final int MAX_REUSE_SHIFT_PIXELS = 96;
    private static final int MAX_PENDING_REFRESH_PASSES = 12;
    private static final int MAX_EXTRA_PASSES_PER_TICK = 3;
    private static final int AUTO_REFRESH_DELAY_TICKS = 8;
    private static final int AUTO_REFRESH_CHUNK_RADIUS = 1;
    private static final int AUTO_REFRESH_CHUNK_LOADS_PER_TICK = 2;
    private static final int AUTO_REFRESH_MAX_WARMUP_TICKS = 40;

    public ItemAdvancedMagicMap() {
        this.maxStackSize = 1;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            TwilightForestAddons.proxy.toggleAdvancedMagicMapGui(player, itemStack);
        }
        return itemStack;
    }

    @Override
    public void onUpdate(ItemStack itemStack, World world, Entity entity, int slot, boolean isHeld) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        TFMagicMapData mapData = this.getMapData(itemStack, world);
        if (mapData == null) {
            return;
        }

        mapData.updateVisiblePlayers(player, itemStack);
        MovementProfile movement = this.trackMovement(itemStack, player);

        if (!isHeld) {
            this.syncCenterIfNeeded(itemStack, mapData, player);
            return;
        }

        if (this.tryRunAutoRefresh(itemStack, world, player)) {
            return;
        }

        this.tickDelayedRefresh(itemStack);
        int recenterMode = this.recenterIfNeeded(world, player, mapData);
        this.scheduleRecoveryIfNeeded(itemStack, movement, recenterMode);

        this.updateMapData(world, entity, mapData);
        int extraPasses = this.consumePendingRefreshPasses(itemStack);
        for (int i = 0; i < extraPasses; i++) {
            this.updateMapData(world, entity, mapData);
        }
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
        this.syncCenterIfNeeded(itemStack, mapData, player);
    }

    public void scheduleAutoRefresh(ItemStack itemStack) {
        if (itemStack == null || !Config.advancedMapAutoRefreshAfterTeleport) {
            return;
        }

        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        tag.setInteger(TAG_AUTO_REFRESH_TICKS, Math.max(tag.getInteger(TAG_AUTO_REFRESH_TICKS), AUTO_REFRESH_DELAY_TICKS));
        tag.setInteger(TAG_AUTO_REFRESH_WARMUP_TICKS, 0);
    }

    private int recenterIfNeeded(World world, EntityPlayer player, TFMagicMapData mapData) {
        int blocksPerPixel = 1 << mapData.scale;
        int thresholdBlocks = Math.max(blocksPerPixel, Config.advancedMapRecenterChunks * 16);
        int playerX = MathHelper.floor_double(player.posX);
        int playerZ = MathHelper.floor_double(player.posZ);

        if (Math.abs(playerX - mapData.xCenter) < thresholdBlocks
            && Math.abs(playerZ - mapData.zCenter) < thresholdBlocks) {
            return RECENTER_NONE;
        }

        int desiredCenterX = snapToPixel(player.posX, blocksPerPixel);
        int desiredCenterZ = snapToPixel(player.posZ, blocksPerPixel);
        int shiftXPixels = (desiredCenterX - mapData.xCenter) >> mapData.scale;
        int shiftZPixels = (desiredCenterZ - mapData.zCenter) >> mapData.scale;
        if (shiftXPixels == 0 && shiftZPixels == 0) {
            return RECENTER_NONE;
        }

        int shiftDistancePixels = Math.max(Math.abs(shiftXPixels), Math.abs(shiftZPixels));
        int recenterMode = shiftDistancePixels > MAX_REUSE_SHIFT_PIXELS ? RECENTER_CLEAR : RECENTER_SHIFT;
        if (recenterMode == RECENTER_CLEAR) {
            this.clearMapData(mapData);
        } else {
            AdvancedMagicMapDataUtils.shiftMapContent(mapData, shiftXPixels, shiftZPixels, true);
        }

        mapData.xCenter = desiredCenterX;
        mapData.zCenter = desiredCenterZ;
        mapData.dimension = world.provider.dimensionId;
        mapData.markDirty();
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
        return recenterMode;
    }

    private void clearMapData(TFMagicMapData mapData) {
        Arrays.fill(mapData.colors, (byte) 0);
        mapData.featuresVisibleOnMap.clear();
        for (int x = 0; x < 128; x++) {
            mapData.setColumnDirty(x, 0, 127);
        }
    }

    private void syncCenterIfNeeded(ItemStack itemStack, TFMagicMapData mapData, EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP) || !this.shouldSyncCenter(itemStack, mapData)) {
            return;
        }

        ModNetwork.CHANNEL.sendTo(
            new PacketSyncAdvancedMapCenter(
                itemStack.getItemDamage(),
                mapData.xCenter,
                mapData.zCenter,
                mapData.dimension,
                mapData.scale),
            (EntityPlayerMP) player);
        this.writeCenterToStack(itemStack, mapData);
    }

    private boolean shouldSyncCenter(ItemStack itemStack, TFMagicMapData mapData) {
        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        return tag.getInteger("advCenterX") != mapData.xCenter || tag.getInteger("advCenterZ") != mapData.zCenter
            || tag.getInteger("advDimension") != mapData.dimension
            || tag.getByte("advScale") != mapData.scale;
    }

    private void writeCenterToStack(ItemStack itemStack, TFMagicMapData mapData) {
        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        tag.setInteger("advCenterX", mapData.xCenter);
        tag.setInteger("advCenterZ", mapData.zCenter);
        tag.setInteger("advDimension", mapData.dimension);
        tag.setByte("advScale", mapData.scale);
    }

    private NBTTagCompound getOrCreateTag(ItemStack itemStack) {
        if (itemStack.getTagCompound() == null) {
            itemStack.setTagCompound(new NBTTagCompound());
        }
        return itemStack.getTagCompound();
    }

    private MovementProfile trackMovement(ItemStack itemStack, EntityPlayer player) {
        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        int currentX = MathHelper.floor_double(player.posX);
        int currentZ = MathHelper.floor_double(player.posZ);
        boolean hasPrevious = tag.hasKey(TAG_LAST_PLAYER_X) && tag.hasKey(TAG_LAST_PLAYER_Z);
        int deltaX = hasPrevious ? currentX - tag.getInteger(TAG_LAST_PLAYER_X) : 0;
        int deltaZ = hasPrevious ? currentZ - tag.getInteger(TAG_LAST_PLAYER_Z) : 0;

        tag.setInteger(TAG_LAST_PLAYER_X, currentX);
        tag.setInteger(TAG_LAST_PLAYER_Z, currentZ);

        boolean teleportDetected = hasPrevious
            && (Math.abs(deltaX) >= TELEPORT_DETECT_BLOCKS || Math.abs(deltaZ) >= TELEPORT_DETECT_BLOCKS);
        return new MovementProfile(deltaX, deltaZ, teleportDetected);
    }

    private void scheduleRecoveryIfNeeded(ItemStack itemStack, MovementProfile movement, int recenterMode) {
        if (recenterMode == RECENTER_NONE && (movement == null || !movement.teleportDetected)) {
            return;
        }

        if (recenterMode == RECENTER_CLEAR) {
            this.addPendingRefreshPasses(itemStack, 6);
            this.setDelayedRefresh(itemStack, 8, 6);
            return;
        }

        if (recenterMode == RECENTER_SHIFT) {
            this.addPendingRefreshPasses(itemStack, 4);
            this.setDelayedRefresh(itemStack, 6, 4);
            return;
        }

        if (movement != null && movement.teleportDetected) {
            this.addPendingRefreshPasses(itemStack, 3);
            this.setDelayedRefresh(itemStack, 5, 3);
        }
    }

    private void tickDelayedRefresh(ItemStack itemStack) {
        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        int remainingTicks = tag.getInteger(TAG_DELAYED_REFRESH_TICKS);
        if (remainingTicks <= 0) {
            return;
        }

        remainingTicks--;
        if (remainingTicks <= 0) {
            this.addPendingRefreshPasses(itemStack, tag.getInteger(TAG_DELAYED_REFRESH_PASSES));
            tag.setInteger(TAG_DELAYED_REFRESH_PASSES, 0);
            tag.setInteger(TAG_DELAYED_REFRESH_TICKS, 0);
        } else {
            tag.setInteger(TAG_DELAYED_REFRESH_TICKS, remainingTicks);
        }
    }

    private void setDelayedRefresh(ItemStack itemStack, int ticks, int passes) {
        if (ticks <= 0 || passes <= 0) {
            return;
        }

        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        tag.setInteger(TAG_DELAYED_REFRESH_TICKS, Math.max(tag.getInteger(TAG_DELAYED_REFRESH_TICKS), ticks));
        tag.setInteger(
            TAG_DELAYED_REFRESH_PASSES,
            Math.min(MAX_PENDING_REFRESH_PASSES, tag.getInteger(TAG_DELAYED_REFRESH_PASSES) + passes));
    }

    private void addPendingRefreshPasses(ItemStack itemStack, int passes) {
        if (passes <= 0) {
            return;
        }

        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        tag.setInteger(
            TAG_PENDING_REFRESH_PASSES,
            Math.min(MAX_PENDING_REFRESH_PASSES, tag.getInteger(TAG_PENDING_REFRESH_PASSES) + passes));
    }

    private int consumePendingRefreshPasses(ItemStack itemStack) {
        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        int pending = tag.getInteger(TAG_PENDING_REFRESH_PASSES);
        if (pending <= 0) {
            return 0;
        }

        int consumed = Math.min(MAX_EXTRA_PASSES_PER_TICK, pending);
        tag.setInteger(TAG_PENDING_REFRESH_PASSES, pending - consumed);
        return consumed;
    }

    private boolean tryRunAutoRefresh(ItemStack itemStack, World world, EntityPlayer player) {
        if (!Config.advancedMapAutoRefreshAfterTeleport || !(player instanceof EntityPlayerMP)) {
            return false;
        }

        NBTTagCompound tag = this.getOrCreateTag(itemStack);
        int remainingTicks = tag.getInteger(TAG_AUTO_REFRESH_TICKS);
        if (remainingTicks <= 0) {
            return false;
        }

        remainingTicks--;
        if (remainingTicks > 0) {
            tag.setInteger(TAG_AUTO_REFRESH_TICKS, remainingTicks);
            return false;
        }

        int warmupTicks = tag.getInteger(TAG_AUTO_REFRESH_WARMUP_TICKS);
        boolean readyToRefresh = this.prewarmRefreshChunks(world, player);
        if (!readyToRefresh && warmupTicks < AUTO_REFRESH_MAX_WARMUP_TICKS) {
            tag.setInteger(TAG_AUTO_REFRESH_WARMUP_TICKS, warmupTicks + 1);
            return false;
        }

        tag.setInteger(TAG_AUTO_REFRESH_TICKS, 0);
        tag.setInteger(TAG_AUTO_REFRESH_WARMUP_TICKS, 0);
        AdvancedMagicMapRefreshService.handleRefreshRequest((EntityPlayerMP) player, itemStack.getItemDamage());
        TFMagicMapData refreshedData = this.getMapData(itemStack, world);
        if (refreshedData != null) {
            this.writeCenterToStack(itemStack, refreshedData);
        }
        return true;
    }

    private boolean prewarmRefreshChunks(World world, EntityPlayer player) {
        if (world == null || player == null) {
            return true;
        }

        int centerChunkX = MathHelper.floor_double(player.posX) >> 4;
        int centerChunkZ = MathHelper.floor_double(player.posZ) >> 4;
        int chunkLoads = 0;
        boolean allReady = true;

        for (int radius = 0; radius <= AUTO_REFRESH_CHUNK_RADIUS; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    if (world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
                        continue;
                    }

                    allReady = false;
                    if (chunkLoads < AUTO_REFRESH_CHUNK_LOADS_PER_TICK) {
                        world.getChunkFromChunkCoords(chunkX, chunkZ);
                        chunkLoads++;
                    }
                }
            }
        }

        return allReady;
    }

    private static int snapToPixel(double coord, int blocksPerPixel) {
        return MathHelper.floor_double(coord / blocksPerPixel) * blocksPerPixel;
    }

    private static final class MovementProfile {

        private final int deltaX;
        private final int deltaZ;
        private final boolean teleportDetected;

        private MovementProfile(int deltaX, int deltaZ, boolean teleportDetected) {
            this.deltaX = deltaX;
            this.deltaZ = deltaZ;
            this.teleportDetected = teleportDetected;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon("TwilightForest:magicMap");
    }
}
