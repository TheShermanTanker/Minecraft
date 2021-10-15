package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootEntryGroup extends LootEntryChildrenAbstract {
    LootEntryGroup(LootEntryAbstract[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.GROUP;
    }

    @Override
    protected LootEntryChildren compose(LootEntryChildren[] children) {
        switch(children.length) {
        case 0:
            return ALWAYS_TRUE;
        case 1:
            return children[0];
        case 2:
            LootEntryChildren composableEntryContainer = children[0];
            LootEntryChildren composableEntryContainer2 = children[1];
            return (context, consumer) -> {
                composableEntryContainer.expand(context, consumer);
                composableEntryContainer2.expand(context, consumer);
                return true;
            };
        default:
            return (context, lootChoiceExpander) -> {
                for(LootEntryChildren composableEntryContainer : children) {
                    composableEntryContainer.expand(context, lootChoiceExpander);
                }

                return true;
            };
        }
    }

    public static LootEntryGroup.Builder list(LootEntryAbstract.Builder<?>... entries) {
        return new LootEntryGroup.Builder(entries);
    }

    public static class Builder extends LootEntryAbstract.Builder<LootEntryGroup.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();

        public Builder(LootEntryAbstract.Builder<?>... entries) {
            for(LootEntryAbstract.Builder<?> builder : entries) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected LootEntryGroup.Builder getThis() {
            return this;
        }

        @Override
        public LootEntryGroup.Builder append(LootEntryAbstract.Builder<?> entry) {
            this.entries.add(entry.build());
            return this;
        }

        @Override
        public LootEntryAbstract build() {
            return new LootEntryGroup(this.entries.toArray(new LootEntryAbstract[0]), this.getConditions());
        }
    }
}
