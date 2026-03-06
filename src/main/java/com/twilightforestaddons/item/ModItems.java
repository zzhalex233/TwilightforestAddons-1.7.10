package com.twilightforestaddons.item;

import net.minecraft.creativetab.CreativeTabs;

import com.twilightforestaddons.TwilightForestAddons;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static ItemAdvancedEmptyMagicMap advancedEmptyMagicMap;
    public static ItemAdvancedMagicMap advancedMagicMap;

    private ModItems() {}

    public static void init() {
        advancedEmptyMagicMap = new ItemAdvancedEmptyMagicMap();
        advancedEmptyMagicMap.setUnlocalizedName("advancedEmptyMagicMap");
        advancedEmptyMagicMap.setTextureName("TwilightForest:emptyMagicMap");
        advancedEmptyMagicMap.setCreativeTab(CreativeTabs.tabMisc);

        advancedMagicMap = new ItemAdvancedMagicMap();
        advancedMagicMap.setUnlocalizedName("advancedMagicMap");
        advancedMagicMap.setCreativeTab(CreativeTabs.tabMisc);

        GameRegistry.registerItem(advancedEmptyMagicMap, "advanced_empty_magic_map");
        GameRegistry.registerItem(advancedMagicMap, "advanced_magic_map");

        TwilightForestAddons.LOG.info("Registered advanced magic map items.");
    }
}
