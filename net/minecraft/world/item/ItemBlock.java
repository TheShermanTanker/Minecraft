package net.minecraft.world.item;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class ItemBlock extends Item {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    public static final String BLOCK_STATE_TAG = "BlockStateTag";
    /** @deprecated */
    @Deprecated
    private final Block block;

    public ItemBlock(Block block, Item.Info settings) {
        super(settings);
        this.block = block;
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        EnumInteractionResult interactionResult = this.place(new BlockActionContext(context));
        if (!interactionResult.consumesAction() && this.isFood()) {
            EnumInteractionResult interactionResult2 = this.use(context.getWorld(), context.getEntity(), context.getHand()).getResult();
            return interactionResult2 == EnumInteractionResult.CONSUME ? EnumInteractionResult.CONSUME_PARTIAL : interactionResult2;
        } else {
            return interactionResult;
        }
    }

    public EnumInteractionResult place(BlockActionContext context) {
        if (!context.canPlace()) {
            return EnumInteractionResult.FAIL;
        } else {
            BlockActionContext blockPlaceContext = this.updatePlacementContext(context);
            if (blockPlaceContext == null) {
                return EnumInteractionResult.FAIL;
            } else {
                IBlockData blockState = this.getPlacementState(blockPlaceContext);
                if (blockState == null) {
                    return EnumInteractionResult.FAIL;
                } else if (!this.placeBlock(blockPlaceContext, blockState)) {
                    return EnumInteractionResult.FAIL;
                } else {
                    BlockPosition blockPos = blockPlaceContext.getClickPosition();
                    World level = blockPlaceContext.getWorld();
                    EntityHuman player = blockPlaceContext.getEntity();
                    ItemStack itemStack = blockPlaceContext.getItemStack();
                    IBlockData blockState2 = level.getType(blockPos);
                    if (blockState2.is(blockState.getBlock())) {
                        blockState2 = this.updateBlockStateFromTag(blockPos, level, itemStack, blockState2);
                        this.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState2);
                        blockState2.getBlock().postPlace(level, blockPos, blockState2, player, itemStack);
                        if (player instanceof EntityPlayer) {
                            CriterionTriggers.PLACED_BLOCK.trigger((EntityPlayer)player, blockPos, itemStack);
                        }
                    }

                    SoundEffectType soundType = blockState2.getStepSound();
                    level.playSound(player, blockPos, this.getPlaceSound(blockState2), EnumSoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                    level.gameEvent(player, GameEvent.BLOCK_PLACE, blockPos);
                    if (player == null || !player.getAbilities().instabuild) {
                        itemStack.subtract(1);
                    }

                    return EnumInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
    }

    protected SoundEffect getPlaceSound(IBlockData state) {
        return state.getStepSound().getPlaceSound();
    }

    @Nullable
    public BlockActionContext updatePlacementContext(BlockActionContext context) {
        return context;
    }

    protected boolean updateCustomBlockEntityTag(BlockPosition pos, World world, @Nullable EntityHuman player, ItemStack stack, IBlockData state) {
        return updateCustomBlockEntityTag(world, player, pos, stack);
    }

    @Nullable
    protected IBlockData getPlacementState(BlockActionContext context) {
        IBlockData blockState = this.getBlock().getPlacedState(context);
        return blockState != null && this.canPlace(context, blockState) ? blockState : null;
    }

    private IBlockData updateBlockStateFromTag(BlockPosition pos, World world, ItemStack stack, IBlockData state) {
        IBlockData blockState = state;
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null) {
            NBTTagCompound compoundTag2 = compoundTag.getCompound("BlockStateTag");
            BlockStateList<Block, IBlockData> stateDefinition = state.getBlock().getStates();

            for(String string : compoundTag2.getKeys()) {
                IBlockState<?> property = stateDefinition.getProperty(string);
                if (property != null) {
                    String string2 = compoundTag2.get(string).asString();
                    blockState = updateState(blockState, property, string2);
                }
            }
        }

        if (blockState != state) {
            world.setTypeAndData(pos, blockState, 2);
        }

        return blockState;
    }

    private static <T extends Comparable<T>> IBlockData updateState(IBlockData state, IBlockState<T> property, String name) {
        return property.getValue(name).map((value) -> {
            return state.set(property, value);
        }).orElse(state);
    }

    protected boolean canPlace(BlockActionContext context, IBlockData state) {
        EntityHuman player = context.getEntity();
        VoxelShapeCollision collisionContext = player == null ? VoxelShapeCollision.empty() : VoxelShapeCollision.of(player);
        return (!this.isCheckCollisions() || state.canPlace(context.getWorld(), context.getClickPosition())) && context.getWorld().isUnobstructed(state, context.getClickPosition(), collisionContext);
    }

    protected boolean isCheckCollisions() {
        return true;
    }

    protected boolean placeBlock(BlockActionContext context, IBlockData state) {
        return context.getWorld().setTypeAndData(context.getClickPosition(), state, 11);
    }

    public static boolean updateCustomBlockEntityTag(World world, @Nullable EntityHuman player, BlockPosition pos, ItemStack stack) {
        MinecraftServer minecraftServer = world.getMinecraftServer();
        if (minecraftServer == null) {
            return false;
        } else {
            NBTTagCompound compoundTag = getBlockEntityData(stack);
            if (compoundTag != null) {
                TileEntity blockEntity = world.getTileEntity(pos);
                if (blockEntity != null) {
                    if (!world.isClientSide && blockEntity.isFilteredNBT() && (player == null || !player.isCreativeAndOp())) {
                        return false;
                    }

                    NBTTagCompound compoundTag2 = blockEntity.saveWithoutMetadata();
                    NBTTagCompound compoundTag3 = compoundTag2.copy();
                    compoundTag2.merge(compoundTag);
                    if (!compoundTag2.equals(compoundTag3)) {
                        blockEntity.load(compoundTag2);
                        blockEntity.update();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public String getName() {
        return this.getBlock().getDescriptionId();
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            this.getBlock().fillItemCategory(group, stacks);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        this.getBlock().appendHoverText(stack, world, tooltip, context);
    }

    public Block getBlock() {
        return this.block;
    }

    public void registerBlocks(Map<Block, Item> map, Item item) {
        map.put(this.getBlock(), item);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return !(this.block instanceof BlockShulkerBox);
    }

    @Override
    public void onDestroyed(EntityItem entity) {
        if (this.block instanceof BlockShulkerBox) {
            ItemStack itemStack = entity.getItemStack();
            NBTTagCompound compoundTag = getBlockEntityData(itemStack);
            if (compoundTag != null && compoundTag.hasKeyOfType("Items", 9)) {
                NBTTagList listTag = compoundTag.getList("Items", 10);
                ItemLiquidUtil.onContainerDestroyed(entity, listTag.stream().map(NBTTagCompound.class::cast).map(ItemStack::of));
            }
        }

    }

    @Nullable
    public static NBTTagCompound getBlockEntityData(ItemStack stack) {
        return stack.getTagElement("BlockEntityTag");
    }

    public static void setBlockEntityData(ItemStack stack, TileEntityTypes<?> blockEntityType, NBTTagCompound tag) {
        if (tag.isEmpty()) {
            stack.removeTag("BlockEntityTag");
        } else {
            TileEntity.addEntityType(tag, blockEntityType);
            stack.addTagElement("BlockEntityTag", tag);
        }

    }
}
