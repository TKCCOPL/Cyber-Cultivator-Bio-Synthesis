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
        addBlock(ModBlocks.ATMOSPHERIC_CONDENSER, "大气冷凝器");
        addBlock(ModBlocks.SERUM_BOTTLER, "血清灌装机");
        addBlock(ModBlocks.FIBER_REED_CROP, "纤维草");
        addBlock(ModBlocks.PROTEIN_SOY_CROP, "蛋白质豆");
        addBlock(ModBlocks.ALCOHOL_BLOOM_CROP, "酒精花");

        addItem(ModItems.SILICON_SHARD, "硅碎片");
        addItem(ModItems.RARE_EARTH_DUST, "稀土粉末");
        addItem(ModItems.PLANT_FIBER, "植物纤维");
        addItem(ModItems.BIOCHEMICAL_SOLUTION, "生化原液");
        addItem(ModItems.INDUSTRIAL_ETHANOL, "工业乙醇");
        addItem(ModItems.PURIFIED_WATER_BOTTLE, "纯净水瓶");
        addItem(ModItems.SYNAPTIC_NEURAL_BERRY, "突触神经莓");
        addItem(ModItems.SYNAPTIC_SERUM_S01, "S-01 突触超频血清");
        addItem(ModItems.SYNAPTIC_SERUM_S02, "S-02 视觉强化血清");
        addItem(ModItems.SYNAPTIC_SERUM_S03, "S-03 代谢加速血清");
        addItem(ModItems.FIBER_REED_SEEDS, "纤维草种子");
        addItem(ModItems.PROTEIN_SOY_SEEDS, "蛋白质豆种子");
        addItem(ModItems.ALCOHOL_BLOOM_SEEDS, "酒精花种子");
        addItem(ModItems.SPECTRUM_MONOCLE, "光谱单片镜");
        addItem(ModItems.BIO_PULSE_BELT, "生化脉冲腰带");
        addItem(ModItems.LIFE_SUPPORT_PACK, "生命支持箱");
        add("tooltip.cybercultivator.spectrum_monocle", "佩戴后可解析培养槽与种子基因信息");
        add("tooltip.cybercultivator.curios_loaded", "Curios: 已连接");
        add("tooltip.cybercultivator.curios_missing", "Curios: 未加载");
        add("tooltip.cybercultivator.seed_genes_hidden", "需要佩戴光谱单片镜以解析基因");
        add("tooltip.cybercultivator.gene_speed", "速度基因: %s");
        add("tooltip.cybercultivator.gene_yield", "产量基因: %s");
        add("tooltip.cybercultivator.gene_potency", "效价基因: %s");
        add("tooltip.cybercultivator.gene_generation", "代数: %s");
        add("tooltip.cybercultivator.gene_purity", "纯度基因: %s");
        add("tooltip.cybercultivator.mutation", "★ 突变体");
        add("tooltip.cybercultivator.bio_incubator", "放入种子后，可注入纯净水/营养液/数据信号进行培养");
        add("tooltip.cybercultivator.gene_splicer", "放入两颗基因种子后自动拼接；潜行可取回输入或输出");
        add("tooltip.cybercultivator.atmospheric_condenser", "自动凝结纯净水；下方放置培养槽可自动注入纯净度");
        add("tooltip.cybercultivator.serum_bottler", "将突触神经莓加工为高级血清；支持漏斗自动化");
        add("tooltip.cybercultivator.bio_pulse_belt", "自动为附近培养槽注入营养液/纯净水/数据信号");
        add("tooltip.cybercultivator.life_support_pack", "缓解血清副作用；低血量时自动注射治疗");
        add("effect.cybercultivator.synaptic_overclock", "突触超频");
        add("effect.cybercultivator.neural_overload", "神经过载");
        add("effect.cybercultivator.visual_enhancement", "视觉强化");
        add("effect.cybercultivator.metabolic_boost", "代谢加速");

        // Advancements
        add("advancement.cybercultivator.root.title", "赛博农夫");
        add("advancement.cybercultivator.root.description", "欢迎来到赛博农业的世界");
        add("advancement.cybercultivator.silicon_start.title", "硅基起步");
        add("advancement.cybercultivator.silicon_start.description", "获得你的第一块硅碎片");
        add("advancement.cybercultivator.rare_earth.title", "稀土之源");
        add("advancement.cybercultivator.rare_earth.description", "获得稀土粉末，精密机器的核心材料");
        add("advancement.cybercultivator.bio_cultivate.title", "生化培育");
        add("advancement.cybercultivator.bio_cultivate.description", "获得生化原液，开启培养之路");
        add("advancement.cybercultivator.gene_code.title", "基因密码");
        add("advancement.cybercultivator.gene_code.description", "通过基因拼接机创造新的种子");
        add("advancement.cybercultivator.serum_path.title", "血清之路");
        add("advancement.cybercultivator.serum_path.description", "制造第一支突触超频血清");
        add("advancement.cybercultivator.visual_enhancement.title", "视觉超越");
        add("advancement.cybercultivator.visual_enhancement.description", "获得 S-02 视觉强化血清");
        add("advancement.cybercultivator.metabolic_boost.title", "代谢狂飙");
        add("advancement.cybercultivator.metabolic_boost.description", "获得 S-03 代谢加速血清");
        add("advancement.cybercultivator.cyber_equip.title", "赛博装备");
        add("advancement.cybercultivator.cyber_equip.description", "佩戴一件 Curios 饰品");

        // Serum quality chain tooltips
        add("tooltip.cybercultivator.serum_activity", "突触活性: %s");
        add("tooltip.cybercultivator.serum_base_level", "基础等级: %s");
        add("tooltip.cybercultivator.serum_duration_mult", "时长倍率: ×%s");
        add("tooltip.cybercultivator.quality_potency", "品质: %s/10");
        add("tooltip.cybercultivator.quality_purity", "纯度: %s/10");
        add("tooltip.cybercultivator.quality_concentration", "浓度: %s/10");
        add("tooltip.cybercultivator.serum_activity_bonus", "★ 活性突破: %s — 效果等级增强");
    }
}
