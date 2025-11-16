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
        if (event.getObject() instanceof net.minecraft.world.entity.player.Player player) {
            event.addCapability(new ResourceLocation("mindmotion", "sanity"), new SanityProvider());
            event.addCapability(new ResourceLocation("mindmotion", "tempo"), new TempoProvider());
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
            tag.putFloat("Sanity", instance.getSanity());
            tag.putFloat("Insanity", instance.getInsanity());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.setSanity(nbt.getFloat("Sanity"));
            instance.setInsanity(nbt.getFloat("Insanity"));
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
