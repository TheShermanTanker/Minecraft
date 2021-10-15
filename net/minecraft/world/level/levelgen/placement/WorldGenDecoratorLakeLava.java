package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;

public class WorldGenDecoratorLakeLava extends RepeatingDecorator<WorldGenDecoratorDungeonConfiguration> {
    public WorldGenDecoratorLakeLava(Codec<WorldGenDecoratorDungeonConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected int count(Random random, WorldGenDecoratorDungeonConfiguration config, BlockPosition pos) {
        return pos.getY() >= 63 && random.nextInt(10) != 0 ? 0 : 1;
    }
}
