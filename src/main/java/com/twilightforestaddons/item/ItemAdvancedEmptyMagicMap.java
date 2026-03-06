package com.twilightforestaddons.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMapBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import twilightforest.TFMagicMapData;
import twilightforest.TwilightForestMod;

public class ItemAdvancedEmptyMagicMap extends ItemMapBase {

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (world.provider.dimensionId != TwilightForestMod.dimensionID) {
            return itemStack;
        }

        ItemStack advancedMap = new ItemStack(ModItems.advancedMagicMap, 1, world.getUniqueDataId("magicmap"));
        String mapName = "magicmap_" + advancedMap.getItemDamage();
        TFMagicMapData mapData = new TFMagicMapData(mapName);
        world.setItemData(mapName, mapData);

        mapData.scale = 4;
        int mapSize = 128 * (1 << mapData.scale);
        mapData.xCenter = (int) Math.round(player.posX / mapSize) * mapSize;
        mapData.zCenter = (int) Math.round(player.posZ / mapSize) * mapSize;
        mapData.dimension = world.provider.dimensionId;
        mapData.markDirty();

        advancedMap.setTagCompound(
            advancedMap.getTagCompound() == null ? new net.minecraft.nbt.NBTTagCompound()
                : advancedMap.getTagCompound());
        advancedMap.getTagCompound()
            .setInteger("advCenterX", mapData.xCenter);
        advancedMap.getTagCompound()
            .setInteger("advCenterZ", mapData.zCenter);
        advancedMap.getTagCompound()
            .setInteger("advDimension", mapData.dimension);
        advancedMap.getTagCompound()
            .setByte("advScale", mapData.scale);

        itemStack.stackSize--;

        if (itemStack.stackSize <= 0) {
            return advancedMap;
        }

        if (!player.inventory.addItemStackToInventory(advancedMap.copy())) {
            EntityItem dropped = player.dropPlayerItemWithRandomChoice(advancedMap, false);
            if (dropped != null) {
                dropped.delayBeforeCanPickup = 0;
            }
        }

        return itemStack;
    }
}
