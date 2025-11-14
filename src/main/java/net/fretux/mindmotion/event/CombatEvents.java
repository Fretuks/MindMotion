package net.fretux.mindmotion.event;

import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class CombatEvents {

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.addTempo(3));
            PlayerTickHandler.markCombat(player);
        }

        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player attacker) {
            attacker.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempo -> tempo.addTempo(4));
            PlayerTickHandler.markCombat(attacker);
        }
    }
}
