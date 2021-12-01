package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

public class ChunkSection {
    public static final int SECTION_WIDTH = 16;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_SIZE = 4096;
    public static final int BIOME_CONTAINER_BITS = 2;
    private final int bottomBlockY;
    private short nonEmptyBlockCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    public final DataPaletteBlock<IBlockData> states;
    private final DataPaletteBlock<BiomeBase> biomes;

    public ChunkSection(int chunkPos, DataPaletteBlock<IBlockData> blockStateContainer, DataPaletteBlock<BiomeBase> biomeContainer) {
        this.bottomBlockY = getBottomBlockY(chunkPos);
        this.states = blockStateContainer;
        this.biomes = biomeContainer;
        this.recalcBlockCounts();
    }

    public ChunkSection(int chunkPos, IRegistry<BiomeBase> biomeRegistry) {
        this.bottomBlockY = getBottomBlockY(chunkPos);
        this.states = new DataPaletteBlock<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.getBlockData(), PalettedContainer$Strategy.SECTION_STATES);
        this.biomes = new DataPaletteBlock<>(biomeRegistry, biomeRegistry.getOrThrow(Biomes.PLAINS), PalettedContainer$Strategy.SECTION_BIOMES);
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

    public boolean hasOnlyAir() {
        return this.nonEmptyBlockCount == 0;
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

    public DataPaletteBlock<BiomeBase> getBiomes() {
        return this.biomes;
    }

    public void read(PacketDataSerializer buf) {
        this.nonEmptyBlockCount = buf.readShort();
        this.states.read(buf);
        this.biomes.read(buf);
    }

    public void write(PacketDataSerializer buf) {
        buf.writeShort(this.nonEmptyBlockCount);
        this.states.write(buf);
        this.biomes.write(buf);
    }

    public int getSerializedSize() {
        return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<IBlockData> predicate) {
        return this.states.contains(predicate);
    }

    public BiomeBase getNoiseBiome(int x, int y, int z) {
        return this.biomes.get(x, y, z);
    }

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler, int x, int z) {
        DataPaletteBlock<BiomeBase> palettedContainer = this.getBiomes();
        palettedContainer.acquire();

        try {
            int i = QuartPos.fromBlock(this.getYPosition());
            int j = 4;

            for(int k = 0; k < 4; ++k) {
                for(int l = 0; l < 4; ++l) {
                    for(int m = 0; m < 4; ++m) {
                        palettedContainer.getAndSetUnchecked(k, l, m, biomeSupplier.getNoiseBiome(x + k, i + l, z + m, sampler));
                    }
                }
            }
        } finally {
            palettedContainer.release();
        }

    }
}
