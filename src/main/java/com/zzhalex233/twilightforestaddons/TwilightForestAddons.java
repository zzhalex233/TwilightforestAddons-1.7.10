package com.zzhalex233.twilightforestaddons;

import com.zzhalex233.twilightforestaddons.proxy.IProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = TwilightForestAddons.MODID,
    name = TwilightForestAddons.MOD_NAME,
    version = TwilightForestAddons.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:twilightforest"
)
public final class TwilightForestAddons {
    public static final String MODID = "twilightforestaddons";
    public static final String MOD_NAME = "Twilight Forest Addons";
    public static final String VERSION = "1.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @SidedProxy(
        modId = MODID,
        clientSide = "com.zzhalex233.twilightforestaddons.proxy.ClientProxy",
        serverSide = "com.zzhalex233.twilightforestaddons.proxy.CommonProxy"
    )
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Pre-initializing {} {}", MOD_NAME, VERSION);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}