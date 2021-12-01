package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityComparator;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyComparatorMode;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.ticks.TickPriority;

public class BlockRedstoneComparator extends BlockDiodeAbstract implements ITileEntity {
    public static final BlockStateEnum<BlockPropertyComparatorMode> MODE = BlockProperties.MODE_COMPARATOR;

    public BlockRedstoneComparator(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(POWERED, Boolean.valueOf(false)).set(MODE, BlockPropertyComparatorMode.COMPARE));
    }

    @Override
    protected int getDelay(IBlockData state) {
        return 2;
    }

    @Override
    protected int getOutputSignal(IBlockAccess world, BlockPosition pos, IBlockData state) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof TileEntityComparator ? ((TileEntityComparator)blockEntity).getOutputSignal() : 0;
    }

    private int calculateOutputSignal(World world, BlockPosition pos, IBlockData state) {
        int i = this.getInputSignal(world, pos, state);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getAlternateSignal(world, pos, state);
            if (j > i) {
                return 0;
            } else {
                return state.get(MODE) == BlockPropertyComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }

    @Override
    protected boolean shouldTurnOn(World world, BlockPosition pos, IBlockData state) {
        int i = this.getInputSignal(world, pos, state);
        if (i == 0) {
            return false;
        } else {
            int j = this.getAlternateSignal(world, pos, state);
            if (i > j) {
                return true;
            } else {
                return i == j && state.get(MODE) == BlockPropertyComparatorMode.COMPARE;
            }
        }
    }

    @Override
    protected int getInputSignal(World world, BlockPosition pos, IBlockData state) {
        int i = super.getInputSignal(world, pos, state);
        EnumDirection direction = state.get(FACING);
        BlockPosition blockPos = pos.relative(direction);
        IBlockData blockState = world.getType(blockPos);
        if (blockState.isComplexRedstone()) {
            i = blockState.getAnalogOutputSignal(world, blockPos);
        } else if (i < 15 && blockState.isOccluding(world, blockPos)) {
            blockPos = blockPos.relative(direction);
            blockState = world.getType(blockPos);
            EntityItemFrame itemFrame = this.getItemFrame(world, direction, blockPos);
            int j = Math.max(itemFrame == null ? Integer.MIN_VALUE : itemFrame.getAnalogOutput(), blockState.isComplexRedstone() ? blockState.getAnalogOutputSignal(world, blockPos) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }

        return i;
    }

    @Nullable
    private EntityItemFrame getItemFrame(World world, EnumDirection facing, BlockPosition pos) {
        List<EntityItemFrame> list = world.getEntitiesOfClass(EntityItemFrame.class, new AxisAlignedBB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1)), (itemFrame) -> {
            return itemFrame != null && itemFrame.getDirection() == facing;
        });
        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (!player.getAbilities().mayBuild) {
            return EnumInteractionResult.PASS;
        } else {
            state = state.cycle(MODE);
            float f = state.get(MODE) == BlockPropertyComparatorMode.SUBTRACT ? 0.55F : 0.5F;
            world.playSound(player, pos, SoundEffects.COMPARATOR_CLICK, EnumSoundCategory.BLOCKS, 0.3F, f);
            world.setTypeAndData(pos, state, 2);
            this.refreshOutputState(world, pos, state);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    @Override
    protected void checkTickOnNeighbor(World world, BlockPosition pos, IBlockData state) {
        if (!world.getBlockTicks().willTickThisTick(pos, this)) {
            int i = this.calculateOutputSignal(world, pos, state);
            TileEntity blockEntity = world.getTileEntity(pos);
            int j = blockEntity instanceof TileEntityComparator ? ((TileEntityComparator)blockEntity).getOutputSignal() : 0;
            if (i != j || state.get(POWERED) != this.shouldTurnOn(world, pos, state)) {
                TickPriority tickPriority = this.shouldPrioritize(world, pos, state) ? TickPriority.HIGH : TickPriority.NORMAL;
                world.scheduleTick(pos, this, 2, tickPriority);
            }

        }
    }

    private void refreshOutputState(World world, BlockPosition pos, IBlockData state) {
        int i = this.calculateOutputSignal(world, pos, state);
        TileEntity blockEntity = world.getTileEntity(pos);
        int j = 0;
        if (blockEntity instanceof TileEntityComparator) {
            TileEntityComparator comparatorBlockEntity = (TileEntityComparator)blockEntity;
            j = comparatorBlockEntity.getOutputSignal();
            comparatorBlockEntity.setOutputSignal(i);
        }

        if (j != i || state.get(MODE) == BlockPropertyComparatorMode.COMPARE) {
            boolean bl = this.shouldTurnOn(world, pos, state);
            boolean bl2 = state.get(POWERED);
            if (bl2 && !bl) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)), 2);
            } else if (!bl2 && bl) {
                world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(true)), 2);
            }

            this.updateNeighborsInFront(world, pos, state);
        }

    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.refreshOutputState(world, pos, state);
    }

    @Override
    public boolean triggerEvent(IBlockData state, World world, BlockPosition pos, int type, int data) {
        super.triggerEvent(state, world, pos, type, data);
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity != null && blockEntity.setProperty(type, data);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityComparator(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, MODE, POWERED);
    }
}
