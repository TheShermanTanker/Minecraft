package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureRuinedPortalPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;

public class WorldGenFeatureRuinedPortal extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration> {
    private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{"ruined_portal/portal_1", "ruined_portal/portal_2", "ruined_portal/portal_3", "ruined_portal/portal_4", "ruined_portal/portal_5", "ruined_portal/portal_6", "ruined_portal/portal_7", "ruined_portal/portal_8", "ruined_portal/portal_9", "ruined_portal/portal_10"};
    private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"};
    private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
    private static final float PROBABILITY_OF_AIR_POCKET = 0.5F;
    private static final float PROBABILITY_OF_UNDERGROUND = 0.5F;
    private static final float UNDERWATER_MOSSINESS = 0.8F;
    private static final float JUNGLE_MOSSINESS = 0.8F;
    private static final float SWAMP_MOSSINESS = 0.5F;
    private static final int MIN_Y_INDEX = 15;

    public WorldGenFeatureRuinedPortal(Codec<WorldGenFeatureRuinedPortalConfiguration> configCodec) {
        super(configCodec, WorldGenFeatureRuinedPortal::pieceGeneratorSupplier);
    }

    private static Optional<PieceGenerator<WorldGenFeatureRuinedPortalConfiguration>> pieceGeneratorSupplier(PieceGeneratorSupplier.Context<WorldGenFeatureRuinedPortalConfiguration> context) {
        WorldGenFeatureRuinedPortalPieces.Properties properties = new WorldGenFeatureRuinedPortalPieces.Properties();
        WorldGenFeatureRuinedPortalConfiguration ruinedPortalConfiguration = context.config();
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        WorldGenFeatureRuinedPortalPieces.Position verticalPlacement;
        if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.DESERT) {
            verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.PARTLY_BURIED;
            properties.airPocket = false;
            properties.mossiness = 0.0F;
        } else if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.JUNGLE) {
            verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
            properties.airPocket = worldgenRandom.nextFloat() < 0.5F;
            properties.mossiness = 0.8F;
            properties.overgrown = true;
            properties.vines = true;
        } else if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.SWAMP) {
            verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR;
            properties.airPocket = false;
            properties.mossiness = 0.5F;
            properties.vines = true;
        } else if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.MOUNTAIN) {
            boolean bl = worldgenRandom.nextFloat() < 0.5F;
            verticalPlacement = bl ? WorldGenFeatureRuinedPortalPieces.Position.IN_MOUNTAIN : WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
            properties.airPocket = bl || worldgenRandom.nextFloat() < 0.5F;
        } else if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.OCEAN) {
            verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR;
            properties.airPocket = false;
            properties.mossiness = 0.8F;
        } else if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.NETHER) {
            verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.IN_NETHER;
            properties.airPocket = worldgenRandom.nextFloat() < 0.5F;
            properties.mossiness = 0.0F;
            properties.replaceWithBlackstone = true;
        } else {
            boolean bl2 = worldgenRandom.nextFloat() < 0.5F;
            verticalPlacement = bl2 ? WorldGenFeatureRuinedPortalPieces.Position.UNDERGROUND : WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
            properties.airPocket = bl2 || worldgenRandom.nextFloat() < 0.5F;
        }

        MinecraftKey resourceLocation;
        if (worldgenRandom.nextFloat() < 0.05F) {
            resourceLocation = new MinecraftKey(STRUCTURE_LOCATION_GIANT_PORTALS[worldgenRandom.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
        } else {
            resourceLocation = new MinecraftKey(STRUCTURE_LOCATION_PORTALS[worldgenRandom.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
        }

        DefinedStructure structureTemplate = context.structureManager().getOrCreate(resourceLocation);
        EnumBlockRotation rotation = SystemUtils.getRandom(EnumBlockRotation.values(), worldgenRandom);
        EnumBlockMirror mirror = worldgenRandom.nextFloat() < 0.5F ? EnumBlockMirror.NONE : EnumBlockMirror.FRONT_BACK;
        BlockPosition blockPos = new BlockPosition(structureTemplate.getSize().getX() / 2, 0, structureTemplate.getSize().getZ() / 2);
        BlockPosition blockPos2 = context.chunkPos().getWorldPosition();
        StructureBoundingBox boundingBox = structureTemplate.getBoundingBox(blockPos2, rotation, blockPos, mirror);
        BlockPosition blockPos3 = boundingBox.getCenter();
        int i = context.chunkGenerator().getBaseHeight(blockPos3.getX(), blockPos3.getZ(), WorldGenFeatureRuinedPortalPieces.getHeightMapType(verticalPlacement), context.heightAccessor()) - 1;
        int j = findSuitableY(worldgenRandom, context.chunkGenerator(), verticalPlacement, properties.airPocket, i, boundingBox.getYSpan(), boundingBox, context.heightAccessor());
        BlockPosition blockPos4 = new BlockPosition(blockPos2.getX(), j, blockPos2.getZ());
        return !context.validBiome().test(context.chunkGenerator().getBiome(QuartPos.fromBlock(blockPos4.getX()), QuartPos.fromBlock(blockPos4.getY()), QuartPos.fromBlock(blockPos4.getZ()))) ? Optional.empty() : Optional.of((collector, contextx) -> {
            if (ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.MOUNTAIN || ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.OCEAN || ruinedPortalConfiguration.portalType == WorldGenFeatureRuinedPortal.Type.STANDARD) {
                properties.cold = isCold(blockPos4, context.chunkGenerator().getBiome(QuartPos.fromBlock(blockPos4.getX()), QuartPos.fromBlock(blockPos4.getY()), QuartPos.fromBlock(blockPos4.getZ())));
            }

            collector.addPiece(new WorldGenFeatureRuinedPortalPieces(contextx.structureManager(), blockPos4, verticalPlacement, properties, resourceLocation, structureTemplate, rotation, mirror, blockPos));
        });
    }

    private static boolean isCold(BlockPosition pos, BiomeBase biome) {
        return biome.coldEnoughToSnow(pos);
    }

    private static int findSuitableY(Random random, ChunkGenerator chunkGenerator, WorldGenFeatureRuinedPortalPieces.Position verticalPlacement, boolean airPocket, int height, int blockCountY, StructureBoundingBox box, IWorldHeightAccess world) {
        int i = world.getMinBuildHeight() + 15;
        int j;
        if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.IN_NETHER) {
            if (airPocket) {
                j = MathHelper.randomBetweenInclusive(random, 32, 100);
            } else if (random.nextFloat() < 0.5F) {
                j = MathHelper.randomBetweenInclusive(random, 27, 29);
            } else {
                j = MathHelper.randomBetweenInclusive(random, 29, 100);
            }
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.IN_MOUNTAIN) {
            int m = height - blockCountY;
            j = getRandomWithinInterval(random, 70, m);
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.UNDERGROUND) {
            int o = height - blockCountY;
            j = getRandomWithinInterval(random, i, o);
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.PARTLY_BURIED) {
            j = height - blockCountY + MathHelper.randomBetweenInclusive(random, 2, 8);
        } else {
            j = height;
        }

        List<BlockPosition> list = ImmutableList.of(new BlockPosition(box.minX(), 0, box.minZ()), new BlockPosition(box.maxX(), 0, box.minZ()), new BlockPosition(box.minX(), 0, box.maxZ()), new BlockPosition(box.maxX(), 0, box.maxZ()));
        List<BlockColumn> list2 = list.stream().map((pos) -> {
            return chunkGenerator.getBaseColumn(pos.getX(), pos.getZ(), world);
        }).collect(Collectors.toList());
        HeightMap.Type types = verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR ? HeightMap.Type.OCEAN_FLOOR_WG : HeightMap.Type.WORLD_SURFACE_WG;

        int s;
        for(s = j; s > i; --s) {
            int t = 0;

            for(BlockColumn noiseColumn : list2) {
                IBlockData blockState = noiseColumn.getBlock(s);
                if (types.isOpaque().test(blockState)) {
                    ++t;
                    if (t == 3) {
                        return s;
                    }
                }
            }
        }

        return s;
    }

    private static int getRandomWithinInterval(Random random, int min, int max) {
        return min < max ? MathHelper.randomBetweenInclusive(random, min, max) : max;
    }

    public static enum Type implements INamable {
        STANDARD("standard"),
        DESERT("desert"),
        JUNGLE("jungle"),
        SWAMP("swamp"),
        MOUNTAIN("mountain"),
        OCEAN("ocean"),
        NETHER("nether");

        public static final Codec<WorldGenFeatureRuinedPortal.Type> CODEC = INamable.fromEnum(WorldGenFeatureRuinedPortal.Type::values, WorldGenFeatureRuinedPortal.Type::byName);
        private static final Map<String, WorldGenFeatureRuinedPortal.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenFeatureRuinedPortal.Type::getName, (type) -> {
            return type;
        }));
        private final String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static WorldGenFeatureRuinedPortal.Type byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
