package net.minecraft.world.level.material;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Fluid extends IBlockDataHolder<FluidType, Fluid> {
    public static final Codec<Fluid> CODEC = codec(IRegistry.FLUID, FluidType::defaultFluidState).stable();
    public static final int AMOUNT_MAX = 9;
    public static final int AMOUNT_FULL = 8;

    public Fluid(FluidType fluid, ImmutableMap<IBlockState<?>, Comparable<?>> propertiesMap, MapCodec<Fluid> codec) {
        super(fluid, propertiesMap, codec);
    }

    public FluidType getType() {
        return this.owner;
    }

    public boolean isSource() {
        return this.getType().isSource(this);
    }

    public boolean isSourceOfType(FluidType fluid) {
        return this.owner == fluid && this.owner.isSource(this);
    }

    public boolean isEmpty() {
        return this.getType().isEmpty();
    }

    public float getHeight(IBlockAccess world, BlockPosition pos) {
        return this.getType().getHeight(this, world, pos);
    }

    public float getOwnHeight() {
        return this.getType().getOwnHeight(this);
    }

    public int getAmount() {
        return this.getType().getAmount(this);
    }

    public boolean shouldRenderBackwardUpFace(IBlockAccess world, BlockPosition pos) {
        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                BlockPosition blockPos = pos.offset(i, 0, j);
                Fluid fluidState = world.getFluid(blockPos);
                if (!fluidState.getType().isSame(this.getType()) && !world.getType(blockPos).isSolidRender(world, blockPos)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void tick(World world, BlockPosition pos) {
        this.getType().tick(world, pos, this);
    }

    public void animateTick(World world, BlockPosition pos, Random random) {
        this.getType().animateTick(world, pos, this, random);
    }

    public boolean isRandomlyTicking() {
        return this.getType().isRandomlyTicking();
    }

    public void randomTick(World world, BlockPosition pos, Random random) {
        this.getType().randomTick(world, pos, this, random);
    }

    public Vec3D getFlow(IBlockAccess world, BlockPosition pos) {
        return this.getType().getFlow(world, pos, this);
    }

    public IBlockData getBlockData() {
        return this.getType().createLegacyBlock(this);
    }

    @Nullable
    public ParticleParam getDripParticle() {
        return this.getType().getDripParticle();
    }

    public boolean is(Tag<FluidType> tag) {
        return this.getType().is(tag);
    }

    public float getExplosionResistance() {
        return this.getType().getExplosionResistance();
    }

    public boolean canBeReplacedWith(IBlockAccess world, BlockPosition pos, FluidType fluid, EnumDirection direction) {
        return this.getType().canBeReplacedWith(this, world, pos, fluid, direction);
    }

    public VoxelShape getShape(IBlockAccess world, BlockPosition pos) {
        return this.getType().getShape(this, world, pos);
    }
}
