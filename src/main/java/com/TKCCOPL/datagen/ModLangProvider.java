package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLangProvider extends LanguageProvider {
    public ModLangProvider(PackOutput output) {
        super(output, cybercultivator.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.cybercultivator", "赛博农夫");

        addBlock(ModBlocks.SILICON_ORE, "硅晶矿");
        addBlock(ModBlocks.RARE_EARTH_ORE, "稀土矿");
        addBlock(ModBlocks.BIO_INCUBATOR, "生物培养槽");
        addBlock(ModBlocks.GENE_SPLICER, "基因拼接机");
        addBlock(ModBlocks.FIBER_REED_CROP, "纤维草");
        addBlock(ModBlocks.PROTEIN_SOY_CROP, "蛋白质豆");
        addBlock(ModBlocks.ALCOHOL_BLOOM_CROP, "酒精花");

        addItem(ModItems.SILICON_SHARD, "硅碎片");
        addItem(ModItems.RARE_EARTH_DUST, "稀土粉末");
        addItem(ModItems.PLANT_FIBER, "植物纤维");
        addItem(ModItems.BIOCHEMICAL_SOLUTION, "生化原液");
        addItem(ModItems.INDUSTRIAL_ETHANOL, "工业乙醇");
        addItem(ModItems.SYNAPTIC_NEURAL_BERRY, "突触神经莓");
        addItem(ModItems.SYNAPTIC_SERUM_S01, "S-01 突触超频血清");
        addItem(ModItems.FIBER_REED_SEEDS, "纤维草种子");
        addItem(ModItems.PROTEIN_SOY_SEEDS, "蛋白质豆种子");
        addItem(ModItems.ALCOHOL_BLOOM_SEEDS, "酒精花种子");
        addItem(ModItems.SPECTRUM_MONOCLE, "光谱单片镜");
        add("tooltip.cybercultivator.spectrum_monocle", "佩戴后可解析培养槽与种子基因信息");
        add("tooltip.cybercultivator.curios_loaded", "Curios: 已连接");
        add("tooltip.cybercultivator.curios_missing", "Curios: 未加载");
        add("tooltip.cybercultivator.seed_genes_hidden", "需要佩戴光谱单片镜以解析基因");
        add("tooltip.cybercultivator.gene_speed", "Gene_Speed: %s");
        add("tooltip.cybercultivator.gene_yield", "Gene_Yield: %s");
        add("tooltip.cybercultivator.gene_potency", "Gene_Potency: %s");
        add("tooltip.cybercultivator.bio_incubator", "放入种子后，可注入纯净水/营养液/数据信号进行培养");
        add("tooltip.cybercultivator.gene_splicer", "放入两颗基因种子后自动拼接；潜行可取回输入或输出");
        add("effect.cybercultivator.synaptic_overclock", "突触超频");
        add("effect.cybercultivator.neural_overload", "神经过载");
    }
}
