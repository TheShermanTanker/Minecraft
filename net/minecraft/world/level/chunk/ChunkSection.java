package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class ChunkSection {
    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = 4096;
    public static final DataPalette<IBlockData> GLOBAL_BLOCKSTATE_PALETTE = new DataPaletteGlobal<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.getBlockData());
    private final int bottomBlockY;
    private short nonEmptyBlockCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    private final DataPaletteBlock<IBlockData> states;

    public ChunkSection(int yOffset) {
        this(yOffset, (short)0, (short)0, (short)0);
    }

    public ChunkSection(int yOffset, short nonEmptyBlockCount, short randomTickableBlockCount, short nonEmptyFluidCount) {
        this.bottomBlockY = getBottomBlockY(yOffset);
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.tickingBlockCount = randomTickableBlockCount;
        this.tickingFluidCount = nonEmptyFluidCount;
        this.states = new DataPaletteBlock<>(GLOBAL_BLOCKSTATE_PALETTE, Block.BLOCK_STATE_REGISTRY, GameProfileSerializer::readBlockState, GameProfileSerializer::writeBlockState, Blocks.AIR.getBlockData());
    }

    public static int getBottomBlockY(int chunkPos) {
        return chunkPos << 4;
    }

    public IBlockData getType(int x, int y, int z) {
        return this.states.get(x, y, z);
    }

    public Fluid getFluidState(int x, int y, int z) {
        return this.states.get(x, y, z).getFluid();
    }

    public void acquire() {
        this.states.acquire();
    }

    public void release() {
        this.states.release();
    }

    public IBlockData setType(int x, int y, int z, IBlockData state) {
        return this.setType(x, y, z, state, true);
    }

    public IBlockData setType(int x, int y, int z, IBlockData state, boolean lock) {
        IBlockData blockState;
        if (lock) {
            blockState = this.states.setBlock(x, y, z, state);
        } else {
            blockState = this.states.getAndSetUnchecked(x, y, z, state);
        }

        Fluid fluidState = blockState.getFluid();
        Fluid fluidState2 = state.getFluid();
        if (!blockState.isAir()) {
            --this.nonEmptyBlockCount;
            if (blockState.isTicking()) {
                --this.tickingBlockCount;
            }
        }

        if (!fluidState.isEmpty()) {
            --this.tickingFluidCount;
        }

        if (!state.isAir()) {
            ++this.nonEmptyBlockCount;
            if (state.isTicking()) {
                ++this.tickingBlockCount;
            }
        }

        if (!fluidState2.isEmpty()) {
            ++this.tickingFluidCount;
        }

        return blockState;
    }

    public boolean isEmpty() {
        return this.nonEmptyBlockCount == 0;
    }

    public static boolean isEmpty(@Nullable ChunkSection section) {
        return section == Chunk.EMPTY_SECTION || section.isEmpty();
    }

    public boolean isRandomlyTicking() {
        return this.shouldTick() || this.isRandomlyTickingFluids();
    }

    public boolean shouldTick() {
        return this.tickingBlockCount > 0;
    }

    public boolean isRandomlyTickingFluids() {
        return this.tickingFluidCount > 0;
    }

    public int getYPosition() {
        return this.bottomBlockY;
    }

    public void recalcBlockCounts() {
        this.nonEmptyBlockCount = 0;
        this.tickingBlockCount = 0;
        this.tickingFluidCount = 0;
        this.states.count((state, count) -> {
            Fluid fluidState = state.getFluid();
            if (!state.isAir()) {
                this.nonEmptyBlockCount = (short)(this.nonEmptyBlockCount + count);
                if (state.isTicking()) {
                    this.tickingBlockCount = (short)(this.tickingBlockCount + count);
                }
            }

            if (!fluidState.isEmpty()) {
                this.nonEmptyBlockCount = (short)(this.nonEmptyBlockCount + count);
                if (fluidState.isRandomlyTicking()) {
                    this.tickingFluidCount = (short)(this.tickingFluidCount + count);
                }
            }

        });
    }

    public DataPaletteBlock<IBlockData> getBlocks() {
        return this.states;
    }

    public void read(PacketDataSerializer buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.states.read(buf);
    }

    public void write(PacketDataSerializer buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.states.write(buf);
    }

    public int getSerializedSize() {
        return 2 + this.states.getSerializedSize();
    }

    public boolean maybeHas(Predicate<IBlockData> predicate) {
        return this.states.contains(predicate);
    }
}
