package net.fretux.mindmotion.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = "mindmotion")
public class PlayerCapabilityProvider {
    public static final Capability<ISanity> SANITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<ITempo> TEMPO = CapabilityManager.get(new CapabilityToken<>() {
    });

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof net.minecraft.world.entity.player.Player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath("mindmotion", "sanity"), new SanityProvider());
            event.addCapability(ResourceLocation.fromNamespaceAndPath("mindmotion", "tempo"), new TempoProvider());
        }
    }

    private static class SanityProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final SanityCapability instance = new SanityCapability();
        private final LazyOptional<ISanity> optional = LazyOptional.of(() -> instance);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == SANITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("BaseMaxSanity", instance.getBaseMaxSanity());
            tag.putFloat("BonusMaxSanity", instance.getBonusMaxSanity());
            tag.putFloat("Sanity", instance.getSanity());
            tag.putFloat("Insanity", instance.getInsanity());
            tag.putFloat("BaseMaxMadness", instance.getBaseMaxMadness());
            tag.putFloat("BonusMaxMadness", instance.getBonusMaxMadness());
            tag.putFloat("BonusMadnessDecayPerTick", instance.getBonusMadnessDecayPerTick());
            tag.putFloat("Madness", instance.getMadness());
            tag.putInt("MadnessDecayDelayTicks", instance.getMadnessDecayDelayTicks());
            tag.putInt("MadnessStunTicks", instance.getMadnessStunTicks());
            tag.putBoolean("MadnessBacklashPending", instance.isMadnessBacklashPending());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains("BaseMaxSanity")) {
                instance.setBaseMaxSanity(nbt.getFloat("BaseMaxSanity"));
            }
            if (nbt.contains("BonusMaxSanity")) {
                instance.setBonusMaxSanity(nbt.getFloat("BonusMaxSanity"));
            }
            if (nbt.contains("BaseMaxMadness")) {
                instance.setBaseMaxMadness(nbt.getFloat("BaseMaxMadness"));
            }
            if (nbt.contains("BonusMaxMadness")) {
                instance.setBonusMaxMadness(nbt.getFloat("BonusMaxMadness"));
            }
            if (nbt.contains("BonusMadnessDecayPerTick")) {
                instance.setBonusMadnessDecayPerTick(nbt.getFloat("BonusMadnessDecayPerTick"));
            }
            instance.setSanity(nbt.getFloat("Sanity"));
            instance.setInsanity(nbt.getFloat("Insanity"));
            if (nbt.contains("Madness")) {
                instance.setMadness(nbt.getFloat("Madness"));
            }
            if (nbt.contains("MadnessDecayDelayTicks")) {
                instance.setMadnessDecayDelayTicks(nbt.getInt("MadnessDecayDelayTicks"));
            }
            if (nbt.contains("MadnessStunTicks")) {
                instance.setMadnessStunTicks(nbt.getInt("MadnessStunTicks"));
            }
            if (nbt.contains("MadnessBacklashPending")) {
                instance.setMadnessBacklashPending(nbt.getBoolean("MadnessBacklashPending"));
            }
        }
    }

    private static class TempoProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final TempoCapability instance = new TempoCapability();
        private final LazyOptional<ITempo> optional = LazyOptional.of(() -> instance);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == TEMPO ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Tempo", instance.getTempo());
            tag.putInt("VentCooldown", instance.getVentCooldown());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.setTempo(nbt.getInt("Tempo"));
            instance.setVentCooldown(nbt.getInt("VentCooldown"));
        }
    }
}
