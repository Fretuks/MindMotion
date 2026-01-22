package net.fretux.mindmotion;

import com.mojang.logging.LogUtils;
import net.fretux.mindmotion.client.Keybinds;
import net.fretux.mindmotion.command.SanityCommand;
import net.fretux.mindmotion.event.CombatEvents;
import net.fretux.mindmotion.event.PlayerTickHandler;
import net.fretux.mindmotion.network.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AscendMindMotion.MODID)
public class AscendMindMotion {
    public static final String MODID = "mindmotion";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AscendMindMotion() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(Keybinds::register);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PlayerTickHandler.class);
        MinecraftForge.EVENT_BUS.register(CombatEvents.class);
        MinecraftForge.EVENT_BUS.register(SanityCommand.class);
        LOGGER.info("Ascend: Mind and Motion initialized!");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigMM.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigMM.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModMessages.register();
    }
}