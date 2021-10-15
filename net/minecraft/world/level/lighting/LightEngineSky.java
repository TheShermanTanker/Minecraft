package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.mutable.MutableInt;

public final class LightEngineSky extends LightEngineLayer<LightEngineStorageSky.SkyDataLayerStorageMap, LightEngineStorageSky> {
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    private static final EnumDirection[] HORIZONTALS = new EnumDirection[]{EnumDirection.NORTH, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.EAST};

    public LightEngineSky(ILightAccess chunkProvider) {
        super(chunkProvider, EnumSkyBlock.SKY, new LightEngineStorageSky(chunkProvider));
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        if (targetId != Long.MAX_VALUE && sourceId != Long.MAX_VALUE) {
            if (level >= 15) {
                return level;
            } else {
                MutableInt mutableInt = new MutableInt();
                IBlockData blockState = this.getStateAndOpacity(targetId, mutableInt);
                if (mutableInt.getValue() >= 15) {
                    return 15;
                } else {
                    int i = BlockPosition.getX(sourceId);
                    int j = BlockPosition.getY(sourceId);
                    int k = BlockPosition.getZ(sourceId);
                    int l = BlockPosition.getX(targetId);
                    int m = BlockPosition.getY(targetId);
                    int n = BlockPosition.getZ(targetId);
                    int o = Integer.signum(l - i);
                    int p = Integer.signum(m - j);
                    int q = Integer.signum(n - k);
                    EnumDirection direction = EnumDirection.fromNormal(o, p, q);
                    if (direction == null) {
                        throw new IllegalStateException(String.format("Light was spread in illegal direction %d, %d, %d", o, p, q));
                    } else {
                        IBlockData blockState2 = this.getStateAndOpacity(sourceId, (MutableInt)null);
                        VoxelShape voxelShape = this.getShape(blockState2, sourceId, direction);
                        VoxelShape voxelShape2 = this.getShape(blockState, targetId, direction.opposite());
                        if (VoxelShapes.faceShapeOccludes(voxelShape, voxelShape2)) {
                            return 15;
                        } else {
                            boolean bl = i == l && k == n;
                            boolean bl2 = bl && j > m;
                            return bl2 && level == 0 && mutableInt.getValue() == 0 ? 0 : level + Math.max(1, mutableInt.getValue());
                        }
                    }
                }
            }
        } else {
            return 15;
        }
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        long l = SectionPosition.blockToSection(id);
        int i = BlockPosition.getY(id);
        int j = SectionPosition.sectionRelative(i);
        int k = SectionPosition.blockToSectionCoord(i);
        int m;
        if (j != 0) {
            m = 0;
        } else {
            int n;
            for(n = 0; !this.storage.storingLightForSection(SectionPosition.offset(l, 0, -n - 1, 0)) && this.storage.hasSectionsBelow(k - n - 1); ++n) {
            }

            m = n;
        }

        long p = BlockPosition.offset(id, 0, -1 - m * 16, 0);
        long q = SectionPosition.blockToSection(p);
        if (l == q || this.storage.storingLightForSection(q)) {
            this.checkNeighbor(id, p, level, decrease);
        }

        long r = BlockPosition.offset(id, EnumDirection.UP);
        long s = SectionPosition.blockToSection(r);
        if (l == s || this.storage.storingLightForSection(s)) {
            this.checkNeighbor(id, r, level, decrease);
        }

        for(EnumDirection direction : HORIZONTALS) {
            int t = 0;

            while(true) {
                long u = BlockPosition.offset(id, direction.getAdjacentX(), -t, direction.getAdjacentZ());
                long v = SectionPosition.blockToSection(u);
                if (l == v) {
                    this.checkNeighbor(id, u, level, decrease);
                    break;
                }

                if (this.storage.storingLightForSection(v)) {
                    long w = BlockPosition.offset(id, 0, -t, 0);
                    this.checkNeighbor(w, u, level, decrease);
                }

                ++t;
                if (t > m * 16) {
                    break;
                }
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;
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

                int j;
                if (dataLayer2 != null) {
                    j = this.getLevel(dataLayer2, m);
                } else {
                    if (direction == EnumDirection.DOWN) {
                        continue;
                    }

                    j = 15 - this.storage.getLightValue(m, true);
                }

                int o = this.computeLevelFromNeighbor(m, id, j);
                if (i > o) {
                    i = o;
                }

                if (i == 0) {
                    return i;
                }
            }
        }

        return i;
    }

    @Override
    protected void checkNode(long id) {
        this.storage.runAllUpdates();
        long l = SectionPosition.blockToSection(id);
        if (this.storage.storingLightForSection(l)) {
            super.checkNode(id);
        } else {
            for(id = BlockPosition.getFlatIndex(id); !this.storage.storingLightForSection(l) && !this.storage.isAboveData(l); id = BlockPosition.offset(id, 0, 16, 0)) {
                l = SectionPosition.offset(l, EnumDirection.UP);
            }

            if (this.storage.storingLightForSection(l)) {
                super.checkNode(id);
            }
        }

    }

    @Override
    public String getDebugData(long sectionPos) {
        return super.getDebugData(sectionPos) + (this.storage.isAboveData(sectionPos) ? "*" : "");
    }
}
