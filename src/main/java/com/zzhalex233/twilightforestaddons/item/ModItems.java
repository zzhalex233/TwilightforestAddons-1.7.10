package com.zzhalex233.twilightforestaddons.item;

import com.zzhalex233.twilightforestaddons.TwilightForestAddons;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = TwilightForestAddons.MODID)
public final class ModItems {
    public static final ItemAdvancedEmptyMagicMap ADVANCED_EMPTY_MAGIC_MAP = new ItemAdvancedEmptyMagicMap();
    public static final ItemAdvancedMagicMap ADVANCED_MAGIC_MAP = new ItemAdvancedMagicMap();

    private ModItems() {
    }

    public static void init() {
        TwilightForestAddons.LOGGER.info("Preparing item registry for {}.", TwilightForestAddons.MOD_NAME);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
            ADVANCED_EMPTY_MAGIC_MAP,
            ADVANCED_MAGIC_MAP
        );
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerSimpleModel(ADVANCED_EMPTY_MAGIC_MAP);
        registerMapModel(ADVANCED_MAGIC_MAP);
    }

    @SideOnly(Side.CLIENT)
    private static void registerSimpleModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
            item,
            0,
            new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }

    @SideOnly(Side.CLIENT)
    private static void registerMapModel(Item item) {
        ModelResourceLocation model = new ModelResourceLocation(item.getRegistryName(), "inventory");
        ModelLoader.setCustomMeshDefinition(item, stack -> model);
    }
}