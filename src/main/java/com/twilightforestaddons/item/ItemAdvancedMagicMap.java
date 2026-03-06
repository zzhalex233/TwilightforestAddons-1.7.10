package com.twilightforestaddons.item;

import java.util.HashMap;
import java.util.Map;

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
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;
import twilightforest.TwilightForestMod;
import twilightforest.item.ItemTFMagicMap;

public class ItemAdvancedMagicMap extends ItemTFMagicMap {

    private static final Map<String, PassState> PASS_STATES = new HashMap<String, PassState>();

    private static final String TAG_LAST_SYNC_X = "advLastSyncX";
    private static final String TAG_LAST_SYNC_Z = "advLastSyncZ";
    private static final String TAG_LAST_SYNC_DIM = "advLastSyncDim";
    private static final String TAG_LAST_SYNC_SCALE = "advLastSyncScale";

    private static final int MAP_SIZE_PIXELS = 128;
    private static final int MAX_EXTRA_PASSES_PER_TICK = 24;
    private static final int MAX_GAP_FILL_PASSES_PER_TICK = 32;
    private static final int MAX_PENDING_EXTRA_PASSES = 48;
    private static final int MIN_SPEED_PIXELS_FOR_BONUS = 2;

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
        TFMagicMapData mapData = null;
        EntityPlayer player = null;
        PassState passState = null;
        boolean shouldTrack = false;

        if (!world.isRemote && entity instanceof EntityPlayer && world.provider.dimensionId == TwilightForestMod.dimensionID) {
            player = (EntityPlayer) entity;
            mapData = this.getMapData(itemStack, world);

            if (mapData != null) {
                boolean equipped = player.getCurrentEquippedItem() == itemStack;
                shouldTrack = isHeld || equipped;

                if (shouldTrack) {
                    passState = getPassState(world, itemStack);
                    int budgetFromShift = recenterIfNeeded(itemStack, player, world, mapData, passState);
                    addPendingPasses(passState, budgetFromShift);
                    this.updateMapData(world, entity, mapData);
                }
            }
        }

        super.onUpdate(itemStack, world, entity, slot, false);

        if (passState == null || mapData == null || player == null) {
            return;
        }

        addSpeedBudget(passState, player, mapData);
        if (hasCoverageGapNearPlayer(mapData, player)) {
            addPendingPasses(passState, 1);
        }

        runExtraPasses(world, entity, mapData, passState);
        runGapFillPasses(world, entity, mapData, player);
        dedupeFeatures(mapData);
        writeCenterToStack(itemStack, mapData);

        if (player instanceof EntityPlayerMP && shouldSyncCenter(itemStack, mapData)) {
            syncCenterToClient((EntityPlayerMP) player, itemStack, mapData);
            recordSyncedCenter(itemStack, mapData);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon("TwilightForest:magicMap");
    }

    private int recenterIfNeeded(ItemStack itemStack, EntityPlayer player, World world, TFMagicMapData mapData,
        PassState passState) {
        int recenterBlocks = Math.max(1, Config.advancedMapRecenterChunks) << 4;
        int blocksPerPixel = 1 << mapData.scale;
        int targetCenterX = MathHelper.floor_double(player.posX);
        int targetCenterZ = MathHelper.floor_double(player.posZ);
        int dx = targetCenterX - mapData.xCenter;
        int dz = targetCenterZ - mapData.zCenter;

        if (Math.abs(dx) < recenterBlocks && Math.abs(dz) < recenterBlocks) {
            return 0;
        }

        int shiftXPixels = dx >> mapData.scale;
        int shiftZPixels = dz >> mapData.scale;
        if (shiftXPixels == 0 && shiftZPixels == 0) {
            return 0;
        }

        AdvancedMagicMapDataUtils.shiftMapContent(mapData, shiftXPixels, shiftZPixels, true);
        shiftPassState(passState, shiftXPixels, shiftZPixels);
        mapData.xCenter += shiftXPixels * blocksPerPixel;
        mapData.zCenter += shiftZPixels * blocksPerPixel;
        mapData.dimension = world.provider.dimensionId;
        writeCenterToStack(itemStack, mapData);
        mapData.markDirty();

        return estimateShiftBudget(shiftXPixels, shiftZPixels);
    }

    private int estimateShiftBudget(int shiftXPixels, int shiftZPixels) {
        int primaryShift = Math.max(Math.abs(shiftXPixels), Math.abs(shiftZPixels));
        int secondaryShift = Math.min(Math.abs(shiftXPixels), Math.abs(shiftZPixels));
        return Math.min(MAX_PENDING_EXTRA_PASSES, Math.max(4, primaryShift + secondaryShift));
    }

    private void shiftPassState(PassState passState, int shiftXPixels, int shiftZPixels) {
        if (passState == null) {
            return;
        }

        if (passState.lastPlayerPixelX != Integer.MIN_VALUE) {
            passState.lastPlayerPixelX -= shiftXPixels;
        }
        if (passState.lastPlayerPixelZ != Integer.MIN_VALUE) {
            passState.lastPlayerPixelZ -= shiftZPixels;
        }
    }

    private void addSpeedBudget(PassState passState, EntityPlayer player, TFMagicMapData mapData) {
        int playerPixelX = getPlayerPixelX(mapData, player);
        int playerPixelZ = getPlayerPixelZ(mapData, player);

        if (passState.lastPlayerPixelX != Integer.MIN_VALUE && passState.lastPlayerPixelZ != Integer.MIN_VALUE) {
            int dx = Math.abs(playerPixelX - passState.lastPlayerPixelX);
            int dz = Math.abs(playerPixelZ - passState.lastPlayerPixelZ);
            int delta = Math.max(dx, dz);
            if (delta >= MIN_SPEED_PIXELS_FOR_BONUS) {
                addPendingPasses(passState, Math.min(2, delta - 1));
            }
        }

        passState.lastPlayerPixelX = playerPixelX;
        passState.lastPlayerPixelZ = playerPixelZ;
    }

    private boolean hasCoverageGapNearPlayer(TFMagicMapData mapData, EntityPlayer player) {
        int playerPixelX = getPlayerPixelX(mapData, player);
        int playerPixelZ = getPlayerPixelZ(mapData, player);
        int radius = getVisibleRadiusPixels(mapData);
        int radiusSq = radius * radius;

        int minX = Math.max(0, playerPixelX - radius);
        int maxX = Math.min(MAP_SIZE_PIXELS - 1, playerPixelX + radius);
        int minZ = Math.max(0, playerPixelZ - radius);
        int maxZ = Math.min(MAP_SIZE_PIXELS - 1, playerPixelZ + radius);

        for (int x = minX; x <= maxX; x++) {
            int dx = x - playerPixelX;
            for (int z = minZ; z <= maxZ; z++) {
                int dz = z - playerPixelZ;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }

                if (mapData.colors[x + z * MAP_SIZE_PIXELS] == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private void runExtraPasses(World world, Entity entity, TFMagicMapData mapData, PassState passState) {
        int passes = Math.min(MAX_EXTRA_PASSES_PER_TICK, passState.pendingExtraPasses);
        for (int i = 0; i < passes; i++) {
            this.updateMapData(world, entity, mapData);
        }
        passState.pendingExtraPasses -= passes;
    }

    private void runGapFillPasses(World world, Entity entity, TFMagicMapData mapData, EntityPlayer player) {
        for (int i = 0; i < MAX_GAP_FILL_PASSES_PER_TICK; i++) {
            if (!hasCoverageGapNearPlayer(mapData, player)) {
                break;
            }
            this.updateMapData(world, entity, mapData);
        }
    }

    private void addPendingPasses(PassState passState, int passes) {
        if (passState == null || passes <= 0) {
            return;
        }

        passState.pendingExtraPasses = Math.min(MAX_PENDING_EXTRA_PASSES, passState.pendingExtraPasses + passes);
    }

    private int getPlayerPixelX(TFMagicMapData mapData, EntityPlayer player) {
        int blocksPerPixel = 1 << mapData.scale;
        return MathHelper.floor_double((player.posX - mapData.xCenter) / blocksPerPixel) + MAP_SIZE_PIXELS / 2;
    }

    private int getPlayerPixelZ(TFMagicMapData mapData, EntityPlayer player) {
        int blocksPerPixel = 1 << mapData.scale;
        return MathHelper.floor_double((player.posZ - mapData.zCenter) / blocksPerPixel) + MAP_SIZE_PIXELS / 2;
    }

    private int getVisibleRadiusPixels(TFMagicMapData mapData) {
        return Math.max(1, 512 / (1 << mapData.scale));
    }

    private PassState getPassState(World world, ItemStack itemStack) {
        String key = world.provider.dimensionId + ":" + itemStack.getItemDamage();
        PassState passState = PASS_STATES.get(key);
        if (passState == null) {
            passState = new PassState();
            PASS_STATES.put(key, passState);
        }
        return passState;
    }

    private void syncCenterToClient(EntityPlayerMP player, ItemStack itemStack, TFMagicMapData mapData) {
        if (player == null || itemStack == null || mapData == null) {
            return;
        }

        ModNetwork.CHANNEL.sendTo(
            new PacketSyncAdvancedMapCenter(
                itemStack.getItemDamage(),
                mapData.xCenter,
                mapData.zCenter,
                mapData.dimension,
                mapData.scale),
            player);
    }

    private boolean shouldSyncCenter(ItemStack itemStack, TFMagicMapData mapData) {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null) {
            return true;
        }

        return !tag.hasKey(TAG_LAST_SYNC_X) || !tag.hasKey(TAG_LAST_SYNC_Z) || !tag.hasKey(TAG_LAST_SYNC_DIM)
            || !tag.hasKey(TAG_LAST_SYNC_SCALE) || tag.getInteger(TAG_LAST_SYNC_X) != mapData.xCenter
            || tag.getInteger(TAG_LAST_SYNC_Z) != mapData.zCenter
            || tag.getInteger(TAG_LAST_SYNC_DIM) != mapData.dimension
            || tag.getByte(TAG_LAST_SYNC_SCALE) != mapData.scale;
    }

    private void recordSyncedCenter(ItemStack itemStack, TFMagicMapData mapData) {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            itemStack.setTagCompound(tag);
        }

        tag.setInteger(TAG_LAST_SYNC_X, mapData.xCenter);
        tag.setInteger(TAG_LAST_SYNC_Z, mapData.zCenter);
        tag.setInteger(TAG_LAST_SYNC_DIM, mapData.dimension);
        tag.setByte(TAG_LAST_SYNC_SCALE, mapData.scale);
    }

    private void dedupeFeatures(TFMagicMapData mapData) {
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.featuresVisibleOnMap);
    }

    private void writeCenterToStack(ItemStack itemStack, TFMagicMapData mapData) {
        NBTTagCompound tag = itemStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            itemStack.setTagCompound(tag);
        }
        tag.setInteger("advCenterX", mapData.xCenter);
        tag.setInteger("advCenterZ", mapData.zCenter);
        tag.setInteger("advDimension", mapData.dimension);
        tag.setByte("advScale", mapData.scale);
    }

    private static final class PassState {

        private int pendingExtraPasses;
        private int lastPlayerPixelX = Integer.MIN_VALUE;
        private int lastPlayerPixelZ = Integer.MIN_VALUE;
    }
}
