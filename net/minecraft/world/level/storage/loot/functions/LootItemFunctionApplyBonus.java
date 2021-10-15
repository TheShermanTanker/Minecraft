package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionApplyBonus extends LootItemFunctionConditional {
    static final Map<MinecraftKey, LootItemFunctionApplyBonus.FormulaDeserializer> FORMULAS = Maps.newHashMap();
    final Enchantment enchantment;
    final LootItemFunctionApplyBonus.Formula formula;

    LootItemFunctionApplyBonus(LootItemCondition[] conditions, Enchantment enchantment, LootItemFunctionApplyBonus.Formula formula) {
        super(conditions);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.APPLY_BONUS;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.TOOL);
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        ItemStack itemStack = context.getContextParameter(LootContextParameters.TOOL);
        if (itemStack != null) {
            int i = EnchantmentManager.getEnchantmentLevel(this.enchantment, itemStack);
            int j = this.formula.calculateNewCount(context.getRandom(), stack.getCount(), i);
            stack.setCount(j);
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> addBonusBinomialDistributionCount(Enchantment enchantment, float probability, int extra) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionApplyBonus(conditions, enchantment, new LootItemFunctionApplyBonus.BinomialWithBonusCount(extra, probability));
        });
    }

    public static LootItemFunctionConditional.Builder<?> addOreBonusCount(Enchantment enchantment) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionApplyBonus(conditions, enchantment, new LootItemFunctionApplyBonus.OreDrops());
        });
    }

    public static LootItemFunctionConditional.Builder<?> addUniformBonusCount(Enchantment enchantment) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionApplyBonus(conditions, enchantment, new LootItemFunctionApplyBonus.UniformBonusCount(1));
        });
    }

    public static LootItemFunctionConditional.Builder<?> addUniformBonusCount(Enchantment enchantment, int bonusMultiplier) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionApplyBonus(conditions, enchantment, new LootItemFunctionApplyBonus.UniformBonusCount(bonusMultiplier));
        });
    }

    static {
        FORMULAS.put(LootItemFunctionApplyBonus.BinomialWithBonusCount.TYPE, LootItemFunctionApplyBonus.BinomialWithBonusCount::deserialize);
        FORMULAS.put(LootItemFunctionApplyBonus.OreDrops.TYPE, LootItemFunctionApplyBonus.OreDrops::deserialize);
        FORMULAS.put(LootItemFunctionApplyBonus.UniformBonusCount.TYPE, LootItemFunctionApplyBonus.UniformBonusCount::deserialize);
    }

    static final class BinomialWithBonusCount implements LootItemFunctionApplyBonus.Formula {
        public static final MinecraftKey TYPE = new MinecraftKey("binomial_with_bonus_count");
        private final int extraRounds;
        private final float probability;

        public BinomialWithBonusCount(int extra, float probability) {
            this.extraRounds = extra;
            this.probability = probability;
        }

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            for(int i = 0; i < enchantmentLevel + this.extraRounds; ++i) {
                if (random.nextFloat() < this.probability) {
                    ++initialCount;
                }
            }

            return initialCount;
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
            json.addProperty("extra", this.extraRounds);
            json.addProperty("probability", this.probability);
        }

        public static LootItemFunctionApplyBonus.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            int i = ChatDeserializer.getAsInt(json, "extra");
            float f = ChatDeserializer.getAsFloat(json, "probability");
            return new LootItemFunctionApplyBonus.BinomialWithBonusCount(i, f);
        }

        @Override
        public MinecraftKey getType() {
            return TYPE;
        }
    }

    interface Formula {
        int calculateNewCount(Random random, int initialCount, int enchantmentLevel);

        void serializeParams(JsonObject json, JsonSerializationContext context);

        MinecraftKey getType();
    }

    interface FormulaDeserializer {
        LootItemFunctionApplyBonus.Formula deserialize(JsonObject functionJson, JsonDeserializationContext context);
    }

    static final class OreDrops implements LootItemFunctionApplyBonus.Formula {
        public static final MinecraftKey TYPE = new MinecraftKey("ore_drops");

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            if (enchantmentLevel > 0) {
                int i = random.nextInt(enchantmentLevel + 2) - 1;
                if (i < 0) {
                    i = 0;
                }

                return initialCount * (i + 1);
            } else {
                return initialCount;
            }
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
        }

        public static LootItemFunctionApplyBonus.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            return new LootItemFunctionApplyBonus.OreDrops();
        }

        @Override
        public MinecraftKey getType() {
            return TYPE;
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionApplyBonus> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionApplyBonus object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("enchantment", IRegistry.ENCHANTMENT.getKey(object.enchantment).toString());
            json.addProperty("formula", object.formula.getType().toString());
            JsonObject jsonObject = new JsonObject();
            object.formula.serializeParams(jsonObject, context);
            if (jsonObject.size() > 0) {
                json.add("parameters", jsonObject);
            }

        }

        @Override
        public LootItemFunctionApplyBonus deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "enchantment"));
            Enchantment enchantment = IRegistry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonParseException("Invalid enchantment id: " + resourceLocation);
            });
            MinecraftKey resourceLocation2 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "formula"));
            LootItemFunctionApplyBonus.FormulaDeserializer formulaDeserializer = LootItemFunctionApplyBonus.FORMULAS.get(resourceLocation2);
            if (formulaDeserializer == null) {
                throw new JsonParseException("Invalid formula id: " + resourceLocation2);
            } else {
                LootItemFunctionApplyBonus.Formula formula;
                if (jsonObject.has("parameters")) {
                    formula = formulaDeserializer.deserialize(ChatDeserializer.getAsJsonObject(jsonObject, "parameters"), jsonDeserializationContext);
                } else {
                    formula = formulaDeserializer.deserialize(new JsonObject(), jsonDeserializationContext);
                }

                return new LootItemFunctionApplyBonus(lootItemConditions, enchantment, formula);
            }
        }
    }

    static final class UniformBonusCount implements LootItemFunctionApplyBonus.Formula {
        public static final MinecraftKey TYPE = new MinecraftKey("uniform_bonus_count");
        private final int bonusMultiplier;

        public UniformBonusCount(int bonusMultiplier) {
            this.bonusMultiplier = bonusMultiplier;
        }

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            return initialCount + random.nextInt(this.bonusMultiplier * enchantmentLevel + 1);
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
            json.addProperty("bonusMultiplier", this.bonusMultiplier);
        }

        public static LootItemFunctionApplyBonus.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            int i = ChatDeserializer.getAsInt(json, "bonusMultiplier");
            return new LootItemFunctionApplyBonus.UniformBonusCount(i);
        }

        @Override
        public MinecraftKey getType() {
            return TYPE;
        }
    }
}
