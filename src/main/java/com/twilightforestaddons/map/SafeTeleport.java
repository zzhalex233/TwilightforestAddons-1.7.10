package com.twilightforestaddons.map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class SafeTeleport {

    private static final String TAG_ROOT = "safeTeleportCorrection";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_DELAY = "delay";
    private static final String TAG_TTL = "ttl";
    private static final String TAG_RADIUS = "radius";
    private static final String TAG_CHUNK_RADIUS = "chunkRadius";
    private static final String TAG_CHUNK_LOADS = "chunkLoadsPerTick";
    private static final String TAG_ANCHOR_X = "anchorX";
    private static final String TAG_ANCHOR_Y = "anchorY";
    private static final String TAG_ANCHOR_Z = "anchorZ";
    private static final String TAG_SEARCH_UP = "searchUp";
    private static final String TAG_SEARCH_DOWN = "searchDown";

    private SafeTeleport() {}

    public static int[] findSafeTeleport(World world, int x, int z, EntityPlayer player) {
        return findSafeTeleportResult(world, x, z, player, Options.defaults()).toArray();
    }

    public static Result findSafeTeleportResult(World world, int x, int z, EntityPlayer player) {
        return findSafeTeleportResult(world, x, z, player, Options.defaults());
    }

    public static Result findSafeTeleportResult(World world, int x, int z, EntityPlayer player, Options options) {
        Options resolved = options != null ? options : Options.defaults();
        if (world == null) {
            return new Result(x, 64, z, false, true);
        }

        prewarmChunks(world, x, z, resolved.initialChunkWarmupRadius, Integer.MAX_VALUE);

        Result nearby = findNearestSafeSpot(world, x, z, player, resolved.horizontalAdjustRadius, resolved);
        if (nearby != null) {
            return nearby;
        }

        Result wider = findNearestSafeSpot(world, x, z, player, resolved.landSearchRadius, resolved);
        if (wider != null) {
            return wider;
        }

        return new Result(x, clamp(getBaseY(world, x, z, player), 10, world.getActualHeight() - 2), z, false, true);
    }

    public static void schedulePostTeleportCorrection(EntityPlayer player) {
        schedulePostTeleportCorrection(player, Options.defaults());
    }

    public static void schedulePostTeleportCorrection(EntityPlayer player, Options options) {
        if (player == null || player.worldObj == null) {
            return;
        }

        Options resolved = options != null ? options : Options.defaults();
        NBTTagCompound tag = getOrCreateCorrectionTag(player);
        tag.setBoolean(TAG_ACTIVE, true);
        tag.setInteger(TAG_DELAY, resolved.correctionDelayTicks);
        tag.setInteger(TAG_TTL, resolved.correctionMaxTicks);
        tag.setInteger(TAG_RADIUS, resolved.correctionHorizontalRadius);
        tag.setInteger(TAG_CHUNK_RADIUS, resolved.correctionChunkWarmupRadius);
        tag.setInteger(TAG_CHUNK_LOADS, resolved.chunkLoadsPerTick);
        tag.setInteger(TAG_SEARCH_UP, resolved.searchUpSteps);
        tag.setInteger(TAG_SEARCH_DOWN, resolved.searchDownSteps);
        tag.setInteger(TAG_ANCHOR_X, MathHelper.floor_double(player.posX));
        tag.setInteger(TAG_ANCHOR_Y, MathHelper.floor_double(player.posY));
        tag.setInteger(TAG_ANCHOR_Z, MathHelper.floor_double(player.posZ));
    }

    public static void tickScheduledCorrection(EntityPlayer player) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }

        NBTTagCompound tag = getCorrectionTag(player);
        if (tag == null || !tag.getBoolean(TAG_ACTIVE)) {
            return;
        }

        World world = player.worldObj;
        int anchorX = tag.getInteger(TAG_ANCHOR_X);
        int anchorZ = tag.getInteger(TAG_ANCHOR_Z);
        int radius = Math.max(0, tag.getInteger(TAG_RADIUS));
        int chunkRadius = Math.max(0, tag.getInteger(TAG_CHUNK_RADIUS));
        int chunkLoadsPerTick = Math.max(1, tag.getInteger(TAG_CHUNK_LOADS));

        prewarmChunks(world, anchorX, anchorZ, chunkRadius, chunkLoadsPerTick);

        int delay = tag.getInteger(TAG_DELAY);
        if (delay > 0) {
            tag.setInteger(TAG_DELAY, delay - 1);
            return;
        }

        int ttl = tag.getInteger(TAG_TTL);
        if (ttl <= 0) {
            clearCorrectionTag(player);
            return;
        }
        tag.setInteger(TAG_TTL, ttl - 1);

        int currentX = MathHelper.floor_double(player.posX);
        int currentY = MathHelper.floor_double(player.posY);
        int currentZ = MathHelper.floor_double(player.posZ);
        int maxDrift = Math.max(8, radius * 3);
        if (Math.abs(currentX - anchorX) > maxDrift || Math.abs(currentZ - anchorZ) > maxDrift) {
            clearCorrectionTag(player);
            return;
        }

        if (isSafeStand(world, currentX, currentY, currentZ)) {
            clearCorrectionTag(player);
            return;
        }

        Options correctionOptions = Options.defaults()
            .setHorizontalAdjustRadius(radius)
            .setLandSearchRadius(radius)
            .setSearchUpSteps(tag.getInteger(TAG_SEARCH_UP))
            .setSearchDownSteps(tag.getInteger(TAG_SEARCH_DOWN))
            .setInitialChunkWarmupRadius(0)
            .setCorrectionDelayTicks(0)
            .setCorrectionMaxTicks(0)
            .setCorrectionHorizontalRadius(radius)
            .setCorrectionChunkWarmupRadius(chunkRadius)
            .setChunkLoadsPerTick(chunkLoadsPerTick);

        Result corrected = findNearestSafeSpot(world, anchorX, anchorZ, player, radius, correctionOptions);
        if (corrected == null) {
            return;
        }

        relocatePlayer(player, corrected.x, corrected.y, corrected.z);
        clearCorrectionTag(player);
    }

    public static void prewarmChunks(World world, int blockX, int blockZ, int chunkRadius, int maxChunkLoads) {
        if (world == null || chunkRadius < 0 || maxChunkLoads == 0) {
            return;
        }

        int centerChunkX = blockX >> 4;
        int centerChunkZ = blockZ >> 4;
        int loads = 0;

        for (int radius = 0; radius <= chunkRadius; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    if (!world.getChunkProvider()
                        .chunkExists(chunkX, chunkZ)) {
                        world.getChunkFromChunkCoords(chunkX, chunkZ);
                        loads++;
                        if (loads >= maxChunkLoads) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static boolean isSafeStand(World world, int x, int y, int z) {
        if (world == null || y < 2 || y >= world.getActualHeight() - 1) {
            return false;
        }

        Block below = world.getBlock(x, y - 1, z);
        Block feet = world.getBlock(x, y, z);
        Block head = world.getBlock(x, y + 1, z);

        if (below == null || below.isAir(world, x, y - 1, z)) {
            return false;
        }
        if (below.getMaterial() != null && below.getMaterial()
            .isLiquid()) {
            return false;
        }
        if (below.getMaterial() != null && below.getMaterial()
            .isReplaceable()) {
            return false;
        }

        if (!isPassable(world, feet, x, y, z)) {
            return false;
        }
        if (!isPassable(world, head, x, y + 1, z)) {
            return false;
        }

        return true;
    }

    private static Result findNearestSafeSpot(World world, int x, int z, EntityPlayer player, int maxRadius,
        Options options) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int targetX = x + dx;
                    int targetZ = z + dz;
                    int safeY = findSafeY(targetX, targetZ, world, player, options);
                    if (safeY != -1) {
                        return new Result(targetX, safeY, targetZ, dx != 0 || dz != 0, false);
                    }
                }
            }
        }
        return null;
    }

    private static int getBaseY(World world, int x, int z, EntityPlayer player) {
        if (world == null) {
            return 64;
        }

        int minY = 1;
        int maxY = world.getActualHeight() - 2;

        if (world.provider != null && world.provider.hasNoSky) {
            int prefer = 64;
            if (player != null && player.worldObj == world) {
                int py = MathHelper.floor_double(player.posY);
                if (py > 20 && py < 110) {
                    prefer = py;
                }
            }

            return clamp(prefer, minY, maxY);
        }

        return clamp(world.getTopSolidOrLiquidBlock(x, z), minY, maxY);
    }

    private static int findSafeY(int x, int z, World world, EntityPlayer player, Options options) {
        int y = getBaseY(world, x, z, player);

        int up = searchUp(world, x, y, z, options.searchUpSteps);
        if (up != -1) {
            return up;
        }

        int down = searchDown(world, x, y, z, options.searchDownSteps);
        if (down != -1) {
            return down;
        }

        return -1;
    }

    private static int searchUp(World world, int x, int startY, int z, int maxSteps) {
        int maxY = world.getActualHeight() - 2;
        int y = clamp(startY, 1, maxY);

        for (int i = 0; i <= maxSteps && y + i <= maxY; i++) {
            int ty = y + i;
            if (isSafeStand(world, x, ty, z)) {
                return ty;
            }
        }
        return -1;
    }

    private static int searchDown(World world, int x, int startY, int z, int maxSteps) {
        int y = clamp(startY, 1, world.getActualHeight() - 2);

        for (int i = 0; i <= maxSteps && y - i >= 1; i++) {
            int ty = y - i;
            if (isSafeStand(world, x, ty, z)) {
                return ty;
            }
        }
        return -1;
    }

    private static void relocatePlayer(EntityPlayer player, int x, int y, int z) {
        if (player == null) {
            return;
        }

        player.mountEntity(null);
        player.fallDistance = 0.0F;

        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).playerNetServerHandler
                .setPlayerLocation(x + 0.5D, y, z + 0.5D, player.rotationYaw, player.rotationPitch);
        } else {
            player.setPosition(x + 0.5D, y, z + 0.5D);
        }
    }

    private static boolean isPassable(World world, Block block, int x, int y, int z) {
        if (block == null) {
            return true;
        }
        if (block.isAir(world, x, y, z)) {
            return true;
        }

        return block.getMaterial() != null && block.getMaterial()
            .isReplaceable();
    }

    private static NBTTagCompound getCorrectionTag(EntityPlayer player) {
        if (player == null) {
            return null;
        }

        NBTTagCompound root = player.getEntityData();
        return root.hasKey(TAG_ROOT) ? root.getCompoundTag(TAG_ROOT) : null;
    }

    private static NBTTagCompound getOrCreateCorrectionTag(EntityPlayer player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(TAG_ROOT)) {
            root.setTag(TAG_ROOT, new NBTTagCompound());
        }
        return root.getCompoundTag(TAG_ROOT);
    }

    private static void clearCorrectionTag(EntityPlayer player) {
        if (player != null) {
            player.getEntityData()
                .removeTag(TAG_ROOT);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static final class Result {

        public final int x;
        public final int y;
        public final int z;
        public final boolean adjustedXZ;
        public final boolean fallback;

        private Result(int x, int y, int z, boolean adjustedXZ, boolean fallback) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.adjustedXZ = adjustedXZ;
            this.fallback = fallback;
        }

        public int[] toArray() {
            return new int[] { this.x, this.y, this.z };
        }
    }

    public static final class Options {

        private int horizontalAdjustRadius = 8;
        private int landSearchRadius = 48;
        private int searchUpSteps = 32;
        private int searchDownSteps = 48;
        private int initialChunkWarmupRadius = 1;
        private int correctionDelayTicks = 10;
        private int correctionMaxTicks = 40;
        private int correctionHorizontalRadius = 8;
        private int correctionChunkWarmupRadius = 1;
        private int chunkLoadsPerTick = 2;

        public static Options defaults() {
            return new Options();
        }

        public Options setHorizontalAdjustRadius(int value) {
            this.horizontalAdjustRadius = Math.max(0, value);
            return this;
        }

        public Options setLandSearchRadius(int value) {
            this.landSearchRadius = Math.max(0, value);
            return this;
        }

        public Options setSearchUpSteps(int value) {
            this.searchUpSteps = Math.max(0, value);
            return this;
        }

        public Options setSearchDownSteps(int value) {
            this.searchDownSteps = Math.max(0, value);
            return this;
        }

        public Options setInitialChunkWarmupRadius(int value) {
            this.initialChunkWarmupRadius = Math.max(0, value);
            return this;
        }

        public Options setCorrectionDelayTicks(int value) {
            this.correctionDelayTicks = Math.max(0, value);
            return this;
        }

        public Options setCorrectionMaxTicks(int value) {
            this.correctionMaxTicks = Math.max(0, value);
            return this;
        }

        public Options setCorrectionHorizontalRadius(int value) {
            this.correctionHorizontalRadius = Math.max(0, value);
            return this;
        }

        public Options setCorrectionChunkWarmupRadius(int value) {
            this.correctionChunkWarmupRadius = Math.max(0, value);
            return this;
        }

        public Options setChunkLoadsPerTick(int value) {
            this.chunkLoadsPerTick = Math.max(1, value);
            return this;
        }
    }
}
