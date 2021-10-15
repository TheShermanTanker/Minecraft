package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.mutable.MutableInt;

public final class LightEngineBlock extends LightEngineLayer<LightEngineStorageBlock.BlockDataLayerStorageMap, LightEngineStorageBlock> {
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    private final BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition();

    public LightEngineBlock(ILightAccess chunkProvider) {
        super(chunkProvider, EnumSkyBlock.BLOCK, new LightEngineStorageBlock(chunkProvider));
    }

    private int getLightEmission(long blockPos) {
        int i = BlockPosition.getX(blockPos);
        int j = BlockPosition.getY(blockPos);
        int k = BlockPosition.getZ(blockPos);
        IBlockAccess blockGetter = this.chunkSource.getChunkForLighting(SectionPosition.blockToSectionCoord(i), SectionPosition.blockToSectionCoord(k));
        return blockGetter != null ? blockGetter.getLightEmission(this.pos.set(i, j, k)) : 0;
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        if (targetId == Long.MAX_VALUE) {
            return 15;
        } else if (sourceId == Long.MAX_VALUE) {
            return level + 15 - this.getLightEmission(targetId);
        } else if (level >= 15) {
            return level;
        } else {
            int i = Integer.signum(BlockPosition.getX(targetId) - BlockPosition.getX(sourceId));
            int j = Integer.signum(BlockPosition.getY(targetId) - BlockPosition.getY(sourceId));
            int k = Integer.signum(BlockPosition.getZ(targetId) - BlockPosition.getZ(sourceId));
            EnumDirection direction = EnumDirection.fromNormal(i, j, k);
            if (direction == null) {
                return 15;
            } else {
                MutableInt mutableInt = new MutableInt();
                IBlockData blockState = this.getStateAndOpacity(targetId, mutableInt);
                if (mutableInt.getValue() >= 15) {
                    return 15;
                } else {
                    IBlockData blockState2 = this.getStateAndOpacity(sourceId, (MutableInt)null);
                    VoxelShape voxelShape = this.getShape(blockState2, sourceId, direction);
                    VoxelShape voxelShape2 = this.getShape(blockState, targetId, direction.opposite());
                    return VoxelShapes.faceShapeOccludes(voxelShape, voxelShape2) ? 15 : level + Math.max(1, mutableInt.getValue());
                }
            }
        }
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        long l = SectionPosition.blockToSection(id);

        for(EnumDirection direction : DIRECTIONS) {
            long m = BlockPosition.offset(id, direction);
            long n = SectionPosition.blockToSection(m);
            if (l == n || this.storage.storingLightForSection(n)) {
                this.checkNeighbor(id, m, level, decrease);
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;
        if (Long.MAX_VALUE != excludedId) {
            int j = this.computeLevelFromNeighbor(Long.MAX_VALUE, id, 0);
            if (maxLevel > j) {
                i = j;
            }

            if (i == 0) {
                return i;
            }
        }

        long l = SectionPosition.blockToSection(id);
        NibbleArray dataLayer = this.storage.getDataLayer(l, true);

        for(EnumDirection direction : DIRECTIONS) {
            long m = BlockPosition.offset(id, direction);
            if (m != excludedId) {
                long n = SectionPosition.blockToSection(m);
                NibbleArray dataLayer2;
                if (l == n) {
                    dataLayer2 = dataLayer;
                } else {
                    dataLayer2 = this.storage.getDataLayer(n, true);
                }

                if (dataLayer2 != null) {
                    int k = this.computeLevelFromNeighbor(m, id, this.getLevel(dataLayer2, m));
                    if (i > k) {
                        i = k;
                    }

                    if (i == 0) {
                        return i;
                    }
                }
            }
        }

        return i;
    }

    @Override
    public void onBlockEmissionIncrease(BlockPosition pos, int level) {
        this.storage.runAllUpdates();
        this.checkEdge(Long.MAX_VALUE, pos.asLong(), 15 - level, true);
    }
}
