package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.blocks.ArgumentTile;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandFill {
    private static final int MAX_FILL_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new ChatMessage("commands.fill.toobig", maxCount, count);
    });
    static final ArgumentTileLocation HOLLOW_CORE = new ArgumentTileLocation(Blocks.AIR.getBlockData(), Collections.emptySet(), (NBTTagCompound)null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.fill.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("fill").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("from", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("to", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("block", ArgumentTile.block()).executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.REPLACE, (Predicate<ShapeDetectorBlock>)null);
        }).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.REPLACE, (Predicate<ShapeDetectorBlock>)null);
        }).then(net.minecraft.commands.CommandDispatcher.argument("filter", ArgumentBlockPredicate.blockPredicate()).executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.REPLACE, ArgumentBlockPredicate.getBlockPredicate(context, "filter"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("keep").executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.REPLACE, (pos) -> {
                return pos.getLevel().isEmpty(pos.getPosition());
            });
        })).then(net.minecraft.commands.CommandDispatcher.literal("outline").executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.OUTLINE, (Predicate<ShapeDetectorBlock>)null);
        })).then(net.minecraft.commands.CommandDispatcher.literal("hollow").executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.HOLLOW, (Predicate<ShapeDetectorBlock>)null);
        })).then(net.minecraft.commands.CommandDispatcher.literal("destroy").executes((context) -> {
            return fillBlocks(context.getSource(), StructureBoundingBox.fromCorners(ArgumentPosition.getLoadedBlockPos(context, "from"), ArgumentPosition.getLoadedBlockPos(context, "to")), ArgumentTile.getBlock(context, "block"), CommandFill.Mode.DESTROY, (Predicate<ShapeDetectorBlock>)null);
        }))))));
    }

    private static int fillBlocks(CommandListenerWrapper source, StructureBoundingBox range, ArgumentTileLocation block, CommandFill.Mode mode, @Nullable Predicate<ShapeDetectorBlock> filter) throws CommandSyntaxException {
        int i = range.getXSpan() * range.getYSpan() * range.getZSpan();
        if (i > 32768) {
            throw ERROR_AREA_TOO_LARGE.create(32768, i);
        } else {
            List<BlockPosition> list = Lists.newArrayList();
            WorldServer serverLevel = source.getWorld();
            int j = 0;

            for(BlockPosition blockPos : BlockPosition.betweenClosed(range.minX(), range.minY(), range.minZ(), range.maxX(), range.maxY(), range.maxZ())) {
                if (filter == null || filter.test(new ShapeDetectorBlock(serverLevel, blockPos, true))) {
                    ArgumentTileLocation blockInput = mode.filter.filter(range, blockPos, block, serverLevel);
                    if (blockInput != null) {
                        TileEntity blockEntity = serverLevel.getTileEntity(blockPos);
                        Clearable.tryClear(blockEntity);
                        if (blockInput.place(serverLevel, blockPos, 2)) {
                            list.add(blockPos.immutableCopy());
                            ++j;
                        }
                    }
                }
            }

            for(BlockPosition blockPos2 : list) {
                Block block2 = serverLevel.getType(blockPos2).getBlock();
                serverLevel.update(blockPos2, block2);
            }

            if (j == 0) {
                throw ERROR_FAILED.create();
            } else {
                source.sendMessage(new ChatMessage("commands.fill.success", j), true);
                return j;
            }
        }
    }

    static enum Mode {
        REPLACE((range, pos, block, world) -> {
            return block;
        }),
        OUTLINE((range, pos, block, world) -> {
            return pos.getX() != range.minX() && pos.getX() != range.maxX() && pos.getY() != range.minY() && pos.getY() != range.maxY() && pos.getZ() != range.minZ() && pos.getZ() != range.maxZ() ? null : block;
        }),
        HOLLOW((range, pos, block, world) -> {
            return pos.getX() != range.minX() && pos.getX() != range.maxX() && pos.getY() != range.minY() && pos.getY() != range.maxY() && pos.getZ() != range.minZ() && pos.getZ() != range.maxZ() ? CommandFill.HOLLOW_CORE : block;
        }),
        DESTROY((range, pos, block, world) -> {
            world.destroyBlock(pos, true);
            return block;
        });

        public final CommandSetBlock.Filter filter;

        private Mode(CommandSetBlock.Filter filter) {
            this.filter = filter;
        }
    }
}
