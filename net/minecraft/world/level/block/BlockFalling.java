package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;

public class BlockFalling extends Block implements Fallable {
    public BlockFalling(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        world.getBlockTickList().scheduleTick(pos, this, this.getDelayAfterPlace());
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        world.getBlockTickList().scheduleTick(pos, this, this.getDelayAfterPlace());
        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (canFallThrough(world.getType(pos.below())) && pos.getY() >= world.getMinBuildHeight()) {
            EntityFallingBlock fallingBlockEntity = new EntityFallingBlock(world, (double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, world.getType(pos));
            this.falling(fallingBlockEntity);
            world.addEntity(fallingBlockEntity);
        }
    }

    protected void falling(EntityFallingBlock entity) {
    }

    protected int getDelayAfterPlace() {
        return 2;
    }

    public static boolean canFallThrough(IBlockData state) {
        Material material = state.getMaterial();
        return state.isAir() || state.is(TagsBlock.FIRE) || material.isLiquid() || material.isReplaceable();
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (random.nextInt(16) == 0) {
            BlockPosition blockPos = pos.below();
            if (canFallThrough(world.getType(blockPos))) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() - 0.05D;
                double f = (double)pos.getZ() + random.nextDouble();
                world.addParticle(new ParticleParamBlock(Particles.FALLING_DUST, state), d, e, f, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    public int getDustColor(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return -16777216;
    }
}
