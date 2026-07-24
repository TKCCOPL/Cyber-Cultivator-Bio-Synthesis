package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * v1.1.7 §10.1 物品标签 Provider。
 *
 * <p>生成两类标签（与 {@link ModTags} 对应）：</p>
 * <ul>
 *   <li>跨模组材料标签（§10.2）：将本模组的硅/稀土物品归并到 {@code forge:*} 父标签</li>
 *   <li>本模组语义标签（§10.3）：机器输入验证用的命名空间标签，默认仅包含原物品</li>
 * </ul>
 */
public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              BlockTagsProvider blockTagsProvider,
                              ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTagsProvider.contentsGetter(), cybercultivator.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // === §10.2 跨模组材料标签 ===

        // 硅链路
        tag(ModTags.ForgeItems.SILICON).add(ModItems.SILICON_SHARD.get());
        tag(ModTags.ForgeItems.ORES_SILICON).add(
                ModItems.SILICON_ORE_ITEM.get(),
                ModItems.DEEPSLATE_SILICON_ORE_ITEM.get());
        tag(ModTags.ForgeItems.RAW_MATERIALS_SILICON).add(ModItems.RAW_SILICON_CRYSTAL.get());
        tag(ModTags.ForgeItems.STORAGE_BLOCKS_RAW_SILICON).add(ModItems.RAW_SILICON_BLOCK_ITEM.get());
        tag(ModTags.ForgeItems.STORAGE_BLOCKS_SILICON).add(ModItems.SILICON_BLOCK_ITEM.get());

        // 稀土链路
        tag(ModTags.ForgeItems.ORES_RARE_EARTH).add(
                ModItems.RARE_EARTH_ORE_ITEM.get(),
                ModItems.DEEPSLATE_RARE_EARTH_ORE_ITEM.get());
        tag(ModTags.ForgeItems.RAW_MATERIALS_RARE_EARTH).add(ModItems.RAW_RARE_EARTH.get());
        tag(ModTags.ForgeItems.DUSTS_RARE_EARTH).add(ModItems.RARE_EARTH_DUST.get());
        tag(ModTags.ForgeItems.STORAGE_BLOCKS_RAW_RARE_EARTH).add(ModItems.RAW_RARE_EARTH_BLOCK_ITEM.get());
        tag(ModTags.ForgeItems.STORAGE_BLOCKS_RARE_EARTH).add(ModItems.RARE_EARTH_BLOCK_ITEM.get());

        // 父标签（包含上述所有子标签，让第三方模组的硅/稀土物品自动归并到父标签）
        tag(ModTags.ForgeItems.ORES)
                .addTag(ModTags.ForgeItems.ORES_SILICON)
                .addTag(ModTags.ForgeItems.ORES_RARE_EARTH);
        tag(ModTags.ForgeItems.RAW_MATERIALS)
                .addTag(ModTags.ForgeItems.RAW_MATERIALS_SILICON)
                .addTag(ModTags.ForgeItems.RAW_MATERIALS_RARE_EARTH);
        tag(ModTags.ForgeItems.DUSTS)
                .addTag(ModTags.ForgeItems.DUSTS_RARE_EARTH);
        tag(ModTags.ForgeItems.STORAGE_BLOCKS)
                .addTag(ModTags.ForgeItems.STORAGE_BLOCKS_RAW_SILICON)
                .addTag(ModTags.ForgeItems.STORAGE_BLOCKS_SILICON)
                .addTag(ModTags.ForgeItems.STORAGE_BLOCKS_RAW_RARE_EARTH)
                .addTag(ModTags.ForgeItems.STORAGE_BLOCKS_RARE_EARTH);

        // === §10.3 本模组语义标签 ===

        // 遗传种子：用于拼接机和培养槽自动输入验证
        tag(ModTags.SemanticItems.GENETIC_SEEDS).add(
                ModItems.FIBER_REED_SEEDS.get(),
                ModItems.PROTEIN_SOY_SEEDS.get(),
                ModItems.ALCOHOL_BLOOM_SEEDS.get()
        );

        // 培养槽三通道合法输入（默认仅包含原物品；整合包可通过数据包扩展）
        tag(ModTags.SemanticItems.INCUBATOR_NUTRITION).add(ModItems.BIOCHEMICAL_SOLUTION.get());
        tag(ModTags.SemanticItems.INCUBATOR_PURITY).add(ModItems.PURIFIED_WATER_BOTTLE.get());
        tag(ModTags.SemanticItems.INCUBATOR_DATA_SIGNAL).add(ModItems.SILICON_SHARD.get());
    }
}
