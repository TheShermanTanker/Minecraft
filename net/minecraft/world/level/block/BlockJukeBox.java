package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityJukeBox;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockJukeBox extends BlockTileEntity {
    public static final BlockStateBoolean HAS_RECORD = BlockProperties.HAS_RECORD;

    protected BlockJukeBox(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HAS_RECORD, Boolean.valueOf(false)));
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        super.postPlace(world, pos, state, placer, itemStack);
        NBTTagCompound compoundTag = itemStack.getOrCreateTag();
        if (compoundTag.hasKey("BlockEntityTag")) {
            NBTTagCompound compoundTag2 = compoundTag.getCompound("BlockEntityTag");
            if (compoundTag2.hasKey("RecordItem")) {
                world.setTypeAndData(pos, state.set(HAS_RECORD, Boolean.valueOf(true)), 2);
            }
        }

    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (state.get(HAS_RECORD)) {
            this.dropRecord(world, pos);
            state = state.set(HAS_RECORD, Boolean.valueOf(false));
            world.setTypeAndData(pos, state, 2);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    public void setRecord(GeneratorAccess world, BlockPosition pos, IBlockData state, ItemStack stack) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityJukeBox) {
            ((TileEntityJukeBox)blockEntity).setRecord(stack.cloneItemStack());
            world.setTypeAndData(pos, state.set(HAS_RECORD, Boolean.valueOf(true)), 2);
        }
    }

    public void dropRecord(World world, BlockPosition pos) {
        if (!world.isClientSide) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityJukeBox) {
                TileEntityJukeBox jukeboxBlockEntity = (TileEntityJukeBox)blockEntity;
                ItemStack itemStack = jukeboxBlockEntity.getRecord();
                if (!itemStack.isEmpty()) {
                    world.triggerEffect(1010, pos, 0);
                    jukeboxBlockEntity.clear();
                    float f = 0.7F;
                    double d = (double)(world.random.nextFloat() * 0.7F) + (double)0.15F;
                    double e = (double)(world.random.nextFloat() * 0.7F) + (double)0.060000002F + 0.6D;
                    double g = (double)(world.random.nextFloat() * 0.7F) + (double)0.15F;
                    ItemStack itemStack2 = itemStack.cloneItemStack();
                    EntityItem itemEntity = new EntityItem(world, (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + g, itemStack2);
                    itemEntity.defaultPickupDelay();
                    world.addEntity(itemEntity);
                }
            }
        }
    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            this.dropRecord(world, pos);
            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityJukeBox(pos, state);
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityJukeBox) {
            Item item = ((TileEntityJukeBox)blockEntity).getRecord().getItem();
            if (item instanceof ItemRecord) {
                return ((ItemRecord)item).getAnalogOutput();
            }
        }

        return 0;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HAS_RECORD);
    }
}
