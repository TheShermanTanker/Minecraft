package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBase;

public abstract class BlockStemmed extends Block {
    public BlockStemmed(BlockBase.Info settings) {
        super(settings);
    }

    public abstract BlockStem getStem();

    public abstract BlockStemAttached getAttachedStem();
}
