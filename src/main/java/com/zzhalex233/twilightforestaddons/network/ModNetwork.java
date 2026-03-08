package com.zzhalex233.twilightforestaddons.network;

import com.zzhalex233.twilightforestaddons.TwilightForestAddons;
import com.zzhalex233.twilightforestaddons.client.network.packet.PacketSyncAdvancedMapCenterHandler;
import com.zzhalex233.twilightforestaddons.network.packet.PacketSyncAdvancedMapCenter;
import com.zzhalex233.twilightforestaddons.network.packet.PacketTeleportToBossFeature;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(TwilightForestAddons.MODID);

    private ModNetwork() {
    }

    public static void initCommon() {
        CHANNEL.registerMessage(PacketTeleportToBossFeature.Handler.class, PacketTeleportToBossFeature.class, 0, Side.SERVER);
    }

    public static void initClient() {
        CHANNEL.registerMessage(PacketSyncAdvancedMapCenterHandler.class, PacketSyncAdvancedMapCenter.class, 1, Side.CLIENT);
    }
}