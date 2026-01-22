package net.fretux.mindmotion.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import net.fretux.mindmotion.AscendMindMotion;

public class ModMessages {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AscendMindMotion.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;
    private static int nextId() { return id++; }

    public static void register() {
        CHANNEL.messageBuilder(SyncStatsS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatsS2CPacket::encode)
                .decoder(SyncStatsS2CPacket::decode)
                .consumerMainThread(SyncStatsS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(VentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(VentPacket::new)
                .encoder(VentPacket::toBytes)
                .consumerMainThread(VentPacket::handle)
                .add();
        CHANNEL.messageBuilder(VentClientEffectPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(VentClientEffectPacket::new)
                .encoder(VentClientEffectPacket::toBytes)
                .consumerMainThread(VentClientEffectPacket::handle)
                .add();
    }
}
