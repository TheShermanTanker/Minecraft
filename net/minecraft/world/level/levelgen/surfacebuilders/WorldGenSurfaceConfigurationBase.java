package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenSurfaceConfigurationBase implements WorldGenSurfaceConfiguration {
    public static final Codec<WorldGenSurfaceConfigurationBase> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("top_material").forGetter((config) -> {
            return config.topMaterial;
        }), IBlockData.CODEC.fieldOf("under_material").forGetter((config) -> {
            return config.underMaterial;
        }), IBlockData.CODEC.fieldOf("underwater_material").forGetter((config) -> {
            return config.underwaterMaterial;
        })).apply(instance, WorldGenSurfaceConfigurationBase::new);
    });
    private final IBlockData topMaterial;
    private final IBlockData underMaterial;
    private final IBlockData underwaterMaterial;

    public WorldGenSurfaceConfigurationBase(IBlockData topMaterial, IBlockData underMaterial, IBlockData underwaterMaterial) {
        this.topMaterial = topMaterial;
        this.underMaterial = underMaterial;
        this.underwaterMaterial = underwaterMaterial;
    }

    @Override
    public IBlockData getTopMaterial() {
        return this.topMaterial;
    }

    @Override
    public IBlockData getUnderMaterial() {
        return this.underMaterial;
    }

    @Override
    public IBlockData getUnderwaterMaterial() {
        return this.underwaterMaterial;
    }
}
