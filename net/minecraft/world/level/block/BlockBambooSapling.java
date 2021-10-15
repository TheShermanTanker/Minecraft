package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSword;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBambooSapling extends Block implements IBlockFragilePlantElement {
    protected static final float SAPLING_AABB_OFFSET = 4.0F;
    protected static final VoxelShape SAPLING_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D);

    public BlockBambooSapling(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        Vec3D vec3 = state.getOffset(world, pos);
        return SAPLING_SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (random.nextInt(3) == 0 && world.isEmpty(pos.above()) && world.getLightLevel(pos.above(), 0) >= 9) {
            this.growBamboo(world, pos);
        }

    }

    @Override
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return world.getType(pos.below()).is(TagsBlock.BAMBOO_PLANTABLE_ON);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (!state.canPlace(world, pos)) {
            return Blocks.AIR.getBlockData();
        } else {
            if (direction == EnumDirection.UP && neighborState.is(Blocks.BAMBOO)) {
                world.setTypeAndData(pos, Blocks.BAMBOO.getBlockData(), 2);
            }

            return super.updateState(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(Items.BAMBOO);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess world, BlockPosition pos, IBlockData state, boolean isClient) {
        return world.getType(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition pos, IBlockData state) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer world, Random random, BlockPosition pos, IBlockData state) {
        this.growBamboo(world, pos);
    }

    @Override
    public float getDamage(IBlockData state, EntityHuman player, IBlockAccess world, BlockPosition pos) {
        return player.getItemInMainHand().getItem() instanceof ItemSword ? 1.0F : super.getDamage(state, player, world, pos);
    }

    protected void growBamboo(World world, BlockPosition pos) {
        world.setTypeAndData(pos.above(), Blocks.BAMBOO.getBlockData().set(BlockBamboo.LEAVES, BlockPropertyBambooSize.SMALL), 3);
    }
}
