package net.fretux.mindmotion.event;

import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonEvents {

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;

        player.getCapability(PlayerCapabilityProvider.SANITY).ifPresent(sanity -> sanity.reduceSanity(1));
        player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.addTempo(4));
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getEntity().getCapability(PlayerCapabilityProvider.SANITY).ifPresent(newCap ->
            event.getOriginal().getCapability(PlayerCapabilityProvider.SANITY)
                    .ifPresent(oldCap -> newCap.setSanity(oldCap.getSanity()))
        );
        event.getOriginal().invalidateCaps();
    }
}
