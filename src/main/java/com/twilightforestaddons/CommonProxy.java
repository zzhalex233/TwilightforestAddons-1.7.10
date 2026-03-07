package com.twilightforestaddons;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.map.SafeTeleportEventHandler;
import com.twilightforestaddons.network.ModNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ModItems.init();
        ModNetwork.initCommon();

        TwilightForestAddons.LOG.info(Config.greeting);
        TwilightForestAddons.LOG.info("I am Twilight Forest Addons at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new SafeTeleportEventHandler());
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}

    public void toggleAdvancedMagicMapGui(EntityPlayer player, ItemStack itemStack) {}
}
