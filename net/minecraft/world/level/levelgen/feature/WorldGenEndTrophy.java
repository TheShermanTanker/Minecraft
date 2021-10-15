package net.minecraft.world.level.levelgen.feature;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockTorchWall;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenEndTrophy extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public static final int PODIUM_RADIUS = 4;
    public static final int PODIUM_PILLAR_HEIGHT = 4;
    public static final int RIM_RADIUS = 1;
    public static final float CORNER_ROUNDING = 0.5F;
    public static final BlockPosition END_PODIUM_LOCATION = BlockPosition.ZERO;
    private final boolean active;

    public WorldGenEndTrophy(boolean open) {
        super(WorldGenFeatureEmptyConfiguration.CODEC);
        this.active = open;
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();

        for(BlockPosition blockPos2 : BlockPosition.betweenClosed(new BlockPosition(blockPos.getX() - 4, blockPos.getY() - 1, blockPos.getZ() - 4), new BlockPosition(blockPos.getX() + 4, blockPos.getY() + 32, blockPos.getZ() + 4))) {
            boolean bl = blockPos2.closerThan(blockPos, 2.5D);
            if (bl || blockPos2.closerThan(blockPos, 3.5D)) {
                if (blockPos2.getY() < blockPos.getY()) {
                    if (bl) {
                        this.setBlock(worldGenLevel, blockPos2, Blocks.BEDROCK.getBlockData());
                    } else if (blockPos2.getY() < blockPos.getY()) {
                        this.setBlock(worldGenLevel, blockPos2, Blocks.END_STONE.getBlockData());
                    }
                } else if (blockPos2.getY() > blockPos.getY()) {
                    this.setBlock(worldGenLevel, blockPos2, Blocks.AIR.getBlockData());
                } else if (!bl) {
                    this.setBlock(worldGenLevel, blockPos2, Blocks.BEDROCK.getBlockData());
                } else if (this.active) {
                    this.setBlock(worldGenLevel, new BlockPosition(blockPos2), Blocks.END_PORTAL.getBlockData());
                } else {
                    this.setBlock(worldGenLevel, new BlockPosition(blockPos2), Blocks.AIR.getBlockData());
                }
            }
        }

        for(int i = 0; i < 4; ++i) {
            this.setBlock(worldGenLevel, blockPos.above(i), Blocks.BEDROCK.getBlockData());
        }

        BlockPosition blockPos3 = blockPos.above(2);

        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.setBlock(worldGenLevel, blockPos3.relative(direction), Blocks.WALL_TORCH.getBlockData().set(BlockTorchWall.FACING, direction));
        }

        return true;
    }
}
