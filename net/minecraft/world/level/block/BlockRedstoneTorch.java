package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamRedstone;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;

public class BlockRedstoneTorch extends BlockTorch {
    public static final BlockStateBoolean LIT = BlockProperties.LIT;
    private static final Map<IBlockAccess, List<BlockRedstoneTorch.RedstoneUpdateInfo>> RECENT_TOGGLES = new WeakHashMap<>();
    public static final int RECENT_TOGGLE_TIMER = 60;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int TOGGLE_DELAY = 2;

    protected BlockRedstoneTorch(BlockBase.Info settings) {
        super(settings, ParticleParamRedstone.REDSTONE);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LIT, Boolean.valueOf(true)));
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        for(EnumDirection direction : EnumDirection.values()) {
            world.applyPhysics(pos.relative(direction), this);
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!moved) {
            for(EnumDirection direction : EnumDirection.values()) {
                world.applyPhysics(pos.relative(direction), this);
            }

        }
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(LIT) && EnumDirection.UP != direction ? 15 : 0;
    }

    protected boolean hasNeighborSignal(World world, BlockPosition pos, IBlockData state) {
        return world.isBlockFacePowered(pos.below(), EnumDirection.DOWN);
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        boolean bl = this.hasNeighborSignal(world, pos, state);
        List<BlockRedstoneTorch.RedstoneUpdateInfo> list = RECENT_TOGGLES.get(world);

        while(list != null && !list.isEmpty() && world.getTime() - (list.get(0)).when > 60L) {
            list.remove(0);
        }

        if (state.get(LIT)) {
            if (bl) {
                world.setTypeAndData(pos, state.set(LIT, Boolean.valueOf(false)), 3);
                if (isToggledTooFrequently(world, pos, true)) {
                    world.triggerEffect(1502, pos, 0);
                    world.scheduleTick(pos, world.getType(pos).getBlock(), 160);
                }
            }
        } else if (!bl && !isToggledTooFrequently(world, pos, false)) {
            world.setTypeAndData(pos, state.set(LIT, Boolean.valueOf(true)), 3);
        }

    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (state.get(LIT) == this.hasNeighborSignal(world, pos, state) && !world.getBlockTicks().willTickThisTick(pos, this)) {
            world.scheduleTick(pos, this, 2);
        }

    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return direction == EnumDirection.DOWN ? state.getSignal(world, pos, direction) : 0;
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(LIT)) {
            double d = (double)pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            double e = (double)pos.getY() + 0.7D + (random.nextDouble() - 0.5D) * 0.2D;
            double f = (double)pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            world.addParticle(this.flameParticle, d, e, f, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LIT);
    }

    private static boolean isToggledTooFrequently(World world, BlockPosition pos, boolean addNew) {
        List<BlockRedstoneTorch.RedstoneUpdateInfo> list = RECENT_TOGGLES.computeIfAbsent(world, (worldx) -> {
            return Lists.newArrayList();
        });
        if (addNew) {
            list.add(new BlockRedstoneTorch.RedstoneUpdateInfo(pos.immutableCopy(), world.getTime()));
        }

        int i = 0;

        for(int j = 0; j < list.size(); ++j) {
            BlockRedstoneTorch.RedstoneUpdateInfo toggle = list.get(j);
            if (toggle.pos.equals(pos)) {
                ++i;
                if (i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    public static class RedstoneUpdateInfo {
        final BlockPosition pos;
        final long when;

        public RedstoneUpdateInfo(BlockPosition pos, long time) {
            this.pos = pos;
            this.when = time;
        }
    }
}
