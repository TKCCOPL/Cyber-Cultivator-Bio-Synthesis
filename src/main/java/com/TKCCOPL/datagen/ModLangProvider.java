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
        add("tooltip.cybercultivator.gene_synergy", "协同基因: %s");
        add("tooltip.cybercultivator.mutation_numerical", "⚡ 突变: 数值突破 %s");
        add("tooltip.cybercultivator.mutation_synergy", "⚡ 突变: 协同基因 %s");
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

        // HUD translations (Spectrum Monocle overlay)
        add("hud.cybercultivator.incubator", "生物培养槽");
        add("hud.cybercultivator.seed_in", "种子: 已放入");
        add("hud.cybercultivator.seed_empty", "种子: 空");
        add("hud.cybercultivator.mutation_numerical", "⚡数值突破");
        add("hud.cybercultivator.mutation_synergy", "⚡协同基因");
        add("hud.cybercultivator.bottler", "血清灌装机");
        add("hud.cybercultivator.recipe", "配方: ");
        add("hud.cybercultivator.activity", "活性: %s");
        add("hud.cybercultivator.output_item", "输出: %s");
        add("hud.cybercultivator.condenser", "大气冷凝器");
        add("hud.cybercultivator.stock", "库存: %s/%s");
        add("hud.cybercultivator.producing", "状态: 生产中");
        add("hud.cybercultivator.full", "状态: 已满");
        add("hud.cybercultivator.idle", "状态: 空闲");
        add("hud.cybercultivator.splicer", "基因拼接机");
        add("hud.cybercultivator.ready_extract", "就绪 - 潜行右键取出");
        add("hud.cybercultivator.out_empty", "输出: 空");
        add("hud.cybercultivator.recipe_berry", "莓合成");
        add("hud.cybercultivator.recipe_s01", "S-01");
        add("hud.cybercultivator.recipe_s02", "S-02");
        add("hud.cybercultivator.recipe_s03", "S-03");
        add("hud.cybercultivator.recipe_idle", "空闲");
        add("hud.cybercultivator.eta", "ETA: 约%ss");
        add("hud.cybercultivator.eta_insufficient", "ETA: 资源不足");
        add("hud.cybercultivator.seed_empty_slot", "%s: 空");

        // JEI 配方类别
        add("jei.cybercultivator.serum_bottling", "血清灌装");
        add("jei.cybercultivator.gene_splicing", "基因拼接");
        add("jei.cybercultivator.incubator_output", "培养槽产出");
        add("jei.cybercultivator.output_quality", "品质: %s (← 潜力基因)");

        // JEI 信息
        add("jei.cybercultivator.activity", "活性: %s");
        add("jei.cybercultivator.processing_time", "%ss");
        add("jei.cybercultivator.mutation_chance", "突变: %s%%");
        add("jei.cybercultivator.gene_info_a", "A: S:%s Y:%s P:%s");
        add("jei.cybercultivator.gene_info_b", "B: S:%s Y:%s P:%s");
        add("jei.cybercultivator.gene_range", "子代: S:%s-%s Y:%s-%s P:%s-%s");
        add("jei.cybercultivator.gene_speed", "速度: %s");
        add("jei.cybercultivator.gene_yield", "产量: %s");
        add("jei.cybercultivator.gene_potency", "潜力: %s");
        add("jei.cybercultivator.output_range", "产出: %s-%s");
        add("jei.cybercultivator.growth_rate", "速率: %sx");

        // JEI 物品信息页面
        add("jei.cybercultivator.info.s01", "S-01 突触超频血清：配方 = 神经莓 + 生化原液 + 玻璃瓶。效果：攻速+力量（随活性缩放），持续 25 秒。副作用：凋零+饥饿。可叠加饮用提升等级（上限 VIII），活性 ≥8 起步 II 级。");
        add("jei.cybercultivator.info.s02", "S-02 视觉强化血清：配方 = 神经莓 + 稀土粉末 + 玻璃瓶。效果：夜视+发光（范围 16-48 格）+抗火，持续 30 秒。副作用：失明+饥饿。可叠加饮用提升等级（上限 VIII），活性 ≥8 起步 II 级。");
        add("jei.cybercultivator.info.s03", "S-03 代谢加速血清：配方 = 神经莓 + 工业乙醇 + 玻璃瓶。效果：回血+移速+跳跃提升，持续 15 秒。副作用：缓慢+中毒。可叠加饮用提升等级（上限 VIII），活性 ≥8 起步 II 级。");
        add("jei.cybercultivator.info.monocle", "光谱单片镜：佩戴后可以解析培养槽的状态信息（N/P/D 数值、生长进度、预计成熟时间）和种子的基因值。在 HUD 上显示浮窗信息。获取：末地城宝库、林地府邸、沙漠神殿（15% 掉率）。");
        add("jei.cybercultivator.info.belt", "生化脉冲腰带：自动扫描附近的培养槽，消耗背包中的材料自动注入营养液、纯净水和数据信号。获取：废弃矿井、要塞、大型海底废墟（20% 掉率）。");
        add("jei.cybercultivator.info.pack", "生命支持箱：加速血清副作用（神经过载）的消退。当生命值过低时自动注射治疗（冷却 60 秒）。获取：废弃矿井、地牢、掠夺者前哨站（20% 掉率）。");
        add("jei.cybercultivator.info.seeds.fiber", "纤维草种子：破坏草丛获得。放入培养槽中培育，产出植物纤维。通过基因拼接机可改变基因值（速度、产量、效价）。");
        add("jei.cybercultivator.info.seeds.soy", "蛋白质豆种子：地牢或村庄战利品箱获得。放入培养槽中培育，产出生化原液。通过基因拼接机可改变基因值（速度、产量、效价）。");
        add("jei.cybercultivator.info.seeds.bloom", "酒精花种子：地牢或村庄战利品箱获得。放入培养槽中培育，产出工业乙醇。通过基因拼接机可改变基因值（速度、产量、效价）。");

        // JEI 基础材料信息
        add("jei.cybercultivator.info.silicon_shard", "硅碎片：从硅晶矿中开采获得，是制造数据信号注入剂的基础材料。");
        add("jei.cybercultivator.info.rare_earth_dust", "稀土粉末：从稀土矿中开采获得，用于合成 S-02 视觉强化血清。");
        add("jei.cybercultivator.info.plant_fiber", "植物纤维：由纤维草在培养槽中培育产出。品质（Potency）继承自种子基因，影响血清活性。用于合成突触神经莓。");
        add("jei.cybercultivator.info.biochemical_solution", "生化原液：由蛋白质豆在培养槽中培育产出。品质（Concentration）继承自种子基因，影响血清活性。用于合成突触神经莓和 S-01 血清。");
        add("jei.cybercultivator.info.industrial_ethanol", "工业乙醇：由酒精花在培养槽中培育产出。品质（Purity）继承自种子基因，影响血清活性。用于合成突触神经莓和 S-03 血清。");
        add("jei.cybercultivator.info.purified_water", "纯净水瓶：由大气冷凝器自动生产（每 30 秒 1 瓶），用于注入培养槽提升纯净度（Purity）。冷凝器放置在培养槽上方时可自动注入。");
        add("jei.cybercultivator.info.neural_berry", "突触神经莓：血清链核心中间产物。配方：植物纤维 + 工业乙醇 + 生化原液 → 神经莓。突触活性 = Potency×0.25 + Purity×0.375 + Concentration×0.375。活性越高，血清效果越强。可用于合成 S-01/S-02/S-03 血清。");

        // JEI 机器信息
        add("jei.cybercultivator.info.bio_incubator", "生物培养槽：放入基因种子后，可注入纯净水（提升纯度）、生化原液（提升营养）、硅碎片（提升数据信号）进行培养。三项数值影响作物产出品质。");
        add("jei.cybercultivator.info.gene_splicer", "基因拼接机：放入两颗基因种子进行拼接，子代基因 = (父本+母本)/2 ± 随机变异。同类拼接突变率 5%，跨类型拼接突变率 9%。");
        add("jei.cybercultivator.info.atmospheric_condenser", "大气冷凝器：每 30 秒自动生产 1 纯净水瓶，库存上限 32。下方放置培养槽时自动注入纯净度 +20。支持漏斗自动化。");
        add("jei.cybercultivator.info.serum_bottler", "血清灌装机：将突触神经莓加工为高级血清（S-01/S-02/S-03）。支持漏斗自动化（顶部/侧面注入，底部抽取）。");

        // JEI 矿石信息
        add("jei.cybercultivator.info.silicon_ore", "硅晶矿：在地下生成，挖掘后掉落硅碎片。用于制造数据信号注入剂。");
        add("jei.cybercultivator.info.rare_earth_ore", "稀土矿：在地下生成，挖掘后掉落稀土粉末。用于合成 S-02 视觉强化血清。");

        // JEI 血清效果简介
        add("jei.cybercultivator.serum_effect.s01", "攻速+力量 | 副作用: 凋零+饥饿");
        add("jei.cybercultivator.serum_effect.s02", "夜视+发光 | 副作用: 失明+饥饿");
        add("jei.cybercultivator.serum_effect.s03", "回血+移速 | 副作用: 缓慢+中毒");

        // JEI 原料品质标签
        add("jei.cybercultivator.tag_potency", "效价");
        add("jei.cybercultivator.tag_purity", "纯度");
        add("jei.cybercultivator.tag_concentration", "浓度");

        // JEI 血清等级
        add("jei.cybercultivator.serum_level", "等级: I→VIII");
        add("jei.cybercultivator.tooltip.level_scaling", "持续时间 = 基础 × (0.5 + 活性×0.1)\n活性 ≥8 起步 II 级，叠加上限 VIII 级");

        // JEI 莓合成链路
        add("jei.cybercultivator.berry_chain", "血清链: 种子→作物→莓→血清");
        add("jei.cybercultivator.tooltip.berry_chain_detail", "链路：培养槽种植→收获原料→灌装机合成莓→莓+辅料+瓶→血清");
        add("jei.cybercultivator.tooltip.quality_tags", "原料品质继承自种子基因，影响突触活性");

        // JEI 血清链路
        add("jei.cybercultivator.tooltip.serum_chain", "链路：神经莓 + 辅料 + 玻璃瓶 → 血清");

        // JEI Tooltip 公式说明
        add("jei.cybercultivator.tooltip.mutation_formula", "突变概率：基础 5% + 每点基因差异 +1%");
        add("jei.cybercultivator.tooltip.gene_formula", "子代基因 = (A+B)/2 ± 随机(-2,+2)，限制 1-10");
        add("jei.cybercultivator.tooltip.output_formula", "产出数量 = 2 + Yield/3");
        add("jei.cybercultivator.tooltip.rate_formula", "生长速率 = 0.5 + Speed/10 × 1.5");
        add("jei.cybercultivator.tooltip.gene_speed", "速度基因 → 控制生长速率");
        add("jei.cybercultivator.tooltip.gene_yield", "产量基因 → 控制产出数量");
        add("jei.cybercultivator.tooltip.gene_potency", "潜力基因 → 决定产出品质");
        add("jei.cybercultivator.tooltip.quality_formula", "品质 = 潜力基因值（影响血清活性）");
        add("jei.cybercultivator.tooltip.activity_formula", "活性 = Potency×0.25 + Purity×0.375 + Concentration×0.375");
        add("jei.cybercultivator.tooltip.serum_stackable", "可叠加饮用提升等级，上限 VIII 级");
        add("jei.cybercultivator.tooltip.serum_side_effect", "效果结束后自动施加神经过载副作用");
    }
}
