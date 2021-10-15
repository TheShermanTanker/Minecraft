package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLectern;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockLectern extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean HAS_BOOK = BlockProperties.HAS_BOOK;
    public static final VoxelShape SHAPE_BASE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    public static final VoxelShape SHAPE_POST = Block.box(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D);
    public static final VoxelShape SHAPE_COMMON = VoxelShapes.or(SHAPE_BASE, SHAPE_POST);
    public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    public static final VoxelShape SHAPE_COLLISION = VoxelShapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
    public static final VoxelShape SHAPE_WEST = VoxelShapes.or(Block.box(1.0D, 10.0D, 0.0D, 5.333333D, 14.0D, 16.0D), Block.box(5.333333D, 12.0D, 0.0D, 9.666667D, 16.0D, 16.0D), Block.box(9.666667D, 14.0D, 0.0D, 14.0D, 18.0D, 16.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_NORTH = VoxelShapes.or(Block.box(0.0D, 10.0D, 1.0D, 16.0D, 14.0D, 5.333333D), Block.box(0.0D, 12.0D, 5.333333D, 16.0D, 16.0D, 9.666667D), Block.box(0.0D, 14.0D, 9.666667D, 16.0D, 18.0D, 14.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_EAST = VoxelShapes.or(Block.box(10.666667D, 10.0D, 0.0D, 15.0D, 14.0D, 16.0D), Block.box(6.333333D, 12.0D, 0.0D, 10.666667D, 16.0D, 16.0D), Block.box(2.0D, 14.0D, 0.0D, 6.333333D, 18.0D, 16.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_SOUTH = VoxelShapes.or(Block.box(0.0D, 10.0D, 10.666667D, 16.0D, 14.0D, 15.0D), Block.box(0.0D, 12.0D, 6.333333D, 16.0D, 16.0D, 10.666667D), Block.box(0.0D, 14.0D, 2.0D, 16.0D, 18.0D, 6.333333D), SHAPE_COMMON);
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    protected BlockLectern(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(POWERED, Boolean.valueOf(false)).set(HAS_BOOK, Boolean.valueOf(false)));
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return SHAPE_COMMON;
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        World level = ctx.getWorld();
        ItemStack itemStack = ctx.getItemStack();
        NBTTagCompound compoundTag = itemStack.getTag();
        EntityHuman player = ctx.getEntity();
        boolean bl = false;
        if (!level.isClientSide && player != null && compoundTag != null && player.isCreativeAndOp() && compoundTag.hasKey("BlockEntityTag")) {
            NBTTagCompound compoundTag2 = compoundTag.getCompound("BlockEntityTag");
            if (compoundTag2.hasKey("Book")) {
                bl = true;
            }
        }

        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite()).set(HAS_BOOK, Boolean.valueOf(bl));
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE_COLLISION;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection)state.get(FACING)) {
        case NORTH:
            return SHAPE_NORTH;
        case SOUTH:
            return SHAPE_SOUTH;
        case EAST:
            return SHAPE_EAST;
        case WEST:
            return SHAPE_WEST;
        default:
            return SHAPE_COMMON;
        }
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
        builder.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityLectern(pos, state);
    }

    public static boolean tryPlaceBook(@Nullable EntityHuman player, World world, BlockPosition pos, IBlockData state, ItemStack stack) {
        if (!state.get(HAS_BOOK)) {
            if (!world.isClientSide) {
                placeBook(player, world, pos, state, stack);
            }

            return true;
        } else {
            return false;
        }
    }

    private static void placeBook(@Nullable EntityHuman player, World world, BlockPosition pos, IBlockData state, ItemStack stack) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityLectern) {
            TileEntityLectern lecternBlockEntity = (TileEntityLectern)blockEntity;
            lecternBlockEntity.setBook(stack.cloneAndSubtract(1));
            setHasBook(world, pos, state, true);
            world.playSound((EntityHuman)null, pos, SoundEffects.BOOK_PUT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }

    }

    public static void setHasBook(World world, BlockPosition pos, IBlockData state, boolean hasBook) {
        world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(false)).set(HAS_BOOK, Boolean.valueOf(hasBook)), 3);
        updateBelow(world, pos, state);
    }

    public static void signalPageChange(World world, BlockPosition pos, IBlockData state) {
        changePowered(world, pos, state, true);
        world.getBlockTickList().scheduleTick(pos, state.getBlock(), 2);
        world.triggerEffect(1043, pos, 0);
    }

    private static void changePowered(World world, BlockPosition pos, IBlockData state, boolean powered) {
        world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(powered)), 3);
        updateBelow(world, pos, state);
    }

    private static void updateBelow(World world, BlockPosition pos, IBlockData state) {
        world.applyPhysics(pos.below(), state.getBlock());
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        changePowered(world, pos, state, false);
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (state.get(HAS_BOOK)) {
                this.popBook(state, world, pos);
            }

            if (state.get(POWERED)) {
                world.applyPhysics(pos.below(), this);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    private void popBook(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityLectern) {
            TileEntityLectern lecternBlockEntity = (TileEntityLectern)blockEntity;
            EnumDirection direction = state.get(FACING);
            ItemStack itemStack = lecternBlockEntity.getBook().cloneItemStack();
            float f = 0.25F * (float)direction.getAdjacentX();
            float g = 0.25F * (float)direction.getAdjacentZ();
            EntityItem itemEntity = new EntityItem(world, (double)pos.getX() + 0.5D + (double)f, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D + (double)g, itemStack);
            itemEntity.defaultPickupDelay();
            world.addEntity(itemEntity);
            lecternBlockEntity.clear();
        }

    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return direction == EnumDirection.UP && state.get(POWERED) ? 15 : 0;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        if (state.get(HAS_BOOK)) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityLectern) {
                return ((TileEntityLectern)blockEntity).getRedstoneSignal();
            }
        }

        return 0;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (state.get(HAS_BOOK)) {
            if (!world.isClientSide) {
                this.openScreen(world, pos, player);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            ItemStack itemStack = player.getItemInHand(hand);
            return !itemStack.isEmpty() && !itemStack.is(TagsItem.LECTERN_BOOKS) ? EnumInteractionResult.CONSUME : EnumInteractionResult.PASS;
        }
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return !state.get(HAS_BOOK) ? null : super.getInventory(state, world, pos);
    }

    private void openScreen(World world, BlockPosition pos, EntityHuman player) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityLectern) {
            player.openContainer((TileEntityLectern)blockEntity);
            player.awardStat(StatisticList.INTERACT_WITH_LECTERN);
        }

    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
