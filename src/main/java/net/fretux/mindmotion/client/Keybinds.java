package net.fretux.mindmotion.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraft.client.KeyMapping;

public class Keybinds {

    public static KeyMapping VENT_KEY;

    public static void register(RegisterKeyMappingsEvent event) {
        VENT_KEY = new KeyMapping(
                "key.mindmotion.vent",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_G,
                "key.categories.mindmotion"
        );

        event.register(VENT_KEY);
    }
}
