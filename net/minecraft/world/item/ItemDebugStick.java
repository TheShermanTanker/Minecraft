package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class ItemDebugStick extends Item {
    public ItemDebugStick(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canAttackBlock(IBlockData state, World world, BlockPosition pos, EntityHuman miner) {
        if (!world.isClientSide) {
            this.handleInteraction(miner, state, world, pos, false, miner.getItemInHand(EnumHand.MAIN_HAND));
        }

        return false;
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        EntityHuman player = context.getEntity();
        World level = context.getWorld();
        if (!level.isClientSide && player != null) {
            BlockPosition blockPos = context.getClickPosition();
            if (!this.handleInteraction(player, level.getType(blockPos), level, blockPos, true, context.getItemStack())) {
                return EnumInteractionResult.FAIL;
            }
        }

        return EnumInteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean handleInteraction(EntityHuman player, IBlockData state, GeneratorAccess world, BlockPosition pos, boolean update, ItemStack stack) {
        if (!player.isCreativeAndOp()) {
            return false;
        } else {
            Block block = state.getBlock();
            BlockStateList<Block, IBlockData> stateDefinition = block.getStates();
            Collection<IBlockState<?>> collection = stateDefinition.getProperties();
            String string = IRegistry.BLOCK.getKey(block).toString();
            if (collection.isEmpty()) {
                message(player, new ChatMessage(this.getName() + ".empty", string));
                return false;
            } else {
                NBTTagCompound compoundTag = stack.getOrCreateTagElement("DebugProperty");
                String string2 = compoundTag.getString(string);
                IBlockState<?> property = stateDefinition.getProperty(string2);
                if (update) {
                    if (property == null) {
                        property = collection.iterator().next();
                    }

                    IBlockData blockState = cycleState(state, property, player.isSecondaryUseActive());
                    world.setTypeAndData(pos, blockState, 18);
                    message(player, new ChatMessage(this.getName() + ".update", property.getName(), getNameHelper(blockState, property)));
                } else {
                    property = getRelative(collection, property, player.isSecondaryUseActive());
                    String string3 = property.getName();
                    compoundTag.setString(string, string3);
                    message(player, new ChatMessage(this.getName() + ".select", string3, getNameHelper(state, property)));
                }

                return true;
            }
        }
    }

    private static <T extends Comparable<T>> IBlockData cycleState(IBlockData state, IBlockState<T> property, boolean inverse) {
        return state.set(property, getRelative(property.getValues(), state.get(property), inverse));
    }

    private static <T> T getRelative(Iterable<T> elements, @Nullable T current, boolean inverse) {
        return (T)(inverse ? SystemUtils.findPreviousInIterable(elements, current) : SystemUtils.findNextInIterable(elements, current));
    }

    private static void message(EntityHuman player, IChatBaseComponent message) {
        ((EntityPlayer)player).sendMessage(message, ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
    }

    private static <T extends Comparable<T>> String getNameHelper(IBlockData state, IBlockState<T> property) {
        return property.getName(state.get(property));
    }
}
