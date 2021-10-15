package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLightDetector;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockDaylightDetector extends BlockTileEntity {
    public static final BlockStateInteger POWER = BlockProperties.POWER;
    public static final BlockStateBoolean INVERTED = BlockProperties.INVERTED;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);

    public BlockDaylightDetector(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(POWER, Integer.valueOf(0)).set(INVERTED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return state.get(POWER);
    }

    private static void updateSignalStrength(IBlockData state, World world, BlockPosition pos) {
        int i = world.getBrightness(EnumSkyBlock.SKY, pos) - world.getSkyDarken();
        float f = world.getSunAngle(1.0F);
        boolean bl = state.get(INVERTED);
        if (bl) {
            i = 15 - i;
        } else if (i > 0) {
            float g = f < (float)Math.PI ? 0.0F : ((float)Math.PI * 2F);
            f = f + (g - f) * 0.2F;
            i = Math.round((float)i * MathHelper.cos(f));
        }

        i = MathHelper.clamp(i, 0, 15);
        if (state.get(POWER) != i) {
            world.setTypeAndData(pos, state.set(POWER, Integer.valueOf(i)), 3);
        }

    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (player.mayBuild()) {
            if (world.isClientSide) {
                return EnumInteractionResult.SUCCESS;
            } else {
                IBlockData blockState = state.cycle(INVERTED);
                world.setTypeAndData(pos, blockState, 4);
                updateSignalStrength(blockState, world, pos);
                return EnumInteractionResult.CONSUME;
            }
        } else {
            return super.interact(state, world, pos, player, hand, hit);
        }
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public boolean isPowerSource(IBlockData state) {
        return true;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityLightDetector(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return !world.isClientSide && world.getDimensionManager().hasSkyLight() ? createTickerHelper(type, TileEntityTypes.DAYLIGHT_DETECTOR, BlockDaylightDetector::tickEntity) : null;
    }

    private static void tickEntity(World world, BlockPosition pos, IBlockData state, TileEntityLightDetector blockEntity) {
        if (world.getTime() % 20L == 0L) {
            updateSignalStrength(state, world, pos);
        }

    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(POWER, INVERTED);
    }
}
