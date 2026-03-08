package com.zzhalex233.twilightforestaddons.map;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import twilightforest.TFMagicMapData;

public final class AdvancedMagicMapDataUtils {
    private static final Field FEATURE_ID_FIELD = resolveField(
        TFMagicMapData.TFMapDecoration.class,
        "Twilight Forest map decoration feature id field",
        "featureId"
    );
    private static final Field MAP_DIMENSION_FIELD = resolveField(
        TFMagicMapData.class,
        "Twilight Forest magic map dimension field",
        "dimension",
        "d",
        "field_76200_c"
    );

    private AdvancedMagicMapDataUtils() {
    }

    public static void shiftMapContent(TFMagicMapData mapData, int shiftXPixels, int shiftZPixels, boolean markDirtyEdges) {
        if (mapData == null || (shiftXPixels == 0 && shiftZPixels == 0)) {
            return;
        }

        if (Math.abs(shiftXPixels) >= 128 || Math.abs(shiftZPixels) >= 128) {
            Arrays.fill(mapData.colors, (byte) 0);
            mapData.tfDecorations.clear();
            if (markDirtyEdges) {
                mapData.markDirty();
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
            mapData.markDirty();
        }

        int shiftDecorationX = shiftXPixels << 1;
        int shiftDecorationY = shiftZPixels << 1;
        Set<TFMagicMapData.TFMapDecoration> shiftedDecorations = new LinkedHashSet<>();
        for (TFMagicMapData.TFMapDecoration decoration : mapData.tfDecorations) {
            if (decoration == null) {
                continue;
            }

            int shiftedX = decoration.getX() - shiftDecorationX;
            int shiftedY = decoration.getY() - shiftDecorationY;
            if (Math.abs(shiftedX) > 126 || Math.abs(shiftedY) > 126) {
                continue;
            }

            shiftedDecorations.add(new TFMagicMapData.TFMapDecoration(
                getFeatureId(decoration),
                (byte) shiftedX,
                (byte) shiftedY,
                decoration.getRotation()
            ));
        }

        mapData.tfDecorations.clear();
        mapData.tfDecorations.addAll(shiftedDecorations);
    }

    public static void dedupeFeaturesInPlace(Set<TFMagicMapData.TFMapDecoration> decorations) {
        if (decorations == null || decorations.isEmpty()) {
            return;
        }

        Set<TFMagicMapData.TFMapDecoration> deduped = new LinkedHashSet<>();
        for (TFMagicMapData.TFMapDecoration decoration : decorations) {
            if (decoration == null || containsDuplicate(deduped, decoration)) {
                continue;
            }
            deduped.add(decoration);
        }

        decorations.clear();
        decorations.addAll(deduped);
    }

    public static int getFeatureId(TFMagicMapData.TFMapDecoration decoration) {
        return getIntField(FEATURE_ID_FIELD, decoration, "Twilight Forest map decoration feature id");
    }

    public static int getMapDimension(TFMagicMapData mapData) {
        return getIntField(MAP_DIMENSION_FIELD, mapData, "Twilight Forest magic map dimension");
    }

    public static void setMapDimension(TFMagicMapData mapData, int dimension) {
        setIntField(MAP_DIMENSION_FIELD, mapData, dimension, "Twilight Forest magic map dimension");
    }

    public static boolean isDuplicate(TFMagicMapData.TFMapDecoration a, TFMagicMapData.TFMapDecoration b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getImage() != b.getImage()) {
            return false;
        }
        if (getFeatureId(a) != getFeatureId(b)) {
            return false;
        }

        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy <= 25;
    }

    private static boolean containsDuplicate(Set<TFMagicMapData.TFMapDecoration> decorations, TFMagicMapData.TFMapDecoration target) {
        for (TFMagicMapData.TFMapDecoration decoration : decorations) {
            if (isDuplicate(decoration, target)) {
                return true;
            }
        }
        return false;
    }

    private static int getIntField(Field field, Object target, String description) {
        try {
            return field.getInt(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read " + description + ".", e);
        }
    }

    private static void setIntField(Field field, Object target, int value, String description) {
        try {
            field.setInt(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to write " + description + ".", e);
        }
    }

    private static Field resolveField(Class<?> owner, String description, String... candidateNames) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (String candidateName : candidateNames) {
                try {
                    Field field = current.getDeclaredField(candidateName);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        throw new IllegalStateException("Unable to resolve " + description + ".");
    }
}