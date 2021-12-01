package net.minecraft.world.level.levelgen.material;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.NoiseChunk;

public class MaterialRuleList implements WorldGenMaterialRule {
    private final List<WorldGenMaterialRule> materialRuleList;

    public MaterialRuleList(List<WorldGenMaterialRule> samplers) {
        this.materialRuleList = samplers;
    }

    @Nullable
    @Override
    public IBlockData apply(NoiseChunk sampler, int x, int y, int z) {
        for(WorldGenMaterialRule worldGenMaterialRule : this.materialRuleList) {
            IBlockData blockState = worldGenMaterialRule.apply(sampler, x, y, z);
            if (blockState != null) {
                return blockState;
            }
        }

        return null;
    }
}
