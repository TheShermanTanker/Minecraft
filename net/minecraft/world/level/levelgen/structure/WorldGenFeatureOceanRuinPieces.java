package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.monster.EntityDrowned;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorRotation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenFeatureOceanRuinPieces {
    private static final MinecraftKey[] WARM_RUINS = new MinecraftKey[]{new MinecraftKey("underwater_ruin/warm_1"), new MinecraftKey("underwater_ruin/warm_2"), new MinecraftKey("underwater_ruin/warm_3"), new MinecraftKey("underwater_ruin/warm_4"), new MinecraftKey("underwater_ruin/warm_5"), new MinecraftKey("underwater_ruin/warm_6"), new MinecraftKey("underwater_ruin/warm_7"), new MinecraftKey("underwater_ruin/warm_8")};
    private static final MinecraftKey[] RUINS_BRICK = new MinecraftKey[]{new MinecraftKey("underwater_ruin/brick_1"), new MinecraftKey("underwater_ruin/brick_2"), new MinecraftKey("underwater_ruin/brick_3"), new MinecraftKey("underwater_ruin/brick_4"), new MinecraftKey("underwater_ruin/brick_5"), new MinecraftKey("underwater_ruin/brick_6"), new MinecraftKey("underwater_ruin/brick_7"), new MinecraftKey("underwater_ruin/brick_8")};
    private static final MinecraftKey[] RUINS_CRACKED = new MinecraftKey[]{new MinecraftKey("underwater_ruin/cracked_1"), new MinecraftKey("underwater_ruin/cracked_2"), new MinecraftKey("underwater_ruin/cracked_3"), new MinecraftKey("underwater_ruin/cracked_4"), new MinecraftKey("underwater_ruin/cracked_5"), new MinecraftKey("underwater_ruin/cracked_6"), new MinecraftKey("underwater_ruin/cracked_7"), new MinecraftKey("underwater_ruin/cracked_8")};
    private static final MinecraftKey[] RUINS_MOSSY = new MinecraftKey[]{new MinecraftKey("underwater_ruin/mossy_1"), new MinecraftKey("underwater_ruin/mossy_2"), new MinecraftKey("underwater_ruin/mossy_3"), new MinecraftKey("underwater_ruin/mossy_4"), new MinecraftKey("underwater_ruin/mossy_5"), new MinecraftKey("underwater_ruin/mossy_6"), new MinecraftKey("underwater_ruin/mossy_7"), new MinecraftKey("underwater_ruin/mossy_8")};
    private static final MinecraftKey[] BIG_RUINS_BRICK = new MinecraftKey[]{new MinecraftKey("underwater_ruin/big_brick_1"), new MinecraftKey("underwater_ruin/big_brick_2"), new MinecraftKey("underwater_ruin/big_brick_3"), new MinecraftKey("underwater_ruin/big_brick_8")};
    private static final MinecraftKey[] BIG_RUINS_MOSSY = new MinecraftKey[]{new MinecraftKey("underwater_ruin/big_mossy_1"), new MinecraftKey("underwater_ruin/big_mossy_2"), new MinecraftKey("underwater_ruin/big_mossy_3"), new MinecraftKey("underwater_ruin/big_mossy_8")};
    private static final MinecraftKey[] BIG_RUINS_CRACKED = new MinecraftKey[]{new MinecraftKey("underwater_ruin/big_cracked_1"), new MinecraftKey("underwater_ruin/big_cracked_2"), new MinecraftKey("underwater_ruin/big_cracked_3"), new MinecraftKey("underwater_ruin/big_cracked_8")};
    private static final MinecraftKey[] BIG_WARM_RUINS = new MinecraftKey[]{new MinecraftKey("underwater_ruin/big_warm_4"), new MinecraftKey("underwater_ruin/big_warm_5"), new MinecraftKey("underwater_ruin/big_warm_6"), new MinecraftKey("underwater_ruin/big_warm_7")};

    private static MinecraftKey getSmallWarmRuin(Random random) {
        return SystemUtils.getRandom(WARM_RUINS, random);
    }

    private static MinecraftKey getBigWarmRuin(Random random) {
        return SystemUtils.getRandom(BIG_WARM_RUINS, random);
    }

    public static void addPieces(DefinedStructureManager manager, BlockPosition pos, EnumBlockRotation rotation, StructurePieceAccessor structurePieceAccessor, Random random, WorldGenFeatureOceanRuinConfiguration config) {
        boolean bl = random.nextFloat() <= config.largeProbability;
        float f = bl ? 0.9F : 0.8F;
        addPiece(manager, pos, rotation, structurePieceAccessor, random, config, bl, f);
        if (bl && random.nextFloat() <= config.clusterProbability) {
            addClusterRuins(manager, random, rotation, pos, config, structurePieceAccessor);
        }

    }

    private static void addClusterRuins(DefinedStructureManager manager, Random random, EnumBlockRotation rotation, BlockPosition pos, WorldGenFeatureOceanRuinConfiguration config, StructurePieceAccessor structurePieceAccessor) {
        BlockPosition blockPos = new BlockPosition(pos.getX(), 90, pos.getZ());
        BlockPosition blockPos2 = DefinedStructure.transform(new BlockPosition(15, 0, 15), EnumBlockMirror.NONE, rotation, BlockPosition.ZERO).offset(blockPos);
        StructureBoundingBox boundingBox = StructureBoundingBox.fromCorners(blockPos, blockPos2);
        BlockPosition blockPos3 = new BlockPosition(Math.min(blockPos.getX(), blockPos2.getX()), blockPos.getY(), Math.min(blockPos.getZ(), blockPos2.getZ()));
        List<BlockPosition> list = allPositions(random, blockPos3);
        int i = MathHelper.nextInt(random, 4, 8);

        for(int j = 0; j < i; ++j) {
            if (!list.isEmpty()) {
                int k = random.nextInt(list.size());
                BlockPosition blockPos4 = list.remove(k);
                EnumBlockRotation rotation2 = EnumBlockRotation.getRandom(random);
                BlockPosition blockPos5 = DefinedStructure.transform(new BlockPosition(5, 0, 6), EnumBlockMirror.NONE, rotation2, BlockPosition.ZERO).offset(blockPos4);
                StructureBoundingBox boundingBox2 = StructureBoundingBox.fromCorners(blockPos4, blockPos5);
                if (!boundingBox2.intersects(boundingBox)) {
                    addPiece(manager, blockPos4, rotation2, structurePieceAccessor, random, config, false, 0.8F);
                }
            }
        }

    }

    private static List<BlockPosition> allPositions(Random random, BlockPosition pos) {
        List<BlockPosition> list = Lists.newArrayList();
        list.add(pos.offset(-16 + MathHelper.nextInt(random, 1, 8), 0, 16 + MathHelper.nextInt(random, 1, 7)));
        list.add(pos.offset(-16 + MathHelper.nextInt(random, 1, 8), 0, MathHelper.nextInt(random, 1, 7)));
        list.add(pos.offset(-16 + MathHelper.nextInt(random, 1, 8), 0, -16 + MathHelper.nextInt(random, 4, 8)));
        list.add(pos.offset(MathHelper.nextInt(random, 1, 7), 0, 16 + MathHelper.nextInt(random, 1, 7)));
        list.add(pos.offset(MathHelper.nextInt(random, 1, 7), 0, -16 + MathHelper.nextInt(random, 4, 6)));
        list.add(pos.offset(16 + MathHelper.nextInt(random, 1, 7), 0, 16 + MathHelper.nextInt(random, 3, 8)));
        list.add(pos.offset(16 + MathHelper.nextInt(random, 1, 7), 0, MathHelper.nextInt(random, 1, 7)));
        list.add(pos.offset(16 + MathHelper.nextInt(random, 1, 7), 0, -16 + MathHelper.nextInt(random, 4, 8)));
        return list;
    }

    private static void addPiece(DefinedStructureManager manager, BlockPosition pos, EnumBlockRotation rotation, StructurePieceAccessor structurePieceAccessor, Random random, WorldGenFeatureOceanRuinConfiguration config, boolean large, float integrity) {
        switch(config.biomeTemp) {
        case WARM:
        default:
            MinecraftKey resourceLocation = large ? getBigWarmRuin(random) : getSmallWarmRuin(random);
            structurePieceAccessor.addPiece(new WorldGenFeatureOceanRuinPieces.OceanRuinPiece(manager, resourceLocation, pos, rotation, integrity, config.biomeTemp, large));
            break;
        case COLD:
            MinecraftKey[] resourceLocations = large ? BIG_RUINS_BRICK : RUINS_BRICK;
            MinecraftKey[] resourceLocations2 = large ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            MinecraftKey[] resourceLocations3 = large ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int i = random.nextInt(resourceLocations.length);
            structurePieceAccessor.addPiece(new WorldGenFeatureOceanRuinPieces.OceanRuinPiece(manager, resourceLocations[i], pos, rotation, integrity, config.biomeTemp, large));
            structurePieceAccessor.addPiece(new WorldGenFeatureOceanRuinPieces.OceanRuinPiece(manager, resourceLocations2[i], pos, rotation, 0.7F, config.biomeTemp, large));
            structurePieceAccessor.addPiece(new WorldGenFeatureOceanRuinPieces.OceanRuinPiece(manager, resourceLocations3[i], pos, rotation, 0.5F, config.biomeTemp, large));
        }

    }

    public static class OceanRuinPiece extends DefinedStructurePiece {
        private final WorldGenFeatureOceanRuin.Temperature biomeType;
        private final float integrity;
        private final boolean isLarge;

        public OceanRuinPiece(DefinedStructureManager structureManager, MinecraftKey template, BlockPosition pos, EnumBlockRotation rotation, float integrity, WorldGenFeatureOceanRuin.Temperature biomeType, boolean large) {
            super(WorldGenFeatureStructurePieceType.OCEAN_RUIN, 0, structureManager, template, template.toString(), makeSettings(rotation), pos);
            this.integrity = integrity;
            this.biomeType = biomeType;
            this.isLarge = large;
        }

        public OceanRuinPiece(WorldServer world, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.OCEAN_RUIN, nbt, world, (resourceLocation) -> {
                return makeSettings(EnumBlockRotation.valueOf(nbt.getString("Rot")));
            });
            this.integrity = nbt.getFloat("Integrity");
            this.biomeType = WorldGenFeatureOceanRuin.Temperature.valueOf(nbt.getString("BiomeType"));
            this.isLarge = nbt.getBoolean("IsLarge");
        }

        private static DefinedStructureInfo makeSettings(EnumBlockRotation rotation) {
            return (new DefinedStructureInfo()).setRotation(rotation).setMirror(EnumBlockMirror.NONE).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
            nbt.setFloat("Integrity", this.integrity);
            nbt.setString("BiomeType", this.biomeType.toString());
            nbt.setBoolean("IsLarge", this.isLarge);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            if ("chest".equals(metadata)) {
                world.setTypeAndData(pos, Blocks.CHEST.getBlockData().set(BlockChest.WATERLOGGED, Boolean.valueOf(world.getFluid(pos).is(TagsFluid.WATER))), 2);
                TileEntity blockEntity = world.getTileEntity(pos);
                if (blockEntity instanceof TileEntityChest) {
                    ((TileEntityChest)blockEntity).setLootTable(this.isLarge ? LootTables.UNDERWATER_RUIN_BIG : LootTables.UNDERWATER_RUIN_SMALL, random.nextLong());
                }
            } else if ("drowned".equals(metadata)) {
                EntityDrowned drowned = EntityTypes.DROWNED.create(world.getLevel());
                drowned.setPersistent();
                drowned.setPositionRotation(pos, 0.0F, 0.0F);
                drowned.prepare(world, world.getDamageScaler(pos), EnumMobSpawn.STRUCTURE, (GroupDataEntity)null, (NBTTagCompound)null);
                world.addAllEntities(drowned);
                if (pos.getY() > world.getSeaLevel()) {
                    world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 2);
                } else {
                    world.setTypeAndData(pos, Blocks.WATER.getBlockData(), 2);
                }
            }

        }

        @Override
        public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            this.placeSettings.clearProcessors().addProcessor(new DefinedStructureProcessorRotation(this.integrity)).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
            int i = world.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
            this.templatePosition = new BlockPosition(this.templatePosition.getX(), i, this.templatePosition.getZ());
            BlockPosition blockPos = DefinedStructure.transform(new BlockPosition(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1), EnumBlockMirror.NONE, this.placeSettings.getRotation(), BlockPosition.ZERO).offset(this.templatePosition);
            this.templatePosition = new BlockPosition(this.templatePosition.getX(), this.getHeight(this.templatePosition, world, blockPos), this.templatePosition.getZ());
            return super.postProcess(world, structureAccessor, chunkGenerator, random, boundingBox, chunkPos, pos);
        }

        private int getHeight(BlockPosition start, IBlockAccess world, BlockPosition end) {
            int i = start.getY();
            int j = 512;
            int k = i - 1;
            int l = 0;

            for(BlockPosition blockPos : BlockPosition.betweenClosed(start, end)) {
                int m = blockPos.getX();
                int n = blockPos.getZ();
                int o = start.getY() - 1;
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(m, o, n);
                IBlockData blockState = world.getType(mutableBlockPos);

                for(Fluid fluidState = world.getFluid(mutableBlockPos); (blockState.isAir() || fluidState.is(TagsFluid.WATER) || blockState.is(TagsBlock.ICE)) && o > world.getMinBuildHeight() + 1; fluidState = world.getFluid(mutableBlockPos)) {
                    --o;
                    mutableBlockPos.set(m, o, n);
                    blockState = world.getType(mutableBlockPos);
                }

                j = Math.min(j, o);
                if (o < k - 2) {
                    ++l;
                }
            }

            int p = Math.abs(start.getX() - end.getX());
            if (k - j > 2 && l > p - 2) {
                i = j + 1;
            }

            return i;
        }
    }
}
