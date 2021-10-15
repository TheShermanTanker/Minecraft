package net.minecraft.world.level.block;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.commons.lang3.StringUtils;

public class BlockSkullPlayer extends BlockSkull {
    protected BlockSkullPlayer(BlockBase.Info settings) {
        super(BlockSkull.Type.PLAYER, settings);
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        super.postPlace(world, pos, state, placer, itemStack);
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntitySkull) {
            TileEntitySkull skullBlockEntity = (TileEntitySkull)blockEntity;
            GameProfile gameProfile = null;
            if (itemStack.hasTag()) {
                NBTTagCompound compoundTag = itemStack.getTag();
                if (compoundTag.hasKeyOfType("SkullOwner", 10)) {
                    gameProfile = GameProfileSerializer.deserialize(compoundTag.getCompound("SkullOwner"));
                } else if (compoundTag.hasKeyOfType("SkullOwner", 8) && !StringUtils.isBlank(compoundTag.getString("SkullOwner"))) {
                    gameProfile = new GameProfile((UUID)null, compoundTag.getString("SkullOwner"));
                }
            }

            skullBlockEntity.setGameProfile(gameProfile);
        }

    }
}
