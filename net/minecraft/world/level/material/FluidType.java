package net.minecraft.world.level.material;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FluidType {
    public static final RegistryBlockID<Fluid> FLUID_STATE_REGISTRY = new RegistryBlockID<>();
    protected final BlockStateList<FluidType, Fluid> stateDefinition;
    private Fluid defaultFluidState;

    protected FluidType() {
        BlockStateList.Builder<FluidType, Fluid> builder = new BlockStateList.Builder<>(this);
        this.createFluidStateDefinition(builder);
        this.stateDefinition = builder.create(FluidType::defaultFluidState, Fluid::new);
        this.registerDefaultState(this.stateDefinition.getBlockData());
    }

    protected void createFluidStateDefinition(BlockStateList.Builder<FluidType, Fluid> builder) {
    }

    public BlockStateList<FluidType, Fluid> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(Fluid state) {
        this.defaultFluidState = state;
    }

    public final Fluid defaultFluidState() {
        return this.defaultFluidState;
    }

    public abstract Item getBucket();

    protected void animateTick(World world, BlockPosition pos, Fluid state, Random random) {
    }

    protected void tick(World world, BlockPosition pos, Fluid state) {
    }

    protected void randomTick(World world, BlockPosition pos, Fluid state, Random random) {
    }

    @Nullable
    protected ParticleParam getDripParticle() {
        return null;
    }

    protected abstract boolean canBeReplacedWith(Fluid state, IBlockAccess world, BlockPosition pos, FluidType fluid, EnumDirection direction);

    protected abstract Vec3D getFlow(IBlockAccess world, BlockPosition pos, Fluid state);

    public abstract int getTickDelay(IWorldReader world);

    protected boolean isRandomlyTicking() {
        return false;
    }

    protected boolean isEmpty() {
        return false;
    }

    protected abstract float getExplosionResistance();

    public abstract float getHeight(Fluid state, IBlockAccess world, BlockPosition pos);

    public abstract float getOwnHeight(Fluid state);

    protected abstract IBlockData createLegacyBlock(Fluid state);

    public abstract boolean isSource(Fluid state);

    public abstract int getAmount(Fluid state);

    public boolean isSame(FluidType fluid) {
        return fluid == this;
    }

    public boolean is(Tag<FluidType> tag) {
        return tag.isTagged(this);
    }

    public abstract VoxelShape getShape(Fluid state, IBlockAccess world, BlockPosition pos);

    public Optional<SoundEffect> getPickupSound() {
        return Optional.empty();
    }
}
