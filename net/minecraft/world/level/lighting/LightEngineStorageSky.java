package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;

public class LightEngineStorageSky extends LightEngineStorage<LightEngineStorageSky.SkyDataLayerStorageMap> {
    private static final EnumDirection[] HORIZONTALS = new EnumDirection[]{EnumDirection.NORTH, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.EAST};
    private final LongSet sectionsWithSources = new LongOpenHashSet();
    private final LongSet sectionsToAddSourcesTo = new LongOpenHashSet();
    private final LongSet sectionsToRemoveSourcesFrom = new LongOpenHashSet();
    private final LongSet columnsWithSkySources = new LongOpenHashSet();
    private volatile boolean hasSourceInconsistencies;

    protected LightEngineStorageSky(ILightAccess chunkProvider) {
        super(EnumSkyBlock.SKY, chunkProvider, new LightEngineStorageSky.SkyDataLayerStorageMap(new Long2ObjectOpenHashMap<>(), new Long2IntOpenHashMap(), Integer.MAX_VALUE));
    }

    @Override
    protected int getLightValue(long blockPos) {
        return this.getLightValue(blockPos, false);
    }

    protected int getLightValue(long l, boolean bl) {
        long m = SectionPosition.blockToSection(l);
        int i = SectionPosition.y(m);
        LightEngineStorageSky.SkyDataLayerStorageMap skyDataLayerStorageMap = bl ? this.updatingSectionData : this.visibleSectionData;
        int j = skyDataLayerStorageMap.topSections.get(SectionPosition.getZeroNode(m));
        if (j != skyDataLayerStorageMap.currentLowestY && i < j) {
            NibbleArray dataLayer = this.getDataLayer(skyDataLayerStorageMap, m);
            if (dataLayer == null) {
                for(l = BlockPosition.getFlatIndex(l); dataLayer == null; dataLayer = this.getDataLayer(skyDataLayerStorageMap, m)) {
                    ++i;
                    if (i >= j) {
                        return 15;
                    }

                    l = BlockPosition.offset(l, 0, 16, 0);
                    m = SectionPosition.offset(m, EnumDirection.UP);
                }
            }

            return dataLayer.get(SectionPosition.sectionRelative(BlockPosition.getX(l)), SectionPosition.sectionRelative(BlockPosition.getY(l)), SectionPosition.sectionRelative(BlockPosition.getZ(l)));
        } else {
            return bl && !this.lightOnInSection(m) ? 0 : 15;
        }
    }

    @Override
    protected void onNodeAdded(long sectionPos) {
        int i = SectionPosition.y(sectionPos);
        if ((this.updatingSectionData).currentLowestY > i) {
            (this.updatingSectionData).currentLowestY = i;
            (this.updatingSectionData).topSections.defaultReturnValue((this.updatingSectionData).currentLowestY);
        }

        long l = SectionPosition.getZeroNode(sectionPos);
        int j = (this.updatingSectionData).topSections.get(l);
        if (j < i + 1) {
            (this.updatingSectionData).topSections.put(l, i + 1);
            if (this.columnsWithSkySources.contains(l)) {
                this.queueAddSource(sectionPos);
                if (j > (this.updatingSectionData).currentLowestY) {
                    long m = SectionPosition.asLong(SectionPosition.x(sectionPos), j - 1, SectionPosition.z(sectionPos));
                    this.queueRemoveSource(m);
                }

                this.recheckInconsistencyFlag();
            }
        }

    }

    private void queueRemoveSource(long sectionPos) {
        this.sectionsToRemoveSourcesFrom.add(sectionPos);
        this.sectionsToAddSourcesTo.remove(sectionPos);
    }

    private void queueAddSource(long sectionPos) {
        this.sectionsToAddSourcesTo.add(sectionPos);
        this.sectionsToRemoveSourcesFrom.remove(sectionPos);
    }

    private void recheckInconsistencyFlag() {
        this.hasSourceInconsistencies = !this.sectionsToAddSourcesTo.isEmpty() || !this.sectionsToRemoveSourcesFrom.isEmpty();
    }

    @Override
    protected void onNodeRemoved(long sectionPos) {
        long l = SectionPosition.getZeroNode(sectionPos);
        boolean bl = this.columnsWithSkySources.contains(l);
        if (bl) {
            this.queueRemoveSource(sectionPos);
        }

        int i = SectionPosition.y(sectionPos);
        if ((this.updatingSectionData).topSections.get(l) == i + 1) {
            long m;
            for(m = sectionPos; !this.storingLightForSection(m) && this.hasSectionsBelow(i); m = SectionPosition.offset(m, EnumDirection.DOWN)) {
                --i;
            }

            if (this.storingLightForSection(m)) {
                (this.updatingSectionData).topSections.put(l, i + 1);
                if (bl) {
                    this.queueAddSource(m);
                }
            } else {
                (this.updatingSectionData).topSections.remove(l);
            }
        }

        if (bl) {
            this.recheckInconsistencyFlag();
        }

    }

    @Override
    protected void enableLightSources(long columnPos, boolean enabled) {
        this.runAllUpdates();
        if (enabled && this.columnsWithSkySources.add(columnPos)) {
            int i = (this.updatingSectionData).topSections.get(columnPos);
            if (i != (this.updatingSectionData).currentLowestY) {
                long l = SectionPosition.asLong(SectionPosition.x(columnPos), i - 1, SectionPosition.z(columnPos));
                this.queueAddSource(l);
                this.recheckInconsistencyFlag();
            }
        } else if (!enabled) {
            this.columnsWithSkySources.remove(columnPos);
        }

    }

    @Override
    protected boolean hasInconsistencies() {
        return super.hasInconsistencies() || this.hasSourceInconsistencies;
    }

    @Override
    protected NibbleArray createDataLayer(long sectionPos) {
        NibbleArray dataLayer = this.queuedSections.get(sectionPos);
        if (dataLayer != null) {
            return dataLayer;
        } else {
            long l = SectionPosition.offset(sectionPos, EnumDirection.UP);
            int i = (this.updatingSectionData).topSections.get(SectionPosition.getZeroNode(sectionPos));
            if (i != (this.updatingSectionData).currentLowestY && SectionPosition.y(l) < i) {
                NibbleArray dataLayer2;
                while((dataLayer2 = this.getDataLayer(l, true)) == null) {
                    l = SectionPosition.offset(l, EnumDirection.UP);
                }

                return repeatFirstLayer(dataLayer2);
            } else {
                return new NibbleArray();
            }
        }
    }

    private static NibbleArray repeatFirstLayer(NibbleArray source) {
        if (source.isEmpty()) {
            return new NibbleArray();
        } else {
            byte[] bs = source.asBytes();
            byte[] cs = new byte[2048];

            for(int i = 0; i < 16; ++i) {
                System.arraycopy(bs, 0, cs, i * 128, 128);
            }

            return new NibbleArray(cs);
        }
    }

    @Override
    protected void markNewInconsistencies(LightEngineLayer<LightEngineStorageSky.SkyDataLayerStorageMap, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation) {
        super.markNewInconsistencies(lightProvider, doSkylight, skipEdgeLightPropagation);
        if (doSkylight) {
            if (!this.sectionsToAddSourcesTo.isEmpty()) {
                for(long l : this.sectionsToAddSourcesTo) {
                    int i = this.getLevel(l);
                    if (i != 2 && !this.sectionsToRemoveSourcesFrom.contains(l) && this.sectionsWithSources.add(l)) {
                        if (i == 1) {
                            this.clearQueuedSectionBlocks(lightProvider, l);
                            if (this.changedSections.add(l)) {
                                this.updatingSectionData.copyDataLayer(l);
                            }

                            Arrays.fill(this.getDataLayer(l, true).asBytes(), (byte)-1);
                            int j = SectionPosition.sectionToBlockCoord(SectionPosition.x(l));
                            int k = SectionPosition.sectionToBlockCoord(SectionPosition.y(l));
                            int m = SectionPosition.sectionToBlockCoord(SectionPosition.z(l));

                            for(EnumDirection direction : HORIZONTALS) {
                                long n = SectionPosition.offset(l, direction);
                                if ((this.sectionsToRemoveSourcesFrom.contains(n) || !this.sectionsWithSources.contains(n) && !this.sectionsToAddSourcesTo.contains(n)) && this.storingLightForSection(n)) {
                                    for(int o = 0; o < 16; ++o) {
                                        for(int p = 0; p < 16; ++p) {
                                            long q;
                                            long r;
                                            switch(direction) {
                                            case NORTH:
                                                q = BlockPosition.asLong(j + o, k + p, m);
                                                r = BlockPosition.asLong(j + o, k + p, m - 1);
                                                break;
                                            case SOUTH:
                                                q = BlockPosition.asLong(j + o, k + p, m + 16 - 1);
                                                r = BlockPosition.asLong(j + o, k + p, m + 16);
                                                break;
                                            case WEST:
                                                q = BlockPosition.asLong(j, k + o, m + p);
                                                r = BlockPosition.asLong(j - 1, k + o, m + p);
                                                break;
                                            default:
                                                q = BlockPosition.asLong(j + 16 - 1, k + o, m + p);
                                                r = BlockPosition.asLong(j + 16, k + o, m + p);
                                            }

                                            lightProvider.checkEdge(q, r, lightProvider.computeLevelFromNeighbor(q, r, 0), true);
                                        }
                                    }
                                }
                            }

                            for(int y = 0; y < 16; ++y) {
                                for(int z = 0; z < 16; ++z) {
                                    long aa = BlockPosition.asLong(SectionPosition.sectionToBlockCoord(SectionPosition.x(l), y), SectionPosition.sectionToBlockCoord(SectionPosition.y(l)), SectionPosition.sectionToBlockCoord(SectionPosition.z(l), z));
                                    long ab = BlockPosition.asLong(SectionPosition.sectionToBlockCoord(SectionPosition.x(l), y), SectionPosition.sectionToBlockCoord(SectionPosition.y(l)) - 1, SectionPosition.sectionToBlockCoord(SectionPosition.z(l), z));
                                    lightProvider.checkEdge(aa, ab, lightProvider.computeLevelFromNeighbor(aa, ab, 0), true);
                                }
                            }
                        } else {
                            for(int ac = 0; ac < 16; ++ac) {
                                for(int ad = 0; ad < 16; ++ad) {
                                    long ae = BlockPosition.asLong(SectionPosition.sectionToBlockCoord(SectionPosition.x(l), ac), SectionPosition.sectionToBlockCoord(SectionPosition.y(l), 15), SectionPosition.sectionToBlockCoord(SectionPosition.z(l), ad));
                                    lightProvider.checkEdge(Long.MAX_VALUE, ae, 0, true);
                                }
                            }
                        }
                    }
                }
            }

            this.sectionsToAddSourcesTo.clear();
            if (!this.sectionsToRemoveSourcesFrom.isEmpty()) {
                for(long af : this.sectionsToRemoveSourcesFrom) {
                    if (this.sectionsWithSources.remove(af) && this.storingLightForSection(af)) {
                        for(int ag = 0; ag < 16; ++ag) {
                            for(int ah = 0; ah < 16; ++ah) {
                                long ai = BlockPosition.asLong(SectionPosition.sectionToBlockCoord(SectionPosition.x(af), ag), SectionPosition.sectionToBlockCoord(SectionPosition.y(af), 15), SectionPosition.sectionToBlockCoord(SectionPosition.z(af), ah));
                                lightProvider.checkEdge(Long.MAX_VALUE, ai, 15, false);
                            }
                        }
                    }
                }
            }

            this.sectionsToRemoveSourcesFrom.clear();
            this.hasSourceInconsistencies = false;
        }
    }

    protected boolean hasSectionsBelow(int sectionY) {
        return sectionY >= (this.updatingSectionData).currentLowestY;
    }

    protected boolean isAboveData(long sectionPos) {
        long l = SectionPosition.getZeroNode(sectionPos);
        int i = (this.updatingSectionData).topSections.get(l);
        return i == (this.updatingSectionData).currentLowestY || SectionPosition.y(sectionPos) >= i;
    }

    protected boolean lightOnInSection(long sectionPos) {
        long l = SectionPosition.getZeroNode(sectionPos);
        return this.columnsWithSkySources.contains(l);
    }

    protected static final class SkyDataLayerStorageMap extends LightEngineStorageArray<LightEngineStorageSky.SkyDataLayerStorageMap> {
        int currentLowestY;
        final Long2IntOpenHashMap topSections;

        public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<NibbleArray> arrays, Long2IntOpenHashMap columnToTopSection, int minSectionY) {
            super(arrays);
            this.topSections = columnToTopSection;
            columnToTopSection.defaultReturnValue(minSectionY);
            this.currentLowestY = minSectionY;
        }

        @Override
        public LightEngineStorageSky.SkyDataLayerStorageMap copy() {
            return new LightEngineStorageSky.SkyDataLayerStorageMap(this.map.clone(), this.topSections.clone(), this.currentLowestY);
        }
    }
}
