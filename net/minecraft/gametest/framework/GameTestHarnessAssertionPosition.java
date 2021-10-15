package net.minecraft.gametest.framework;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;

public class GameTestHarnessAssertionPosition extends GameTestHarnessAssertion {
    private final BlockPosition absolutePos;
    private final BlockPosition relativePos;
    private final long tick;

    public GameTestHarnessAssertionPosition(String message, BlockPosition pos, BlockPosition relativePos, long tick) {
        super(message);
        this.absolutePos = pos;
        this.relativePos = relativePos;
        this.tick = tick;
    }

    @Override
    public String getMessage() {
        String string = this.absolutePos.getX() + "," + this.absolutePos.getY() + "," + this.absolutePos.getZ() + " (relative: " + this.relativePos.getX() + "," + this.relativePos.getY() + "," + this.relativePos.getZ() + ")";
        return super.getMessage() + " at " + string + " (t=" + this.tick + ")";
    }

    @Nullable
    public String getMessageToShowAtBlock() {
        return super.getMessage();
    }

    @Nullable
    public BlockPosition getRelativePos() {
        return this.relativePos;
    }

    @Nullable
    public BlockPosition getAbsolutePos() {
        return this.absolutePos;
    }
}
