package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.LootCollector;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.commons.lang3.ArrayUtils;

public class LootEntryAlternatives extends LootEntryChildrenAbstract {
    LootEntryAlternatives(LootEntryAbstract[] children, LootItemCondition[] conditions) {
        super(children, conditions);
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.ALTERNATIVES;
    }

    @Override
    protected LootEntryChildren compose(LootEntryChildren[] children) {
        switch(children.length) {
        case 0:
            return ALWAYS_FALSE;
        case 1:
            return children[0];
        case 2:
            return children[0].or(children[1]);
        default:
            return (context, lootChoiceExpander) -> {
                for(LootEntryChildren composableEntryContainer : children) {
                    if (composableEntryContainer.expand(context, lootChoiceExpander)) {
                        return true;
                    }
                }

                return false;
            };
        }
    }

    @Override
    public void validate(LootCollector reporter) {
        super.validate(reporter);

        for(int i = 0; i < this.children.length - 1; ++i) {
            if (ArrayUtils.isEmpty((Object[])this.children[i].conditions)) {
                reporter.reportProblem("Unreachable entry!");
            }
        }

    }

    public static LootEntryAlternatives.Builder alternatives(LootEntryAbstract.Builder<?>... children) {
        return new LootEntryAlternatives.Builder(children);
    }

    public static class Builder extends LootEntryAbstract.Builder<LootEntryAlternatives.Builder> {
        private final List<LootEntryAbstract> entries = Lists.newArrayList();

        public Builder(LootEntryAbstract.Builder<?>... children) {
            for(LootEntryAbstract.Builder<?> builder : children) {
                this.entries.add(builder.build());
            }

        }

        @Override
        protected LootEntryAlternatives.Builder getThis() {
            return this;
        }

        @Override
        public LootEntryAlternatives.Builder otherwise(LootEntryAbstract.Builder<?> builder) {
            this.entries.add(builder.build());
            return this;
        }

        @Override
        public LootEntryAbstract build() {
            return new LootEntryAlternatives(this.entries.toArray(new LootEntryAbstract[0]), this.getConditions());
        }
    }
}
