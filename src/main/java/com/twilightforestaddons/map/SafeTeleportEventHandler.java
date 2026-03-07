package com.twilightforestaddons.map;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class SafeTeleportEventHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = event.player;
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }

        SafeTeleport.tickScheduledCorrection(player);
    }
}
