package net.minecraft.world.level.levelgen;

import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.state.IBlockData;

public class DepthBasedReplacingBaseStoneSource implements BaseStoneSource {
    private static final int ALWAYS_REPLACE_BELOW_Y = -8;
    private static final int NEVER_REPLACE_ABOVE_Y = 0;
    private final SeededRandom random;
    private final long seed;
    private final IBlockData normalBlock;
    private final IBlockData replacementBlock;
    private final GeneratorSettingBase settings;

    public DepthBasedReplacingBaseStoneSource(long seed, IBlockData defaultBlock, IBlockData deepslateState, GeneratorSettingBase settings) {
        this.random = new SeededRandom(seed);
        this.seed = seed;
        this.normalBlock = defaultBlock;
        this.replacementBlock = deepslateState;
        this.settings = settings;
    }

    @Override
    public IBlockData getBaseBlock(int x, int y, int z) {
        if (!this.settings.isDeepslateEnabled()) {
            return this.normalBlock;
        } else if (y < -8) {
            return this.replacementBlock;
        } else if (y > 0) {
            return this.normalBlock;
        } else {
            double d = MathHelper.map((double)y, -8.0D, 0.0D, 1.0D, 0.0D);
            this.random.setBaseStoneSeed(this.seed, x, y, z);
            return (double)this.random.nextFloat() < d ? this.replacementBlock : this.normalBlock;
        }
    }
}
