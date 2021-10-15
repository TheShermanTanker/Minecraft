package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockDirectional;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyPistonType;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockPiston extends BlockDirectional {
    public static final BlockStateBoolean EXTENDED = BlockProperties.EXTENDED;
    public static final int TRIGGER_EXTEND = 0;
    public static final int TRIGGER_CONTRACT = 1;
    public static final int TRIGGER_DROP = 2;
    public static final float PLATFORM_THICKNESS = 4.0F;
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 4.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private final boolean isSticky;

    public BlockPiston(boolean sticky, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(EXTENDED, Boolean.valueOf(false)));
        this.isSticky = sticky;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        if (state.get(EXTENDED)) {
            switch((EnumDirection)state.get(FACING)) {
            case DOWN:
                return DOWN_AABB;
            case UP:
            default:
                return UP_AABB;
            case NORTH:
                return NORTH_AABB;
            case SOUTH:
                return SOUTH_AABB;
            case WEST:
                return WEST_AABB;
            case EAST:
                return EAST_AABB;
            }
        } else {
            return VoxelShapes.block();
        }
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (!world.isClientSide) {
            this.checkIfExtend(world, pos, state);
        }

    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            this.checkIfExtend(world, pos, state);
        }

    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (!world.isClientSide && world.getTileEntity(pos) == null) {
                this.checkIfExtend(world, pos, state);
            }

        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getNearestLookingDirection().opposite()).set(EXTENDED, Boolean.valueOf(false));
    }

    private void checkIfExtend(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(FACING);
        boolean bl = this.getNeighborSignal(world, pos, direction);
        if (bl && !state.get(EXTENDED)) {
            if ((new PistonExtendsChecker(world, pos, direction, true)).resolve()) {
                world.playBlockAction(pos, this, 0, direction.get3DDataValue());
            }
        } else if (!bl && state.get(EXTENDED)) {
            BlockPosition blockPos = pos.relative(direction, 2);
            IBlockData blockState = world.getType(blockPos);
            int i = 1;
            if (blockState.is(Blocks.MOVING_PISTON) && blockState.get(FACING) == direction) {
                TileEntity blockEntity = world.getTileEntity(blockPos);
                if (blockEntity instanceof TileEntityPiston) {
                    TileEntityPiston pistonMovingBlockEntity = (TileEntityPiston)blockEntity;
                    if (pistonMovingBlockEntity.isExtending() && (pistonMovingBlockEntity.getProgress(0.0F) < 0.5F || world.getTime() == pistonMovingBlockEntity.getLastTicked() || ((WorldServer)world).isHandlingTick())) {
                        i = 2;
                    }
                }
            }

            world.playBlockAction(pos, this, i, direction.get3DDataValue());
        }

    }

    private boolean getNeighborSignal(World world, BlockPosition pos, EnumDirection pistonFace) {
        for(EnumDirection direction : EnumDirection.values()) {
            if (direction != pistonFace && world.isBlockFacePowered(pos.relative(direction), direction)) {
                return true;
            }
        }

        if (world.isBlockFacePowered(pos, EnumDirection.DOWN)) {
            return true;
        } else {
            BlockPosition blockPos = pos.above();

            for(EnumDirection direction2 : EnumDirection.values()) {
                if (direction2 != EnumDirection.DOWN && world.isBlockFacePowered(blockPos.relative(direction2), direction2)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean triggerEvent(IBlockData state, World world, BlockPosition pos, int type, int data) {
        EnumDirection direction = state.get(FACING);
        if (!world.isClientSide) {
            boolean bl = this.getNeighborSignal(world, pos, direction);
            if (bl && (type == 1 || type == 2)) {
                world.setTypeAndData(pos, state.set(EXTENDED, Boolean.valueOf(true)), 2);
                return false;
            }

            if (!bl && type == 0) {
                return false;
            }
        }

        if (type == 0) {
            if (!this.moveBlocks(world, pos, direction, true)) {
                return false;
            }

            world.setTypeAndData(pos, state.set(EXTENDED, Boolean.valueOf(true)), 67);
            world.playSound((EntityHuman)null, pos, SoundEffects.PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.25F + 0.6F);
            world.gameEvent(GameEvent.PISTON_EXTEND, pos);
        } else if (type == 1 || type == 2) {
            TileEntity blockEntity = world.getTileEntity(pos.relative(direction));
            if (blockEntity instanceof TileEntityPiston) {
                ((TileEntityPiston)blockEntity).finalTick();
            }

            IBlockData blockState = Blocks.MOVING_PISTON.getBlockData().set(BlockPistonMoving.FACING, direction).set(BlockPistonMoving.TYPE, this.isSticky ? BlockPropertyPistonType.STICKY : BlockPropertyPistonType.DEFAULT);
            world.setTypeAndData(pos, blockState, 20);
            world.setTileEntity(BlockPistonMoving.newMovingBlockEntity(pos, blockState, this.getBlockData().set(FACING, EnumDirection.fromType1(data & 7)), direction, false, true));
            world.update(pos, blockState.getBlock());
            blockState.updateNeighbourShapes(world, pos, 2);
            if (this.isSticky) {
                BlockPosition blockPos = pos.offset(direction.getAdjacentX() * 2, direction.getAdjacentY() * 2, direction.getAdjacentZ() * 2);
                IBlockData blockState2 = world.getType(blockPos);
                boolean bl2 = false;
                if (blockState2.is(Blocks.MOVING_PISTON)) {
                    TileEntity blockEntity2 = world.getTileEntity(blockPos);
                    if (blockEntity2 instanceof TileEntityPiston) {
                        TileEntityPiston pistonMovingBlockEntity = (TileEntityPiston)blockEntity2;
                        if (pistonMovingBlockEntity.getDirection() == direction && pistonMovingBlockEntity.isExtending()) {
                            pistonMovingBlockEntity.finalTick();
                            bl2 = true;
                        }
                    }
                }

                if (!bl2) {
                    if (type != 1 || blockState2.isAir() || !isPushable(blockState2, world, blockPos, direction.opposite(), false, direction) || blockState2.getPushReaction() != EnumPistonReaction.NORMAL && !blockState2.is(Blocks.PISTON) && !blockState2.is(Blocks.STICKY_PISTON)) {
                        world.removeBlock(pos.relative(direction), false);
                    } else {
                        this.moveBlocks(world, pos, direction, false);
                    }
                }
            } else {
                world.removeBlock(pos.relative(direction), false);
            }

            world.playSound((EntityHuman)null, pos, SoundEffects.PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
            world.gameEvent(GameEvent.PISTON_CONTRACT, pos);
        }

        return true;
    }

    public static boolean isPushable(IBlockData state, World world, BlockPosition pos, EnumDirection direction, boolean canBreak, EnumDirection pistonDir) {
        if (pos.getY() >= world.getMinBuildHeight() && pos.getY() <= world.getMaxBuildHeight() - 1 && world.getWorldBorder().isWithinBounds(pos)) {
            if (state.isAir()) {
                return true;
            } else if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.CRYING_OBSIDIAN) && !state.is(Blocks.RESPAWN_ANCHOR)) {
                if (direction == EnumDirection.DOWN && pos.getY() == world.getMinBuildHeight()) {
                    return false;
                } else if (direction == EnumDirection.UP && pos.getY() == world.getMaxBuildHeight() - 1) {
                    return false;
                } else {
                    if (!state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)) {
                        if (state.getDestroySpeed(world, pos) == -1.0F) {
                            return false;
                        }

                        switch(state.getPushReaction()) {
                        case BLOCK:
                            return false;
                        case DESTROY:
                            return canBreak;
                        case PUSH_ONLY:
                            return direction == pistonDir;
                        }
                    } else if (state.get(EXTENDED)) {
                        return false;
                    }

                    return !state.isTileEntity();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean moveBlocks(World world, BlockPosition pos, EnumDirection dir, boolean retract) {
        BlockPosition blockPos = pos.relative(dir);
        if (!retract && world.getType(blockPos).is(Blocks.PISTON_HEAD)) {
            world.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 20);
        }

        PistonExtendsChecker pistonStructureResolver = new PistonExtendsChecker(world, pos, dir, retract);
        if (!pistonStructureResolver.resolve()) {
            return false;
        } else {
            Map<BlockPosition, IBlockData> map = Maps.newHashMap();
            List<BlockPosition> list = pistonStructureResolver.getMovedBlocks();
            List<IBlockData> list2 = Lists.newArrayList();

            for(int i = 0; i < list.size(); ++i) {
                BlockPosition blockPos2 = list.get(i);
                IBlockData blockState = world.getType(blockPos2);
                list2.add(blockState);
                map.put(blockPos2, blockState);
            }

            List<BlockPosition> list3 = pistonStructureResolver.getBrokenBlocks();
            IBlockData[] blockStates = new IBlockData[list.size() + list3.size()];
            EnumDirection direction = retract ? dir : dir.opposite();
            int j = 0;

            for(int k = list3.size() - 1; k >= 0; --k) {
                BlockPosition blockPos3 = list3.get(k);
                IBlockData blockState2 = world.getType(blockPos3);
                TileEntity blockEntity = blockState2.isTileEntity() ? world.getTileEntity(blockPos3) : null;
                dropResources(blockState2, world, blockPos3, blockEntity);
                world.setTypeAndData(blockPos3, Blocks.AIR.getBlockData(), 18);
                if (!blockState2.is(TagsBlock.FIRE)) {
                    world.addDestroyBlockEffect(blockPos3, blockState2);
                }

                blockStates[j++] = blockState2;
            }

            for(int l = list.size() - 1; l >= 0; --l) {
                BlockPosition blockPos4 = list.get(l);
                IBlockData blockState3 = world.getType(blockPos4);
                blockPos4 = blockPos4.relative(direction);
                map.remove(blockPos4);
                IBlockData blockState4 = Blocks.MOVING_PISTON.getBlockData().set(FACING, dir);
                world.setTypeAndData(blockPos4, blockState4, 68);
                world.setTileEntity(BlockPistonMoving.newMovingBlockEntity(blockPos4, blockState4, list2.get(l), dir, retract, false));
                blockStates[j++] = blockState3;
            }

            if (retract) {
                BlockPropertyPistonType pistonType = this.isSticky ? BlockPropertyPistonType.STICKY : BlockPropertyPistonType.DEFAULT;
                IBlockData blockState5 = Blocks.PISTON_HEAD.getBlockData().set(BlockPistonExtension.FACING, dir).set(BlockPistonExtension.TYPE, pistonType);
                IBlockData blockState6 = Blocks.MOVING_PISTON.getBlockData().set(BlockPistonMoving.FACING, dir).set(BlockPistonMoving.TYPE, this.isSticky ? BlockPropertyPistonType.STICKY : BlockPropertyPistonType.DEFAULT);
                map.remove(blockPos);
                world.setTypeAndData(blockPos, blockState6, 68);
                world.setTileEntity(BlockPistonMoving.newMovingBlockEntity(blockPos, blockState6, blockState5, dir, true, true));
            }

            IBlockData blockState7 = Blocks.AIR.getBlockData();

            for(BlockPosition blockPos5 : map.keySet()) {
                world.setTypeAndData(blockPos5, blockState7, 82);
            }

            for(Entry<BlockPosition, IBlockData> entry : map.entrySet()) {
                BlockPosition blockPos6 = entry.getKey();
                IBlockData blockState8 = entry.getValue();
                blockState8.updateIndirectNeighbourShapes(world, blockPos6, 2);
                blockState7.updateNeighbourShapes(world, blockPos6, 2);
                blockState7.updateIndirectNeighbourShapes(world, blockPos6, 2);
            }

            j = 0;

            for(int m = list3.size() - 1; m >= 0; --m) {
                IBlockData blockState9 = blockStates[j++];
                BlockPosition blockPos7 = list3.get(m);
                blockState9.updateIndirectNeighbourShapes(world, blockPos7, 2);
                world.applyPhysics(blockPos7, blockState9.getBlock());
            }

            for(int n = list.size() - 1; n >= 0; --n) {
                world.applyPhysics(list.get(n), blockStates[j++].getBlock());
            }

            if (retract) {
                world.applyPhysics(blockPos, Blocks.PISTON_HEAD);
            }

            return true;
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
        builder.add(FACING, EXTENDED);
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return state.get(EXTENDED);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
