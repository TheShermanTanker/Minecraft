package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockFlowerPot extends Block {
    private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
    public static final float AABB_SIZE = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    private final Block content;

    public BlockFlowerPot(Block content, BlockBase.Info settings) {
        super(settings);
        this.content = content;
        POTTED_BY_CONTENT.put(content, this);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        IBlockData blockState = (item instanceof ItemBlock ? POTTED_BY_CONTENT.getOrDefault(((ItemBlock)item).getBlock(), Blocks.AIR) : Blocks.AIR).getBlockData();
        boolean bl = blockState.is(Blocks.AIR);
        boolean bl2 = this.isEmpty();
        if (bl != bl2) {
            if (bl2) {
                world.setTypeAndData(pos, blockState, 3);
                player.awardStat(StatisticList.POT_FLOWER);
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }
            } else {
                ItemStack itemStack2 = new ItemStack(this.content);
                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, itemStack2);
                } else if (!player.addItem(itemStack2)) {
                    player.drop(itemStack2, false);
                }

                world.setTypeAndData(pos, Blocks.FLOWER_POT.getBlockData(), 3);
            }

            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return this.isEmpty() ? super.getCloneItemStack(world, pos, state) : new ItemStack(this.content);
    }

    private boolean isEmpty() {
        return this.content == Blocks.AIR;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN && !state.canPlace(world, pos) ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    public Block getContent() {
        return this.content;
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
