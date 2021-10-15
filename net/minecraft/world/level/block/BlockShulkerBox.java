package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityShulkerBox;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockShulkerBox extends BlockTileEntity {
    public static final BlockStateEnum<EnumDirection> FACING = BlockDirectional.FACING;
    public static final MinecraftKey CONTENTS = new MinecraftKey("contents");
    @Nullable
    public final EnumColor color;

    public BlockShulkerBox(@Nullable EnumColor color, BlockBase.Info settings) {
        super(settings);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.UP));
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityShulkerBox(this.color, pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return createTickerHelper(type, TileEntityTypes.SHULKER_BOX, TileEntityShulkerBox::tick);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else if (player.isSpectator()) {
            return EnumInteractionResult.CONSUME;
        } else {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityShulkerBox) {
                TileEntityShulkerBox shulkerBoxBlockEntity = (TileEntityShulkerBox)blockEntity;
                if (canOpen(state, world, pos, shulkerBoxBlockEntity)) {
                    player.openContainer(shulkerBoxBlockEntity);
                    player.awardStat(StatisticList.OPEN_SHULKER_BOX);
                    PiglinAI.angerNearbyPiglins(player, true);
                }

                return EnumInteractionResult.CONSUME;
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    private static boolean canOpen(IBlockData state, World world, BlockPosition pos, TileEntityShulkerBox entity) {
        if (entity.getAnimationStatus() != TileEntityShulkerBox.AnimationPhase.CLOSED) {
            return true;
        } else {
            AxisAlignedBB aABB = EntityShulker.getProgressDeltaAabb(state.get(FACING), 0.0F, 0.5F).move(pos).shrink(1.0E-6D);
            return world.noCollision(aABB);
        }
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING);
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityShulkerBox) {
            TileEntityShulkerBox shulkerBoxBlockEntity = (TileEntityShulkerBox)blockEntity;
            if (!world.isClientSide && player.isCreative() && !shulkerBoxBlockEntity.isEmpty()) {
                ItemStack itemStack = getColoredItemStack(this.getColor());
                NBTTagCompound compoundTag = shulkerBoxBlockEntity.saveToTag(new NBTTagCompound());
                if (!compoundTag.isEmpty()) {
                    itemStack.addTagElement("BlockEntityTag", compoundTag);
                }

                if (shulkerBoxBlockEntity.hasCustomName()) {
                    itemStack.setHoverName(shulkerBoxBlockEntity.getCustomName());
                }

                EntityItem itemEntity = new EntityItem(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, itemStack);
                itemEntity.defaultPickupDelay();
                world.addEntity(itemEntity);
            } else {
                shulkerBoxBlockEntity.unpackLootTable(player);
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        TileEntity blockEntity = builder.getOptionalParameter(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof TileEntityShulkerBox) {
            TileEntityShulkerBox shulkerBoxBlockEntity = (TileEntityShulkerBox)blockEntity;
            builder = builder.withDynamicDrop(CONTENTS, (lootContext, consumer) -> {
                for(int i = 0; i < shulkerBoxBlockEntity.getSize(); ++i) {
                    consumer.accept(shulkerBoxBlockEntity.getItem(i));
                }

            });
        }

        return super.getDrops(state, builder);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        if (itemStack.hasName()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityShulkerBox) {
                ((TileEntityShulkerBox)blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityShulkerBox) {
                world.updateAdjacentComparators(pos, state.getBlock());
            }

            super.remove(state, world, pos, newState, moved);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable IBlockAccess world, List<IChatBaseComponent> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, world, tooltip, options);
        NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
        if (compoundTag != null) {
            if (compoundTag.hasKeyOfType("LootTable", 8)) {
                tooltip.add(new ChatComponentText("???????"));
            }

            if (compoundTag.hasKeyOfType("Items", 9)) {
                NonNullList<ItemStack> nonNullList = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerUtil.loadAllItems(compoundTag, nonNullList);
                int i = 0;
                int j = 0;

                for(ItemStack itemStack : nonNullList) {
                    if (!itemStack.isEmpty()) {
                        ++j;
                        if (i <= 4) {
                            ++i;
                            IChatMutableComponent mutableComponent = itemStack.getName().mutableCopy();
                            mutableComponent.append(" x").append(String.valueOf(itemStack.getCount()));
                            tooltip.add(mutableComponent);
                        }
                    }
                }

                if (j - i > 0) {
                    tooltip.add((new ChatMessage("container.shulkerBox.more", j - i)).withStyle(EnumChatFormat.ITALIC));
                }
            }
        }

    }

    @Override
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return EnumPistonReaction.DESTROY;
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof TileEntityShulkerBox ? VoxelShapes.create(((TileEntityShulkerBox)blockEntity).getBoundingBox(state)) : VoxelShapes.block();
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return Container.getRedstoneSignalFromContainer((IInventory)world.getTileEntity(pos));
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        ItemStack itemStack = super.getCloneItemStack(world, pos, state);
        TileEntityShulkerBox shulkerBoxBlockEntity = (TileEntityShulkerBox)world.getTileEntity(pos);
        NBTTagCompound compoundTag = shulkerBoxBlockEntity.saveToTag(new NBTTagCompound());
        if (!compoundTag.isEmpty()) {
            itemStack.addTagElement("BlockEntityTag", compoundTag);
        }

        return itemStack;
    }

    @Nullable
    public static EnumColor getColorFromItem(Item item) {
        return getColorFromBlock(Block.asBlock(item));
    }

    @Nullable
    public static EnumColor getColorFromBlock(Block block) {
        return block instanceof BlockShulkerBox ? ((BlockShulkerBox)block).getColor() : null;
    }

    public static Block getBlockByColor(@Nullable EnumColor dyeColor) {
        if (dyeColor == null) {
            return Blocks.SHULKER_BOX;
        } else {
            switch(dyeColor) {
            case WHITE:
                return Blocks.WHITE_SHULKER_BOX;
            case ORANGE:
                return Blocks.ORANGE_SHULKER_BOX;
            case MAGENTA:
                return Blocks.MAGENTA_SHULKER_BOX;
            case LIGHT_BLUE:
                return Blocks.LIGHT_BLUE_SHULKER_BOX;
            case YELLOW:
                return Blocks.YELLOW_SHULKER_BOX;
            case LIME:
                return Blocks.LIME_SHULKER_BOX;
            case PINK:
                return Blocks.PINK_SHULKER_BOX;
            case GRAY:
                return Blocks.GRAY_SHULKER_BOX;
            case LIGHT_GRAY:
                return Blocks.LIGHT_GRAY_SHULKER_BOX;
            case CYAN:
                return Blocks.CYAN_SHULKER_BOX;
            case PURPLE:
            default:
                return Blocks.PURPLE_SHULKER_BOX;
            case BLUE:
                return Blocks.BLUE_SHULKER_BOX;
            case BROWN:
                return Blocks.BROWN_SHULKER_BOX;
            case GREEN:
                return Blocks.GREEN_SHULKER_BOX;
            case RED:
                return Blocks.RED_SHULKER_BOX;
            case BLACK:
                return Blocks.BLACK_SHULKER_BOX;
            }
        }
    }

    @Nullable
    public EnumColor getColor() {
        return this.color;
    }

    public static ItemStack getColoredItemStack(@Nullable EnumColor color) {
        return new ItemStack(getBlockByColor(color));
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
