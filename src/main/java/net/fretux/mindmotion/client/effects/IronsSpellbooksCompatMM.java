package net.fretux.mindmotion.client.effects;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

public final class IronsSpellbooksCompatMM {
    public static final double TEMPO_PER_PERCENT = 3;
    private static final UUID TEMPO_MANA_REGEN_UUID =
            UUID.fromString("de4f16ad-73b3-4b99-bf77-44c9878222aa");

    private IronsSpellbooksCompatMM() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded("irons_spellbooks");
    }
    public static void applyTempo(Player player) {
        if (!isLoaded()) return;
        player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempoCap -> {
            int tempo = tempoCap.getTempo();
            applyTempoToManaRegen(player, tempo);
        });
    }

    public static void applyTempoToManaRegen(Player player, int tempo) {
        Attribute manaRegenAttr = (Attribute) AttributeRegistry.MANA_REGEN.get();
        if (manaRegenAttr == null) return;
        AttributeInstance inst = player.getAttribute(manaRegenAttr);
        if (inst == null) return;
        inst.removeModifier(TEMPO_MANA_REGEN_UUID);
        if (tempo <= 0) {
            return;
        }
        double bonusPercent = (double) tempo / TEMPO_PER_PERCENT;
        double multiplier = bonusPercent / 100.0;
        inst.addTransientModifier(new AttributeModifier(
                TEMPO_MANA_REGEN_UUID,
                "MindMotion Tempo mana regen",
                multiplier,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
    }
}