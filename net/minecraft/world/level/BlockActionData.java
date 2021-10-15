package net.minecraft.world.level;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.Block;

public class BlockActionData {
    private final BlockPosition pos;
    private final Block block;
    private final int paramA;
    private final int paramB;

    public BlockActionData(BlockPosition pos, Block block, int type, int data) {
        this.pos = pos;
        this.block = block;
        this.paramA = type;
        this.paramB = data;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public Block getBlock() {
        return this.block;
    }

    public int getParamA() {
        return this.paramA;
    }

    public int getParamB() {
        return this.paramB;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof BlockActionData)) {
            return false;
        } else {
            BlockActionData blockEventData = (BlockActionData)object;
            return this.pos.equals(blockEventData.pos) && this.paramA == blockEventData.paramA && this.paramB == blockEventData.paramB && this.block == blockEventData.block;
        }
    }

    @Override
    public int hashCode() {
        int i = this.pos.hashCode();
        i = 31 * i + this.block.hashCode();
        i = 31 * i + this.paramA;
        return 31 * i + this.paramB;
    }

    @Override
    public String toString() {
        return "TE(" + this.pos + ")," + this.paramA + "," + this.paramB + "," + this.block;
    }
}
