package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public final class BinomialDistributionGenerator implements NumberProvider {
    final NumberProvider n;
    final NumberProvider p;

    BinomialDistributionGenerator(NumberProvider numberProvider, NumberProvider numberProvider2) {
        this.n = numberProvider;
        this.p = numberProvider2;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.BINOMIAL;
    }

    @Override
    public int getInt(LootTableInfo context) {
        int i = this.n.getInt(context);
        float f = this.p.getFloat(context);
        Random random = context.getRandom();
        int j = 0;

        for(int k = 0; k < i; ++k) {
            if (random.nextFloat() < f) {
                ++j;
            }
        }

        return j;
    }

    @Override
    public float getFloat(LootTableInfo context) {
        return (float)this.getInt(context);
    }

    public static BinomialDistributionGenerator binomial(int n, float p) {
        return new BinomialDistributionGenerator(ConstantValue.exactly((float)n), ConstantValue.exactly(p));
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
    }

    public static class Serializer implements LootSerializer<BinomialDistributionGenerator> {
        @Override
        public BinomialDistributionGenerator deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            NumberProvider numberProvider = ChatDeserializer.getAsObject(jsonObject, "n", jsonDeserializationContext, NumberProvider.class);
            NumberProvider numberProvider2 = ChatDeserializer.getAsObject(jsonObject, "p", jsonDeserializationContext, NumberProvider.class);
            return new BinomialDistributionGenerator(numberProvider, numberProvider2);
        }

        @Override
        public void serialize(JsonObject json, BinomialDistributionGenerator object, JsonSerializationContext context) {
            json.add("n", context.serialize(object.n));
            json.add("p", context.serialize(object.p));
        }
    }
}
