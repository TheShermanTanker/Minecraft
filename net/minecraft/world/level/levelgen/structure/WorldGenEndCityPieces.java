package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityLootable;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenEndCityPieces {
    private static final int MAX_GEN_DEPTH = 8;
    static final WorldGenEndCityPieces.PieceGenerator HOUSE_TOWER_GENERATOR = new WorldGenEndCityPieces.PieceGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(DefinedStructureManager manager, int depth, WorldGenEndCityPieces.Piece root, BlockPosition pos, List<StructurePiece> pieces, Random random) {
            if (depth > 8) {
                return false;
            } else {
                EnumBlockRotation rotation = root.placeSettings.getRotation();
                WorldGenEndCityPieces.Piece endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, root, pos, "base_floor", rotation, true));
                int i = random.nextInt(3);
                if (i == 0) {
                    WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 4, -1), "base_roof", rotation, true));
                } else if (i == 1) {
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 0, -1), "second_floor_2", rotation, false));
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 8, -1), "second_roof", rotation, false));
                    WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPosition)null, pieces, random);
                } else if (i == 2) {
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 0, -1), "second_floor_2", rotation, false));
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 4, -1), "third_floor_2", rotation, false));
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 8, -1), "third_roof", rotation, true));
                    WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPosition)null, pieces, random);
                }

                return true;
            }
        }
    };
    static final List<Tuple<EnumBlockRotation, BlockPosition>> TOWER_BRIDGES = Lists.newArrayList(new Tuple<>(EnumBlockRotation.NONE, new BlockPosition(1, -1, 0)), new Tuple<>(EnumBlockRotation.CLOCKWISE_90, new BlockPosition(6, -1, 1)), new Tuple<>(EnumBlockRotation.COUNTERCLOCKWISE_90, new BlockPosition(0, -1, 5)), new Tuple<>(EnumBlockRotation.CLOCKWISE_180, new BlockPosition(5, -1, 6)));
    static final WorldGenEndCityPieces.PieceGenerator TOWER_GENERATOR = new WorldGenEndCityPieces.PieceGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(DefinedStructureManager manager, int depth, WorldGenEndCityPieces.Piece root, BlockPosition pos, List<StructurePiece> pieces, Random random) {
            EnumBlockRotation rotation = root.placeSettings.getRotation();
            WorldGenEndCityPieces.Piece endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, root, new BlockPosition(3 + random.nextInt(2), -3, 3 + random.nextInt(2)), "tower_base", rotation, true));
            endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, 7, 0), "tower_piece", rotation, true));
            WorldGenEndCityPieces.Piece endCityPiece2 = random.nextInt(3) == 0 ? endCityPiece : null;
            int i = 1 + random.nextInt(3);

            for(int j = 0; j < i; ++j) {
                endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, 4, 0), "tower_piece", rotation, true));
                if (j < i - 1 && random.nextBoolean()) {
                    endCityPiece2 = endCityPiece;
                }
            }

            if (endCityPiece2 != null) {
                for(Tuple<EnumBlockRotation, BlockPosition> tuple : WorldGenEndCityPieces.TOWER_BRIDGES) {
                    if (random.nextBoolean()) {
                        WorldGenEndCityPieces.Piece endCityPiece3 = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece2, tuple.getB(), "bridge_end", rotation.getRotated(tuple.getA()), true));
                        WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.TOWER_BRIDGE_GENERATOR, depth + 1, endCityPiece3, (BlockPosition)null, pieces, random);
                    }
                }

                WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 4, -1), "tower_top", rotation, true));
            } else {
                if (depth != 7) {
                    return WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.FAT_TOWER_GENERATOR, depth + 1, endCityPiece, (BlockPosition)null, pieces, random);
                }

                WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-1, 4, -1), "tower_top", rotation, true));
            }

            return true;
        }
    };
    static final WorldGenEndCityPieces.PieceGenerator TOWER_BRIDGE_GENERATOR = new WorldGenEndCityPieces.PieceGenerator() {
        public boolean shipCreated;

        @Override
        public void init() {
            this.shipCreated = false;
        }

        @Override
        public boolean generate(DefinedStructureManager manager, int depth, WorldGenEndCityPieces.Piece root, BlockPosition pos, List<StructurePiece> pieces, Random random) {
            EnumBlockRotation rotation = root.placeSettings.getRotation();
            int i = random.nextInt(4) + 1;
            WorldGenEndCityPieces.Piece endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, root, new BlockPosition(0, 0, -4), "bridge_piece", rotation, true));
            endCityPiece.genDepth = -1;
            int j = 0;

            for(int k = 0; k < i; ++k) {
                if (random.nextBoolean()) {
                    endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, j, -4), "bridge_piece", rotation, true));
                    j = 0;
                } else {
                    if (random.nextBoolean()) {
                        endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, j, -4), "bridge_steep_stairs", rotation, true));
                    } else {
                        endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, j, -8), "bridge_gentle_stairs", rotation, true));
                    }

                    j = 4;
                }
            }

            if (!this.shipCreated && random.nextInt(10 - depth) == 0) {
                WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-8 + random.nextInt(8), j, -70 + random.nextInt(10)), "ship", rotation, true));
                this.shipCreated = true;
            } else if (!WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.HOUSE_TOWER_GENERATOR, depth + 1, endCityPiece, new BlockPosition(-3, j + 1, -11), pieces, random)) {
                return false;
            }

            endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(4, j, 0), "bridge_end", rotation.getRotated(EnumBlockRotation.CLOCKWISE_180), true));
            endCityPiece.genDepth = -1;
            return true;
        }
    };
    static final List<Tuple<EnumBlockRotation, BlockPosition>> FAT_TOWER_BRIDGES = Lists.newArrayList(new Tuple<>(EnumBlockRotation.NONE, new BlockPosition(4, -1, 0)), new Tuple<>(EnumBlockRotation.CLOCKWISE_90, new BlockPosition(12, -1, 4)), new Tuple<>(EnumBlockRotation.COUNTERCLOCKWISE_90, new BlockPosition(0, -1, 8)), new Tuple<>(EnumBlockRotation.CLOCKWISE_180, new BlockPosition(8, -1, 12)));
    static final WorldGenEndCityPieces.PieceGenerator FAT_TOWER_GENERATOR = new WorldGenEndCityPieces.PieceGenerator() {
        @Override
        public void init() {
        }

        @Override
        public boolean generate(DefinedStructureManager manager, int depth, WorldGenEndCityPieces.Piece root, BlockPosition pos, List<StructurePiece> pieces, Random random) {
            EnumBlockRotation rotation = root.placeSettings.getRotation();
            WorldGenEndCityPieces.Piece endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, root, new BlockPosition(-3, 4, -3), "fat_tower_base", rotation, true));
            endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, 4, 0), "fat_tower_middle", rotation, true));

            for(int i = 0; i < 2 && random.nextInt(3) != 0; ++i) {
                endCityPiece = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(0, 8, 0), "fat_tower_middle", rotation, true));

                for(Tuple<EnumBlockRotation, BlockPosition> tuple : WorldGenEndCityPieces.FAT_TOWER_BRIDGES) {
                    if (random.nextBoolean()) {
                        WorldGenEndCityPieces.Piece endCityPiece2 = WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, tuple.getB(), "bridge_end", rotation.getRotated(tuple.getA()), true));
                        WorldGenEndCityPieces.recursiveChildren(manager, WorldGenEndCityPieces.TOWER_BRIDGE_GENERATOR, depth + 1, endCityPiece2, (BlockPosition)null, pieces, random);
                    }
                }
            }

            WorldGenEndCityPieces.addHelper(pieces, WorldGenEndCityPieces.addPiece(manager, endCityPiece, new BlockPosition(-2, 8, -2), "fat_tower_top", rotation, true));
            return true;
        }
    };

    static WorldGenEndCityPieces.Piece addPiece(DefinedStructureManager structureManager, WorldGenEndCityPieces.Piece lastPiece, BlockPosition relativePosition, String template, EnumBlockRotation rotation, boolean ignoreAir) {
        WorldGenEndCityPieces.Piece endCityPiece = new WorldGenEndCityPieces.Piece(structureManager, template, lastPiece.templatePosition, rotation, ignoreAir);
        BlockPosition blockPos = lastPiece.template.calculateConnectedPosition(lastPiece.placeSettings, relativePosition, endCityPiece.placeSettings, BlockPosition.ZERO);
        endCityPiece.move(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        return endCityPiece;
    }

    public static void startHouseTower(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, List<StructurePiece> pieces, Random random) {
        FAT_TOWER_GENERATOR.init();
        HOUSE_TOWER_GENERATOR.init();
        TOWER_BRIDGE_GENERATOR.init();
        TOWER_GENERATOR.init();
        WorldGenEndCityPieces.Piece endCityPiece = addHelper(pieces, new WorldGenEndCityPieces.Piece(structureManager, "base_floor", pos, rotation, true));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPosition(-1, 0, -1), "second_floor_1", rotation, false));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPosition(-1, 4, -1), "third_floor_1", rotation, false));
        endCityPiece = addHelper(pieces, addPiece(structureManager, endCityPiece, new BlockPosition(-1, 8, -1), "third_roof", rotation, true));
        recursiveChildren(structureManager, TOWER_GENERATOR, 1, endCityPiece, (BlockPosition)null, pieces, random);
    }

    static WorldGenEndCityPieces.Piece addHelper(List<StructurePiece> pieces, WorldGenEndCityPieces.Piece piece) {
        pieces.add(piece);
        return piece;
    }

    static boolean recursiveChildren(DefinedStructureManager manager, WorldGenEndCityPieces.PieceGenerator piece, int depth, WorldGenEndCityPieces.Piece parent, BlockPosition pos, List<StructurePiece> pieces, Random random) {
        if (depth > 8) {
            return false;
        } else {
            List<StructurePiece> list = Lists.newArrayList();
            if (piece.generate(manager, depth, parent, pos, list, random)) {
                boolean bl = false;
                int i = random.nextInt();

                for(StructurePiece structurePiece : list) {
                    structurePiece.genDepth = i;
                    StructurePiece structurePiece2 = StructureStart.findCollisionPiece(pieces, structurePiece.getBoundingBox());
                    if (structurePiece2 != null && structurePiece2.genDepth != parent.genDepth) {
                        bl = true;
                        break;
                    }
                }

                if (!bl) {
                    pieces.addAll(list);
                    return true;
                }
            }

            return false;
        }
    }

    public static class Piece extends DefinedStructurePiece {
        public Piece(DefinedStructureManager manager, String template, BlockPosition pos, EnumBlockRotation rotation, boolean includeAir) {
            super(WorldGenFeatureStructurePieceType.END_CITY_PIECE, 0, manager, makeResourceLocation(template), template, makeSettings(includeAir, rotation), pos);
        }

        public Piece(WorldServer world, NBTTagCompound compoundTag) {
            super(WorldGenFeatureStructurePieceType.END_CITY_PIECE, compoundTag, world, (resourceLocation) -> {
                return makeSettings(compoundTag.getBoolean("OW"), EnumBlockRotation.valueOf(compoundTag.getString("Rot")));
            });
        }

        private static DefinedStructureInfo makeSettings(boolean includeAir, EnumBlockRotation rotation) {
            DefinedStructureProcessorBlockIgnore blockIgnoreProcessor = includeAir ? DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK : DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR;
            return (new DefinedStructureInfo()).setIgnoreEntities(true).addProcessor(blockIgnoreProcessor).setRotation(rotation);
        }

        @Override
        protected MinecraftKey makeTemplateLocation() {
            return makeResourceLocation(this.templateName);
        }

        private static MinecraftKey makeResourceLocation(String template) {
            return new MinecraftKey("end_city/" + template);
        }

        @Override
        protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
            super.addAdditionalSaveData(world, nbt);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
            nbt.setBoolean("OW", this.placeSettings.getProcessors().get(0) == DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            if (metadata.startsWith("Chest")) {
                BlockPosition blockPos = pos.below();
                if (boundingBox.isInside(blockPos)) {
                    TileEntityLootable.setLootTable(world, random, blockPos, LootTables.END_CITY_TREASURE);
                }
            } else if (boundingBox.isInside(pos) && World.isInSpawnableBounds(pos)) {
                if (metadata.startsWith("Sentry")) {
                    EntityShulker shulker = EntityTypes.SHULKER.create(world.getLevel());
                    shulker.setPosition((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D);
                    world.addEntity(shulker);
                } else if (metadata.startsWith("Elytra")) {
                    EntityItemFrame itemFrame = new EntityItemFrame(world.getLevel(), pos, this.placeSettings.getRotation().rotate(EnumDirection.SOUTH));
                    itemFrame.setItem(new ItemStack(Items.ELYTRA), false);
                    world.addEntity(itemFrame);
                }
            }

        }
    }

    interface PieceGenerator {
        void init();

        boolean generate(DefinedStructureManager manager, int depth, WorldGenEndCityPieces.Piece root, BlockPosition pos, List<StructurePiece> pieces, Random random);
    }
}
