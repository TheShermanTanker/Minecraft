package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;

public class CommandDataAccessorTile implements CommandDataAccessor {
    static final SimpleCommandExceptionType ERROR_NOT_A_BLOCK_ENTITY = new SimpleCommandExceptionType(new ChatMessage("commands.data.block.invalid"));
    public static final Function<String, CommandData.DataProvider> PROVIDER = (string) -> {
        return new CommandData.DataProvider() {
            @Override
            public CommandDataAccessor access(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException {
                BlockPosition blockPos = ArgumentPosition.getLoadedBlockPos(context, string + "Pos");
                TileEntity blockEntity = context.getSource().getWorld().getTileEntity(blockPos);
                if (blockEntity == null) {
                    throw CommandDataAccessorTile.ERROR_NOT_A_BLOCK_ENTITY.create();
                } else {
                    return new CommandDataAccessorTile(blockEntity, blockPos);
                }
            }

            @Override
            public ArgumentBuilder<CommandListenerWrapper, ?> wrap(ArgumentBuilder<CommandListenerWrapper, ?> argument, Function<ArgumentBuilder<CommandListenerWrapper, ?>, ArgumentBuilder<CommandListenerWrapper, ?>> argumentAdder) {
                return argument.then(CommandDispatcher.literal("block").then(argumentAdder.apply(CommandDispatcher.argument(string + "Pos", ArgumentPosition.blockPos()))));
            }
        };
    };
    private final TileEntity entity;
    private final BlockPosition pos;

    public CommandDataAccessorTile(TileEntity blockEntity, BlockPosition pos) {
        this.entity = blockEntity;
        this.pos = pos;
    }

    @Override
    public void setData(NBTTagCompound nbt) {
        nbt.setInt("x", this.pos.getX());
        nbt.setInt("y", this.pos.getY());
        nbt.setInt("z", this.pos.getZ());
        IBlockData blockState = this.entity.getWorld().getType(this.pos);
        this.entity.load(nbt);
        this.entity.update();
        this.entity.getWorld().notify(this.pos, blockState, blockState, 3);
    }

    @Override
    public NBTTagCompound getData() {
        return this.entity.save(new NBTTagCompound());
    }

    @Override
    public IChatBaseComponent getModifiedSuccess() {
        return new ChatMessage("commands.data.block.modified", this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    @Override
    public IChatBaseComponent getPrintSuccess(NBTBase element) {
        return new ChatMessage("commands.data.block.query", this.pos.getX(), this.pos.getY(), this.pos.getZ(), GameProfileSerializer.toPrettyComponent(element));
    }

    @Override
    public IChatBaseComponent getPrintSuccess(ArgumentNBTKey.NbtPath path, double scale, int result) {
        return new ChatMessage("commands.data.block.get", path, this.pos.getX(), this.pos.getY(), this.pos.getZ(), String.format(Locale.ROOT, "%.2f", scale), result);
    }
}
