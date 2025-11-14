package net.fretux.mindmotion;

import com.mojang.logging.LogUtils;
import net.fretux.mindmotion.event.CombatEvents;
import net.fretux.mindmotion.event.PlayerTickHandler;
import net.fretux.mindmotion.network.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
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
        MinecraftForge.EVENT_BUS.register(PlayerTickHandler.class);
        MinecraftForge.EVENT_BUS.register(CombatEvents.class);
        LOGGER.info("Ascend: Mind and Motion initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModMessages.register();
    }
}