package com.TKCCOPL.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class CuriosLootModifier extends LootModifier {
    public static final Supplier<Codec<CuriosLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst)
                    .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
                    .and(Codec.INT.fieldOf("count").forGetter(m -> m.count))
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(ResourceLocation.CODEC.listOf().fieldOf("loot_tables").forGetter(m -> m.lootTables))
                    .apply(inst, CuriosLootModifier::new)));

    private final Item item;
    private final int count;
    private final float chance;
    private final List<ResourceLocation> lootTables;

    public CuriosLootModifier(LootItemCondition[] conditions, Item item, int count, float chance, List<ResourceLocation> lootTables) {
        super(conditions);
        this.item = item;
        this.count = count;
        this.chance = chance;
        this.lootTables = lootTables;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTableId = context.getQueriedLootTableId();
        if (lootTables.contains(lootTableId)) {
            if (context.getRandom().nextFloat() < chance) {
                generatedLoot.add(new ItemStack(item, count));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
