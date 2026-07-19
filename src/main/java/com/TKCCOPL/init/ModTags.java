package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * v1.1.7 §10 材料与语义标签集中入口。
 *
 * <p>包含两类标签：</p>
 * <ul>
 *   <li>跨模组材料标签（§10.2）：归并到 {@code forge:*} 命名空间，与 Forge 通用标签约定一致，
 *       支持整合包通过数据包扩展机器输入。</li>
 *   <li>本模组语义标签（§10.3）：归并到 {@code cybercultivator:} 命名空间，
 *       用于机器输入验证（§10.4）。默认只包含当前原物品。</li>
 * </ul>
 */
public final class ModTags {

    private ModTags() {}

    // === Forge 通用材料标签（§10.2） ===

    public static final class ForgeItems {
        private ForgeItems() {}

        public static final TagKey<Item> SILICON = forge("silicon");
        public static final TagKey<Item> ORES_SILICON = forge("ores/silicon");
        public static final TagKey<Item> RAW_MATERIALS_SILICON = forge("raw_materials/silicon");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_SILICON = forge("storage_blocks/raw_silicon");
        public static final TagKey<Item> STORAGE_BLOCKS_SILICON = forge("storage_blocks/silicon");

        public static final TagKey<Item> ORES_RARE_EARTH = forge("ores/rare_earth");
        public static final TagKey<Item> RAW_MATERIALS_RARE_EARTH = forge("raw_materials/rare_earth");
        public static final TagKey<Item> DUSTS_RARE_EARTH = forge("dusts/rare_earth");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_RARE_EARTH = forge("storage_blocks/raw_rare_earth");
        public static final TagKey<Item> STORAGE_BLOCKS_RARE_EARTH = forge("storage_blocks/rare_earth");

        // 父标签
        public static final TagKey<Item> ORES = forge("ores");
        public static final TagKey<Item> RAW_MATERIALS = forge("raw_materials");
        public static final TagKey<Item> DUSTS = forge("dusts");
        public static final TagKey<Item> STORAGE_BLOCKS = forge("storage_blocks");

        private static TagKey<Item> forge(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path));
        }
    }

    public static final class ForgeBlocks {
        private ForgeBlocks() {}

        public static final TagKey<Block> ORES_SILICON = forge("ores/silicon");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_SILICON = forge("storage_blocks/raw_silicon");
        public static final TagKey<Block> STORAGE_BLOCKS_SILICON = forge("storage_blocks/silicon");

        public static final TagKey<Block> ORES_RARE_EARTH = forge("ores/rare_earth");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_RARE_EARTH = forge("storage_blocks/raw_rare_earth");
        public static final TagKey<Block> STORAGE_BLOCKS_RARE_EARTH = forge("storage_blocks/rare_earth");

        // 父标签
        public static final TagKey<Block> ORES = forge("ores");
        public static final TagKey<Block> STORAGE_BLOCKS = forge("storage_blocks");

        private static TagKey<Block> forge(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", path));
        }
    }

    // === 本模组语义标签（§10.3） ===

    public static final class SemanticItems {
        private SemanticItems() {}

        /** 遗传种子（用于拼接机自动输入验证）。 */
        public static final TagKey<Item> GENETIC_SEEDS = mod("genetic_seeds");

        /** 培养槽营养通道合法输入。 */
        public static final TagKey<Item> INCUBATOR_NUTRITION = mod("machine_inputs/incubator/nutrition");

        /** 培养槽纯度通道合法输入。 */
        public static final TagKey<Item> INCUBATOR_PURITY = mod("machine_inputs/incubator/purity");

        /** 培养槽数据信号通道合法输入。 */
        public static final TagKey<Item> INCUBATOR_DATA_SIGNAL = mod("machine_inputs/incubator/data_signal");

        private static TagKey<Item> mod(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, path));
        }
    }

    public static final class SemanticBlocks {
        private SemanticBlocks() {}

        // 当前无方块语义标签，占位以便未来扩展
    }
}
