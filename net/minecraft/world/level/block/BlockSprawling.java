package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockSprawling extends Block {
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    public static final BlockStateBoolean NORTH = BlockProperties.NORTH;
    public static final BlockStateBoolean EAST = BlockProperties.EAST;
    public static final BlockStateBoolean SOUTH = BlockProperties.SOUTH;
    public static final BlockStateBoolean WEST = BlockProperties.WEST;
    public static final BlockStateBoolean UP = BlockProperties.UP;
    public static final BlockStateBoolean DOWN = BlockProperties.DOWN;
    public static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(SystemUtils.make(Maps.newEnumMap(EnumDirection.class), (directions) -> {
        directions.put(EnumDirection.NORTH, NORTH);
        directions.put(EnumDirection.EAST, EAST);
        directions.put(EnumDirection.SOUTH, SOUTH);
        directions.put(EnumDirection.WEST, WEST);
        directions.put(EnumDirection.UP, UP);
        directions.put(EnumDirection.DOWN, DOWN);
    }));
    protected final VoxelShape[] shapeByIndex;

    protected BlockSprawling(float radius, BlockBase.Info settings) {
        super(settings);
        this.shapeByIndex = this.makeShapes(radius);
    }

    private VoxelShape[] makeShapes(float radius) {
        float f = 0.5F - radius;
        float g = 0.5F + radius;
        VoxelShape voxelShape = Block.box((double)(f * 16.0F), (double)(f * 16.0F), (double)(f * 16.0F), (double)(g * 16.0F), (double)(g * 16.0F), (double)(g * 16.0F));
        VoxelShape[] voxelShapes = new VoxelShape[DIRECTIONS.length];

        for(int i = 0; i < DIRECTIONS.length; ++i) {
            EnumDirection direction = DIRECTIONS[i];
            voxelShapes[i] = VoxelShapes.box(0.5D + Math.min((double)(-radius), (double)direction.getAdjacentX() * 0.5D), 0.5D + Math.min((double)(-radius), (double)direction.getAdjacentY() * 0.5D), 0.5D + Math.min((double)(-radius), (double)direction.getAdjacentZ() * 0.5D), 0.5D + Math.max((double)radius, (double)direction.getAdjacentX() * 0.5D), 0.5D + Math.max((double)radius, (double)direction.getAdjacentY() * 0.5D), 0.5D + Math.max((double)radius, (double)direction.getAdjacentZ() * 0.5D));
        }

        VoxelShape[] voxelShapes2 = new VoxelShape[64];

        for(int j = 0; j < 64; ++j) {
            VoxelShape voxelShape2 = voxelShape;

            for(int k = 0; k < DIRECTIONS.length; ++k) {
                if ((j & 1 << k) != 0) {
                    voxelShape2 = VoxelShapes.or(voxelShape2, voxelShapes[k]);
                }
            }

            voxelShapes2[j] = voxelShape2;
        }

        return voxelShapes2;
    }

    @Override
    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return false;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.shapeByIndex[this.getAABBIndex(state)];
    }

    protected int getAABBIndex(IBlockData state) {
        int i = 0;

        for(int j = 0; j < DIRECTIONS.length; ++j) {
            if (state.get(PROPERTY_BY_DIRECTION.get(DIRECTIONS[j]))) {
                i |= 1 << j;
            }
        }

        return i;
    }
}
