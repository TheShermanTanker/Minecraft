package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockLight extends Block implements IBlockWaterlogged {
    public static final int MAX_LEVEL = 15;
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final ToIntFunction<IBlockData> LIGHT_EMISSION = (state) -> {
        return state.get(LEVEL);
    };

    public BlockLight(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LEVEL, Integer.valueOf(15)).set(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LEVEL, WATERLOGGED);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (!world.isClientSide) {
            world.setTypeAndData(pos, state.cycle(LEVEL), 2);
            return EnumInteractionResult.SUCCESS;
        } else {
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return context.isHoldingItem(Items.LIGHT) ? VoxelShapes.block() : VoxelShapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return true;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return 1.0F;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        ItemStack itemStack = super.getCloneItemStack(world, pos, state);
        if (state.get(LEVEL) != 15) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setString(LEVEL.getName(), String.valueOf((Object)state.get(LEVEL)));
            itemStack.addTagElement("BlockStateTag", compoundTag);
        }

        return itemStack;
    }
}
