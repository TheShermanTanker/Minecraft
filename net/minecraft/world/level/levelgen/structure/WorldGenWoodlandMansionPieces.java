package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.monster.EntityIllagerAbstract;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.storage.loot.LootTables;

public class WorldGenWoodlandMansionPieces {
    public static void generateMansion(DefinedStructureManager manager, BlockPosition pos, EnumBlockRotation rotation, List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, Random random) {
        WorldGenWoodlandMansionPieces.MansionGrid mansionGrid = new WorldGenWoodlandMansionPieces.MansionGrid(random);
        WorldGenWoodlandMansionPieces.MansionPiecePlacer mansionPiecePlacer = new WorldGenWoodlandMansionPieces.MansionPiecePlacer(manager, random);
        mansionPiecePlacer.createMansion(pos, rotation, pieces, mansionGrid);
    }

    public static void main(String[] strings) {
        Random random = new Random();
        long l = random.nextLong();
        System.out.println("Seed: " + l);
        random.setSeed(l);
        WorldGenWoodlandMansionPieces.MansionGrid mansionGrid = new WorldGenWoodlandMansionPieces.MansionGrid(random);
        mansionGrid.print();
    }

    static class FirstFloorRoomCollection extends WorldGenWoodlandMansionPieces.FloorRoomCollection {
        @Override
        public String get1x1(Random random) {
            return "1x1_a" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x1Secret(Random random) {
            return "1x1_as" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2SideEntrance(Random random, boolean staircase) {
            return "1x2_a" + (random.nextInt(9) + 1);
        }

        @Override
        public String get1x2FrontEntrance(Random random, boolean staircase) {
            return "1x2_b" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x2Secret(Random random) {
            return "1x2_s" + (random.nextInt(2) + 1);
        }

        @Override
        public String get2x2(Random random) {
            return "2x2_a" + (random.nextInt(4) + 1);
        }

        @Override
        public String get2x2Secret(Random random) {
            return "2x2_s1";
        }
    }

    abstract static class FloorRoomCollection {
        public abstract String get1x1(Random random);

        public abstract String get1x1Secret(Random random);

        public abstract String get1x2SideEntrance(Random random, boolean staircase);

        public abstract String get1x2FrontEntrance(Random random, boolean staircase);

        public abstract String get1x2Secret(Random random);

        public abstract String get2x2(Random random);

        public abstract String get2x2Secret(Random random);
    }

    static class MansionGrid {
        private static final int DEFAULT_SIZE = 11;
        private static final int CLEAR = 0;
        private static final int CORRIDOR = 1;
        private static final int ROOM = 2;
        private static final int START_ROOM = 3;
        private static final int TEST_ROOM = 4;
        private static final int BLOCKED = 5;
        private static final int ROOM_1x1 = 65536;
        private static final int ROOM_1x2 = 131072;
        private static final int ROOM_2x2 = 262144;
        private static final int ROOM_ORIGIN_FLAG = 1048576;
        private static final int ROOM_DOOR_FLAG = 2097152;
        private static final int ROOM_STAIRS_FLAG = 4194304;
        private static final int ROOM_CORRIDOR_FLAG = 8388608;
        private static final int ROOM_TYPE_MASK = 983040;
        private static final int ROOM_ID_MASK = 65535;
        private final Random random;
        final WorldGenWoodlandMansionPieces.SimpleGrid baseGrid;
        final WorldGenWoodlandMansionPieces.SimpleGrid thirdFloorGrid;
        final WorldGenWoodlandMansionPieces.SimpleGrid[] floorRooms;
        final int entranceX;
        final int entranceY;

        public MansionGrid(Random random) {
            this.random = random;
            int i = 11;
            this.entranceX = 7;
            this.entranceY = 4;
            this.baseGrid = new WorldGenWoodlandMansionPieces.SimpleGrid(11, 11, 5);
            this.baseGrid.set(this.entranceX, this.entranceY, this.entranceX + 1, this.entranceY + 1, 3);
            this.baseGrid.set(this.entranceX - 1, this.entranceY, this.entranceX - 1, this.entranceY + 1, 2);
            this.baseGrid.set(this.entranceX + 2, this.entranceY - 2, this.entranceX + 3, this.entranceY + 3, 5);
            this.baseGrid.set(this.entranceX + 1, this.entranceY - 2, this.entranceX + 1, this.entranceY - 1, 1);
            this.baseGrid.set(this.entranceX + 1, this.entranceY + 2, this.entranceX + 1, this.entranceY + 3, 1);
            this.baseGrid.set(this.entranceX - 1, this.entranceY - 1, 1);
            this.baseGrid.set(this.entranceX - 1, this.entranceY + 2, 1);
            this.baseGrid.set(0, 0, 11, 1, 5);
            this.baseGrid.set(0, 9, 11, 11, 5);
            this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY - 2, EnumDirection.WEST, 6);
            this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY + 3, EnumDirection.WEST, 6);
            this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY - 1, EnumDirection.WEST, 3);
            this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY + 2, EnumDirection.WEST, 3);

            while(this.cleanEdges(this.baseGrid)) {
            }

            this.floorRooms = new WorldGenWoodlandMansionPieces.SimpleGrid[3];
            this.floorRooms[0] = new WorldGenWoodlandMansionPieces.SimpleGrid(11, 11, 5);
            this.floorRooms[1] = new WorldGenWoodlandMansionPieces.SimpleGrid(11, 11, 5);
            this.floorRooms[2] = new WorldGenWoodlandMansionPieces.SimpleGrid(11, 11, 5);
            this.identifyRooms(this.baseGrid, this.floorRooms[0]);
            this.identifyRooms(this.baseGrid, this.floorRooms[1]);
            this.floorRooms[0].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
            this.floorRooms[1].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
            this.thirdFloorGrid = new WorldGenWoodlandMansionPieces.SimpleGrid(this.baseGrid.width, this.baseGrid.height, 5);
            this.setupThirdFloor();
            this.identifyRooms(this.thirdFloorGrid, this.floorRooms[2]);
        }

        public static boolean isHouse(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, int i, int j) {
            int k = simpleGrid.get(i, j);
            return k == 1 || k == 2 || k == 3 || k == 4;
        }

        public boolean isRoomId(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, int i, int j, int k, int l) {
            return (this.floorRooms[k].get(i, j) & '\uffff') == l;
        }

        @Nullable
        public EnumDirection get1x2RoomDirection(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, int i, int j, int k, int l) {
            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                if (this.isRoomId(simpleGrid, i + direction.getAdjacentX(), j + direction.getAdjacentZ(), k, l)) {
                    return direction;
                }
            }

            return null;
        }

        private void recursiveCorridor(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, int i, int j, EnumDirection direction, int k) {
            if (k > 0) {
                simpleGrid.set(i, j, 1);
                simpleGrid.setif(i + direction.getAdjacentX(), j + direction.getAdjacentZ(), 0, 1);

                for(int l = 0; l < 8; ++l) {
                    EnumDirection direction2 = EnumDirection.fromType2(this.random.nextInt(4));
                    if (direction2 != direction.opposite() && (direction2 != EnumDirection.EAST || !this.random.nextBoolean())) {
                        int m = i + direction.getAdjacentX();
                        int n = j + direction.getAdjacentZ();
                        if (simpleGrid.get(m + direction2.getAdjacentX(), n + direction2.getAdjacentZ()) == 0 && simpleGrid.get(m + direction2.getAdjacentX() * 2, n + direction2.getAdjacentZ() * 2) == 0) {
                            this.recursiveCorridor(simpleGrid, i + direction.getAdjacentX() + direction2.getAdjacentX(), j + direction.getAdjacentZ() + direction2.getAdjacentZ(), direction2, k - 1);
                            break;
                        }
                    }
                }

                EnumDirection direction3 = direction.getClockWise();
                EnumDirection direction4 = direction.getCounterClockWise();
                simpleGrid.setif(i + direction3.getAdjacentX(), j + direction3.getAdjacentZ(), 0, 2);
                simpleGrid.setif(i + direction4.getAdjacentX(), j + direction4.getAdjacentZ(), 0, 2);
                simpleGrid.setif(i + direction.getAdjacentX() + direction3.getAdjacentX(), j + direction.getAdjacentZ() + direction3.getAdjacentZ(), 0, 2);
                simpleGrid.setif(i + direction.getAdjacentX() + direction4.getAdjacentX(), j + direction.getAdjacentZ() + direction4.getAdjacentZ(), 0, 2);
                simpleGrid.setif(i + direction.getAdjacentX() * 2, j + direction.getAdjacentZ() * 2, 0, 2);
                simpleGrid.setif(i + direction3.getAdjacentX() * 2, j + direction3.getAdjacentZ() * 2, 0, 2);
                simpleGrid.setif(i + direction4.getAdjacentX() * 2, j + direction4.getAdjacentZ() * 2, 0, 2);
            }
        }

        private boolean cleanEdges(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid) {
            boolean bl = false;

            for(int i = 0; i < simpleGrid.height; ++i) {
                for(int j = 0; j < simpleGrid.width; ++j) {
                    if (simpleGrid.get(j, i) == 0) {
                        int k = 0;
                        k = k + (isHouse(simpleGrid, j + 1, i) ? 1 : 0);
                        k = k + (isHouse(simpleGrid, j - 1, i) ? 1 : 0);
                        k = k + (isHouse(simpleGrid, j, i + 1) ? 1 : 0);
                        k = k + (isHouse(simpleGrid, j, i - 1) ? 1 : 0);
                        if (k >= 3) {
                            simpleGrid.set(j, i, 2);
                            bl = true;
                        } else if (k == 2) {
                            int l = 0;
                            l = l + (isHouse(simpleGrid, j + 1, i + 1) ? 1 : 0);
                            l = l + (isHouse(simpleGrid, j - 1, i + 1) ? 1 : 0);
                            l = l + (isHouse(simpleGrid, j + 1, i - 1) ? 1 : 0);
                            l = l + (isHouse(simpleGrid, j - 1, i - 1) ? 1 : 0);
                            if (l <= 1) {
                                simpleGrid.set(j, i, 2);
                                bl = true;
                            }
                        }
                    }
                }
            }

            return bl;
        }

        private void setupThirdFloor() {
            List<Tuple<Integer, Integer>> list = Lists.newArrayList();
            WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid = this.floorRooms[1];

            for(int i = 0; i < this.thirdFloorGrid.height; ++i) {
                for(int j = 0; j < this.thirdFloorGrid.width; ++j) {
                    int k = simpleGrid.get(j, i);
                    int l = k & 983040;
                    if (l == 131072 && (k & 2097152) == 2097152) {
                        list.add(new Tuple<>(j, i));
                    }
                }
            }

            if (list.isEmpty()) {
                this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
            } else {
                Tuple<Integer, Integer> tuple = list.get(this.random.nextInt(list.size()));
                int m = simpleGrid.get(tuple.getA(), tuple.getB());
                simpleGrid.set(tuple.getA(), tuple.getB(), m | 4194304);
                EnumDirection direction = this.get1x2RoomDirection(this.baseGrid, tuple.getA(), tuple.getB(), 1, m & '\uffff');
                int n = tuple.getA() + direction.getAdjacentX();
                int o = tuple.getB() + direction.getAdjacentZ();

                for(int p = 0; p < this.thirdFloorGrid.height; ++p) {
                    for(int q = 0; q < this.thirdFloorGrid.width; ++q) {
                        if (!isHouse(this.baseGrid, q, p)) {
                            this.thirdFloorGrid.set(q, p, 5);
                        } else if (q == tuple.getA() && p == tuple.getB()) {
                            this.thirdFloorGrid.set(q, p, 3);
                        } else if (q == n && p == o) {
                            this.thirdFloorGrid.set(q, p, 3);
                            this.floorRooms[2].set(q, p, 8388608);
                        }
                    }
                }

                List<EnumDirection> list2 = Lists.newArrayList();

                for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                    if (this.thirdFloorGrid.get(n + direction2.getAdjacentX(), o + direction2.getAdjacentZ()) == 0) {
                        list2.add(direction2);
                    }
                }

                if (list2.isEmpty()) {
                    this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
                    simpleGrid.set(tuple.getA(), tuple.getB(), m);
                } else {
                    EnumDirection direction3 = list2.get(this.random.nextInt(list2.size()));
                    this.recursiveCorridor(this.thirdFloorGrid, n + direction3.getAdjacentX(), o + direction3.getAdjacentZ(), direction3, 4);

                    while(this.cleanEdges(this.thirdFloorGrid)) {
                    }

                }
            }
        }

        private void identifyRooms(WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid2) {
            List<Tuple<Integer, Integer>> list = Lists.newArrayList();

            for(int i = 0; i < simpleGrid.height; ++i) {
                for(int j = 0; j < simpleGrid.width; ++j) {
                    if (simpleGrid.get(j, i) == 2) {
                        list.add(new Tuple<>(j, i));
                    }
                }
            }

            Collections.shuffle(list, this.random);
            int k = 10;

            for(Tuple<Integer, Integer> tuple : list) {
                int l = tuple.getA();
                int m = tuple.getB();
                if (simpleGrid2.get(l, m) == 0) {
                    int n = l;
                    int o = l;
                    int p = m;
                    int q = m;
                    int r = 65536;
                    if (simpleGrid2.get(l + 1, m) == 0 && simpleGrid2.get(l, m + 1) == 0 && simpleGrid2.get(l + 1, m + 1) == 0 && simpleGrid.get(l + 1, m) == 2 && simpleGrid.get(l, m + 1) == 2 && simpleGrid.get(l + 1, m + 1) == 2) {
                        o = l + 1;
                        q = m + 1;
                        r = 262144;
                    } else if (simpleGrid2.get(l - 1, m) == 0 && simpleGrid2.get(l, m + 1) == 0 && simpleGrid2.get(l - 1, m + 1) == 0 && simpleGrid.get(l - 1, m) == 2 && simpleGrid.get(l, m + 1) == 2 && simpleGrid.get(l - 1, m + 1) == 2) {
                        n = l - 1;
                        q = m + 1;
                        r = 262144;
                    } else if (simpleGrid2.get(l - 1, m) == 0 && simpleGrid2.get(l, m - 1) == 0 && simpleGrid2.get(l - 1, m - 1) == 0 && simpleGrid.get(l - 1, m) == 2 && simpleGrid.get(l, m - 1) == 2 && simpleGrid.get(l - 1, m - 1) == 2) {
                        n = l - 1;
                        p = m - 1;
                        r = 262144;
                    } else if (simpleGrid2.get(l + 1, m) == 0 && simpleGrid.get(l + 1, m) == 2) {
                        o = l + 1;
                        r = 131072;
                    } else if (simpleGrid2.get(l, m + 1) == 0 && simpleGrid.get(l, m + 1) == 2) {
                        q = m + 1;
                        r = 131072;
                    } else if (simpleGrid2.get(l - 1, m) == 0 && simpleGrid.get(l - 1, m) == 2) {
                        n = l - 1;
                        r = 131072;
                    } else if (simpleGrid2.get(l, m - 1) == 0 && simpleGrid.get(l, m - 1) == 2) {
                        p = m - 1;
                        r = 131072;
                    }

                    int s = this.random.nextBoolean() ? n : o;
                    int t = this.random.nextBoolean() ? p : q;
                    int u = 2097152;
                    if (!simpleGrid.edgesTo(s, t, 1)) {
                        s = s == n ? o : n;
                        t = t == p ? q : p;
                        if (!simpleGrid.edgesTo(s, t, 1)) {
                            t = t == p ? q : p;
                            if (!simpleGrid.edgesTo(s, t, 1)) {
                                s = s == n ? o : n;
                                t = t == p ? q : p;
                                if (!simpleGrid.edgesTo(s, t, 1)) {
                                    u = 0;
                                    s = n;
                                    t = p;
                                }
                            }
                        }
                    }

                    for(int v = p; v <= q; ++v) {
                        for(int w = n; w <= o; ++w) {
                            if (w == s && v == t) {
                                simpleGrid2.set(w, v, 1048576 | u | r | k);
                            } else {
                                simpleGrid2.set(w, v, r | k);
                            }
                        }
                    }

                    ++k;
                }
            }

        }

        public void print() {
            for(int i = 0; i < 2; ++i) {
                WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid = i == 0 ? this.baseGrid : this.thirdFloorGrid;

                for(int j = 0; j < simpleGrid.height; ++j) {
                    for(int k = 0; k < simpleGrid.width; ++k) {
                        int l = simpleGrid.get(k, j);
                        if (l == 1) {
                            System.out.print("+");
                        } else if (l == 4) {
                            System.out.print("x");
                        } else if (l == 2) {
                            System.out.print("X");
                        } else if (l == 3) {
                            System.out.print("O");
                        } else if (l == 5) {
                            System.out.print("#");
                        } else {
                            System.out.print(" ");
                        }
                    }

                    System.out.println("");
                }

                System.out.println("");
            }

        }
    }

    static class MansionPiecePlacer {
        private final DefinedStructureManager structureManager;
        private final Random random;
        private int startX;
        private int startY;

        public MansionPiecePlacer(DefinedStructureManager manager, Random random) {
            this.structureManager = manager;
            this.random = random;
        }

        public void createMansion(BlockPosition pos, EnumBlockRotation rotation, List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, WorldGenWoodlandMansionPieces.MansionGrid parameters) {
            WorldGenWoodlandMansionPieces.PlacementData placementData = new WorldGenWoodlandMansionPieces.PlacementData();
            placementData.position = pos;
            placementData.rotation = rotation;
            placementData.wallType = "wall_flat";
            WorldGenWoodlandMansionPieces.PlacementData placementData2 = new WorldGenWoodlandMansionPieces.PlacementData();
            this.entrance(pieces, placementData);
            placementData2.position = placementData.position.above(8);
            placementData2.rotation = placementData.rotation;
            placementData2.wallType = "wall_window";
            if (!pieces.isEmpty()) {
            }

            WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid = parameters.baseGrid;
            WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid2 = parameters.thirdFloorGrid;
            this.startX = parameters.entranceX + 1;
            this.startY = parameters.entranceY + 1;
            int i = parameters.entranceX + 1;
            int j = parameters.entranceY;
            this.traverseOuterWalls(pieces, placementData, simpleGrid, EnumDirection.SOUTH, this.startX, this.startY, i, j);
            this.traverseOuterWalls(pieces, placementData2, simpleGrid, EnumDirection.SOUTH, this.startX, this.startY, i, j);
            WorldGenWoodlandMansionPieces.PlacementData placementData3 = new WorldGenWoodlandMansionPieces.PlacementData();
            placementData3.position = placementData.position.above(19);
            placementData3.rotation = placementData.rotation;
            placementData3.wallType = "wall_window";
            boolean bl = false;

            for(int k = 0; k < simpleGrid2.height && !bl; ++k) {
                for(int l = simpleGrid2.width - 1; l >= 0 && !bl; --l) {
                    if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid2, l, k)) {
                        placementData3.position = placementData3.position.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (k - this.startY) * 8);
                        placementData3.position = placementData3.position.relative(rotation.rotate(EnumDirection.EAST), (l - this.startX) * 8);
                        this.traverseWallPiece(pieces, placementData3);
                        this.traverseOuterWalls(pieces, placementData3, simpleGrid2, EnumDirection.SOUTH, l, k, l, k);
                        bl = true;
                    }
                }
            }

            this.createRoof(pieces, pos.above(16), rotation, simpleGrid, simpleGrid2);
            this.createRoof(pieces, pos.above(27), rotation, simpleGrid2, (WorldGenWoodlandMansionPieces.SimpleGrid)null);
            if (!pieces.isEmpty()) {
            }

            WorldGenWoodlandMansionPieces.FloorRoomCollection[] floorRoomCollections = new WorldGenWoodlandMansionPieces.FloorRoomCollection[]{new WorldGenWoodlandMansionPieces.FirstFloorRoomCollection(), new WorldGenWoodlandMansionPieces.SecondFloorRoomCollection(), new WorldGenWoodlandMansionPieces.ThirdFloorRoomCollection()};

            for(int m = 0; m < 3; ++m) {
                BlockPosition blockPos = pos.above(8 * m + (m == 2 ? 3 : 0));
                WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid3 = parameters.floorRooms[m];
                WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid4 = m == 2 ? simpleGrid2 : simpleGrid;
                String string = m == 0 ? "carpet_south_1" : "carpet_south_2";
                String string2 = m == 0 ? "carpet_west_1" : "carpet_west_2";

                for(int n = 0; n < simpleGrid4.height; ++n) {
                    for(int o = 0; o < simpleGrid4.width; ++o) {
                        if (simpleGrid4.get(o, n) == 1) {
                            BlockPosition blockPos2 = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (n - this.startY) * 8);
                            blockPos2 = blockPos2.relative(rotation.rotate(EnumDirection.EAST), (o - this.startX) * 8);
                            pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "corridor_floor", blockPos2, rotation));
                            if (simpleGrid4.get(o, n - 1) == 1 || (simpleGrid3.get(o, n - 1) & 8388608) == 8388608) {
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "carpet_north", blockPos2.relative(rotation.rotate(EnumDirection.EAST), 1).above(), rotation));
                            }

                            if (simpleGrid4.get(o + 1, n) == 1 || (simpleGrid3.get(o + 1, n) & 8388608) == 8388608) {
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "carpet_east", blockPos2.relative(rotation.rotate(EnumDirection.SOUTH), 1).relative(rotation.rotate(EnumDirection.EAST), 5).above(), rotation));
                            }

                            if (simpleGrid4.get(o, n + 1) == 1 || (simpleGrid3.get(o, n + 1) & 8388608) == 8388608) {
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, string, blockPos2.relative(rotation.rotate(EnumDirection.SOUTH), 5).relative(rotation.rotate(EnumDirection.WEST), 1), rotation));
                            }

                            if (simpleGrid4.get(o - 1, n) == 1 || (simpleGrid3.get(o - 1, n) & 8388608) == 8388608) {
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, string2, blockPos2.relative(rotation.rotate(EnumDirection.WEST), 1).relative(rotation.rotate(EnumDirection.NORTH), 1), rotation));
                            }
                        }
                    }
                }

                String string3 = m == 0 ? "indoors_wall_1" : "indoors_wall_2";
                String string4 = m == 0 ? "indoors_door_1" : "indoors_door_2";
                List<EnumDirection> list = Lists.newArrayList();

                for(int p = 0; p < simpleGrid4.height; ++p) {
                    for(int q = 0; q < simpleGrid4.width; ++q) {
                        boolean bl2 = m == 2 && simpleGrid4.get(q, p) == 3;
                        if (simpleGrid4.get(q, p) == 2 || bl2) {
                            int r = simpleGrid3.get(q, p);
                            int s = r & 983040;
                            int t = r & '\uffff';
                            bl2 = bl2 && (r & 8388608) == 8388608;
                            list.clear();
                            if ((r & 2097152) == 2097152) {
                                for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                                    if (simpleGrid4.get(q + direction.getAdjacentX(), p + direction.getAdjacentZ()) == 1) {
                                        list.add(direction);
                                    }
                                }
                            }

                            EnumDirection direction2 = null;
                            if (!list.isEmpty()) {
                                direction2 = list.get(this.random.nextInt(list.size()));
                            } else if ((r & 1048576) == 1048576) {
                                direction2 = EnumDirection.UP;
                            }

                            BlockPosition blockPos3 = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (p - this.startY) * 8);
                            blockPos3 = blockPos3.relative(rotation.rotate(EnumDirection.EAST), -1 + (q - this.startX) * 8);
                            if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid4, q - 1, p) && !parameters.isRoomId(simpleGrid4, q - 1, p, m, t)) {
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, direction2 == EnumDirection.WEST ? string4 : string3, blockPos3, rotation));
                            }

                            if (simpleGrid4.get(q + 1, p) == 1 && !bl2) {
                                BlockPosition blockPos4 = blockPos3.relative(rotation.rotate(EnumDirection.EAST), 8);
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, direction2 == EnumDirection.EAST ? string4 : string3, blockPos4, rotation));
                            }

                            if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid4, q, p + 1) && !parameters.isRoomId(simpleGrid4, q, p + 1, m, t)) {
                                BlockPosition blockPos5 = blockPos3.relative(rotation.rotate(EnumDirection.SOUTH), 7);
                                blockPos5 = blockPos5.relative(rotation.rotate(EnumDirection.EAST), 7);
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, direction2 == EnumDirection.SOUTH ? string4 : string3, blockPos5, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                            }

                            if (simpleGrid4.get(q, p - 1) == 1 && !bl2) {
                                BlockPosition blockPos6 = blockPos3.relative(rotation.rotate(EnumDirection.NORTH), 1);
                                blockPos6 = blockPos6.relative(rotation.rotate(EnumDirection.EAST), 7);
                                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, direction2 == EnumDirection.NORTH ? string4 : string3, blockPos6, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                            }

                            if (s == 65536) {
                                this.addRoom1x1(pieces, blockPos3, rotation, direction2, floorRoomCollections[m]);
                            } else if (s == 131072 && direction2 != null) {
                                EnumDirection direction3 = parameters.get1x2RoomDirection(simpleGrid4, q, p, m, t);
                                boolean bl3 = (r & 4194304) == 4194304;
                                this.addRoom1x2(pieces, blockPos3, rotation, direction3, direction2, floorRoomCollections[m], bl3);
                            } else if (s == 262144 && direction2 != null && direction2 != EnumDirection.UP) {
                                EnumDirection direction4 = direction2.getClockWise();
                                if (!parameters.isRoomId(simpleGrid4, q + direction4.getAdjacentX(), p + direction4.getAdjacentZ(), m, t)) {
                                    direction4 = direction4.opposite();
                                }

                                this.addRoom2x2(pieces, blockPos3, rotation, direction4, direction2, floorRoomCollections[m]);
                            } else if (s == 262144 && direction2 == EnumDirection.UP) {
                                this.addRoom2x2Secret(pieces, blockPos3, rotation, floorRoomCollections[m]);
                            }
                        }
                    }
                }
            }

        }

        private void traverseOuterWalls(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, WorldGenWoodlandMansionPieces.PlacementData placementData, WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, EnumDirection direction, int i, int j, int k, int l) {
            int m = i;
            int n = j;
            EnumDirection direction2 = direction;

            do {
                if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, m + direction.getAdjacentX(), n + direction.getAdjacentZ())) {
                    this.traverseTurn(pieces, placementData);
                    direction = direction.getClockWise();
                    if (m != k || n != l || direction2 != direction) {
                        this.traverseWallPiece(pieces, placementData);
                    }
                } else if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, m + direction.getAdjacentX(), n + direction.getAdjacentZ()) && WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, m + direction.getAdjacentX() + direction.getCounterClockWise().getAdjacentX(), n + direction.getAdjacentZ() + direction.getCounterClockWise().getAdjacentZ())) {
                    this.traverseInnerTurn(pieces, placementData);
                    m += direction.getAdjacentX();
                    n += direction.getAdjacentZ();
                    direction = direction.getCounterClockWise();
                } else {
                    m += direction.getAdjacentX();
                    n += direction.getAdjacentZ();
                    if (m != k || n != l || direction2 != direction) {
                        this.traverseWallPiece(pieces, placementData);
                    }
                }
            } while(m != k || n != l || direction2 != direction);

        }

        private void createRoof(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list, BlockPosition blockPos, EnumBlockRotation rotation, WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid, @Nullable WorldGenWoodlandMansionPieces.SimpleGrid simpleGrid2) {
            for(int i = 0; i < simpleGrid.height; ++i) {
                for(int j = 0; j < simpleGrid.width; ++j) {
                    BlockPosition blockPos16 = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (i - this.startY) * 8);
                    blockPos16 = blockPos16.relative(rotation.rotate(EnumDirection.EAST), (j - this.startX) * 8);
                    boolean bl = simpleGrid2 != null && WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid2, j, i);
                    if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, j, i) && !bl) {
                        list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof", blockPos16.above(3), rotation));
                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, j + 1, i)) {
                            BlockPosition blockPos3 = blockPos16.relative(rotation.rotate(EnumDirection.EAST), 6);
                            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_front", blockPos3, rotation));
                        }

                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, j - 1, i)) {
                            BlockPosition blockPos4 = blockPos16.relative(rotation.rotate(EnumDirection.EAST), 0);
                            blockPos4 = blockPos4.relative(rotation.rotate(EnumDirection.SOUTH), 7);
                            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_front", blockPos4, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
                        }

                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, j, i - 1)) {
                            BlockPosition blockPos5 = blockPos16.relative(rotation.rotate(EnumDirection.WEST), 1);
                            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_front", blockPos5, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
                        }

                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, j, i + 1)) {
                            BlockPosition blockPos6 = blockPos16.relative(rotation.rotate(EnumDirection.EAST), 6);
                            blockPos6 = blockPos6.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_front", blockPos6, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                        }
                    }
                }
            }

            if (simpleGrid2 != null) {
                for(int k = 0; k < simpleGrid.height; ++k) {
                    for(int l = 0; l < simpleGrid.width; ++l) {
                        BlockPosition var17 = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (k - this.startY) * 8);
                        var17 = var17.relative(rotation.rotate(EnumDirection.EAST), (l - this.startX) * 8);
                        boolean bl2 = WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid2, l, k);
                        if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k) && bl2) {
                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l + 1, k)) {
                                BlockPosition blockPos8 = var17.relative(rotation.rotate(EnumDirection.EAST), 7);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall", blockPos8, rotation));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l - 1, k)) {
                                BlockPosition blockPos9 = var17.relative(rotation.rotate(EnumDirection.WEST), 1);
                                blockPos9 = blockPos9.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall", blockPos9, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k - 1)) {
                                BlockPosition blockPos10 = var17.relative(rotation.rotate(EnumDirection.WEST), 0);
                                blockPos10 = blockPos10.relative(rotation.rotate(EnumDirection.NORTH), 1);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall", blockPos10, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k + 1)) {
                                BlockPosition blockPos11 = var17.relative(rotation.rotate(EnumDirection.EAST), 6);
                                blockPos11 = blockPos11.relative(rotation.rotate(EnumDirection.SOUTH), 7);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall", blockPos11, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l + 1, k)) {
                                if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k - 1)) {
                                    BlockPosition blockPos12 = var17.relative(rotation.rotate(EnumDirection.EAST), 7);
                                    blockPos12 = blockPos12.relative(rotation.rotate(EnumDirection.NORTH), 2);
                                    list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall_corner", blockPos12, rotation));
                                }

                                if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k + 1)) {
                                    BlockPosition blockPos13 = var17.relative(rotation.rotate(EnumDirection.EAST), 8);
                                    blockPos13 = blockPos13.relative(rotation.rotate(EnumDirection.SOUTH), 7);
                                    list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall_corner", blockPos13, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                                }
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l - 1, k)) {
                                if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k - 1)) {
                                    BlockPosition blockPos14 = var17.relative(rotation.rotate(EnumDirection.WEST), 2);
                                    blockPos14 = blockPos14.relative(rotation.rotate(EnumDirection.NORTH), 1);
                                    list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall_corner", blockPos14, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
                                }

                                if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, l, k + 1)) {
                                    BlockPosition blockPos15 = var17.relative(rotation.rotate(EnumDirection.WEST), 1);
                                    blockPos15 = blockPos15.relative(rotation.rotate(EnumDirection.SOUTH), 8);
                                    list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "small_wall_corner", blockPos15, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
                                }
                            }
                        }
                    }
                }
            }

            for(int m = 0; m < simpleGrid.height; ++m) {
                for(int n = 0; n < simpleGrid.width; ++n) {
                    BlockPosition var19 = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), 8 + (m - this.startY) * 8);
                    var19 = var19.relative(rotation.rotate(EnumDirection.EAST), (n - this.startX) * 8);
                    boolean bl3 = simpleGrid2 != null && WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid2, n, m);
                    if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n, m) && !bl3) {
                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n + 1, m)) {
                            BlockPosition blockPos17 = var19.relative(rotation.rotate(EnumDirection.EAST), 6);
                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n, m + 1)) {
                                BlockPosition blockPos18 = blockPos17.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_corner", blockPos18, rotation));
                            } else if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n + 1, m + 1)) {
                                BlockPosition blockPos19 = blockPos17.relative(rotation.rotate(EnumDirection.SOUTH), 5);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_inner_corner", blockPos19, rotation));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n, m - 1)) {
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_corner", blockPos17, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
                            } else if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n + 1, m - 1)) {
                                BlockPosition blockPos20 = var19.relative(rotation.rotate(EnumDirection.EAST), 9);
                                blockPos20 = blockPos20.relative(rotation.rotate(EnumDirection.NORTH), 2);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_inner_corner", blockPos20, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                            }
                        }

                        if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n - 1, m)) {
                            BlockPosition blockPos21 = var19.relative(rotation.rotate(EnumDirection.EAST), 0);
                            blockPos21 = blockPos21.relative(rotation.rotate(EnumDirection.SOUTH), 0);
                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n, m + 1)) {
                                BlockPosition blockPos22 = blockPos21.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_corner", blockPos22, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
                            } else if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n - 1, m + 1)) {
                                BlockPosition blockPos23 = blockPos21.relative(rotation.rotate(EnumDirection.SOUTH), 8);
                                blockPos23 = blockPos23.relative(rotation.rotate(EnumDirection.WEST), 3);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_inner_corner", blockPos23, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
                            }

                            if (!WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n, m - 1)) {
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_corner", blockPos21, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
                            } else if (WorldGenWoodlandMansionPieces.MansionGrid.isHouse(simpleGrid, n - 1, m - 1)) {
                                BlockPosition blockPos24 = blockPos21.relative(rotation.rotate(EnumDirection.SOUTH), 1);
                                list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "roof_inner_corner", blockPos24, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
                            }
                        }
                    }
                }
            }

        }

        private void entrance(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, WorldGenWoodlandMansionPieces.PlacementData placementData) {
            EnumDirection direction = placementData.rotation.rotate(EnumDirection.WEST);
            pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "entrance", placementData.position.relative(direction, 9), placementData.rotation));
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.SOUTH), 16);
        }

        private void traverseWallPiece(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list, WorldGenWoodlandMansionPieces.PlacementData placementData) {
            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, placementData.wallType, placementData.position.relative(placementData.rotation.rotate(EnumDirection.EAST), 7), placementData.rotation));
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.SOUTH), 8);
        }

        private void traverseTurn(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list, WorldGenWoodlandMansionPieces.PlacementData placementData) {
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.SOUTH), -1);
            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, "wall_corner", placementData.position, placementData.rotation));
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.SOUTH), -7);
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.WEST), -6);
            placementData.rotation = placementData.rotation.getRotated(EnumBlockRotation.CLOCKWISE_90);
        }

        private void traverseInnerTurn(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list, WorldGenWoodlandMansionPieces.PlacementData placementData) {
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.SOUTH), 6);
            placementData.position = placementData.position.relative(placementData.rotation.rotate(EnumDirection.EAST), 8);
            placementData.rotation = placementData.rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90);
        }

        private void addRoom1x1(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, BlockPosition pos, EnumBlockRotation rotation, EnumDirection direction, WorldGenWoodlandMansionPieces.FloorRoomCollection floorRoomCollection) {
            EnumBlockRotation rotation2 = EnumBlockRotation.NONE;
            String string = floorRoomCollection.get1x1(this.random);
            if (direction != EnumDirection.EAST) {
                if (direction == EnumDirection.NORTH) {
                    rotation2 = rotation2.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90);
                } else if (direction == EnumDirection.WEST) {
                    rotation2 = rotation2.getRotated(EnumBlockRotation.CLOCKWISE_180);
                } else if (direction == EnumDirection.SOUTH) {
                    rotation2 = rotation2.getRotated(EnumBlockRotation.CLOCKWISE_90);
                } else {
                    string = floorRoomCollection.get1x1Secret(this.random);
                }
            }

            BlockPosition blockPos = DefinedStructure.getZeroPositionWithTransform(new BlockPosition(1, 0, 0), EnumBlockMirror.NONE, rotation2, 7, 7);
            rotation2 = rotation2.getRotated(rotation);
            blockPos = blockPos.rotate(rotation);
            BlockPosition blockPos2 = pos.offset(blockPos.getX(), 0, blockPos.getZ());
            pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, string, blockPos2, rotation2));
        }

        private void addRoom1x2(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, BlockPosition pos, EnumBlockRotation rotation, EnumDirection direction, EnumDirection direction2, WorldGenWoodlandMansionPieces.FloorRoomCollection floorRoomCollection, boolean staircase) {
            if (direction2 == EnumDirection.EAST && direction == EnumDirection.SOUTH) {
                BlockPosition blockPos = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos, rotation));
            } else if (direction2 == EnumDirection.EAST && direction == EnumDirection.NORTH) {
                BlockPosition blockPos2 = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                blockPos2 = blockPos2.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos2, rotation, EnumBlockMirror.LEFT_RIGHT));
            } else if (direction2 == EnumDirection.WEST && direction == EnumDirection.NORTH) {
                BlockPosition blockPos3 = pos.relative(rotation.rotate(EnumDirection.EAST), 7);
                blockPos3 = blockPos3.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos3, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
            } else if (direction2 == EnumDirection.WEST && direction == EnumDirection.SOUTH) {
                BlockPosition blockPos4 = pos.relative(rotation.rotate(EnumDirection.EAST), 7);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos4, rotation, EnumBlockMirror.FRONT_BACK));
            } else if (direction2 == EnumDirection.SOUTH && direction == EnumDirection.EAST) {
                BlockPosition blockPos5 = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos5, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90), EnumBlockMirror.LEFT_RIGHT));
            } else if (direction2 == EnumDirection.SOUTH && direction == EnumDirection.WEST) {
                BlockPosition blockPos6 = pos.relative(rotation.rotate(EnumDirection.EAST), 7);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos6, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
            } else if (direction2 == EnumDirection.NORTH && direction == EnumDirection.WEST) {
                BlockPosition blockPos7 = pos.relative(rotation.rotate(EnumDirection.EAST), 7);
                blockPos7 = blockPos7.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos7, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90), EnumBlockMirror.FRONT_BACK));
            } else if (direction2 == EnumDirection.NORTH && direction == EnumDirection.EAST) {
                BlockPosition blockPos8 = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                blockPos8 = blockPos8.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2SideEntrance(this.random, staircase), blockPos8, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
            } else if (direction2 == EnumDirection.SOUTH && direction == EnumDirection.NORTH) {
                BlockPosition blockPos9 = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                blockPos9 = blockPos9.relative(rotation.rotate(EnumDirection.NORTH), 8);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2FrontEntrance(this.random, staircase), blockPos9, rotation));
            } else if (direction2 == EnumDirection.NORTH && direction == EnumDirection.SOUTH) {
                BlockPosition blockPos10 = pos.relative(rotation.rotate(EnumDirection.EAST), 7);
                blockPos10 = blockPos10.relative(rotation.rotate(EnumDirection.SOUTH), 14);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2FrontEntrance(this.random, staircase), blockPos10, rotation.getRotated(EnumBlockRotation.CLOCKWISE_180)));
            } else if (direction2 == EnumDirection.WEST && direction == EnumDirection.EAST) {
                BlockPosition blockPos11 = pos.relative(rotation.rotate(EnumDirection.EAST), 15);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2FrontEntrance(this.random, staircase), blockPos11, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
            } else if (direction2 == EnumDirection.EAST && direction == EnumDirection.WEST) {
                BlockPosition blockPos12 = pos.relative(rotation.rotate(EnumDirection.WEST), 7);
                blockPos12 = blockPos12.relative(rotation.rotate(EnumDirection.SOUTH), 6);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2FrontEntrance(this.random, staircase), blockPos12, rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90)));
            } else if (direction2 == EnumDirection.UP && direction == EnumDirection.EAST) {
                BlockPosition blockPos13 = pos.relative(rotation.rotate(EnumDirection.EAST), 15);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2Secret(this.random), blockPos13, rotation.getRotated(EnumBlockRotation.CLOCKWISE_90)));
            } else if (direction2 == EnumDirection.UP && direction == EnumDirection.SOUTH) {
                BlockPosition blockPos14 = pos.relative(rotation.rotate(EnumDirection.EAST), 1);
                blockPos14 = blockPos14.relative(rotation.rotate(EnumDirection.NORTH), 0);
                pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get1x2Secret(this.random), blockPos14, rotation));
            }

        }

        private void addRoom2x2(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> pieces, BlockPosition pos, EnumBlockRotation rotation, EnumDirection direction, EnumDirection direction2, WorldGenWoodlandMansionPieces.FloorRoomCollection floorRoomCollection) {
            int i = 0;
            int j = 0;
            EnumBlockRotation rotation2 = rotation;
            EnumBlockMirror mirror = EnumBlockMirror.NONE;
            if (direction2 == EnumDirection.EAST && direction == EnumDirection.SOUTH) {
                i = -7;
            } else if (direction2 == EnumDirection.EAST && direction == EnumDirection.NORTH) {
                i = -7;
                j = 6;
                mirror = EnumBlockMirror.LEFT_RIGHT;
            } else if (direction2 == EnumDirection.NORTH && direction == EnumDirection.EAST) {
                i = 1;
                j = 14;
                rotation2 = rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90);
            } else if (direction2 == EnumDirection.NORTH && direction == EnumDirection.WEST) {
                i = 7;
                j = 14;
                rotation2 = rotation.getRotated(EnumBlockRotation.COUNTERCLOCKWISE_90);
                mirror = EnumBlockMirror.LEFT_RIGHT;
            } else if (direction2 == EnumDirection.SOUTH && direction == EnumDirection.WEST) {
                i = 7;
                j = -8;
                rotation2 = rotation.getRotated(EnumBlockRotation.CLOCKWISE_90);
            } else if (direction2 == EnumDirection.SOUTH && direction == EnumDirection.EAST) {
                i = 1;
                j = -8;
                rotation2 = rotation.getRotated(EnumBlockRotation.CLOCKWISE_90);
                mirror = EnumBlockMirror.LEFT_RIGHT;
            } else if (direction2 == EnumDirection.WEST && direction == EnumDirection.NORTH) {
                i = 15;
                j = 6;
                rotation2 = rotation.getRotated(EnumBlockRotation.CLOCKWISE_180);
            } else if (direction2 == EnumDirection.WEST && direction == EnumDirection.SOUTH) {
                i = 15;
                mirror = EnumBlockMirror.FRONT_BACK;
            }

            BlockPosition blockPos = pos.relative(rotation.rotate(EnumDirection.EAST), i);
            blockPos = blockPos.relative(rotation.rotate(EnumDirection.SOUTH), j);
            pieces.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get2x2(this.random), blockPos, rotation2, mirror));
        }

        private void addRoom2x2Secret(List<WorldGenWoodlandMansionPieces.WoodlandMansionPiece> list, BlockPosition blockPos, EnumBlockRotation rotation, WorldGenWoodlandMansionPieces.FloorRoomCollection floorRoomCollection) {
            BlockPosition blockPos2 = blockPos.relative(rotation.rotate(EnumDirection.EAST), 1);
            list.add(new WorldGenWoodlandMansionPieces.WoodlandMansionPiece(this.structureManager, floorRoomCollection.get2x2Secret(this.random), blockPos2, rotation, EnumBlockMirror.NONE));
        }
    }

    static class PlacementData {
        public EnumBlockRotation rotation;
        public BlockPosition position;
        public String wallType;
    }

    static class SecondFloorRoomCollection extends WorldGenWoodlandMansionPieces.FloorRoomCollection {
        @Override
        public String get1x1(Random random) {
            return "1x1_b" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x1Secret(Random random) {
            return "1x1_as" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2SideEntrance(Random random, boolean staircase) {
            return staircase ? "1x2_c_stairs" : "1x2_c" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2FrontEntrance(Random random, boolean staircase) {
            return staircase ? "1x2_d_stairs" : "1x2_d" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x2Secret(Random random) {
            return "1x2_se" + (random.nextInt(1) + 1);
        }

        @Override
        public String get2x2(Random random) {
            return "2x2_b" + (random.nextInt(5) + 1);
        }

        @Override
        public String get2x2Secret(Random random) {
            return "2x2_s1";
        }
    }

    static class SimpleGrid {
        private final int[][] grid;
        final int width;
        final int height;
        private final int valueIfOutside;

        public SimpleGrid(int n, int m, int fallback) {
            this.width = n;
            this.height = m;
            this.valueIfOutside = fallback;
            this.grid = new int[n][m];
        }

        public void set(int i, int j, int value) {
            if (i >= 0 && i < this.width && j >= 0 && j < this.height) {
                this.grid[i][j] = value;
            }

        }

        public void set(int i0, int j0, int i1, int j1, int value) {
            for(int i = j0; i <= j1; ++i) {
                for(int j = i0; j <= i1; ++j) {
                    this.set(j, i, value);
                }
            }

        }

        public int get(int i, int j) {
            return i >= 0 && i < this.width && j >= 0 && j < this.height ? this.grid[i][j] : this.valueIfOutside;
        }

        public void setif(int i, int j, int expected, int newValue) {
            if (this.get(i, j) == expected) {
                this.set(i, j, newValue);
            }

        }

        public boolean edgesTo(int i, int j, int value) {
            return this.get(i - 1, j) == value || this.get(i + 1, j) == value || this.get(i, j + 1) == value || this.get(i, j - 1) == value;
        }
    }

    static class ThirdFloorRoomCollection extends WorldGenWoodlandMansionPieces.SecondFloorRoomCollection {
    }

    public static class WoodlandMansionPiece extends DefinedStructurePiece {
        public WoodlandMansionPiece(DefinedStructureManager manager, String template, BlockPosition pos, EnumBlockRotation rotation) {
            this(manager, template, pos, rotation, EnumBlockMirror.NONE);
        }

        public WoodlandMansionPiece(DefinedStructureManager manager, String template, BlockPosition pos, EnumBlockRotation rotation, EnumBlockMirror mirror) {
            super(WorldGenFeatureStructurePieceType.WOODLAND_MANSION_PIECE, 0, manager, makeLocation(template), template, makeSettings(mirror, rotation), pos);
        }

        public WoodlandMansionPiece(DefinedStructureManager manager, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.WOODLAND_MANSION_PIECE, nbt, manager, (resourceLocation) -> {
                return makeSettings(EnumBlockMirror.valueOf(nbt.getString("Mi")), EnumBlockRotation.valueOf(nbt.getString("Rot")));
            });
        }

        @Override
        protected MinecraftKey makeTemplateLocation() {
            return makeLocation(this.templateName);
        }

        private static MinecraftKey makeLocation(String identifier) {
            return new MinecraftKey("woodland_mansion/" + identifier);
        }

        private static DefinedStructureInfo makeSettings(EnumBlockMirror mirror, EnumBlockRotation rotation) {
            return (new DefinedStructureInfo()).setIgnoreEntities(true).setRotation(rotation).setMirror(mirror).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
            nbt.setString("Mi", this.placeSettings.getMirror().name());
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
            if (metadata.startsWith("Chest")) {
                EnumBlockRotation rotation = this.placeSettings.getRotation();
                IBlockData blockState = Blocks.CHEST.getBlockData();
                if ("ChestWest".equals(metadata)) {
                    blockState = blockState.set(BlockChest.FACING, rotation.rotate(EnumDirection.WEST));
                } else if ("ChestEast".equals(metadata)) {
                    blockState = blockState.set(BlockChest.FACING, rotation.rotate(EnumDirection.EAST));
                } else if ("ChestSouth".equals(metadata)) {
                    blockState = blockState.set(BlockChest.FACING, rotation.rotate(EnumDirection.SOUTH));
                } else if ("ChestNorth".equals(metadata)) {
                    blockState = blockState.set(BlockChest.FACING, rotation.rotate(EnumDirection.NORTH));
                }

                this.createChest(world, boundingBox, random, pos, LootTables.WOODLAND_MANSION, blockState);
            } else {
                EntityIllagerAbstract abstractIllager;
                switch(metadata) {
                case "Mage":
                    abstractIllager = EntityTypes.EVOKER.create(world.getLevel());
                    break;
                case "Warrior":
                    abstractIllager = EntityTypes.VINDICATOR.create(world.getLevel());
                    break;
                default:
                    return;
                }

                abstractIllager.setPersistent();
                abstractIllager.setPositionRotation(pos, 0.0F, 0.0F);
                abstractIllager.prepare(world, world.getDamageScaler(abstractIllager.getChunkCoordinates()), EnumMobSpawn.STRUCTURE, (GroupDataEntity)null, (NBTTagCompound)null);
                world.addAllEntities(abstractIllager);
                world.setTypeAndData(pos, Blocks.AIR.getBlockData(), 2);
            }

        }
    }
}
