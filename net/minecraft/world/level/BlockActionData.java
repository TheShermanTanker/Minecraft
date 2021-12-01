package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.Block;

public record BlockActionData(BlockPosition pos, Block block, int paramA, int paramB) {
    public BlockActionData(BlockPosition pos, Block block, int type, int data) {
        this.pos = pos;
        this.block = block;
        this.paramA = type;
        this.paramB = data;
    }

    public BlockPosition pos() {
        return this.pos;
    }

    public Block block() {
        return this.block;
    }

    public int paramA() {
        return this.paramA;
    }

    public int paramB() {
        return this.paramB;
    }
}
