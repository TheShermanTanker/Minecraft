package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureProcessorPredicates {
    public static final Codec<DefinedStructureProcessorPredicates> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(DefinedStructureRuleTest.CODEC.fieldOf("input_predicate").forGetter((processorRule) -> {
            return processorRule.inputPredicate;
        }), DefinedStructureRuleTest.CODEC.fieldOf("location_predicate").forGetter((processorRule) -> {
            return processorRule.locPredicate;
        }), PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosRuleTestTrue.INSTANCE).forGetter((processorRule) -> {
            return processorRule.posPredicate;
        }), IBlockData.CODEC.fieldOf("output_state").forGetter((processorRule) -> {
            return processorRule.outputState;
        }), NBTTagCompound.CODEC.optionalFieldOf("output_nbt").forGetter((processorRule) -> {
            return Optional.ofNullable(processorRule.outputTag);
        })).apply(instance, DefinedStructureProcessorPredicates::new);
    });
    private final DefinedStructureRuleTest inputPredicate;
    private final DefinedStructureRuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final IBlockData outputState;
    @Nullable
    private final NBTTagCompound outputTag;

    public DefinedStructureProcessorPredicates(DefinedStructureRuleTest inputPredicate, DefinedStructureRuleTest locationPredicate, IBlockData state) {
        this(inputPredicate, locationPredicate, PosRuleTestTrue.INSTANCE, state, Optional.empty());
    }

    public DefinedStructureProcessorPredicates(DefinedStructureRuleTest inputPredicate, DefinedStructureRuleTest locationPredicate, PosRuleTest positionPredicate, IBlockData state) {
        this(inputPredicate, locationPredicate, positionPredicate, state, Optional.empty());
    }

    public DefinedStructureProcessorPredicates(DefinedStructureRuleTest inputPredicate, DefinedStructureRuleTest locationPredicate, PosRuleTest positionPredicate, IBlockData outputState, Optional<NBTTagCompound> nbt) {
        this.inputPredicate = inputPredicate;
        this.locPredicate = locationPredicate;
        this.posPredicate = positionPredicate;
        this.outputState = outputState;
        this.outputTag = nbt.orElse((NBTTagCompound)null);
    }

    public boolean test(IBlockData input, IBlockData location, BlockPosition blockPos, BlockPosition blockPos2, BlockPosition pivot, Random random) {
        return this.inputPredicate.test(input, random) && this.locPredicate.test(location, random) && this.posPredicate.test(blockPos, blockPos2, pivot, random);
    }

    public IBlockData getOutputState() {
        return this.outputState;
    }

    @Nullable
    public NBTTagCompound getOutputTag() {
        return this.outputTag;
    }
}
