package net.minecraft.world.level.block.entity;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityEnchantTable extends TileEntity implements INamableTileEntity {
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final Random RANDOM = new Random();
    private IChatBaseComponent name;

    public TileEntityEnchantTable(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.ENCHANTING_TABLE, pos, state);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        if (this.hasCustomName()) {
            nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.name = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName"));
        }

    }

    public static void bookAnimationTick(World world, BlockPosition pos, IBlockData state, TileEntityEnchantTable blockEntity) {
        blockEntity.oOpen = blockEntity.open;
        blockEntity.oRot = blockEntity.rot;
        EntityHuman player = world.getNearestPlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 3.0D, false);
        if (player != null) {
            double d = player.locX() - ((double)pos.getX() + 0.5D);
            double e = player.locZ() - ((double)pos.getZ() + 0.5D);
            blockEntity.tRot = (float)MathHelper.atan2(e, d);
            blockEntity.open += 0.1F;
            if (blockEntity.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float f = blockEntity.flipT;

                do {
                    blockEntity.flipT += (float)(RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while(f == blockEntity.flipT);
            }
        } else {
            blockEntity.tRot += 0.02F;
            blockEntity.open -= 0.1F;
        }

        while(blockEntity.rot >= (float)Math.PI) {
            blockEntity.rot -= ((float)Math.PI * 2F);
        }

        while(blockEntity.rot < -(float)Math.PI) {
            blockEntity.rot += ((float)Math.PI * 2F);
        }

        while(blockEntity.tRot >= (float)Math.PI) {
            blockEntity.tRot -= ((float)Math.PI * 2F);
        }

        while(blockEntity.tRot < -(float)Math.PI) {
            blockEntity.tRot += ((float)Math.PI * 2F);
        }

        float g;
        for(g = blockEntity.tRot - blockEntity.rot; g >= (float)Math.PI; g -= ((float)Math.PI * 2F)) {
        }

        while(g < -(float)Math.PI) {
            g += ((float)Math.PI * 2F);
        }

        blockEntity.rot += g * 0.4F;
        blockEntity.open = MathHelper.clamp(blockEntity.open, 0.0F, 1.0F);
        ++blockEntity.time;
        blockEntity.oFlip = blockEntity.flip;
        float h = (blockEntity.flipT - blockEntity.flip) * 0.4F;
        float i = 0.2F;
        h = MathHelper.clamp(h, -0.2F, 0.2F);
        blockEntity.flipA += (h - blockEntity.flipA) * 0.9F;
        blockEntity.flip += blockEntity.flipA;
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return (IChatBaseComponent)(this.name != null ? this.name : new ChatMessage("container.enchant"));
    }

    public void setCustomName(@Nullable IChatBaseComponent value) {
        this.name = value;
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return this.name;
    }
}
