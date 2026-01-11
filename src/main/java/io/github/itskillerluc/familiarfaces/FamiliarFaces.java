package io.github.itskillerluc.familiarfaces;

import io.github.itskillerluc.familiarfaces.client.config.FamiliarFacesConfigScreen;
import io.github.itskillerluc.familiarfaces.server.config.Config;
import io.github.itskillerluc.familiarfaces.server.init.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FamiliarFaces.MODID)
public class FamiliarFaces {

    public static final String MODID = "familiar_faces";

    public FamiliarFaces(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        SoundEventRegistry.SOUND_EVENTS.register(modEventBus);
        EntityTypeRegistry.ENTITY_TYPES.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        MemoryModuleTypeRegistry.MEMORY_MODULE_TYPES.register(modEventBus);
        SensorTypeRegistry.SENSOR_TYPES.register(modEventBus);
        ParticleTypeRegistry.PARTICLES.register(modEventBus);
        MobEffectRegistry.MOB_EFFECTS.register(modEventBus);
        PotionRegistry.POTIONS.register(modEventBus);

        context.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> FamiliarFacesConfigScreen.createScreen(screen)))
        );
    }
}