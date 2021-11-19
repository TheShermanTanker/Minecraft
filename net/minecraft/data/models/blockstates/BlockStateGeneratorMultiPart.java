package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockStateGeneratorMultiPart implements IBlockStateGenerator {
    private final Block block;
    private final List<BlockStateGeneratorMultiPart.Entry> parts = Lists.newArrayList();

    private BlockStateGeneratorMultiPart(Block block) {
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    public static BlockStateGeneratorMultiPart multiPart(Block block) {
        return new BlockStateGeneratorMultiPart(block);
    }

    public BlockStateGeneratorMultiPart with(List<Variant> variants) {
        this.parts.add(new BlockStateGeneratorMultiPart.Entry(variants));
        return this;
    }

    public BlockStateGeneratorMultiPart with(Variant variant) {
        return this.with(ImmutableList.of(variant));
    }

    public BlockStateGeneratorMultiPart with(Condition condition, List<Variant> variants) {
        this.parts.add(new BlockStateGeneratorMultiPart.ConditionalEntry(condition, variants));
        return this;
    }

    public BlockStateGeneratorMultiPart with(Condition condition, Variant... variants) {
        return this.with(condition, ImmutableList.copyOf(variants));
    }

    public BlockStateGeneratorMultiPart with(Condition condition, Variant variant) {
        return this.with(condition, ImmutableList.of(variant));
    }

    @Override
    public JsonElement get() {
        BlockStateList<Block, IBlockData> stateDefinition = this.block.getStates();
        this.parts.forEach((entry) -> {
            entry.validate(stateDefinition);
        });
        JsonArray jsonArray = new JsonArray();
        this.parts.stream().map(BlockStateGeneratorMultiPart.Entry::get).forEach(jsonArray::add);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("multipart", jsonArray);
        return jsonObject;
    }

    static class ConditionalEntry extends BlockStateGeneratorMultiPart.Entry {
        private final Condition condition;

        ConditionalEntry(Condition condition, List<Variant> list) {
            super(list);
            this.condition = condition;
        }

        @Override
        public void validate(BlockStateList<?, ?> stateManager) {
            this.condition.validate(stateManager);
        }

        @Override
        public void decorate(JsonObject json) {
            json.add("when", this.condition.get());
        }
    }

    static class Entry implements Supplier<JsonElement> {
        private final List<Variant> variants;

        Entry(List<Variant> list) {
            this.variants = list;
        }

        public void validate(BlockStateList<?, ?> stateManager) {
        }

        public void decorate(JsonObject json) {
        }

        @Override
        public JsonElement get() {
            JsonObject jsonObject = new JsonObject();
            this.decorate(jsonObject);
            jsonObject.add("apply", Variant.convertList(this.variants));
            return jsonObject;
        }
    }
}
