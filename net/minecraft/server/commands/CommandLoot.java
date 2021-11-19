package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentInventorySlot;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.arguments.item.ArgumentItemStack;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTableRegistry;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public class CommandLoot {
    public static final SuggestionProvider<CommandListenerWrapper> SUGGEST_LOOT_TABLE = (context, builder) -> {
        LootTableRegistry lootTables = context.getSource().getServer().getLootTableRegistry();
        return ICompletionProvider.suggestResource(lootTables.getIds(), builder);
    };
    private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType((entityName) -> {
        return new ChatMessage("commands.drop.no_held_items", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_NO_LOOT_TABLE = new DynamicCommandExceptionType((entityName) -> {
        return new ChatMessage("commands.drop.no_loot_table", entityName);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(addTargets(net.minecraft.commands.CommandDispatcher.literal("loot").requires((source) -> {
            return source.hasPermission(2);
        }), (builder, constructor) -> {
            return builder.then(net.minecraft.commands.CommandDispatcher.literal("fish").then(net.minecraft.commands.CommandDispatcher.argument("loot_table", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_LOOT_TABLE).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).executes((context) -> {
                return dropFishingLoot(context, ArgumentMinecraftKeyRegistered.getId(context, "loot_table"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ItemStack.EMPTY, constructor);
            }).then(net.minecraft.commands.CommandDispatcher.argument("tool", ArgumentItemStack.item()).executes((context) -> {
                return dropFishingLoot(context, ArgumentMinecraftKeyRegistered.getId(context, "loot_table"), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentItemStack.getItem(context, "tool").createItemStack(1, false), constructor);
            })).then(net.minecraft.commands.CommandDispatcher.literal("mainhand").executes((context) -> {
                return dropFishingLoot(context, ArgumentMinecraftKeyRegistered.getId(context, "loot_table"), ArgumentPosition.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EnumItemSlot.MAINHAND), constructor);
            })).then(net.minecraft.commands.CommandDispatcher.literal("offhand").executes((context) -> {
                return dropFishingLoot(context, ArgumentMinecraftKeyRegistered.getId(context, "loot_table"), ArgumentPosition.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EnumItemSlot.OFFHAND), constructor);
            }))))).then(net.minecraft.commands.CommandDispatcher.literal("loot").then(net.minecraft.commands.CommandDispatcher.argument("loot_table", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_LOOT_TABLE).executes((context) -> {
                return dropChestLoot(context, ArgumentMinecraftKeyRegistered.getId(context, "loot_table"), constructor);
            }))).then(net.minecraft.commands.CommandDispatcher.literal("kill").then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentEntity.entity()).executes((context) -> {
                return dropKillLoot(context, ArgumentEntity.getEntity(context, "target"), constructor);
            }))).then(net.minecraft.commands.CommandDispatcher.literal("mine").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).executes((context) -> {
                return dropBlockLoot(context, ArgumentPosition.getLoadedBlockPos(context, "pos"), ItemStack.EMPTY, constructor);
            }).then(net.minecraft.commands.CommandDispatcher.argument("tool", ArgumentItemStack.item()).executes((context) -> {
                return dropBlockLoot(context, ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentItemStack.getItem(context, "tool").createItemStack(1, false), constructor);
            })).then(net.minecraft.commands.CommandDispatcher.literal("mainhand").executes((context) -> {
                return dropBlockLoot(context, ArgumentPosition.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EnumItemSlot.MAINHAND), constructor);
            })).then(net.minecraft.commands.CommandDispatcher.literal("offhand").executes((context) -> {
                return dropBlockLoot(context, ArgumentPosition.getLoadedBlockPos(context, "pos"), getSourceHandItem(context.getSource(), EnumItemSlot.OFFHAND), constructor);
            }))));
        }));
    }

    private static <T extends ArgumentBuilder<CommandListenerWrapper, T>> T addTargets(T rootArgument, CommandLoot.TailProvider sourceConstructor) {
        return rootArgument.then(net.minecraft.commands.CommandDispatcher.literal("replace").then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("entities", ArgumentEntity.multipleEntities()).then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()), (context, stacks, messageSender) -> {
            return entityReplace(ArgumentEntity.getEntities(context, "entities"), ArgumentInventorySlot.getSlot(context, "slot"), stacks.size(), stacks, messageSender);
        }).then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
            return entityReplace(ArgumentEntity.getEntities(context, "entities"), ArgumentInventorySlot.getSlot(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("targetPos", ArgumentPosition.blockPos()).then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("slot", ArgumentInventorySlot.slot()), (context, stacks, messageSender) -> {
            return blockReplace(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "targetPos"), ArgumentInventorySlot.getSlot(context, "slot"), stacks.size(), stacks, messageSender);
        }).then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
            return blockReplace(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "targetPos"), IntegerArgumentType.getInteger(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
        })))))).then(net.minecraft.commands.CommandDispatcher.literal("insert").then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("targetPos", ArgumentPosition.blockPos()), (context, stacks, messageSender) -> {
            return blockDistribute(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "targetPos"), stacks, messageSender);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("give").then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("players", ArgumentEntity.players()), (context, stacks, messageSender) -> {
            return playerGive(ArgumentEntity.getPlayers(context, "players"), stacks, messageSender);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("spawn").then(sourceConstructor.construct(net.minecraft.commands.CommandDispatcher.argument("targetPos", ArgumentVec3.vec3()), (context, stacks, messageSender) -> {
            return dropInWorld(context.getSource(), ArgumentVec3.getVec3(context, "targetPos"), stacks, messageSender);
        })));
    }

    private static IInventory getContainer(CommandListenerWrapper source, BlockPosition pos) throws CommandSyntaxException {
        TileEntity blockEntity = source.getWorld().getTileEntity(pos);
        if (!(blockEntity instanceof IInventory)) {
            throw CommandItem.ERROR_TARGET_NOT_A_CONTAINER.create(pos.getX(), pos.getY(), pos.getZ());
        } else {
            return (IInventory)blockEntity;
        }
    }

    private static int blockDistribute(CommandListenerWrapper source, BlockPosition targetPos, List<ItemStack> stacks, CommandLoot.Callback messageSender) throws CommandSyntaxException {
        IInventory container = getContainer(source, targetPos);
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(ItemStack itemStack : stacks) {
            if (distributeToContainer(container, itemStack.cloneItemStack())) {
                container.update();
                list.add(itemStack);
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static boolean distributeToContainer(IInventory inventory, ItemStack stack) {
        boolean bl = false;

        for(int i = 0; i < inventory.getSize() && !stack.isEmpty(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (inventory.canPlaceItem(i, stack)) {
                if (itemStack.isEmpty()) {
                    inventory.setItem(i, stack);
                    bl = true;
                    break;
                }

                if (canMergeItems(itemStack, stack)) {
                    int j = stack.getMaxStackSize() - itemStack.getCount();
                    int k = Math.min(stack.getCount(), j);
                    stack.subtract(k);
                    itemStack.add(k);
                    bl = true;
                }
            }
        }

        return bl;
    }

    private static int blockReplace(CommandListenerWrapper source, BlockPosition targetPos, int slot, int stackCount, List<ItemStack> stacks, CommandLoot.Callback messageSender) throws CommandSyntaxException {
        IInventory container = getContainer(source, targetPos);
        int i = container.getSize();
        if (slot >= 0 && slot < i) {
            List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

            for(int j = 0; j < stackCount; ++j) {
                int k = slot + j;
                ItemStack itemStack = j < stacks.size() ? stacks.get(j) : ItemStack.EMPTY;
                if (container.canPlaceItem(k, itemStack)) {
                    container.setItem(k, itemStack);
                    list.add(itemStack);
                }
            }

            messageSender.accept(list);
            return list.size();
        } else {
            throw CommandItem.ERROR_TARGET_INAPPLICABLE_SLOT.create(slot);
        }
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        return first.is(second.getItem()) && first.getDamage() == second.getDamage() && first.getCount() <= first.getMaxStackSize() && Objects.equals(first.getTag(), second.getTag());
    }

    private static int playerGive(Collection<EntityPlayer> players, List<ItemStack> stacks, CommandLoot.Callback messageSender) throws CommandSyntaxException {
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(ItemStack itemStack : stacks) {
            for(EntityPlayer serverPlayer : players) {
                if (serverPlayer.getInventory().pickup(itemStack.cloneItemStack())) {
                    list.add(itemStack);
                }
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static void setSlots(Entity entity, List<ItemStack> stacks, int slot, int stackCount, List<ItemStack> addedStacks) {
        for(int i = 0; i < stackCount; ++i) {
            ItemStack itemStack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            SlotAccess slotAccess = entity.getSlot(slot + i);
            if (slotAccess != SlotAccess.NULL && slotAccess.set(itemStack.cloneItemStack())) {
                addedStacks.add(itemStack);
            }
        }

    }

    private static int entityReplace(Collection<? extends Entity> targets, int slot, int stackCount, List<ItemStack> stacks, CommandLoot.Callback messageSender) throws CommandSyntaxException {
        List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

        for(Entity entity : targets) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer serverPlayer = (EntityPlayer)entity;
                setSlots(entity, stacks, slot, stackCount, list);
                serverPlayer.containerMenu.broadcastChanges();
            } else {
                setSlots(entity, stacks, slot, stackCount, list);
            }
        }

        messageSender.accept(list);
        return list.size();
    }

    private static int dropInWorld(CommandListenerWrapper source, Vec3D pos, List<ItemStack> stacks, CommandLoot.Callback messageSender) throws CommandSyntaxException {
        WorldServer serverLevel = source.getWorld();
        stacks.forEach((stack) -> {
            EntityItem itemEntity = new EntityItem(serverLevel, pos.x, pos.y, pos.z, stack.cloneItemStack());
            itemEntity.defaultPickupDelay();
            serverLevel.addEntity(itemEntity);
        });
        messageSender.accept(stacks);
        return stacks.size();
    }

    private static void callback(CommandListenerWrapper source, List<ItemStack> stacks) {
        if (stacks.size() == 1) {
            ItemStack itemStack = stacks.get(0);
            source.sendMessage(new ChatMessage("commands.drop.success.single", itemStack.getCount(), itemStack.getDisplayName()), false);
        } else {
            source.sendMessage(new ChatMessage("commands.drop.success.multiple", stacks.size()), false);
        }

    }

    private static void callback(CommandListenerWrapper source, List<ItemStack> stacks, MinecraftKey lootTable) {
        if (stacks.size() == 1) {
            ItemStack itemStack = stacks.get(0);
            source.sendMessage(new ChatMessage("commands.drop.success.single_with_table", itemStack.getCount(), itemStack.getDisplayName(), lootTable), false);
        } else {
            source.sendMessage(new ChatMessage("commands.drop.success.multiple_with_table", stacks.size(), lootTable), false);
        }

    }

    private static ItemStack getSourceHandItem(CommandListenerWrapper source, EnumItemSlot slot) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        if (entity instanceof EntityLiving) {
            return ((EntityLiving)entity).getEquipment(slot);
        } else {
            throw ERROR_NO_HELD_ITEMS.create(entity.getScoreboardDisplayName());
        }
    }

    private static int dropBlockLoot(CommandContext<CommandListenerWrapper> context, BlockPosition pos, ItemStack stack, CommandLoot.DropConsumer constructor) throws CommandSyntaxException {
        CommandListenerWrapper commandSourceStack = context.getSource();
        WorldServer serverLevel = commandSourceStack.getWorld();
        IBlockData blockState = serverLevel.getType(pos);
        TileEntity blockEntity = serverLevel.getTileEntity(pos);
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(serverLevel)).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(pos)).set(LootContextParameters.BLOCK_STATE, blockState).setOptional(LootContextParameters.BLOCK_ENTITY, blockEntity).setOptional(LootContextParameters.THIS_ENTITY, commandSourceStack.getEntity()).set(LootContextParameters.TOOL, stack);
        List<ItemStack> list = blockState.getDrops(builder);
        return constructor.accept(context, list, (stacks) -> {
            callback(commandSourceStack, stacks, blockState.getBlock().getLootTable());
        });
    }

    private static int dropKillLoot(CommandContext<CommandListenerWrapper> context, Entity entity, CommandLoot.DropConsumer constructor) throws CommandSyntaxException {
        if (!(entity instanceof EntityLiving)) {
            throw ERROR_NO_LOOT_TABLE.create(entity.getScoreboardDisplayName());
        } else {
            MinecraftKey resourceLocation = ((EntityLiving)entity).getLootTable();
            CommandListenerWrapper commandSourceStack = context.getSource();
            LootTableInfo.Builder builder = new LootTableInfo.Builder(commandSourceStack.getWorld());
            Entity entity2 = commandSourceStack.getEntity();
            if (entity2 instanceof EntityHuman) {
                builder.set(LootContextParameters.LAST_DAMAGE_PLAYER, (EntityHuman)entity2);
            }

            builder.set(LootContextParameters.DAMAGE_SOURCE, DamageSource.MAGIC);
            builder.setOptional(LootContextParameters.DIRECT_KILLER_ENTITY, entity2);
            builder.setOptional(LootContextParameters.KILLER_ENTITY, entity2);
            builder.set(LootContextParameters.THIS_ENTITY, entity);
            builder.set(LootContextParameters.ORIGIN, commandSourceStack.getPosition());
            LootTable lootTable = commandSourceStack.getServer().getLootTableRegistry().getLootTable(resourceLocation);
            List<ItemStack> list = lootTable.populateLoot(builder.build(LootContextParameterSets.ENTITY));
            return constructor.accept(context, list, (stacks) -> {
                callback(commandSourceStack, stacks, resourceLocation);
            });
        }
    }

    private static int dropChestLoot(CommandContext<CommandListenerWrapper> context, MinecraftKey lootTable, CommandLoot.DropConsumer constructor) throws CommandSyntaxException {
        CommandListenerWrapper commandSourceStack = context.getSource();
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(commandSourceStack.getWorld())).setOptional(LootContextParameters.THIS_ENTITY, commandSourceStack.getEntity()).set(LootContextParameters.ORIGIN, commandSourceStack.getPosition());
        return drop(context, lootTable, builder.build(LootContextParameterSets.CHEST), constructor);
    }

    private static int dropFishingLoot(CommandContext<CommandListenerWrapper> context, MinecraftKey lootTable, BlockPosition pos, ItemStack stack, CommandLoot.DropConsumer constructor) throws CommandSyntaxException {
        CommandListenerWrapper commandSourceStack = context.getSource();
        LootTableInfo lootContext = (new LootTableInfo.Builder(commandSourceStack.getWorld())).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(pos)).set(LootContextParameters.TOOL, stack).setOptional(LootContextParameters.THIS_ENTITY, commandSourceStack.getEntity()).build(LootContextParameterSets.FISHING);
        return drop(context, lootTable, lootContext, constructor);
    }

    private static int drop(CommandContext<CommandListenerWrapper> context, MinecraftKey lootTable, LootTableInfo lootContext, CommandLoot.DropConsumer constructor) throws CommandSyntaxException {
        CommandListenerWrapper commandSourceStack = context.getSource();
        LootTable lootTable2 = commandSourceStack.getServer().getLootTableRegistry().getLootTable(lootTable);
        List<ItemStack> list = lootTable2.populateLoot(lootContext);
        return constructor.accept(context, list, (stacks) -> {
            callback(commandSourceStack, stacks);
        });
    }

    @FunctionalInterface
    interface Callback {
        void accept(List<ItemStack> items) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface DropConsumer {
        int accept(CommandContext<CommandListenerWrapper> context, List<ItemStack> items, CommandLoot.Callback messageSender) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface TailProvider {
        ArgumentBuilder<CommandListenerWrapper, ?> construct(ArgumentBuilder<CommandListenerWrapper, ?> builder, CommandLoot.DropConsumer target);
    }
}
