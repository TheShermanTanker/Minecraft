package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.INamable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureRuinedPortalPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureRuinedPortal extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration> {
    static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{"ruined_portal/portal_1", "ruined_portal/portal_2", "ruined_portal/portal_3", "ruined_portal/portal_4", "ruined_portal/portal_5", "ruined_portal/portal_6", "ruined_portal/portal_7", "ruined_portal/portal_8", "ruined_portal/portal_9", "ruined_portal/portal_10"};
    static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"};
    private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
    private static final float PROBABILITY_OF_AIR_POCKET = 0.5F;
    private static final float PROBABILITY_OF_UNDERGROUND = 0.5F;
    private static final float UNDERWATER_MOSSINESS = 0.8F;
    private static final float JUNGLE_MOSSINESS = 0.8F;
    private static final float SWAMP_MOSSINESS = 0.5F;
    private static final int MIN_Y = 15;

    public WorldGenFeatureRuinedPortal(Codec<WorldGenFeatureRuinedPortalConfiguration> codec) {
        super(codec);
    }

    @Override
    public StructureGenerator.StructureStartFactory<WorldGenFeatureRuinedPortalConfiguration> getStartFactory() {
        return WorldGenFeatureRuinedPortal.FeatureStart::new;
    }

    static boolean isCold(BlockPosition pos, BiomeBase biome) {
        return biome.getAdjustedTemperature(pos) < 0.15F;
    }

    static int findSuitableY(Random random, ChunkGenerator chunkGenerator, WorldGenFeatureRuinedPortalPieces.Position verticalPlacement, boolean airPocket, int height, int blockCountY, StructureBoundingBox box, IWorldHeightAccess world) {
        int i;
        if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.IN_NETHER) {
            if (airPocket) {
                i = MathHelper.randomBetweenInclusive(random, 32, 100);
            } else if (random.nextFloat() < 0.5F) {
                i = MathHelper.randomBetweenInclusive(random, 27, 29);
            } else {
                i = MathHelper.randomBetweenInclusive(random, 29, 100);
            }
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.IN_MOUNTAIN) {
            int l = height - blockCountY;
            i = getRandomWithinInterval(random, 70, l);
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.UNDERGROUND) {
            int n = height - blockCountY;
            i = getRandomWithinInterval(random, 15, n);
        } else if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.PARTLY_BURIED) {
            i = height - blockCountY + MathHelper.randomBetweenInclusive(random, 2, 8);
        } else {
            i = height;
        }

        List<BlockPosition> list = ImmutableList.of(new BlockPosition(box.minX(), 0, box.minZ()), new BlockPosition(box.maxX(), 0, box.minZ()), new BlockPosition(box.minX(), 0, box.maxZ()), new BlockPosition(box.maxX(), 0, box.maxZ()));
        List<BlockColumn> list2 = list.stream().map((blockPos) -> {
            return chunkGenerator.getBaseColumn(blockPos.getX(), blockPos.getZ(), world);
        }).collect(Collectors.toList());
        HeightMap.Type types = verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR ? HeightMap.Type.OCEAN_FLOOR_WG : HeightMap.Type.WORLD_SURFACE_WG;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        int r;
        for(r = i; r > 15; --r) {
            int s = 0;
            mutableBlockPos.set(0, r, 0);

            for(BlockColumn noiseColumn : list2) {
                IBlockData blockState = noiseColumn.getBlockState(mutableBlockPos);
                if (types.isOpaque().test(blockState)) {
                    ++s;
                    if (s == 3) {
                        return r;
                    }
                }
            }
        }

        return r;
    }

    private static int getRandomWithinInterval(Random random, int min, int max) {
        return min < max ? MathHelper.randomBetweenInclusive(random, min, max) : max;
    }

    public static class FeatureStart extends StructureStart<WorldGenFeatureRuinedPortalConfiguration> {
        protected FeatureStart(StructureGenerator<WorldGenFeatureRuinedPortalConfiguration> feature, ChunkCoordIntPair pos, int references, long seed) {
            super(feature, pos, references, seed);
        }

        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenFeatureRuinedPortalConfiguration config, IWorldHeightAccess world) {
            WorldGenFeatureRuinedPortalPieces.Properties properties = new WorldGenFeatureRuinedPortalPieces.Properties();
            WorldGenFeatureRuinedPortalPieces.Position verticalPlacement;
            if (config.portalType == WorldGenFeatureRuinedPortal.Type.DESERT) {
                verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.PARTLY_BURIED;
                properties.airPocket = false;
                properties.mossiness = 0.0F;
            } else if (config.portalType == WorldGenFeatureRuinedPortal.Type.JUNGLE) {
                verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
                properties.airPocket = this.random.nextFloat() < 0.5F;
                properties.mossiness = 0.8F;
                properties.overgrown = true;
                properties.vines = true;
            } else if (config.portalType == WorldGenFeatureRuinedPortal.Type.SWAMP) {
                verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR;
                properties.airPocket = false;
                properties.mossiness = 0.5F;
                properties.vines = true;
            } else if (config.portalType == WorldGenFeatureRuinedPortal.Type.MOUNTAIN) {
                boolean bl = this.random.nextFloat() < 0.5F;
                verticalPlacement = bl ? WorldGenFeatureRuinedPortalPieces.Position.IN_MOUNTAIN : WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
                properties.airPocket = bl || this.random.nextFloat() < 0.5F;
            } else if (config.portalType == WorldGenFeatureRuinedPortal.Type.OCEAN) {
                verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR;
                properties.airPocket = false;
                properties.mossiness = 0.8F;
            } else if (config.portalType == WorldGenFeatureRuinedPortal.Type.NETHER) {
                verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.IN_NETHER;
                properties.airPocket = this.random.nextFloat() < 0.5F;
                properties.mossiness = 0.0F;
                properties.replaceWithBlackstone = true;
            } else {
                boolean bl2 = this.random.nextFloat() < 0.5F;
                verticalPlacement = bl2 ? WorldGenFeatureRuinedPortalPieces.Position.UNDERGROUND : WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE;
                properties.airPocket = bl2 || this.random.nextFloat() < 0.5F;
            }

            MinecraftKey resourceLocation;
            if (this.random.nextFloat() < 0.05F) {
                resourceLocation = new MinecraftKey(WorldGenFeatureRuinedPortal.STRUCTURE_LOCATION_GIANT_PORTALS[this.random.nextInt(WorldGenFeatureRuinedPortal.STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
            } else {
                resourceLocation = new MinecraftKey(WorldGenFeatureRuinedPortal.STRUCTURE_LOCATION_PORTALS[this.random.nextInt(WorldGenFeatureRuinedPortal.STRUCTURE_LOCATION_PORTALS.length)]);
            }

            DefinedStructure structureTemplate = manager.getOrCreate(resourceLocation);
            EnumBlockRotation rotation = SystemUtils.getRandom(EnumBlockRotation.values(), this.random);
            EnumBlockMirror mirror = this.random.nextFloat() < 0.5F ? EnumBlockMirror.NONE : EnumBlockMirror.FRONT_BACK;
            BlockPosition blockPos = new BlockPosition(structureTemplate.getSize().getX() / 2, 0, structureTemplate.getSize().getZ() / 2);
            BlockPosition blockPos2 = pos.getWorldPosition();
            StructureBoundingBox boundingBox = structureTemplate.getBoundingBox(blockPos2, rotation, blockPos, mirror);
            BlockPosition blockPos3 = boundingBox.getCenter();
            int i = blockPos3.getX();
            int j = blockPos3.getZ();
            int k = chunkGenerator.getBaseHeight(i, j, WorldGenFeatureRuinedPortalPieces.getHeightMapType(verticalPlacement), world) - 1;
            int l = WorldGenFeatureRuinedPortal.findSuitableY(this.random, chunkGenerator, verticalPlacement, properties.airPocket, k, boundingBox.getYSpan(), boundingBox, world);
            BlockPosition blockPos4 = new BlockPosition(blockPos2.getX(), l, blockPos2.getZ());
            if (config.portalType == WorldGenFeatureRuinedPortal.Type.MOUNTAIN || config.portalType == WorldGenFeatureRuinedPortal.Type.OCEAN || config.portalType == WorldGenFeatureRuinedPortal.Type.STANDARD) {
                properties.cold = WorldGenFeatureRuinedPortal.isCold(blockPos4, biome);
            }

            this.addPiece(new WorldGenFeatureRuinedPortalPieces(manager, blockPos4, verticalPlacement, properties, resourceLocation, structureTemplate, rotation, mirror, blockPos));
        }
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
