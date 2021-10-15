package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBanner extends BlockBannerAbstract {
    public static final BlockStateInteger ROTATION = BlockProperties.ROTATION_16;
    private static final Map<EnumColor, Block> BY_COLOR = Maps.newHashMap();
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public BlockBanner(EnumColor color, BlockBase.Info settings) {
        super(color, settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(ROTATION, Integer.valueOf(0)));
        BY_COLOR.put(color, this);
    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.below()).getMaterial().isBuildable();
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(ROTATION, Integer.valueOf(MathHelper.floor((double)((180.0F + ctx.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(ROTATION, Integer.valueOf(rotation.rotate(state.get(ROTATION), 16)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.set(ROTATION, Integer.valueOf(mirror.mirror(state.get(ROTATION), 16)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(ROTATION);
    }

    public static Block byColor(EnumColor color) {
        return BY_COLOR.getOrDefault(color, Blocks.WHITE_BANNER);
    }
}
