package com.zzhalex233.twilightforestaddons.proxy;

import com.zzhalex233.twilightforestaddons.Config;
import com.zzhalex233.twilightforestaddons.item.ModItems;
import com.zzhalex233.twilightforestaddons.network.ModNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy implements IProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ModItems.init();
        ModNetwork.initCommon();
    }

    @Override
    public void init(FMLInitializationEvent event) {
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Override
    public void toggleAdvancedMagicMapGui(EntityPlayer player, ItemStack itemStack) {
    }
}