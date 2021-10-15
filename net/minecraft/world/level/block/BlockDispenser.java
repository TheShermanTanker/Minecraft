package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.core.Position;
import net.minecraft.core.SourceBlock;
import net.minecraft.core.dispenser.DispenseBehaviorItem;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.entity.TileEntityDropper;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockDispenser extends BlockTileEntity {
    public static final BlockStateDirection FACING = BlockDirectional.FACING;
    public static final BlockStateBoolean TRIGGERED = BlockProperties.TRIGGERED;
    public static final Map<Item, IDispenseBehavior> DISPENSER_REGISTRY = SystemUtils.make(new Object2ObjectOpenHashMap<>(), (object2ObjectOpenHashMap) -> {
        object2ObjectOpenHashMap.defaultReturnValue(new DispenseBehaviorItem());
    });
    private static final int TRIGGER_DURATION = 4;

    public static void registerBehavior(IMaterial provider, IDispenseBehavior behavior) {
        DISPENSER_REGISTRY.put(provider.getItem(), behavior);
    }

    protected BlockDispenser(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(TRIGGERED, Boolean.valueOf(false)));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityDispenser) {
                player.openContainer((TileEntityDispenser)blockEntity);
                if (blockEntity instanceof TileEntityDropper) {
                    player.awardStat(StatisticList.INSPECT_DROPPER);
                } else {
                    player.awardStat(StatisticList.INSPECT_DISPENSER);
                }
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    public void dispense(WorldServer world, BlockPosition pos) {
        SourceBlock blockSourceImpl = new SourceBlock(world, pos);
        TileEntityDispenser dispenserBlockEntity = blockSourceImpl.getTileEntity();
        int i = dispenserBlockEntity.getRandomSlot();
        if (i < 0) {
            world.triggerEffect(1001, pos, 0);
            world.gameEvent(GameEvent.DISPENSE_FAIL, pos);
        } else {
            ItemStack itemStack = dispenserBlockEntity.getItem(i);
            IDispenseBehavior dispenseItemBehavior = this.getDispenseMethod(itemStack);
            if (dispenseItemBehavior != IDispenseBehavior.NOOP) {
                dispenserBlockEntity.setItem(i, dispenseItemBehavior.dispense(blockSourceImpl, itemStack));
            }

        }
    }

    protected IDispenseBehavior getDispenseMethod(ItemStack stack) {
        return DISPENSER_REGISTRY.get(stack.getItem());
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        boolean bl = world.isBlockIndirectlyPowered(pos) || world.isBlockIndirectlyPowered(pos.above());
        boolean bl2 = state.get(TRIGGERED);
        if (bl && !bl2) {
            world.getBlockTickList().scheduleTick(pos, this, 4);
            world.setTypeAndData(pos, state.set(TRIGGERED, Boolean.valueOf(true)), 4);
        } else if (!bl && bl2) {
            world.setTypeAndData(pos, state.set(TRIGGERED, Boolean.valueOf(false)), 4);
        }

    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.dispense(world, pos);
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityDispenser(pos, state);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getNearestLookingDirection().opposite());
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityDispenser) {
                ((TileEntityDispenser)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityDispenser) {
                InventoryUtils.dropInventory(world, pos, (TileEntityDispenser)blockEntity);
                world.updateAdjacentComparators(pos, this);
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    public static IPosition getDispensePosition(ISourceBlock pointer) {
        EnumDirection direction = pointer.getBlockData().get(FACING);
        double d = pointer.getX() + 0.7D * (double)direction.getAdjacentX();
        double e = pointer.getY() + 0.7D * (double)direction.getAdjacentY();
        double f = pointer.getZ() + 0.7D * (double)direction.getAdjacentZ();
        return new Position(d, e, f);
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return Container.getRedstoneSignalFromBlockEntity(world.getTileEntity(pos));
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
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
        builder.add(FACING, TRIGGERED);
    }
}
