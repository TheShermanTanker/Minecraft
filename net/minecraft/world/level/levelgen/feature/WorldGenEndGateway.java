package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEndGateway;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenEndGatewayConfiguration;

public class WorldGenEndGateway extends WorldGenerator<WorldGenEndGatewayConfiguration> {
    public WorldGenEndGateway(Codec<WorldGenEndGatewayConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenEndGatewayConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        WorldGenEndGatewayConfiguration endGatewayConfiguration = context.config();

        for(BlockPosition blockPos2 : BlockPosition.betweenClosed(blockPos.offset(-1, -2, -1), blockPos.offset(1, 2, 1))) {
            boolean bl = blockPos2.getX() == blockPos.getX();
            boolean bl2 = blockPos2.getY() == blockPos.getY();
            boolean bl3 = blockPos2.getZ() == blockPos.getZ();
            boolean bl4 = Math.abs(blockPos2.getY() - blockPos.getY()) == 2;
            if (bl && bl2 && bl3) {
                BlockPosition blockPos3 = blockPos2.immutableCopy();
                this.setBlock(worldGenLevel, blockPos3, Blocks.END_GATEWAY.getBlockData());
                endGatewayConfiguration.getExit().ifPresent((blockPos2x) -> {
                    TileEntity blockEntity = worldGenLevel.getTileEntity(blockPos3);
                    if (blockEntity instanceof TileEntityEndGateway) {
                        TileEntityEndGateway theEndGatewayBlockEntity = (TileEntityEndGateway)blockEntity;
                        theEndGatewayBlockEntity.setExitPosition(blockPos2x, endGatewayConfiguration.isExitExact());
                        blockEntity.update();
                    }

                });
            } else if (bl2) {
                this.setBlock(worldGenLevel, blockPos2, Blocks.AIR.getBlockData());
            } else if (bl4 && bl && bl3) {
                this.setBlock(worldGenLevel, blockPos2, Blocks.BEDROCK.getBlockData());
            } else if ((bl || bl3) && !bl4) {
                this.setBlock(worldGenLevel, blockPos2, Blocks.BEDROCK.getBlockData());
            } else {
                this.setBlock(worldGenLevel, blockPos2, Blocks.AIR.getBlockData());
            }
        }

        return true;
    }
}
