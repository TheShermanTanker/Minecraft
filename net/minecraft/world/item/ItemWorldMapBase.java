package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemWorldMapBase extends Item {
    public ItemWorldMapBase(Item.Info settings) {
        super(settings);
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Nullable
    public Packet<?> getUpdatePacket(ItemStack stack, World world, EntityHuman player) {
        return null;
    }
}
