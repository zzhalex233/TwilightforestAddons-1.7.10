package com.twilightforestaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.twilightforestaddons.client.gui.GuiAdvancedMagicMap;
import com.twilightforestaddons.client.network.ClientNetwork;
import com.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer;
import com.twilightforestaddons.item.ModItems;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientNetwork.initClient();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getTextureManager() == null) {
            TwilightForestAddons.LOG
                .warn("TextureManager not ready, skipping advanced magic map renderer registration.");
            return;
        }
        AdvancedMagicMapItemRenderer mapRenderer = new AdvancedMagicMapItemRenderer();
        MinecraftForgeClient.registerItemRenderer(ModItems.advancedMagicMap, mapRenderer);
    }

    @Override
    public void toggleAdvancedMagicMapGui(EntityPlayer player, ItemStack itemStack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiAdvancedMagicMap) {
            minecraft.displayGuiScreen(null);
            return;
        }

        minecraft.displayGuiScreen(new GuiAdvancedMagicMap(itemStack.getItemDamage()));
    }
}
