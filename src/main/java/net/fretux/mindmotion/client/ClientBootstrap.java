package net.fretux.mindmotion.client;

import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(Keybinds::register);
    }
}
