package net.minecraft.world.level.material;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.BlockFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class FluidTypeLava extends FluidTypeFlowing {
    public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

    @Override
    public FluidType getFlowing() {
        return FluidTypes.FLOWING_LAVA;
    }

    @Override
    public FluidType getSource() {
        return FluidTypes.LAVA;
    }

    @Override
    public Item getBucket() {
        return Items.LAVA_BUCKET;
    }

    @Override
    public void animateTick(World world, BlockPosition pos, Fluid state, Random random) {
        BlockPosition blockPos = pos.above();
        if (world.getType(blockPos).isAir() && !world.getType(blockPos).isSolidRender(world, blockPos)) {
            if (random.nextInt(100) == 0) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() + 1.0D;
                double f = (double)pos.getZ() + random.nextDouble();
                world.addParticle(Particles.LAVA, d, e, f, 0.0D, 0.0D, 0.0D);
                world.playLocalSound(d, e, f, SoundEffects.LAVA_POP, SoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }

            if (random.nextInt(200) == 0) {
                world.playLocalSound((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), SoundEffects.LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }

    }

    @Override
    public void randomTick(World world, BlockPosition pos, Fluid state, Random random) {
        if (world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            int i = random.nextInt(3);
            if (i > 0) {
                BlockPosition blockPos = pos;

                for(int j = 0; j < i; ++j) {
                    blockPos = blockPos.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    if (!world.isLoaded(blockPos)) {
                        return;
                    }

                    IBlockData blockState = world.getType(blockPos);
                    if (blockState.isAir()) {
                        if (this.hasFlammableNeighbours(world, blockPos)) {
                            world.setTypeUpdate(blockPos, BlockFireAbstract.getState(world, blockPos));
                            return;
                        }
                    } else if (blockState.getMaterial().isSolid()) {
                        return;
                    }
                }
            } else {
                for(int k = 0; k < 3; ++k) {
                    BlockPosition blockPos2 = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    if (!world.isLoaded(blockPos2)) {
                        return;
                    }

                    if (world.isEmpty(blockPos2.above()) && this.isFlammable(world, blockPos2)) {
                        world.setTypeUpdate(blockPos2.above(), BlockFireAbstract.getState(world, blockPos2));
                    }
                }
            }

        }
    }

    private boolean hasFlammableNeighbours(IWorldReader world, BlockPosition pos) {
        for(EnumDirection direction : EnumDirection.values()) {
            if (this.isFlammable(world, pos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    private boolean isFlammable(IWorldReader world, BlockPosition pos) {
        return pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && !world.isLoaded(pos) ? false : world.getType(pos).getMaterial().isBurnable();
    }

    @Nullable
    @Override
    public ParticleParam getDripParticle() {
        return Particles.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(GeneratorAccess world, BlockPosition pos, IBlockData state) {
        this.fizz(world, pos);
    }

    @Override
    public int getSlopeFindDistance(IWorldReader world) {
        return world.getDimensionManager().isNether() ? 4 : 2;
    }

    @Override
    public IBlockData createLegacyBlock(Fluid state) {
        return Blocks.LAVA.getBlockData().set(BlockFluids.LEVEL, Integer.valueOf(getLegacyLevel(state)));
    }

    @Override
    public boolean isSame(FluidType fluid) {
        return fluid == FluidTypes.LAVA || fluid == FluidTypes.FLOWING_LAVA;
    }

    @Override
    public int getDropOff(IWorldReader world) {
        return world.getDimensionManager().isNether() ? 1 : 2;
    }

    @Override
    public boolean canBeReplacedWith(Fluid state, IBlockAccess world, BlockPosition pos, FluidType fluid, EnumDirection direction) {
        return state.getHeight(world, pos) >= 0.44444445F && fluid.is(TagsFluid.WATER);
    }

    @Override
    public int getTickDelay(IWorldReader world) {
        return world.getDimensionManager().isNether() ? 10 : 30;
    }

    @Override
    public int getSpreadDelay(World world, BlockPosition pos, Fluid oldState, Fluid newState) {
        int i = this.getTickDelay(world);
        if (!oldState.isEmpty() && !newState.isEmpty() && !oldState.get(FALLING) && !newState.get(FALLING) && newState.getHeight(world, pos) > oldState.getHeight(world, pos) && world.getRandom().nextInt(4) != 0) {
            i *= 4;
        }

        return i;
    }

    private void fizz(GeneratorAccess world, BlockPosition pos) {
        world.triggerEffect(1501, pos, 0);
    }

    @Override
    protected boolean canConvertToSource() {
        return false;
    }

    @Override
    protected void spreadTo(GeneratorAccess world, BlockPosition pos, IBlockData state, EnumDirection direction, Fluid fluidState) {
        if (direction == EnumDirection.DOWN) {
            Fluid fluidState2 = world.getFluid(pos);
            if (this.is(TagsFluid.LAVA) && fluidState2.is(TagsFluid.WATER)) {
                if (state.getBlock() instanceof BlockFluids) {
                    world.setTypeAndData(pos, Blocks.STONE.getBlockData(), 3);
                }

                this.fizz(world, pos);
                return;
            }
        }

        super.spreadTo(world, pos, state, direction, fluidState);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEffect> getPickupSound() {
        return Optional.of(SoundEffects.BUCKET_FILL_LAVA);
    }

    public static class Flowing extends FluidTypeLava {
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

    public static class Source extends FluidTypeLava {
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
