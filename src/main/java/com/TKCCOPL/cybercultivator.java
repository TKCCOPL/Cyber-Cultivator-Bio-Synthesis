package com.TKCCOPL;

import com.TKCCOPL.advancement.ModTriggers;
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
import com.TKCCOPL.network.GameplayConfigSync;
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

        // 注册自定义进度触发器（CriteriaTriggers.register 必须在 mod 构造期间调用一次）
        ModTriggers.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        // SERVER 类型保证配置文件随世界走、避免多人服务器与本地客户端配置不一致
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
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
            event.accept(ModItems.DEEPSLATE_SILICON_ORE_ITEM);
            event.accept(ModItems.DEEPSLATE_RARE_EARTH_ORE_ITEM);
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

    /**
     * 玩家登录时把服务端配置快照推送到客户端，确保 Tooltip / JEI 立即用上权威值。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            GameplayConfigSync.sendTo(serverPlayer);
        }
    }

    /**
     * 玩家换维度时：
     * 1. 清除 S-02 私有轮廓残留目标（新维度中旧实体 ID 全部失效）；
     * 2. 重新推送配置快照（防御性同步）。
     * 若饮用者在新世界仍有 S-02，下一轮按等级调节的扫描会重新填充。
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            clearS02Targets(serverPlayer);
            GameplayConfigSync.sendTo(serverPlayer);
        }
    }

    /**
     * 玩家死亡重生时清除 S-02 私有轮廓残留目标。
     * <p>重生不触发 PlayerLoggedOutEvent（连接保持），客户端状态集合不会被自动清空，
     * 旧实体 ID 可能被新生物重新利用导致饮用者看到错误的高亮目标。
     * 若重生后仍持有 S-02 效果，下一轮扫描会重新填充合法目标。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            clearS02Targets(serverPlayer);
            GameplayConfigSync.sendTo(serverPlayer);
        }
    }

    /**
     * 统一入口：向指定玩家发送空 S-02 侦测列表，清空其客户端的目标集合。
     * 登录、换维度、重生共用此逻辑，避免重复构造空数据包。
     */
    private static void clearS02Targets(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S02DetectionSyncPacket(new int[0]));
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
