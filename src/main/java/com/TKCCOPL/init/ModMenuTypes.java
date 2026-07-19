package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import com.TKCCOPL.menu.BioIncubatorMenu;
import com.TKCCOPL.menu.GeneSplicerMenu;
import com.TKCCOPL.menu.SerumBottlerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, cybercultivator.MODID);

    public static final RegistryObject<MenuType<BioIncubatorMenu>> BIO_INCUBATOR =
            MENUS.register("bio_incubator", () -> IForgeMenuType.create(BioIncubatorMenu::new));
    public static final RegistryObject<MenuType<GeneSplicerMenu>> GENE_SPLICER =
            MENUS.register("gene_splicer", () -> IForgeMenuType.create(GeneSplicerMenu::new));
    public static final RegistryObject<MenuType<SerumBottlerMenu>> SERUM_BOTTLER =
            MENUS.register("serum_bottler", () -> IForgeMenuType.create(SerumBottlerMenu::new));
    public static final RegistryObject<MenuType<AtmosphericCondenserMenu>> ATMOSPHERIC_CONDENSER =
            MENUS.register("atmospheric_condenser", () -> IForgeMenuType.create(AtmosphericCondenserMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
