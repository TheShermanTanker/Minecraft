package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTile;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandSetBlock {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.setblock.failed"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("setblock").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("block", ArgumentTile.block()).executes((context) -> {
            return setBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentTile.getBlock(context, "block"), CommandSetBlock.Mode.REPLACE, (Predicate<ShapeDetectorBlock>)null);
        }).then(net.minecraft.commands.CommandDispatcher.literal("destroy").executes((context) -> {
            return setBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentTile.getBlock(context, "block"), CommandSetBlock.Mode.DESTROY, (Predicate<ShapeDetectorBlock>)null);
        })).then(net.minecraft.commands.CommandDispatcher.literal("keep").executes((context) -> {
            return setBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentTile.getBlock(context, "block"), CommandSetBlock.Mode.REPLACE, (pos) -> {
                return pos.getLevel().isEmpty(pos.getPosition());
            });
        })).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((context) -> {
            return setBlock(context.getSource(), ArgumentPosition.getLoadedBlockPos(context, "pos"), ArgumentTile.getBlock(context, "block"), CommandSetBlock.Mode.REPLACE, (Predicate<ShapeDetectorBlock>)null);
        })))));
    }

    private static int setBlock(CommandListenerWrapper source, BlockPosition pos, ArgumentTileLocation block, CommandSetBlock.Mode mode, @Nullable Predicate<ShapeDetectorBlock> condition) throws CommandSyntaxException {
        WorldServer serverLevel = source.getWorld();
        if (condition != null && !condition.test(new ShapeDetectorBlock(serverLevel, pos, true))) {
            throw ERROR_FAILED.create();
        } else {
            boolean bl;
            if (mode == CommandSetBlock.Mode.DESTROY) {
                serverLevel.destroyBlock(pos, true);
                bl = !block.getState().isAir() || !serverLevel.getType(pos).isAir();
            } else {
                TileEntity blockEntity = serverLevel.getTileEntity(pos);
                Clearable.tryClear(blockEntity);
                bl = true;
            }

            if (bl && !block.place(serverLevel, pos, 2)) {
                throw ERROR_FAILED.create();
            } else {
                serverLevel.update(pos, block.getState().getBlock());
                source.sendMessage(new ChatMessage("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ()), true);
                return 1;
            }
        }
    }

    public interface Filter {
        @Nullable
        ArgumentTileLocation filter(StructureBoundingBox box, BlockPosition pos, ArgumentTileLocation block, WorldServer world);
    }

    public static enum Mode {
        REPLACE,
        DESTROY;
    }
}
