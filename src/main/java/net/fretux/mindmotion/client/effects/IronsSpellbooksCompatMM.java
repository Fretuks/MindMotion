package net.fretux.mindmotion.client.effects;

import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.player.PlayerCapabilityProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class IronsSpellbooksCompatMM {
    private static final UUID TEMPO_MANA_REGEN_UUID =
            UUID.fromString("de4f16ad-73b3-4b99-bf77-44c9878222aa");
    private static final String IRONS_MODID = "irons_spellbooks";
    private static final String ATTR_REGISTRY_CLASS = "io.redspace.ironsspellbooks.api.registry.AttributeRegistry";
    private static final String MANA_REGEN_FIELD = "MANA_REGEN";
    private static boolean resolved = false;
    private static boolean available = false;
    private static Object manaRegenHolder = null;
    private static Method holderGetMethod = null;

    private IronsSpellbooksCompatMM() {
    }

    public static int getTempoPerPercent() {
        return ConfigMM.COMMON.TEMPO_PER_MANA_REGEN_PERCENT.get();
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(IRONS_MODID);
    }

    public static void applyTempo(Player player) {
        if (!isLoaded()) return;
        resolveOnce();
        if (!available) return;
        player.getCapability(PlayerCapabilityProvider.TEMPO).ifPresent(tempoCap -> {
            int tempo = tempoCap.getTempo();
            applyTempoToManaRegen(player, tempo);
        });
    }

    private static void resolveOnce() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> attrReg = Class.forName(ATTR_REGISTRY_CLASS);
            Field manaRegenField = attrReg.getField(MANA_REGEN_FIELD);
            manaRegenHolder = manaRegenField.get(null);
            if (manaRegenHolder == null) return;
            holderGetMethod = manaRegenHolder.getClass().getMethod("get");
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static void applyTempoToManaRegen(Player player, int tempo) {
        if (!available) return;
        Attribute manaRegenAttr = getManaRegenAttribute();
        if (manaRegenAttr == null) return;
        AttributeInstance inst = player.getAttribute(manaRegenAttr);
        if (inst == null) return;
        inst.removeModifier(TEMPO_MANA_REGEN_UUID);
        if (tempo <= 0) return;
        double bonusPercent = (double) tempo / getTempoPerPercent();
        double multiplier = bonusPercent / 100.0;
        inst.addTransientModifier(new AttributeModifier(
                TEMPO_MANA_REGEN_UUID,
                "MindMotion Tempo mana regen",
                multiplier,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
    }

    private static Attribute getManaRegenAttribute() {
        try {
            Object attrObj = holderGetMethod.invoke(manaRegenHolder);
            return (attrObj instanceof Attribute a) ? a : null;
        } catch (Throwable t) {
            return null;
        }
    }
}