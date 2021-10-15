package net.minecraft.world.level.block;

import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public abstract class BlockCauldronAbstract extends Block {
    private static final int SIDE_THICKNESS = 2;
    private static final int LEG_WIDTH = 4;
    private static final int LEG_HEIGHT = 3;
    private static final int LEG_DEPTH = 2;
    protected static final int FLOOR_LEVEL = 4;
    private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    protected static final VoxelShape SHAPE = VoxelShapes.join(VoxelShapes.block(), VoxelShapes.or(box(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D), box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D), box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), INSIDE), OperatorBoolean.ONLY_FIRST);
    private final Map<Item, CauldronInteraction> interactions;

    public BlockCauldronAbstract(BlockBase.Info settings, Map<Item, CauldronInteraction> behaviorMap) {
        super(settings);
        this.interactions = behaviorMap;
    }

    protected double getContentHeight(IBlockData state) {
        return 0.0D;
    }

    protected boolean isEntityInsideContent(IBlockData state, BlockPosition pos, Entity entity) {
        return entity.locY() < (double)pos.getY() + this.getContentHeight(state) && entity.getBoundingBox().maxY > (double)pos.getY() + 0.25D;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        CauldronInteraction cauldronInteraction = this.interactions.get(itemStack.getItem());
        return cauldronInteraction.interact(state, world, pos, player, hand, itemStack);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return INSIDE;
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    public abstract boolean isFull(IBlockData state);

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        BlockPosition blockPos = BlockDripstonePointed.findStalactiteTipAboveCauldron(world, pos);
        if (blockPos != null) {
            FluidType fluid = BlockDripstonePointed.getCauldronFillFluidType(world, blockPos);
            if (fluid != FluidTypes.EMPTY && this.canReceiveStalactiteDrip(fluid)) {
                this.receiveStalactiteDrip(state, world, pos, fluid);
            }

        }
    }

    protected boolean canReceiveStalactiteDrip(FluidType fluid) {
        return false;
    }

    protected void receiveStalactiteDrip(IBlockData state, World world, BlockPosition pos, FluidType fluid) {
    }
}
