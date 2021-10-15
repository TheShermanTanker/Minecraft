package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class LootEntryChildrenAbstract extends LootEntryAbstract {
    protected final LootEntryAbstract[] children;
    private final LootEntryChildren composedChildren;

    protected LootEntryChildrenAbstract(LootEntryAbstract[] children, LootItemCondition[] conditions) {
        super(conditions);
        this.children = children;
        this.composedChildren = this.compose(children);
    }

    @Override
    public void validate(LootCollector reporter) {
        super.validate(reporter);
        if (this.children.length == 0) {
            reporter.reportProblem("Empty children list");
        }

        for(int i = 0; i < this.children.length; ++i) {
            this.children[i].validate(reporter.forChild(".entry[" + i + "]"));
        }

    }

    protected abstract LootEntryChildren compose(LootEntryChildren[] children);

    @Override
    public final boolean expand(LootTableInfo context, Consumer<LootEntry> choiceConsumer) {
        return !this.canRun(context) ? false : this.composedChildren.expand(context, choiceConsumer);
    }

    public static <T extends LootEntryChildrenAbstract> LootEntryAbstract.Serializer<T> createSerializer(LootEntryChildrenAbstract.CompositeEntryConstructor<T> factory) {
        return new LootEntryAbstract.Serializer<T>() {
            @Override
            public void serializeType(JsonObject json, T entry, JsonSerializationContext context) {
                json.add("children", context.serialize(entry.children));
            }

            @Override
            public final T deserializeCustom(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
                LootEntryAbstract[] lootPoolEntryContainers = ChatDeserializer.getAsObject(jsonObject, "children", jsonDeserializationContext, LootEntryAbstract[].class);
                return factory.create(lootPoolEntryContainers, lootItemConditions);
            }
        };
    }

    @FunctionalInterface
    public interface CompositeEntryConstructor<T extends LootEntryChildrenAbstract> {
        T create(LootEntryAbstract[] children, LootItemCondition[] conditions);
    }
}
