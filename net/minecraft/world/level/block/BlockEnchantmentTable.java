package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerEnchantTable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEnchantTable;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockEnchantmentTable extends BlockTileEntity {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    protected BlockEnchantmentTable(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return true;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        super.animateTick(state, world, pos, random);

        for(int i = -2; i <= 2; ++i) {
            for(int j = -2; j <= 2; ++j) {
                if (i > -2 && i < 2 && j == -1) {
                    j = 2;
                }

                if (random.nextInt(16) == 0) {
                    for(int k = 0; k <= 1; ++k) {
                        BlockPosition blockPos = pos.offset(i, k, j);
                        if (world.getType(blockPos).is(Blocks.BOOKSHELF)) {
                            if (!world.isEmpty(pos.offset(i / 2, 0, j / 2))) {
                                break;
                            }

                            world.addParticle(Particles.ENCHANT, (double)pos.getX() + 0.5D, (double)pos.getY() + 2.0D, (double)pos.getZ() + 0.5D, (double)((float)i + random.nextFloat()) - 0.5D, (double)((float)k - random.nextFloat() - 1.0F), (double)((float)j + random.nextFloat()) - 0.5D);
                        }
                    }
                }
            }
        }

    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityEnchantTable(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? createTickerHelper(type, TileEntityTypes.ENCHANTING_TABLE, TileEntityEnchantTable::bookAnimationTick) : null;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            player.openContainer(state.getMenuProvider(world, pos));
            return EnumInteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityEnchantTable) {
            IChatBaseComponent component = ((INamableTileEntity)blockEntity).getScoreboardDisplayName();
            return new TileInventory((syncId, inventory, player) -> {
                return new ContainerEnchantTable(syncId, inventory, ContainerAccess.at(world, pos));
            }, component);
        } else {
            return null;
        }
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityEnchantTable) {
                ((TileEntityEnchantTable)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }
}
