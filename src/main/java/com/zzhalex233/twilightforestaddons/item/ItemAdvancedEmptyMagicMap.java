package com.zzhalex233.twilightforestaddons.item;

import com.zzhalex233.twilightforestaddons.TwilightForestAddons;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMapBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import twilightforest.TFConfig;
import twilightforest.TFMagicMapData;

public class ItemAdvancedEmptyMagicMap extends ItemMapBase {
    public ItemAdvancedEmptyMagicMap() {
        setTranslationKey(TwilightForestAddons.MODID + ".advanced_empty_magic_map");
        setRegistryName(TwilightForestAddons.MODID, "advanced_empty_magic_map");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (world.provider.getDimension() != TFConfig.dimension.dimensionID) {
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        ItemStack advancedMap = ItemAdvancedMagicMap.createMapStack(world, player.posX, player.posZ, (byte) 4, true, false);
        TFMagicMapData mapData = ModItems.ADVANCED_MAGIC_MAP.getMapData(advancedMap, world);
        if (mapData != null) {
            NBTTagCompound tag = advancedMap.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                advancedMap.setTagCompound(tag);
            }
            tag.setInteger("advCenterX", mapData.xCenter);
            tag.setInteger("advCenterZ", mapData.zCenter);
            tag.setInteger("advDimension", mapData.d);
            tag.setByte("advScale", mapData.scale);
        }

        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            return new ActionResult<>(EnumActionResult.SUCCESS, advancedMap);
        }

        if (!player.inventory.addItemStackToInventory(advancedMap.copy())) {
            EntityItem dropped = player.dropItem(advancedMap, false);
            if (dropped != null) {
                dropped.setNoPickupDelay();
            }
        }

        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
    }
}