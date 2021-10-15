package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootEntrySequence extends LootEntryChildrenAbstract {
    LootEntrySequence(LootEntryAbstract[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.SEQUENCE;
    }

    @Override
    protected LootEntryChildren compose(LootEntryChildren[] children) {
        switch(children.length) {
        case 0:
            return ALWAYS_TRUE;
        case 1:
            return children[0];
        case 2:
            return children[0].and(children[1]);
        default:
            return (context, lootChoiceExpander) -> {
                for(LootEntryChildren composableEntryContainer : children) {
                    if (!composableEntryContainer.expand(context, lootChoiceExpander)) {
                        return false;
                    }
                }

                return true;
            };
        }
    }

    public static LootEntrySequence.Builder sequential(LootEntryAbstract.Builder<?>... entries) {
        return new LootEntrySequence.Builder(entries);
    }

    public static class Builder extends LootEntryAbstract.Builder<LootEntrySequence.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();

        public Builder(LootEntryAbstract.Builder<?>... entries) {
            for(LootEntryAbstract.Builder<?> builder : entries) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected LootEntrySequence.Builder getThis() {
            return this;
        }

        @Override
        public LootEntrySequence.Builder then(LootEntryAbstract.Builder<?> entry) {
            this.entries.add(entry.build());
            return this;
        }

        @Override
        public LootEntryAbstract build() {
            return new LootEntrySequence(this.entries.toArray(new LootEntryAbstract[0]), this.getConditions());
        }
    }
}
