package net.minecraft.network.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.world.entity.Entity;

public interface ChatComponentContextual {
    IChatMutableComponent resolve(@Nullable CommandListenerWrapper source, @Nullable Entity sender, int depth) throws CommandSyntaxException;
}
