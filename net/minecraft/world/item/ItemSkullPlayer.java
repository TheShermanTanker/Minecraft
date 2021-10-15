package net.minecraft.world.item;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import org.apache.commons.lang3.StringUtils;

public class ItemSkullPlayer extends ItemBlockWallable {
    public static final String TAG_SKULL_OWNER = "SkullOwner";

    public ItemSkullPlayer(Block standingBlock, Block wallBlock, Item.Info settings) {
        super(standingBlock, wallBlock, settings);
    }

    @Override
    public IChatBaseComponent getName(ItemStack stack) {
        if (stack.is(Items.PLAYER_HEAD) && stack.hasTag()) {
            String string = null;
            NBTTagCompound compoundTag = stack.getTag();
            if (compoundTag.hasKeyOfType("SkullOwner", 8)) {
                string = compoundTag.getString("SkullOwner");
            } else if (compoundTag.hasKeyOfType("SkullOwner", 10)) {
                NBTTagCompound compoundTag2 = compoundTag.getCompound("SkullOwner");
                if (compoundTag2.hasKeyOfType("Name", 8)) {
                    string = compoundTag2.getString("Name");
                }
            }

            if (string != null) {
                return new ChatMessage(this.getName() + ".named", string);
            }
        }

        return super.getName(stack);
    }

    @Override
    public void verifyTagAfterLoad(NBTTagCompound nbt) {
        super.verifyTagAfterLoad(nbt);
        if (nbt.hasKeyOfType("SkullOwner", 8) && !StringUtils.isBlank(nbt.getString("SkullOwner"))) {
            GameProfile gameProfile = new GameProfile((UUID)null, nbt.getString("SkullOwner"));
            TileEntitySkull.updateGameprofile(gameProfile, (profile) -> {
                nbt.set("SkullOwner", GameProfileSerializer.serialize(new NBTTagCompound(), profile));
            });
        }

    }
}
