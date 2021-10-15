package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockAnvil extends BlockFalling {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    private static final VoxelShape BASE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);
    private static final VoxelShape X_LEG1 = Block.box(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D);
    private static final VoxelShape X_LEG2 = Block.box(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D);
    private static final VoxelShape X_TOP = Block.box(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D);
    private static final VoxelShape Z_LEG1 = Block.box(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D);
    private static final VoxelShape Z_LEG2 = Block.box(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D);
    private static final VoxelShape Z_TOP = Block.box(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D);
    private static final VoxelShape X_AXIS_AABB = VoxelShapes.or(BASE, X_LEG1, X_LEG2, X_TOP);
    private static final VoxelShape Z_AXIS_AABB = VoxelShapes.or(BASE, Z_LEG1, Z_LEG2, Z_TOP);
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.repair");
    private static final float FALL_DAMAGE_PER_DISTANCE = 2.0F;
    private static final int FALL_DAMAGE_MAX = 40;

    public BlockAnvil(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().getClockWise());
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            player.awardStat(StatisticList.INTERACT_WITH_ANVIL);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> {
            return new ContainerAnvil(syncId, inventory, ContainerAccess.at(world, pos));
        }, CONTAINER_TITLE);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        EnumDirection direction = state.get(FACING);
        return direction.getAxis() == EnumDirection.EnumAxis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    protected void falling(EntityFallingBlock entity) {
        entity.setHurtsEntities(2.0F, 40);
    }

    @Override
    public void onLand(World world, BlockPosition pos, IBlockData fallingBlockState, IBlockData currentStateInPos, EntityFallingBlock fallingBlockEntity) {
        if (!fallingBlockEntity.isSilent()) {
            world.triggerEffect(1031, pos, 0);
        }

    }

    @Override
    public void onBrokenAfterFall(World world, BlockPosition pos, EntityFallingBlock fallingBlockEntity) {
        if (!fallingBlockEntity.isSilent()) {
            world.triggerEffect(1029, pos, 0);
        }

    }

    @Override
    public DamageSource getFallDamageSource() {
        return DamageSource.ANVIL;
    }

    @Nullable
    public static IBlockData damage(IBlockData fallingState) {
        if (fallingState.is(Blocks.ANVIL)) {
            return Blocks.CHIPPED_ANVIL.getBlockData().set(FACING, fallingState.get(FACING));
        } else {
            return fallingState.is(Blocks.CHIPPED_ANVIL) ? Blocks.DAMAGED_ANVIL.getBlockData().set(FACING, fallingState.get(FACING)) : null;
        }
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public int getDustColor(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.getMapColor(world, pos).col;
    }
}
