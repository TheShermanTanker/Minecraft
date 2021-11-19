package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentInventorySlot;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.item.ArgumentItemStack;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class CommandItem {
    static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> {
        return new ChatMessage("commands.item.target.not_a_container", x, y, z);
    });
    private static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> {
        return new ChatMessage("commands.item.source.not_a_container", x, y, z);
    });
    static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType((slot) -> {
        return new ChatMessage("commands.item.target.no_such_slot", slot);
    });
    private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType((slot) -> {
        return new ChatMessage("commands.item.source.no_such_slot", slot);
    });
    private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType((slot) -> {
        return new ChatMessage("commands.item.target.no_changes", slot);
    });
    private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType((itemName, slot) -> {
        return new ChatMessage("commands.item.target.no_changed.known_item", itemName, slot);
    });
    private static final SuggestionProvider<CommandListenerWrapper> SUGGEST_MODIFIER = (context, builder) -> {
        ItemModifierManager itemModifierManager = context.getSource().getServer().getItemModifierManager();
        return ICompletionProvider.suggestResource(itemModifierManager.getKeys(), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("item").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("replace").then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()).then(net.minecraft.commands.CommandDispatcher.literal("with").then(net.minecraft.commands.CommandDispatcher.argument("item", ArgumentItemStack.item()).executes((context) -> {
            return setBlockItem(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentItemStack.getItem(context, "item").createItemStack(1, false));
        }).then(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(1, 64)).executes((context) -> {
            return setBlockItem(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentItemStack.getItem(context, "item").createItemStack(IntegerArgumentType.getInteger(context, "count"), true));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("sourceSlot", ArgumentInventorySlot.slot()).executes((context) -> {
            return blockToBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return blockToBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentEntity.entity()).then(net.minecraft.commands.CommandDispatcher.argument("sourceSlot", ArgumentInventorySlot.slot()).executes((context) -> {
            return entityToBlock(context.getSource(), ArgumentEntity.getEntity(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return entityToBlock(context.getSource(), ArgumentEntity.getEntity(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        }))))))))).then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()).then(net.minecraft.commands.CommandDispatcher.literal("with").then(net.minecraft.commands.CommandDispatcher.argument("item", ArgumentItemStack.item()).executes((context) -> {
            return setEntityItem(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentItemStack.getItem(context, "item").createItemStack(1, false));
        }).then(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(1, 64)).executes((context) -> {
            return setEntityItem(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentItemStack.getItem(context, "item").createItemStack(IntegerArgumentType.getInteger(context, "count"), true));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("from").then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("sourceSlot", ArgumentInventorySlot.slot()).executes((context) -> {
            return blockToEntities(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return blockToEntities(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentEntity.entity()).then(net.minecraft.commands.CommandDispatcher.argument("sourceSlot", ArgumentInventorySlot.slot()).executes((context) -> {
            return entityToEntities(context.getSource(), ArgumentEntity.getEntity(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return entityToEntities(context.getSource(), ArgumentEntity.getEntity(context, "source"), ArgumentInventorySlot.getSlot(context, "sourceSlot"), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        })))))))))).then(net.minecraft.commands.CommandDispatcher.literal("modify").then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return modifyBlockItem(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()).then(net.minecraft.commands.CommandDispatcher.argument("modifier", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_MODIFIER).executes((context) -> {
            return modifyEntityItem(context.getSource(), ArgumentEntity.getEntities(context, "targets"), ArgumentInventorySlot.getSlot(context, "slot"), ArgumentMinecraftKeyRegistered.getItemModifier(context, "modifier"));
        })))))));
    }

    private static int modifyBlockItem(CommandListenerWrapper source, BlockPosition pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        IInventory container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getSize()) {
            ItemStack itemStack = applyModifier(source, modifier, container.getItem(slot));
            container.setItem(slot, itemStack);
            source.sendMessage(new ChatMessage("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true);
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static int modifyEntityItem(CommandListenerWrapper source, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        Map<Entity, ItemStack> map = Maps.newHashMapWithExpectedSize(targets.size());

        for(Entity entity : targets) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != SlotAccess.NULL) {
                ItemStack itemStack = applyModifier(source, modifier, slotAccess.get().cloneItemStack());
                if (slotAccess.set(itemStack)) {
                    map.put(entity, itemStack);
                    if (entity instanceof EntityPlayer) {
                        ((EntityPlayer)entity).containerMenu.broadcastChanges();
                    }
                }
            }
        }

        if (map.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES.create(slot);
        } else {
            if (map.size() == 1) {
                Entry<Entity, ItemStack> entry = map.entrySet().iterator().next();
                source.sendMessage(new ChatMessage("commands.item.entity.set.success.single", entry.getKey().getScoreboardDisplayName(), entry.getValue().getDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.item.entity.set.success.multiple", map.size()), true);
            }

            return map.size();
        }
    }

    private static int setBlockItem(CommandListenerWrapper source, BlockPosition pos, int slot, ItemStack stack) throws CommandSyntaxException {
        IInventory container = getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot >= 0 && slot < container.getSize()) {
            container.setItem(slot, stack);
            source.sendMessage(new ChatMessage("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), stack.getDisplayName()), true);
            return 1;
        } else {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static IInventory getContainer(CommandListenerWrapper source, BlockPosition pos, Dynamic3CommandExceptionType exception) throws CommandSyntaxException {
        TileEntity blockEntity = source.getWorld().getTileEntity(pos);
        if (!(blockEntity instanceof IInventory)) {
            throw exception.create(pos.getX(), pos.getY(), pos.getZ());
        } else {
            return (IInventory)blockEntity;
        }
    }

    private static int setEntityItem(CommandListenerWrapper source, Collection<? extends Entity> targets, int slot, ItemStack stack) throws CommandSyntaxException {
        List<Entity> list = Lists.newArrayListWithCapacity(targets.size());

        for(Entity entity : targets) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess != SlotAccess.NULL && slotAccess.set(stack.cloneItemStack())) {
                list.add(entity);
                if (entity instanceof EntityPlayer) {
                    ((EntityPlayer)entity).containerMenu.broadcastChanges();
                }
            }
        }

        if (list.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(stack.getDisplayName(), slot);
        } else {
            if (list.size() == 1) {
                source.sendMessage(new ChatMessage("commands.item.entity.set.success.single", list.iterator().next().getScoreboardDisplayName(), stack.getDisplayName()), true);
            } else {
                source.sendMessage(new ChatMessage("commands.item.entity.set.success.multiple", list.size(), stack.getDisplayName()), true);
            }

            return list.size();
        }
    }

    private static int blockToEntities(CommandListenerWrapper source, BlockPosition sourcePos, int sourceSlot, Collection<? extends Entity> targets, int slot) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToEntities(CommandListenerWrapper source, BlockPosition sourcePos, int sourceSlot, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int blockToBlock(CommandListenerWrapper source, BlockPosition sourcePos, int sourceSlot, BlockPosition pos, int slot) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToBlock(CommandListenerWrapper source, BlockPosition sourcePos, int sourceSlot, BlockPosition pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, applyModifier(source, modifier, getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int entityToBlock(CommandListenerWrapper source, Entity sourceEntity, int sourceSlot, BlockPosition pos, int slot) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, getEntityItem(sourceEntity, sourceSlot));
    }

    private static int entityToBlock(CommandListenerWrapper source, Entity sourceEntity, int sourceSlot, BlockPosition pos, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setBlockItem(source, pos, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
    }

    private static int entityToEntities(CommandListenerWrapper source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targets, int slot) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, getEntityItem(sourceEntity, sourceSlot));
    }

    private static int entityToEntities(CommandListenerWrapper source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targets, int slot, LootItemFunction modifier) throws CommandSyntaxException {
        return setEntityItem(source, targets, slot, applyModifier(source, modifier, getEntityItem(sourceEntity, sourceSlot)));
    }

    private static ItemStack applyModifier(CommandListenerWrapper source, LootItemFunction modifier, ItemStack stack) {
        WorldServer serverLevel = source.getWorld();
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(serverLevel)).set(LootContextParameters.ORIGIN, source.getPosition()).setOptional(LootContextParameters.THIS_ENTITY, source.getEntity());
        return modifier.apply(stack, builder.build(LootContextParameterSets.COMMAND));
    }

    private static ItemStack getEntityItem(Entity entity, int slotId) throws CommandSyntaxException {
        SlotAccess slotAccess = entity.getSlot(slotId);
        if (slotAccess == SlotAccess.NULL) {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slotId);
        } else {
            return slotAccess.get().cloneItemStack();
        }
    }

    private static ItemStack getBlockItem(CommandListenerWrapper source, BlockPosition pos, int slotId) throws CommandSyntaxException {
        IInventory container = getContainer(source, pos, ERROR_SOURCE_NOT_A_CONTAINER);
        if (slotId >= 0 && slotId < container.getSize()) {
            return container.getItem(slotId).cloneItemStack();
        } else {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(slotId);
        }
    }
}
