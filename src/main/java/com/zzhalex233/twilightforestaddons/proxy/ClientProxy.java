package com.zzhalex233.twilightforestaddons.proxy;

import com.zzhalex233.twilightforestaddons.client.gui.GuiAdvancedMagicMap;
import com.zzhalex233.twilightforestaddons.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ModNetwork.initClient();
    }

    @Override
    public void toggleAdvancedMagicMapGui(EntityPlayer player, ItemStack itemStack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiAdvancedMagicMap) {
            minecraft.displayGuiScreen(null);
            return;
        }
        minecraft.displayGuiScreen(new GuiAdvancedMagicMap(itemStack.getMetadata()));
    }
}