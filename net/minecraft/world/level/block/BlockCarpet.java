package net.minecraft.world.level.block;

import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.block.state.BlockBase;

public class BlockCarpet extends BlockCarpetBase {
    private final EnumColor color;

    protected BlockCarpet(EnumColor dyeColor, BlockBase.Info settings) {
        super(settings);
        this.color = dyeColor;
    }

    public EnumColor getColor() {
        return this.color;
    }
}
