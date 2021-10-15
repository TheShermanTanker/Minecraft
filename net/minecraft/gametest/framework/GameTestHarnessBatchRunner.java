package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameTestHarnessBatchRunner {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPosition firstTestNorthWestCorner;
    final WorldServer level;
    private final GameTestHarnessTicker testTicker;
    private final int testsPerRow;
    private final List<GameTestHarnessInfo> allTestInfos;
    private final List<Pair<GameTestHarnessBatch, Collection<GameTestHarnessInfo>>> batches;
    private final BlockPosition.MutableBlockPosition nextTestNorthWestCorner;

    public GameTestHarnessBatchRunner(Collection<GameTestHarnessBatch> batches, BlockPosition pos, EnumBlockRotation rotation, WorldServer world, GameTestHarnessTicker testManager, int sizeZ) {
        this.nextTestNorthWestCorner = pos.mutable();
        this.firstTestNorthWestCorner = pos;
        this.level = world;
        this.testTicker = testManager;
        this.testsPerRow = sizeZ;
        this.batches = batches.stream().map((batch) -> {
            Collection<GameTestHarnessInfo> collection = batch.getTestFunctions().stream().map((testFunction) -> {
                return new GameTestHarnessInfo(testFunction, rotation, world);
            }).collect(ImmutableList.toImmutableList());
            return Pair.of(batch, collection);
        }).collect(ImmutableList.toImmutableList());
        this.allTestInfos = this.batches.stream().flatMap((batch) -> {
            return batch.getSecond().stream();
        }).collect(ImmutableList.toImmutableList());
    }

    public List<GameTestHarnessInfo> getTestInfos() {
        return this.allTestInfos;
    }

    public void start() {
        this.runBatch(0);
    }

    void runBatch(int index) {
        if (index < this.batches.size()) {
            Pair<GameTestHarnessBatch, Collection<GameTestHarnessInfo>> pair = this.batches.get(index);
            final GameTestHarnessBatch gameTestBatch = pair.getFirst();
            Collection<GameTestHarnessInfo> collection = pair.getSecond();
            Map<GameTestHarnessInfo, BlockPosition> map = this.createStructuresForBatch(collection);
            String string = gameTestBatch.getName();
            LOGGER.info("Running test batch '{}' ({} tests)...", string, collection.size());
            gameTestBatch.runBeforeBatchFunction(this.level);
            final GameTestHarnessCollector multipleTestTracker = new GameTestHarnessCollector();
            collection.forEach(multipleTestTracker::addTestToTrack);
            multipleTestTracker.addListener(new GameTestHarnessListener() {
                private void testCompleted() {
                    if (multipleTestTracker.isDone()) {
                        gameTestBatch.runAfterBatchFunction(GameTestHarnessBatchRunner.this.level);
                        GameTestHarnessBatchRunner.this.runBatch(index + 1);
                    }

                }

                @Override
                public void testStructureLoaded(GameTestHarnessInfo test) {
                }

                @Override
                public void testPassed(GameTestHarnessInfo test) {
                    this.testCompleted();
                }

                @Override
                public void testFailed(GameTestHarnessInfo test) {
                    this.testCompleted();
                }
            });
            collection.forEach((gameTest) -> {
                BlockPosition blockPos = map.get(gameTest);
                GameTestHarnessRunner.runTest(gameTest, blockPos, this.testTicker);
            });
        }
    }

    private Map<GameTestHarnessInfo, BlockPosition> createStructuresForBatch(Collection<GameTestHarnessInfo> gameTests) {
        Map<GameTestHarnessInfo, BlockPosition> map = Maps.newHashMap();
        int i = 0;
        AxisAlignedBB aABB = new AxisAlignedBB(this.nextTestNorthWestCorner);

        for(GameTestHarnessInfo gameTestInfo : gameTests) {
            BlockPosition blockPos = new BlockPosition(this.nextTestNorthWestCorner);
            TileEntityStructure structureBlockEntity = GameTestHarnessStructures.spawnStructure(gameTestInfo.getStructureName(), blockPos, gameTestInfo.getRotation(), 2, this.level, true);
            AxisAlignedBB aABB2 = GameTestHarnessStructures.getStructureBounds(structureBlockEntity);
            gameTestInfo.setStructureBlockPos(structureBlockEntity.getPosition());
            map.put(gameTestInfo, new BlockPosition(this.nextTestNorthWestCorner));
            aABB = aABB.minmax(aABB2);
            this.nextTestNorthWestCorner.move((int)aABB2.getXsize() + 5, 0, 0);
            if (i++ % this.testsPerRow == this.testsPerRow - 1) {
                this.nextTestNorthWestCorner.move(0, 0, (int)aABB.getZsize() + 6);
                this.nextTestNorthWestCorner.setX(this.firstTestNorthWestCorner.getX());
                aABB = new AxisAlignedBB(this.nextTestNorthWestCorner);
            }
        }

        return map;
    }
}
