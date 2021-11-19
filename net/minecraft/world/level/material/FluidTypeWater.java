package net.minecraft.world.level.material;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class FluidTypeWater extends FluidTypeFlowing {
    @Override
    public FluidType getFlowing() {
        return FluidTypes.FLOWING_WATER;
    }

    @Override
    public FluidType getSource() {
        return FluidTypes.WATER;
    }

    @Override
    public Item getBucket() {
        return Items.WATER_BUCKET;
    }

    @Override
    public void animateTick(World world, BlockPosition pos, Fluid state, Random random) {
        if (!state.isSource() && !state.get(FALLING)) {
            if (random.nextInt(64) == 0) {
                world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.WATER_AMBIENT, EnumSoundCategory.BLOCKS, random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F, false);
            }
        } else if (random.nextInt(10) == 0) {
            world.addParticle(Particles.UNDERWATER, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + random.nextDouble(), (double)pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }

    }

    @Nullable
    @Override
    public ParticleParam getDripParticle() {
        return Particles.DRIPPING_WATER;
    }

    @Override
    protected boolean canConvertToSource() {
        return true;
    }

    @Override
    protected void beforeDestroyingBlock(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        TileEntity blockEntity = state.isTileEntity() ? world.getTileEntity(pos) : null;
        Block.dropResources(state, world, pos, blockEntity);
    }

    @Override
    public int getSlopeFindDistance(IWorldReader world) {
        return 4;
    }

    @Override
    public IBlockData createLegacyBlock(Fluid state) {
        return Blocks.WATER.getBlockData().set(BlockFluids.LEVEL, Integer.valueOf(getLegacyLevel(state)));
    }

    @Override
    public boolean isSame(FluidType fluid) {
        return fluid == FluidTypes.WATER || fluid == FluidTypes.FLOWING_WATER;
    }

    @Override
    public int getDropOff(IWorldReader world) {
        return 1;
    }

    @Override
    public int getTickDelay(IWorldReader world) {
        return 5;
    }

    @Override
    public boolean canBeReplacedWith(Fluid state, IBlockAccess world, BlockPosition pos, FluidType fluid, EnumDirection direction) {
        return direction == EnumDirection.DOWN && !fluid.is(TagsFluid.WATER);
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return Optional.of(SoundEffects.BUCKET_FILL);
    }

    public static class Flowing extends FluidTypeWater {
        @Override
        protected void createFluidStateDefinition(BlockStateList.Builder<FluidType, Fluid> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(Fluid state) {
            return state.get(LEVEL);
        }

        @Override
        public boolean isSource(Fluid state) {
            return false;
        }
    }

    public static class Source extends FluidTypeWater {
        @Override
        public int getAmount(Fluid state) {
            return 8;
        }

        @Override
        public boolean isSource(Fluid state) {
            return true;
        }
    }
}
