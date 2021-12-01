package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandClone {
    private static final int MAX_CLONE_AREA = 32768;
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(new ChatMessage("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new ChatMessage("commands.clone.toobig", maxCount, count);
    });
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.clone.failed"));
    public static final Predicate<ShapeDetectorBlock> FILTER_AIR = (pos) -> {
        return !pos.getState().isAir();
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("clone").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("begin", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("end", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentPosition.blockPos()).executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        }).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        }).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CommandClone.Mode.FORCE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CommandClone.Mode.MOVE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("masked").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), FILTER_AIR, CommandClone.Mode.NORMAL);
        }).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), FILTER_AIR, CommandClone.Mode.FORCE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), FILTER_AIR, CommandClone.Mode.MOVE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), FILTER_AIR, CommandClone.Mode.NORMAL);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("filtered").then(net.minecraft.commands.CommandDispatcher.argument("filter", ArgumentBlockPredicate.blockPredicate()).executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), ArgumentBlockPredicate.getBlockPredicate(context, "filter"), CommandClone.Mode.NORMAL);
        }).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), ArgumentBlockPredicate.getBlockPredicate(context, "filter"), CommandClone.Mode.FORCE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), ArgumentBlockPredicate.getBlockPredicate(context, "filter"), CommandClone.Mode.MOVE);
        })).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((context) -> {
            return clone(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "begin"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), ArgumentBlockPredicate.getBlockPredicate(context, "filter"), CommandClone.Mode.NORMAL);
        }))))))));
    }

    private static int clone(CommandListenerWrapper source, BlockPosition begin, BlockPosition end, BlockPosition destination, Predicate<ShapeDetectorBlock> filter, CommandClone.Mode mode) throws CommandSyntaxException {
        StructureBoundingBox boundingBox = StructureBoundingBox.fromCorners(begin, end);
        BlockPosition blockPos = destination.offset(boundingBox.getLength());
        StructureBoundingBox boundingBox2 = StructureBoundingBox.fromCorners(destination, blockPos);
        if (!mode.canOverlap() && boundingBox2.intersects(boundingBox)) {
            throw ERROR_OVERLAP.create();
        } else {
            int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
            if (i > 32768) {
                throw ERROR_AREA_TOO_LARGE.create(32768, i);
            } else {
                WorldServer serverLevel = source.getWorld();
                if (serverLevel.areChunksLoadedBetween(begin, end) && serverLevel.areChunksLoadedBetween(destination, blockPos)) {
                    List<CommandClone.CommandCloneStoredTileEntity> list = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list2 = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list3 = Lists.newArrayList();
                    Deque<BlockPosition> deque = Lists.newLinkedList();
                    BlockPosition blockPos2 = new BlockPosition(boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ());

                    for(int j = boundingBox.minZ(); j <= boundingBox.maxZ(); ++j) {
                        for(int k = boundingBox.minY(); k <= boundingBox.maxY(); ++k) {
                            for(int l = boundingBox.minX(); l <= boundingBox.maxX(); ++l) {
                                BlockPosition blockPos3 = new BlockPosition(l, k, j);
                                BlockPosition blockPos4 = blockPos3.offset(blockPos2);
                                ShapeDetectorBlock blockInWorld = new ShapeDetectorBlock(serverLevel, blockPos3, false);
                                IBlockData blockState = blockInWorld.getState();
                                if (filter.test(blockInWorld)) {
                                    TileEntity blockEntity = serverLevel.getTileEntity(blockPos3);
                                    if (blockEntity != null) {
                                        NBTTagCompound compoundTag = blockEntity.saveWithoutMetadata();
                                        list2.add(new CommandClone.CommandCloneStoredTileEntity(blockPos4, blockState, compoundTag));
                                        deque.addLast(blockPos3);
                                    } else if (!blockState.isSolidRender(serverLevel, blockPos3) && !blockState.isCollisionShapeFullBlock(serverLevel, blockPos3)) {
                                        list3.add(new CommandClone.CommandCloneStoredTileEntity(blockPos4, blockState, (NBTTagCompound)null));
                                        deque.addFirst(blockPos3);
                                    } else {
                                        list.add(new CommandClone.CommandCloneStoredTileEntity(blockPos4, blockState, (NBTTagCompound)null));
                                        deque.addLast(blockPos3);
                                    }
                                }
                            }
                        }
                    }

                    if (mode == CommandClone.Mode.MOVE) {
                        for(BlockPosition blockPos5 : deque) {
                            TileEntity blockEntity2 = serverLevel.getTileEntity(blockPos5);
                            Clearable.tryClear(blockEntity2);
                            serverLevel.setTypeAndData(blockPos5, Blocks.BARRIER.getBlockData(), 2);
                        }

                        for(BlockPosition blockPos6 : deque) {
                            serverLevel.setTypeAndData(blockPos6, Blocks.AIR.getBlockData(), 3);
                        }
                    }

                    List<CommandClone.CommandCloneStoredTileEntity> list4 = Lists.newArrayList();
                    list4.addAll(list);
                    list4.addAll(list2);
                    list4.addAll(list3);
                    List<CommandClone.CommandCloneStoredTileEntity> list5 = Lists.reverse(list4);

                    for(CommandClone.CommandCloneStoredTileEntity cloneBlockInfo : list5) {
                        TileEntity blockEntity3 = serverLevel.getTileEntity(cloneBlockInfo.pos);
                        Clearable.tryClear(blockEntity3);
                        serverLevel.setTypeAndData(cloneBlockInfo.pos, Blocks.BARRIER.getBlockData(), 2);
                    }

                    int m = 0;

                    for(CommandClone.CommandCloneStoredTileEntity cloneBlockInfo2 : list4) {
                        if (serverLevel.setTypeAndData(cloneBlockInfo2.pos, cloneBlockInfo2.state, 2)) {
                            ++m;
                        }
                    }

                    for(CommandClone.CommandCloneStoredTileEntity cloneBlockInfo3 : list2) {
                        TileEntity blockEntity4 = serverLevel.getTileEntity(cloneBlockInfo3.pos);
                        if (cloneBlockInfo3.tag != null && blockEntity4 != null) {
                            blockEntity4.load(cloneBlockInfo3.tag);
                            blockEntity4.update();
                        }

                        serverLevel.setTypeAndData(cloneBlockInfo3.pos, cloneBlockInfo3.state, 2);
                    }

                    for(CommandClone.CommandCloneStoredTileEntity cloneBlockInfo4 : list5) {
                        serverLevel.update(cloneBlockInfo4.pos, cloneBlockInfo4.state.getBlock());
                    }

                    serverLevel.getBlockTicks().copyArea(boundingBox, blockPos2);
                    if (m == 0) {
                        throw ERROR_FAILED.create();
                    } else {
                        source.sendMessage(new ChatMessage("commands.clone.success", m), true);
                        return m;
                    }
                } else {
                    throw ArgumentPosition.ERROR_NOT_LOADED.create();
                }
            }
        }
    }

    static class CommandCloneStoredTileEntity {
        public final BlockPosition pos;
        public final IBlockData state;
        @Nullable
        public final NBTTagCompound tag;

        public CommandCloneStoredTileEntity(BlockPosition pos, IBlockData state, @Nullable NBTTagCompound blockEntityTag) {
            this.pos = pos;
            this.state = state;
            this.tag = blockEntityTag;
        }
    }

    static enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean canOverlap;

        private Mode(boolean allowsOverlap) {
            this.canOverlap = allowsOverlap;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }
}
