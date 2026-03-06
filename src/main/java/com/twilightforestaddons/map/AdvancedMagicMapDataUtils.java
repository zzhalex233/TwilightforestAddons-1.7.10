package com.twilightforestaddons.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.minecraft.world.storage.MapData.MapCoord;

import twilightforest.TFMagicMapData;

public final class AdvancedMagicMapDataUtils {

    private AdvancedMagicMapDataUtils() {}

    public static void shiftMapContent(TFMagicMapData mapData, int shiftXPixels, int shiftZPixels, boolean markDirtyEdges) {
        if (mapData == null || (shiftXPixels == 0 && shiftZPixels == 0)) {
            return;
        }

        if (Math.abs(shiftXPixels) >= 128 || Math.abs(shiftZPixels) >= 128) {
            Arrays.fill(mapData.colors, (byte) 0);
            mapData.featuresVisibleOnMap.clear();
            if (markDirtyEdges) {
                for (int x = 0; x < 128; x++) {
                    mapData.setColumnDirty(x, 0, 127);
                }
            }
            return;
        }

        byte[] oldColors = Arrays.copyOf(mapData.colors, mapData.colors.length);
        Arrays.fill(mapData.colors, (byte) 0);

        for (int z = 0; z < 128; z++) {
            int srcZ = z + shiftZPixels;
            if (srcZ < 0 || srcZ >= 128) {
                continue;
            }
            for (int x = 0; x < 128; x++) {
                int srcX = x + shiftXPixels;
                if (srcX < 0 || srcX >= 128) {
                    continue;
                }
                mapData.colors[x + z * 128] = oldColors[srcX + srcZ * 128];
            }
        }

        if (markDirtyEdges) {
            markDirtyEdges(mapData, shiftXPixels, shiftZPixels);
        }

        int shiftFeatureX = shiftXPixels << 1;
        int shiftFeatureZ = shiftZPixels << 1;
        Iterator<MapCoord> iterator = mapData.featuresVisibleOnMap.iterator();
        while (iterator.hasNext()) {
            MapCoord coord = iterator.next();
            int shiftedX = coord.centerX - shiftFeatureX;
            int shiftedZ = coord.centerZ - shiftFeatureZ;
            if (Math.abs(shiftedX) > 126 || Math.abs(shiftedZ) > 126) {
                iterator.remove();
            } else {
                coord.centerX = (byte) shiftedX;
                coord.centerZ = (byte) shiftedZ;
            }
        }
    }

    public static void dedupeFeaturesInPlace(List<MapCoord> features) {
        if (features == null) {
            return;
        }

        for (int i = 0; i < features.size(); i++) {
            MapCoord a = features.get(i);
            for (int j = features.size() - 1; j > i; j--) {
                MapCoord b = features.get(j);
                if (isDuplicate(a, b)) {
                    features.remove(j);
                }
            }
        }
    }

    public static List<MapCoord> createDedupedFeatureSnapshot(TFMagicMapData mapData) {
        List<MapCoord> snapshot = new ArrayList<MapCoord>();
        if (mapData == null) {
            return snapshot;
        }

        for (MapCoord coord : mapData.featuresVisibleOnMap) {
            if (coord == null || containsDuplicate(snapshot, coord)) {
                continue;
            }
            snapshot.add(mapData.new MapCoord(coord.iconSize, coord.centerX, coord.centerZ, coord.iconRotation));
        }
        return snapshot;
    }

    public static boolean isDuplicate(MapCoord a, MapCoord b) {
        if (a == null || b == null || a.iconSize != b.iconSize) {
            return false;
        }

        int dx = a.centerX - b.centerX;
        int dz = a.centerZ - b.centerZ;
        return dx * dx + dz * dz <= 25;
    }

    private static boolean containsDuplicate(List<MapCoord> features, MapCoord target) {
        for (MapCoord coord : features) {
            if (isDuplicate(coord, target)) {
                return true;
            }
        }
        return false;
    }

    private static void markDirtyEdges(TFMagicMapData mapData, int shiftXPixels, int shiftZPixels) {
        if (shiftXPixels > 0) {
            int start = 128 - shiftXPixels;
            for (int x = Math.max(0, start); x < 128; x++) {
                mapData.setColumnDirty(x, 0, 127);
            }
        } else if (shiftXPixels < 0) {
            int end = -shiftXPixels;
            for (int x = 0; x < Math.min(128, end); x++) {
                mapData.setColumnDirty(x, 0, 127);
            }
        }

        if (shiftZPixels > 0) {
            int start = Math.max(0, 128 - shiftZPixels);
            for (int x = 0; x < 128; x++) {
                mapData.setColumnDirty(x, start, 127);
            }
        } else if (shiftZPixels < 0) {
            int end = Math.min(127, -shiftZPixels - 1);
            for (int x = 0; x < 128; x++) {
                mapData.setColumnDirty(x, 0, end);
            }
        }
    }
}
