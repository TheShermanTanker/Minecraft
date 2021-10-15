package net.minecraft.world.level.block;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyWood;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public abstract class BlockSign extends BlockTileEntity implements IBlockWaterlogged {
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    protected static final float AABB_OFFSET = 4.0F;
    protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private final BlockPropertyWood type;

    protected BlockSign(BlockBase.Info settings, BlockPropertyWood type) {
        super(settings);
        this.type = type;
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickList().scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return true;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntitySign(pos, state);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        Item item = itemStack.getItem();
        boolean bl = item instanceof ItemDye;
        boolean bl2 = itemStack.is(Items.GLOW_INK_SAC);
        boolean bl3 = itemStack.is(Items.INK_SAC);
        boolean bl4 = (bl2 || bl || bl3) && player.getAbilities().mayBuild;
        if (world.isClientSide) {
            return bl4 ? EnumInteractionResult.SUCCESS : EnumInteractionResult.CONSUME;
        } else {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (!(blockEntity instanceof TileEntitySign)) {
                return EnumInteractionResult.PASS;
            } else {
                TileEntitySign signBlockEntity = (TileEntitySign)blockEntity;
                boolean bl5 = signBlockEntity.hasGlowingText();
                if ((!bl2 || !bl5) && (!bl3 || bl5)) {
                    if (bl4) {
                        boolean bl6;
                        if (bl2) {
                            world.playSound((EntityHuman)null, pos, SoundEffects.GLOW_INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            bl6 = signBlockEntity.setHasGlowingText(true);
                            if (player instanceof EntityPlayer) {
                                CriterionTriggers.ITEM_USED_ON_BLOCK.trigger((EntityPlayer)player, pos, itemStack);
                            }
                        } else if (bl3) {
                            world.playSound((EntityHuman)null, pos, SoundEffects.INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            bl6 = signBlockEntity.setHasGlowingText(false);
                        } else {
                            world.playSound((EntityHuman)null, pos, SoundEffects.DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            bl6 = signBlockEntity.setColor(((ItemDye)item).getDyeColor());
                        }

                        if (bl6) {
                            if (!player.isCreative()) {
                                itemStack.subtract(1);
                            }

                            player.awardStat(StatisticList.ITEM_USED.get(item));
                        }
                    }

                    return signBlockEntity.executeClickCommands((EntityPlayer)player) ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
                } else {
                    return EnumInteractionResult.PASS;
                }
            }
        }
    }

    @Override
    public Fluid getFluidState(IBlockData state) {
        return state.get(WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(state);
    }

    public BlockPropertyWood type() {
        return this.type;
    }
}
