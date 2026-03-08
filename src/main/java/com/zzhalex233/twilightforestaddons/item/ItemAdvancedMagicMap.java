package com.zzhalex233.twilightforestaddons.item;

import com.zzhalex233.twilightforestaddons.Config;
import com.zzhalex233.twilightforestaddons.TwilightForestAddons;
import com.zzhalex233.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.zzhalex233.twilightforestaddons.network.ModNetwork;
import com.zzhalex233.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.block.material.MapColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketMaps;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import twilightforest.TFFeature;
import twilightforest.TFMagicMapData;
import twilightforest.biomes.TFBiomes;
import twilightforest.item.ItemTFMagicMap;
import twilightforest.network.PacketMagicMap;
import twilightforest.network.TFPacketHandler;

public class ItemAdvancedMagicMap extends ItemMap {
    private static final ItemTFMagicMap MAGIC_MAP_HELPER = new ItemTFMagicMap();
    private static final Method GET_MAP_COLOR_PER_BIOME = resolveGetMapColorPerBiome();
    private static final Field MAP_COLOR_FIELD = resolveMapColorField();
    private static final Field BRIGHTNESS_FIELD = resolveBrightnessField();
    private static final int MAP_SIZE = 128;
    private static final int MAP_CENTER = 64;
    private static final int MAP_PIXEL_VISIBILITY_RADIUS = 1;
    private static final int BIOME_SAMPLE_SCALE = 4;
    private static final int BLOCKS_PER_PIXEL = 16;
    public static final String MAP_DATA_PREFIX = "magicmap";

    public ItemAdvancedMagicMap() {
        setTranslationKey(TwilightForestAddons.MODID + ".advanced_magic_map");
        setRegistryName(TwilightForestAddons.MODID, "advanced_magic_map");
        setCreativeTab(CreativeTabs.MISC);
    }

    public static ItemStack createMapStack(World world, double centerX, double centerZ, byte scale, boolean trackingPosition,
        boolean unlimitedTracking) {
        ItemStack mapStack = new ItemStack(ModItems.ADVANCED_MAGIC_MAP, 1, world.getUniqueDataId(MAP_DATA_PREFIX));
        String mapName = MAP_DATA_PREFIX + "_" + mapStack.getMetadata();
        TFMagicMapData mapData = new TFMagicMapData(mapName);
        world.setData(mapName, mapData);

        mapData.scale = scale;
        mapData.calculateMapCenter(centerX, centerZ, mapData.scale);
        AdvancedMagicMapDataUtils.setMapDimension(mapData, world.provider.getDimension());
        mapData.trackingPosition = trackingPosition;
        mapData.unlimitedTracking = unlimitedTracking;
        mapData.markDirty();
        writeCenterToStack(mapStack, mapData);
        return mapStack;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            TwilightForestAddons.proxy.toggleAdvancedMagicMapGui(player, stack);
        } else if (player instanceof EntityPlayerMP) {
            TFMagicMapData mapData = getMapData(stack, world);
            if (mapData != null) {
                mapData.updateVisiblePlayers(player, stack);
                recenterIfNeeded(world, player, mapData);
                updateMagicMapData(world, player, mapData);
                AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.tfDecorations);
                sendImmediateMapState(stack, world, (EntityPlayerMP) player, mapData);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return oldStack.isEmpty() != newStack.isEmpty();
        }
        return oldStack.getItem() != newStack.getItem() || oldStack.getMetadata() != newStack.getMetadata();
    }

    @Override
    public TFMagicMapData getMapData(ItemStack stack, World world) {
        String mapName = MAP_DATA_PREFIX + "_" + stack.getMetadata();
        TFMagicMapData mapData = (TFMagicMapData) world.loadData(TFMagicMapData.class, mapName);

        if (mapData == null && !world.isRemote) {
            stack.setItemDamage(world.getUniqueDataId(MAP_DATA_PREFIX));
            mapName = MAP_DATA_PREFIX + "_" + stack.getMetadata();
            mapData = new TFMagicMapData(mapName);
            mapData.scale = 4;
            mapData.calculateMapCenter(world.getWorldInfo().getSpawnX(), world.getWorldInfo().getSpawnZ(), mapData.scale);
            AdvancedMagicMapDataUtils.setMapDimension(mapData, world.provider.getDimension());
            mapData.markDirty();
            world.setData(mapName, mapData);
            writeCenterToStack(stack, mapData);
        }

        return mapData;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (world.isRemote) {
            return;
        }

        TFMagicMapData mapData = getMapData(stack, world);
        if (mapData == null) {
            return;
        }

        EntityPlayer player = getTrackingPlayer(entity);
        if (player != null) {
            mapData.updateVisiblePlayers(player, stack);
            recenterIfNeeded(world, player, mapData);
        }

        if (shouldUpdateHeldMap(entity, stack, isSelected)) {
            updateMagicMapData(world, entity, mapData);
            AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.tfDecorations);
        }

        if (player != null) {
            syncCenterIfNeeded(stack, mapData, player);
        }
    }

    @Override
    public Packet<?> createMapDataPacket(ItemStack stack, World world, EntityPlayer player) {
        TFMagicMapData mapData = getMapData(stack, world);
        if (mapData == null) {
            return null;
        }

        Packet<?> packet = mapData.getMapPacket(stack, world, player);
        if (packet instanceof SPacketMaps) {
            return TFPacketHandler.CHANNEL.getPacketFrom(new PacketMagicMap(stack.getItemDamage(), mapData, (SPacketMaps) packet));
        }
        return packet;
    }

    private void updateMagicMapData(World world, Entity entity, TFMagicMapData mapData) {
        if (world.provider.getDimension() != AdvancedMagicMapDataUtils.getMapDimension(mapData) || !(entity instanceof EntityPlayer)) {
            return;
        }

        int centerX = mapData.xCenter;
        int centerZ = mapData.zCenter;
        int mapX = MathHelper.floor(entity.posX - centerX) / BLOCKS_PER_PIXEL + MAP_CENTER;
        int mapZ = MathHelper.floor(entity.posZ - centerZ) / BLOCKS_PER_PIXEL + MAP_CENTER;
        int radius = 512 / BLOCKS_PER_PIXEL;
        Biome[] biomes = sampleBiomes(world, centerX, centerZ);

        for (int x = mapX - radius + 1; x < mapX + radius; x++) {
            for (int z = mapZ - radius - 1; z < mapZ + radius; z++) {
                if (!isInsideMap(x, z)) {
                    continue;
                }

                int relX = x - mapX;
                int relZ = z - mapZ;
                if (relX * relX + relZ * relZ >= radius * radius) {
                    continue;
                }

                Biome biome = resolveBiomeForPixel(biomes, x, z);
                updatePixelColor(world, mapData, x, z, mapX, mapZ, radius, biome);
                maybeAddFeatureDecoration(world, mapData, centerX, centerZ, x, z);
            }
        }
    }

    private Biome[] sampleBiomes(World world, int centerX, int centerZ) {
        int biomeX = (centerX / BLOCKS_PER_PIXEL - MAP_CENTER) * BIOME_SAMPLE_SCALE;
        int biomeZ = (centerZ / BLOCKS_PER_PIXEL - MAP_CENTER) * BIOME_SAMPLE_SCALE;
        return world.getBiomeProvider().getBiomesForGeneration(null, biomeX, biomeZ, MAP_SIZE * BIOME_SAMPLE_SCALE, MAP_SIZE * BIOME_SAMPLE_SCALE);
    }

    private Biome resolveBiomeForPixel(Biome[] biomes, int x, int z) {
        int biomeIndex = x * BIOME_SAMPLE_SCALE + z * BIOME_SAMPLE_SCALE * MAP_SIZE * BIOME_SAMPLE_SCALE;
        Biome biome = biomes[biomeIndex];
        Biome eastBiome = biomes[biomeIndex + 1];
        Biome southBiome = biomes[biomeIndex + MAP_SIZE * BIOME_SAMPLE_SCALE];
        if (eastBiome == TFBiomes.stream || southBiome == TFBiomes.stream) {
            return TFBiomes.stream;
        }
        return biome;
    }

    private void updatePixelColor(World world, TFMagicMapData mapData, int x, int z, int mapX, int mapZ, int radius, Biome biome) {
        int relX = x - mapX;
        int relZ = z - mapZ;
        boolean edge = relX * relX + relZ * relZ > (radius - 2) * (radius - 2);
        if (edge && ((x + z) & 1) == 0) {
            return;
        }

        MapColorBrightnessData colorData = getMapColorBrightness(world, biome);
        int index = getMapIndex(x, z);
        byte colorByte = (byte) (colorData.color.colorIndex * 4 + colorData.brightness);
        if (mapData.colors[index] != colorByte) {
            mapData.colors[index] = colorByte;
            mapData.updateMapData(x, z);
        }
    }

    private void maybeAddFeatureDecoration(World world, TFMagicMapData mapData, int centerX, int centerZ, int mapX, int mapZ) {
        if (!hasExploredPixelNear(mapData, mapX, mapZ)) {
            return;
        }

        int worldX = (centerX / BLOCKS_PER_PIXEL + mapX - MAP_CENTER) * BLOCKS_PER_PIXEL;
        int worldZ = (centerZ / BLOCKS_PER_PIXEL + mapZ - MAP_CENTER) * BLOCKS_PER_PIXEL;
        if (!TFFeature.isInFeatureChunk(world, worldX, worldZ)) {
            return;
        }

        byte iconX = (byte) (((float) (worldX - centerX) / BLOCKS_PER_PIXEL) * 2.0F);
        byte iconZ = (byte) (((float) (worldZ - centerZ) / BLOCKS_PER_PIXEL) * 2.0F);
        TFFeature feature = TFFeature.getFeatureAt(worldX, worldZ, world);
        mapData.tfDecorations.add(new TFMagicMapData.TFMapDecoration(feature.ordinal(), iconX, iconZ, (byte) 8));
    }

    private MapColorBrightnessData getMapColorBrightness(World world, Biome biome) {
        try {
            Object result = GET_MAP_COLOR_PER_BIOME.invoke(MAGIC_MAP_HELPER, world, biome);
            return new MapColorBrightnessData((MapColor) MAP_COLOR_FIELD.get(result), BRIGHTNESS_FIELD.getInt(result));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to get Twilight Forest magic map biome color.", e);
        }
    }

    private void recenterIfNeeded(World world, EntityPlayer player, TFMagicMapData mapData) {
        int blocksPerPixel = 1 << mapData.scale;
        int thresholdBlocks = Math.max(4, Config.advancedMapRecenterChunks * 4);
        int playerX = MathHelper.floor(player.posX);
        int playerZ = MathHelper.floor(player.posZ);

        if (Math.abs(playerX - mapData.xCenter) < thresholdBlocks && Math.abs(playerZ - mapData.zCenter) < thresholdBlocks) {
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
        AdvancedMagicMapDataUtils.setMapDimension(mapData, world.provider.getDimension());
        markWholeMapDirty(mapData);
        AdvancedMagicMapDataUtils.dedupeFeaturesInPlace(mapData.tfDecorations);
    }

    private void markWholeMapDirty(TFMagicMapData mapData) {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                mapData.updateMapData(x, z);
            }
        }
        mapData.markDirty();
    }

    private boolean hasExploredPixelNear(TFMagicMapData mapData, int centerX, int centerZ) {
        int clampedX = MathHelper.clamp(centerX, 0, MAP_SIZE - 1);
        int clampedZ = MathHelper.clamp(centerZ, 0, MAP_SIZE - 1);
        for (int dx = -MAP_PIXEL_VISIBILITY_RADIUS; dx <= MAP_PIXEL_VISIBILITY_RADIUS; dx++) {
            for (int dz = -MAP_PIXEL_VISIBILITY_RADIUS; dz <= MAP_PIXEL_VISIBILITY_RADIUS; dz++) {
                int x = MathHelper.clamp(clampedX + dx, 0, MAP_SIZE - 1);
                int z = MathHelper.clamp(clampedZ + dz, 0, MAP_SIZE - 1);
                if (mapData.colors[getMapIndex(x, z)] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendImmediateMapState(ItemStack stack, World world, EntityPlayerMP player, TFMagicMapData mapData) {
        Packet<?> packet = createMapDataPacket(stack, world, player);
        if (packet != null) {
            player.connection.sendPacket(packet);
        }
        ModNetwork.CHANNEL.sendTo(
            new PacketSyncAdvancedMapCenter(stack.getMetadata(), mapData.xCenter, mapData.zCenter, AdvancedMagicMapDataUtils.getMapDimension(mapData), mapData.scale),
            player
        );
        writeCenterToStack(stack, mapData);
    }

    private void syncCenterIfNeeded(ItemStack stack, TFMagicMapData mapData, EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP) || !shouldSyncCenter(stack, mapData)) {
            return;
        }

        ModNetwork.CHANNEL.sendTo(
            new PacketSyncAdvancedMapCenter(stack.getMetadata(), mapData.xCenter, mapData.zCenter, AdvancedMagicMapDataUtils.getMapDimension(mapData), mapData.scale),
            (EntityPlayerMP) player
        );
        writeCenterToStack(stack, mapData);
    }

    private boolean shouldSyncCenter(ItemStack stack, TFMagicMapData mapData) {
        NBTTagCompound tag = getOrCreateTag(stack);
        return tag.getInteger("advCenterX") != mapData.xCenter
            || tag.getInteger("advCenterZ") != mapData.zCenter
            || tag.getInteger("advDimension") != AdvancedMagicMapDataUtils.getMapDimension(mapData)
            || tag.getByte("advScale") != mapData.scale;
    }

    private static void writeCenterToStack(ItemStack stack, TFMagicMapData mapData) {
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setInteger("advCenterX", mapData.xCenter);
        tag.setInteger("advCenterZ", mapData.zCenter);
        tag.setInteger("advDimension", AdvancedMagicMapDataUtils.getMapDimension(mapData));
        tag.setByte("advScale", mapData.scale);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private boolean shouldUpdateHeldMap(Entity entity, ItemStack stack, boolean isSelected) {
        if (isSelected) {
            return true;
        }
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer) entity;
        return player.getHeldItemOffhand() == stack;
    }

    private EntityPlayer getTrackingPlayer(Entity entity) {
        return entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
    }

    private static int snapToPixel(double coord, int blocksPerPixel) {
        return MathHelper.floor(coord / blocksPerPixel + 0.5D) * blocksPerPixel;
    }

    private static int getMapIndex(int x, int z) {
        return x + z * MAP_SIZE;
    }

    private static boolean isInsideMap(int x, int z) {
        return x >= 0 && z >= 0 && x < MAP_SIZE && z < MAP_SIZE;
    }

    private static Method resolveGetMapColorPerBiome() {
        try {
            Method method = ItemTFMagicMap.class.getDeclaredMethod("getMapColorPerBiome", World.class, Biome.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Twilight Forest magic map biome color helper.", e);
        }
    }

    private static Field resolveMapColorField() {
        try {
            Class<?> type = Class.forName("twilightforest.item.ItemTFMagicMap$MapColorBrightness");
            Field field = type.getDeclaredField("color");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Twilight Forest map color field.", e);
        }
    }

    private static Field resolveBrightnessField() {
        try {
            Class<?> type = Class.forName("twilightforest.item.ItemTFMagicMap$MapColorBrightness");
            Field field = type.getDeclaredField("brightness");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access Twilight Forest map brightness field.", e);
        }
    }

    private static final class MapColorBrightnessData {
        private final MapColor color;
        private final int brightness;

        private MapColorBrightnessData(MapColor color, int brightness) {
            this.color = color;
            this.brightness = brightness;
        }
    }
}