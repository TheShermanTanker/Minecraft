package net.minecraft.server.commands.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;

public interface CommandDataAccessor {
    void setData(NBTTagCompound nbt) throws CommandSyntaxException;

    NBTTagCompound getData() throws CommandSyntaxException;

    IChatBaseComponent getModifiedSuccess();

    IChatBaseComponent getPrintSuccess(NBTBase element);

    IChatBaseComponent getPrintSuccess(ArgumentNBTKey.NbtPath path, double scale, int result);
}
