package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;

public class ItemAir extends Item {
    private final Block block;

    public ItemAir(Block block, Item.Info settings) {
        super(settings);
        this.block = block;
    }

    @Override
    public String getName() {
        return this.block.getDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        this.block.appendHoverText(stack, world, tooltip, context);
    }
}
