package net.minecraft.world.level.levelgen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.SystemUtils;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructureJigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.WorldGenFeaturePillagerOutpostPoolPiece;

public class Beardifier {
    public static final Beardifier NO_BEARDS = new Beardifier();
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = SystemUtils.make(new float[13824], (array) -> {
        for(int i = 0; i < 24; ++i) {
            for(int j = 0; j < 24; ++j) {
                for(int k = 0; k < 24; ++k) {
                    array[i * 24 * 24 + j * 24 + k] = (float)computeBeardContribution(j - 12, k - 12, i - 12);
                }
            }
        }

    });
    private final ObjectList<StructurePiece> rigids;
    private final ObjectList<WorldGenFeatureDefinedStructureJigsawJunction> junctions;
    private final ObjectListIterator<StructurePiece> pieceIterator;
    private final ObjectListIterator<WorldGenFeatureDefinedStructureJigsawJunction> junctionIterator;

    protected Beardifier(StructureManager accessor, IChunkAccess chunk) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        this.junctions = new ObjectArrayList<>(32);
        this.rigids = new ObjectArrayList<>(10);

        for(StructureGenerator<?> structureFeature : StructureGenerator.NOISE_AFFECTING_FEATURES) {
            accessor.startsForFeature(SectionPosition.bottomOf(chunk), structureFeature).forEach((start) -> {
                for(StructurePiece structurePiece : start.getPieces()) {
                    if (structurePiece.isCloseToChunk(chunkPos, 12)) {
                        if (structurePiece instanceof WorldGenFeaturePillagerOutpostPoolPiece) {
                            WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece = (WorldGenFeaturePillagerOutpostPoolPiece)structurePiece;
                            WorldGenFeatureDefinedStructurePoolTemplate.Matching projection = poolElementStructurePiece.getElement().getProjection();
                            if (projection == WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID) {
                                this.rigids.add(poolElementStructurePiece);
                            }

                            for(WorldGenFeatureDefinedStructureJigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                                int k = jigsawJunction.getSourceX();
                                int l = jigsawJunction.getSourceZ();
                                if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
                                    this.junctions.add(jigsawJunction);
                                }
                            }
                        } else {
                            this.rigids.add(structurePiece);
                        }
                    }
                }

            });
        }

        this.pieceIterator = this.rigids.iterator();
        this.junctionIterator = this.junctions.iterator();
    }

    private Beardifier() {
        this.junctions = new ObjectArrayList<>();
        this.rigids = new ObjectArrayList<>();
        this.pieceIterator = this.rigids.iterator();
        this.junctionIterator = this.junctions.iterator();
    }

    protected double beardifyOrBury(int x, int y, int z) {
        double d = 0.0D;

        while(this.pieceIterator.hasNext()) {
            StructurePiece structurePiece = this.pieceIterator.next();
            StructureBoundingBox boundingBox = structurePiece.getBoundingBox();
            int i = Math.max(0, Math.max(boundingBox.minX() - x, x - boundingBox.maxX()));
            int j = y - (boundingBox.minY() + (structurePiece instanceof WorldGenFeaturePillagerOutpostPoolPiece ? ((WorldGenFeaturePillagerOutpostPoolPiece)structurePiece).getGroundLevelDelta() : 0));
            int k = Math.max(0, Math.max(boundingBox.minZ() - z, z - boundingBox.maxZ()));
            NoiseEffect noiseEffect = structurePiece.getNoiseEffect();
            if (noiseEffect == NoiseEffect.BURY) {
                d += getBuryContribution(i, j, k);
            } else if (noiseEffect == NoiseEffect.BEARD) {
                d += getBeardContribution(i, j, k) * 0.8D;
            }
        }

        this.pieceIterator.back(this.rigids.size());

        while(this.junctionIterator.hasNext()) {
            WorldGenFeatureDefinedStructureJigsawJunction jigsawJunction = this.junctionIterator.next();
            int l = x - jigsawJunction.getSourceX();
            int m = y - jigsawJunction.getSourceGroundY();
            int n = z - jigsawJunction.getSourceZ();
            d += getBeardContribution(l, m, n) * 0.4D;
        }

        this.junctionIterator.back(this.junctions.size());
        return d;
    }

    private static double getBuryContribution(int x, int y, int z) {
        double d = MathHelper.length(x, (double)y / 2.0D, z);
        return MathHelper.clampedMap(d, 0.0D, 6.0D, 1.0D, 0.0D);
    }

    private static double getBeardContribution(int x, int y, int z) {
        int i = x + 12;
        int j = y + 12;
        int k = z + 12;
        if (i >= 0 && i < 24) {
            if (j >= 0 && j < 24) {
                return k >= 0 && k < 24 ? (double)BEARD_KERNEL[k * 24 * 24 + i * 24 + j] : 0.0D;
            } else {
                return 0.0D;
            }
        } else {
            return 0.0D;
        }
    }

    private static double computeBeardContribution(int x, int y, int z) {
        double d = (double)(x * x + z * z);
        double e = (double)y + 0.5D;
        double f = e * e;
        double g = Math.pow(Math.E, -(f / 16.0D + d / 16.0D));
        double h = -e * MathHelper.fastInvSqrt(f / 2.0D + d / 2.0D) / 2.0D;
        return h * g;
    }
}
