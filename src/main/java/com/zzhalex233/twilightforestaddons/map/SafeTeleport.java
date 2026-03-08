package com.zzhalex233.twilightforestaddons.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class SafeTeleport {
    private SafeTeleport() {
    }

    public static Result findSafeTeleportResult(World world, int x, int z, EntityPlayer player) {
        return findSafeTeleportResult(world, x, z, player, new Options());
    }

    public static Result findSafeTeleportResult(World world, int x, int z, EntityPlayer player, Options options) {
        prewarmChunks(world, x, z, options.initialChunkWarmupRadius);

        int preferredGroundY = estimatePreferredGroundY(world, x, z, options);
        Result best = tryFindSafeResult(world, x, z, preferredGroundY, options);
        if (best != null) {
            return best;
        }

        int fallbackY = player == null ? preferredGroundY : MathHelper.floor(player.posY);
        return new Result(x, fallbackY, z, false, true);
    }

    public static void prewarmChunks(World world, int x, int z, int radius) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.getChunk(chunkX + dx, chunkZ + dz);
            }
        }
    }

    private static Result tryFindSafeResult(World world, int originX, int originZ, int preferredGroundY, Options options) {
        Result best = findBestSafeResult(world, originX, originZ, preferredGroundY, options.initialHorizontalRadius, options);
        if (best != null) {
            return best;
        }

        best = findBestSafeResult(world, originX, originZ, preferredGroundY, options.horizontalAdjustRadius, options);
        if (best != null) {
            return best;
        }

        return findBestSafeResult(world, originX, originZ, preferredGroundY, options.landSearchRadius, options);
    }

    private static Result findBestSafeResult(World world, int originX, int originZ, int preferredGroundY, int maxRadius, Options options) {
        Result best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int ring = 0; ring <= maxRadius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (!isOnRing(ring, dx, dz)) {
                        continue;
                    }

                    Result candidate = evaluateCandidate(world, originX, originZ, preferredGroundY, ring, dx, dz, options);
                    if (candidate == null) {
                        continue;
                    }

                    int score = scoreCandidate(world, preferredGroundY, ring, candidate.x, candidate.y, candidate.z);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }

            if (best != null && ring >= options.earlyAcceptRadius) {
                return best;
            }
        }

        return best;
    }

    private static Result evaluateCandidate(World world, int originX, int originZ, int preferredGroundY, int ring, int dx, int dz, Options options) {
        int x = originX + dx;
        int z = originZ + dz;
        int surfaceY = getSurfaceY(world, x, z);
        if (surfaceY > preferredGroundY + options.maxSurfaceRiseFromPreferred) {
            return null;
        }

        int y = findCandidateY(world, x, z, preferredGroundY, surfaceY, options);
        if (y < 0) {
            return null;
        }

        return new Result(x, y, z, ring > 0, false);
    }

    private static int scoreCandidate(World world, int preferredGroundY, int ring, int x, int y, int z) {
        int surfaceY = getSurfaceY(world, x, z);
        int openness = countOpenSides(world, new BlockPos(x, y, z));

        // Prefer nearer, lower, more open candidates that stay close to the sampled ground baseline.
        return ring * 14 + Math.abs(y - preferredGroundY) * 10 + Math.abs(surfaceY - preferredGroundY) * 3 - openness * 6;
    }

    private static int findCandidateY(World world, int x, int z, int preferredGroundY, int surfaceY, Options options) {
        int baseY = MathHelper.clamp(surfaceY, preferredGroundY - options.maxBaseDrop, preferredGroundY + options.maxBaseRise);

        for (int offset = 0; offset <= options.searchUpSteps; offset++) {
            int y = baseY + offset;
            if (isPreferredSafeStand(world, x, y, z, options)) {
                return y;
            }
        }

        for (int offset = 1; offset <= options.searchDownSteps; offset++) {
            int y = baseY - offset;
            if (isPreferredSafeStand(world, x, y, z, options)) {
                return y;
            }
        }

        return -1;
    }

    private static int estimatePreferredGroundY(World world, int originX, int originZ, Options options) {
        List<Integer> heights = new ArrayList<>();
        collectGridSamples(world, originX, originZ, options, heights);
        collectRandomSamples(world, originX, originZ, options, heights);

        if (heights.isEmpty()) {
            return options.defaultPreferredGroundY;
        }

        Collections.sort(heights);
        int index = MathHelper.clamp((int) Math.floor((heights.size() - 1) * options.preferredHeightPercentile), 0, heights.size() - 1);
        int preferred = heights.get(index);
        return MathHelper.clamp(preferred, options.minPreferredGroundY, world.getActualHeight() - 8);
    }

    private static void collectGridSamples(World world, int originX, int originZ, Options options, List<Integer> heights) {
        for (int dx = -options.gridSampleRadius; dx <= options.gridSampleRadius; dx += options.gridSampleStep) {
            for (int dz = -options.gridSampleRadius; dz <= options.gridSampleRadius; dz += options.gridSampleStep) {
                heights.add(getSurfaceY(world, originX + dx, originZ + dz));
            }
        }
    }

    private static void collectRandomSamples(World world, int originX, int originZ, Options options, List<Integer> heights) {
        Random random = createSampleRandom(originX, originZ);
        for (int i = 0; i < options.randomSampleCount; i++) {
            int dx = random.nextInt(options.randomSampleRadius * 2 + 1) - options.randomSampleRadius;
            int dz = random.nextInt(options.randomSampleRadius * 2 + 1) - options.randomSampleRadius;
            heights.add(getSurfaceY(world, originX + dx, originZ + dz));
        }
    }

    private static Random createSampleRandom(int originX, int originZ) {
        long seed = (((long) originX) << 32) ^ (originZ & 0xffffffffL) ^ 0x5EED5AFECAFEL;
        return new Random(seed);
    }

    private static int getSurfaceY(World world, int x, int z) {
        return world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY() + 1;
    }

    public static boolean isSafeStand(World world, int x, int y, int z) {
        return isPreferredSafeStand(world, x, y, z, new Options());
    }

    private static boolean isPreferredSafeStand(World world, int x, int y, int z, Options options) {
        if (y <= 0 || y >= world.getActualHeight() - 2) {
            return false;
        }

        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = feetPos.up();
        BlockPos aboveHeadPos = headPos.up();
        BlockPos belowPos = feetPos.down();
        IBlockState below = world.getBlockState(belowPos);
        IBlockState feet = world.getBlockState(feetPos);
        IBlockState head = world.getBlockState(headPos);
        IBlockState aboveHead = world.getBlockState(aboveHeadPos);

        if (!isSolidGround(below) || !isPassable(feet) || !isPassable(head) || !isPassable(aboveHead)) {
            return false;
        }

        if (countOpenSides(world, feetPos) < options.requiredOpenSides) {
            return false;
        }

        return hasOpenCeiling(world, aboveHeadPos, options.requiredExtraHeadroom);
    }

    private static int countOpenSides(World world, BlockPos feetPos) {
        int openSides = 0;
        for (BlockPos side : new BlockPos[] { feetPos.north(), feetPos.south(), feetPos.east(), feetPos.west() }) {
            if (isSideOpen(world, side)) {
                openSides++;
            }
        }
        return openSides;
    }

    private static boolean isSideOpen(World world, BlockPos sidePos) {
        BlockPos headSidePos = sidePos.up();
        return isPassable(world.getBlockState(sidePos)) && isPassable(world.getBlockState(headSidePos));
    }

    private static boolean hasOpenCeiling(World world, BlockPos startPos, int extraHeadroom) {
        for (int i = 0; i < extraHeadroom; i++) {
            if (!isPassable(world.getBlockState(startPos.up(i)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSolidGround(IBlockState state) {
        return state.getMaterial().blocksMovement() && !state.getMaterial().isLiquid() && !state.getMaterial().isReplaceable();
    }

    private static boolean isPassable(IBlockState state) {
        return state.getMaterial().isReplaceable() || !state.getMaterial().blocksMovement();
    }

    private static boolean isOnRing(int ring, int dx, int dz) {
        return ring == 0 || Math.abs(dx) == ring || Math.abs(dz) == ring;
    }

    public static final class Options {
        public int defaultPreferredGroundY = 30;
        public int minPreferredGroundY = 8;
        public int initialHorizontalRadius = 2;
        public int horizontalAdjustRadius = 8;
        public int landSearchRadius = 48;
        public int earlyAcceptRadius = 6;
        public int searchUpSteps = 8;
        public int searchDownSteps = 4;
        public int maxBaseRise = 6;
        public int maxBaseDrop = 6;
        public int maxSurfaceRiseFromPreferred = 12;
        public int requiredOpenSides = 3;
        public int requiredExtraHeadroom = 2;
        public int initialChunkWarmupRadius = 1;
        public int gridSampleRadius = 24;
        public int gridSampleStep = 4;
        public int randomSampleRadius = 32;
        public int randomSampleCount = 96;
        public double preferredHeightPercentile = 0.35D;
    }

    public static final class Result {
        public final int x;
        public final int y;
        public final int z;
        public final boolean adjustedXZ;
        public final boolean fallback;

        public Result(int x, int y, int z, boolean adjustedXZ, boolean fallback) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.adjustedXZ = adjustedXZ;
            this.fallback = fallback;
        }
    }
}