package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class GameTestHarnessRunner {
    private static final int MAX_TESTS_PER_BATCH = 100;
    public static final int PADDING_AROUND_EACH_STRUCTURE = 2;
    public static final int SPACE_BETWEEN_COLUMNS = 5;
    public static final int SPACE_BETWEEN_ROWS = 6;
    public static final int DEFAULT_TESTS_PER_ROW = 8;

    public static void runTest(GameTestHarnessInfo test, BlockPosition pos, GameTestHarnessTicker testManager) {
        test.startExecution();
        testManager.add(test);
        test.addListener(new ReportGameListener(test, testManager, pos));
        test.spawnStructure(pos, 2);
    }

    public static Collection<GameTestHarnessInfo> runTestBatches(Collection<GameTestHarnessBatch> batches, BlockPosition pos, EnumBlockRotation rotation, WorldServer world, GameTestHarnessTicker testManager, int sizeZ) {
        GameTestHarnessBatchRunner gameTestBatchRunner = new GameTestHarnessBatchRunner(batches, pos, rotation, world, testManager, sizeZ);
        gameTestBatchRunner.start();
        return gameTestBatchRunner.getTestInfos();
    }

    public static Collection<GameTestHarnessInfo> runTests(Collection<GameTestHarnessTestFunction> testFunctions, BlockPosition pos, EnumBlockRotation rotation, WorldServer world, GameTestHarnessTicker testManager, int sizeZ) {
        return runTestBatches(groupTestsIntoBatches(testFunctions), pos, rotation, world, testManager, sizeZ);
    }

    public static Collection<GameTestHarnessBatch> groupTestsIntoBatches(Collection<GameTestHarnessTestFunction> testFunctions) {
        Map<String, List<GameTestHarnessTestFunction>> map = testFunctions.stream().collect(Collectors.groupingBy(GameTestHarnessTestFunction::getBatchName));
        return map.entrySet().stream().flatMap((entry) -> {
            String string = entry.getKey();
            Consumer<WorldServer> consumer = GameTestHarnessRegistry.getBeforeBatchFunction(string);
            Consumer<WorldServer> consumer2 = GameTestHarnessRegistry.getAfterBatchFunction(string);
            MutableInt mutableInt = new MutableInt();
            Collection<GameTestHarnessTestFunction> collection = entry.getValue();
            return Streams.stream(Iterables.partition(collection, 100)).map((testFunctions) -> {
                return new GameTestHarnessBatch(string + ":" + mutableInt.incrementAndGet(), ImmutableList.copyOf(testFunctions), consumer, consumer2);
            });
        }).collect(ImmutableList.toImmutableList());
    }

    public static void clearAllTests(WorldServer world, BlockPosition pos, GameTestHarnessTicker testManager, int radius) {
        testManager.clear();
        BlockPosition blockPos = pos.offset(-radius, 0, -radius);
        BlockPosition blockPos2 = pos.offset(radius, 0, radius);
        BlockPosition.betweenClosedStream(blockPos, blockPos2).filter((posx) -> {
            return world.getType(posx).is(Blocks.STRUCTURE_BLOCK);
        }).forEach((posx) -> {
            TileEntityStructure structureBlockEntity = (TileEntityStructure)world.getTileEntity(posx);
            BlockPosition blockPos = structureBlockEntity.getPosition();
            StructureBoundingBox boundingBox = GameTestHarnessStructures.getStructureBoundingBox(structureBlockEntity);
            GameTestHarnessStructures.clearSpaceForStructure(boundingBox, blockPos.getY(), world);
        });
    }

    public static void clearMarkers(WorldServer world) {
        PacketDebug.sendGameTestClearPacket(world);
    }
}
