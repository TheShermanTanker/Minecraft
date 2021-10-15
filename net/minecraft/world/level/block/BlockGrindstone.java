package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerGrindstone;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyAttachPosition;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockGrindstone extends BlockAttachable {
    public static final VoxelShape FLOOR_NORTH_SOUTH_LEFT_POST = Block.box(2.0D, 0.0D, 6.0D, 4.0D, 7.0D, 10.0D);
    public static final VoxelShape FLOOR_NORTH_SOUTH_RIGHT_POST = Block.box(12.0D, 0.0D, 6.0D, 14.0D, 7.0D, 10.0D);
    public static final VoxelShape FLOOR_NORTH_SOUTH_LEFT_PIVOT = Block.box(2.0D, 7.0D, 5.0D, 4.0D, 13.0D, 11.0D);
    public static final VoxelShape FLOOR_NORTH_SOUTH_RIGHT_PIVOT = Block.box(12.0D, 7.0D, 5.0D, 14.0D, 13.0D, 11.0D);
    public static final VoxelShape FLOOR_NORTH_SOUTH_LEFT_LEG = VoxelShapes.or(FLOOR_NORTH_SOUTH_LEFT_POST, FLOOR_NORTH_SOUTH_LEFT_PIVOT);
    public static final VoxelShape FLOOR_NORTH_SOUTH_RIGHT_LEG = VoxelShapes.or(FLOOR_NORTH_SOUTH_RIGHT_POST, FLOOR_NORTH_SOUTH_RIGHT_PIVOT);
    public static final VoxelShape FLOOR_NORTH_SOUTH_ALL_LEGS = VoxelShapes.or(FLOOR_NORTH_SOUTH_LEFT_LEG, FLOOR_NORTH_SOUTH_RIGHT_LEG);
    public static final VoxelShape FLOOR_NORTH_SOUTH_GRINDSTONE = VoxelShapes.or(FLOOR_NORTH_SOUTH_ALL_LEGS, Block.box(4.0D, 4.0D, 2.0D, 12.0D, 16.0D, 14.0D));
    public static final VoxelShape FLOOR_EAST_WEST_LEFT_POST = Block.box(6.0D, 0.0D, 2.0D, 10.0D, 7.0D, 4.0D);
    public static final VoxelShape FLOOR_EAST_WEST_RIGHT_POST = Block.box(6.0D, 0.0D, 12.0D, 10.0D, 7.0D, 14.0D);
    public static final VoxelShape FLOOR_EAST_WEST_LEFT_PIVOT = Block.box(5.0D, 7.0D, 2.0D, 11.0D, 13.0D, 4.0D);
    public static final VoxelShape FLOOR_EAST_WEST_RIGHT_PIVOT = Block.box(5.0D, 7.0D, 12.0D, 11.0D, 13.0D, 14.0D);
    public static final VoxelShape FLOOR_EAST_WEST_LEFT_LEG = VoxelShapes.or(FLOOR_EAST_WEST_LEFT_POST, FLOOR_EAST_WEST_LEFT_PIVOT);
    public static final VoxelShape FLOOR_EAST_WEST_RIGHT_LEG = VoxelShapes.or(FLOOR_EAST_WEST_RIGHT_POST, FLOOR_EAST_WEST_RIGHT_PIVOT);
    public static final VoxelShape FLOOR_EAST_WEST_ALL_LEGS = VoxelShapes.or(FLOOR_EAST_WEST_LEFT_LEG, FLOOR_EAST_WEST_RIGHT_LEG);
    public static final VoxelShape FLOOR_EAST_WEST_GRINDSTONE = VoxelShapes.or(FLOOR_EAST_WEST_ALL_LEGS, Block.box(2.0D, 4.0D, 4.0D, 14.0D, 16.0D, 12.0D));
    public static final VoxelShape WALL_SOUTH_LEFT_POST = Block.box(2.0D, 6.0D, 0.0D, 4.0D, 10.0D, 7.0D);
    public static final VoxelShape WALL_SOUTH_RIGHT_POST = Block.box(12.0D, 6.0D, 0.0D, 14.0D, 10.0D, 7.0D);
    public static final VoxelShape WALL_SOUTH_LEFT_PIVOT = Block.box(2.0D, 5.0D, 7.0D, 4.0D, 11.0D, 13.0D);
    public static final VoxelShape WALL_SOUTH_RIGHT_PIVOT = Block.box(12.0D, 5.0D, 7.0D, 14.0D, 11.0D, 13.0D);
    public static final VoxelShape WALL_SOUTH_LEFT_LEG = VoxelShapes.or(WALL_SOUTH_LEFT_POST, WALL_SOUTH_LEFT_PIVOT);
    public static final VoxelShape WALL_SOUTH_RIGHT_LEG = VoxelShapes.or(WALL_SOUTH_RIGHT_POST, WALL_SOUTH_RIGHT_PIVOT);
    public static final VoxelShape WALL_SOUTH_ALL_LEGS = VoxelShapes.or(WALL_SOUTH_LEFT_LEG, WALL_SOUTH_RIGHT_LEG);
    public static final VoxelShape WALL_SOUTH_GRINDSTONE = VoxelShapes.or(WALL_SOUTH_ALL_LEGS, Block.box(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 16.0D));
    public static final VoxelShape WALL_NORTH_LEFT_POST = Block.box(2.0D, 6.0D, 7.0D, 4.0D, 10.0D, 16.0D);
    public static final VoxelShape WALL_NORTH_RIGHT_POST = Block.box(12.0D, 6.0D, 7.0D, 14.0D, 10.0D, 16.0D);
    public static final VoxelShape WALL_NORTH_LEFT_PIVOT = Block.box(2.0D, 5.0D, 3.0D, 4.0D, 11.0D, 9.0D);
    public static final VoxelShape WALL_NORTH_RIGHT_PIVOT = Block.box(12.0D, 5.0D, 3.0D, 14.0D, 11.0D, 9.0D);
    public static final VoxelShape WALL_NORTH_LEFT_LEG = VoxelShapes.or(WALL_NORTH_LEFT_POST, WALL_NORTH_LEFT_PIVOT);
    public static final VoxelShape WALL_NORTH_RIGHT_LEG = VoxelShapes.or(WALL_NORTH_RIGHT_POST, WALL_NORTH_RIGHT_PIVOT);
    public static final VoxelShape WALL_NORTH_ALL_LEGS = VoxelShapes.or(WALL_NORTH_LEFT_LEG, WALL_NORTH_RIGHT_LEG);
    public static final VoxelShape WALL_NORTH_GRINDSTONE = VoxelShapes.or(WALL_NORTH_ALL_LEGS, Block.box(4.0D, 2.0D, 0.0D, 12.0D, 14.0D, 12.0D));
    public static final VoxelShape WALL_WEST_LEFT_POST = Block.box(7.0D, 6.0D, 2.0D, 16.0D, 10.0D, 4.0D);
    public static final VoxelShape WALL_WEST_RIGHT_POST = Block.box(7.0D, 6.0D, 12.0D, 16.0D, 10.0D, 14.0D);
    public static final VoxelShape WALL_WEST_LEFT_PIVOT = Block.box(3.0D, 5.0D, 2.0D, 9.0D, 11.0D, 4.0D);
    public static final VoxelShape WALL_WEST_RIGHT_PIVOT = Block.box(3.0D, 5.0D, 12.0D, 9.0D, 11.0D, 14.0D);
    public static final VoxelShape WALL_WEST_LEFT_LEG = VoxelShapes.or(WALL_WEST_LEFT_POST, WALL_WEST_LEFT_PIVOT);
    public static final VoxelShape WALL_WEST_RIGHT_LEG = VoxelShapes.or(WALL_WEST_RIGHT_POST, WALL_WEST_RIGHT_PIVOT);
    public static final VoxelShape WALL_WEST_ALL_LEGS = VoxelShapes.or(WALL_WEST_LEFT_LEG, WALL_WEST_RIGHT_LEG);
    public static final VoxelShape WALL_WEST_GRINDSTONE = VoxelShapes.or(WALL_WEST_ALL_LEGS, Block.box(0.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));
    public static final VoxelShape WALL_EAST_LEFT_POST = Block.box(0.0D, 6.0D, 2.0D, 9.0D, 10.0D, 4.0D);
    public static final VoxelShape WALL_EAST_RIGHT_POST = Block.box(0.0D, 6.0D, 12.0D, 9.0D, 10.0D, 14.0D);
    public static final VoxelShape WALL_EAST_LEFT_PIVOT = Block.box(7.0D, 5.0D, 2.0D, 13.0D, 11.0D, 4.0D);
    public static final VoxelShape WALL_EAST_RIGHT_PIVOT = Block.box(7.0D, 5.0D, 12.0D, 13.0D, 11.0D, 14.0D);
    public static final VoxelShape WALL_EAST_LEFT_LEG = VoxelShapes.or(WALL_EAST_LEFT_POST, WALL_EAST_LEFT_PIVOT);
    public static final VoxelShape WALL_EAST_RIGHT_LEG = VoxelShapes.or(WALL_EAST_RIGHT_POST, WALL_EAST_RIGHT_PIVOT);
    public static final VoxelShape WALL_EAST_ALL_LEGS = VoxelShapes.or(WALL_EAST_LEFT_LEG, WALL_EAST_RIGHT_LEG);
    public static final VoxelShape WALL_EAST_GRINDSTONE = VoxelShapes.or(WALL_EAST_ALL_LEGS, Block.box(4.0D, 2.0D, 4.0D, 16.0D, 14.0D, 12.0D));
    public static final VoxelShape CEILING_NORTH_SOUTH_LEFT_POST = Block.box(2.0D, 9.0D, 6.0D, 4.0D, 16.0D, 10.0D);
    public static final VoxelShape CEILING_NORTH_SOUTH_RIGHT_POST = Block.box(12.0D, 9.0D, 6.0D, 14.0D, 16.0D, 10.0D);
    public static final VoxelShape CEILING_NORTH_SOUTH_LEFT_PIVOT = Block.box(2.0D, 3.0D, 5.0D, 4.0D, 9.0D, 11.0D);
    public static final VoxelShape CEILING_NORTH_SOUTH_RIGHT_PIVOT = Block.box(12.0D, 3.0D, 5.0D, 14.0D, 9.0D, 11.0D);
    public static final VoxelShape CEILING_NORTH_SOUTH_LEFT_LEG = VoxelShapes.or(CEILING_NORTH_SOUTH_LEFT_POST, CEILING_NORTH_SOUTH_LEFT_PIVOT);
    public static final VoxelShape CEILING_NORTH_SOUTH_RIGHT_LEG = VoxelShapes.or(CEILING_NORTH_SOUTH_RIGHT_POST, CEILING_NORTH_SOUTH_RIGHT_PIVOT);
    public static final VoxelShape CEILING_NORTH_SOUTH_ALL_LEGS = VoxelShapes.or(CEILING_NORTH_SOUTH_LEFT_LEG, CEILING_NORTH_SOUTH_RIGHT_LEG);
    public static final VoxelShape CEILING_NORTH_SOUTH_GRINDSTONE = VoxelShapes.or(CEILING_NORTH_SOUTH_ALL_LEGS, Block.box(4.0D, 0.0D, 2.0D, 12.0D, 12.0D, 14.0D));
    public static final VoxelShape CEILING_EAST_WEST_LEFT_POST = Block.box(6.0D, 9.0D, 2.0D, 10.0D, 16.0D, 4.0D);
    public static final VoxelShape CEILING_EAST_WEST_RIGHT_POST = Block.box(6.0D, 9.0D, 12.0D, 10.0D, 16.0D, 14.0D);
    public static final VoxelShape CEILING_EAST_WEST_LEFT_PIVOT = Block.box(5.0D, 3.0D, 2.0D, 11.0D, 9.0D, 4.0D);
    public static final VoxelShape CEILING_EAST_WEST_RIGHT_PIVOT = Block.box(5.0D, 3.0D, 12.0D, 11.0D, 9.0D, 14.0D);
    public static final VoxelShape CEILING_EAST_WEST_LEFT_LEG = VoxelShapes.or(CEILING_EAST_WEST_LEFT_POST, CEILING_EAST_WEST_LEFT_PIVOT);
    public static final VoxelShape CEILING_EAST_WEST_RIGHT_LEG = VoxelShapes.or(CEILING_EAST_WEST_RIGHT_POST, CEILING_EAST_WEST_RIGHT_PIVOT);
    public static final VoxelShape CEILING_EAST_WEST_ALL_LEGS = VoxelShapes.or(CEILING_EAST_WEST_LEFT_LEG, CEILING_EAST_WEST_RIGHT_LEG);
    public static final VoxelShape CEILING_EAST_WEST_GRINDSTONE = VoxelShapes.or(CEILING_EAST_WEST_ALL_LEGS, Block.box(2.0D, 0.0D, 4.0D, 14.0D, 12.0D, 12.0D));
    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.grindstone_title");

    protected BlockGrindstone(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(FACE, BlockPropertyAttachPosition.WALL));
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    private VoxelShape getVoxelShape(IBlockData state) {
        EnumDirection direction = state.get(FACING);
        switch((BlockPropertyAttachPosition)state.get(FACE)) {
        case FLOOR:
            if (direction != EnumDirection.NORTH && direction != EnumDirection.SOUTH) {
                return FLOOR_EAST_WEST_GRINDSTONE;
            }

            return FLOOR_NORTH_SOUTH_GRINDSTONE;
        case WALL:
            if (direction == EnumDirection.NORTH) {
                return WALL_NORTH_GRINDSTONE;
            } else if (direction == EnumDirection.SOUTH) {
                return WALL_SOUTH_GRINDSTONE;
            } else {
                if (direction == EnumDirection.EAST) {
                    return WALL_EAST_GRINDSTONE;
                }

                return WALL_WEST_GRINDSTONE;
            }
        case CEILING:
            if (direction != EnumDirection.NORTH && direction != EnumDirection.SOUTH) {
                return CEILING_EAST_WEST_GRINDSTONE;
            }

            return CEILING_NORTH_SOUTH_GRINDSTONE;
        default:
            return FLOOR_EAST_WEST_GRINDSTONE;
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getVoxelShape(state);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getVoxelShape(state);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return true;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            player.awardStat(StatisticList.INTERACT_WITH_GRINDSTONE);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> {
            return new ContainerGrindstone(syncId, inventory, ContainerAccess.at(world, pos));
        }, CONTAINER_TITLE);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
