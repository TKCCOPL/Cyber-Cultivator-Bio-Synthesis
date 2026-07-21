package com.TKCCOPL;

import com.TKCCOPL.effect.NeuralOverloadEffect;
import com.TKCCOPL.loot.ModLootModifiers;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModCreativeTabs;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.init.ModMenuTypes;
import com.TKCCOPL.client.screen.AtmosphericCondenserScreen;
import com.TKCCOPL.client.screen.BioIncubatorScreen;
import com.TKCCOPL.client.screen.GeneSplicerScreen;
import com.TKCCOPL.client.screen.SerumBottlerScreen;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.network.ModNetwork;
import com.TKCCOPL.network.S02DetectionSyncPacket;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(cybercultivator.MODID)
public class cybercultivator {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "cybercultivator";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public cybercultivator() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 网络通道必须在 commonSetup 阶段注册，早于任何同步数据包发送
        ModNetwork.register();
        LOGGER.info("Cyber-Cultivator common setup");
        LOGGER.info("Curios loaded: {}", CuriosCompat.isCuriosLoaded());
    }

    // Keep key entries visible in vanilla tabs for quick debugging in dev.
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.SILICON_ORE_ITEM);
            event.accept(ModItems.SILICON_BLOCK_ITEM);
            event.accept(ModItems.RARE_EARTH_ORE_ITEM);
            event.accept(ModItems.RARE_EARTH_BLOCK_ITEM);
            event.accept(ModItems.BIO_INCUBATOR_ITEM);
            event.accept(ModItems.GENE_SPLICER_ITEM);
            event.accept(ModItems.ATMOSPHERIC_CONDENSER_ITEM);
            event.accept(ModItems.SERUM_BOTTLER_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            NeuralOverloadEffect.cleanupByUUID(event.getEntity().getUUID());
        }
    }

    /**
     * 玩家换维度时清除 S-02 私有轮廓残留目标。新维度中旧实体 ID 全部失效，
     * 若饮用者在新世界仍有 S-02，下一次 60-tick 扫描会重新填充。
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            S02DetectionSyncPacket packet = new S02DetectionSyncPacket(new int[0]);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenuTypes.BIO_INCUBATOR.get(), BioIncubatorScreen::new);
                MenuScreens.register(ModMenuTypes.GENE_SPLICER.get(), GeneSplicerScreen::new);
                MenuScreens.register(ModMenuTypes.SERUM_BOTTLER.get(), SerumBottlerScreen::new);
                MenuScreens.register(ModMenuTypes.ATMOSPHERIC_CONDENSER.get(), AtmosphericCondenserScreen::new);
            });
        }
    }
}
