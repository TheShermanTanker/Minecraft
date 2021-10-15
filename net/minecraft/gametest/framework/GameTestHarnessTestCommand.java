package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.structures.DebugReportNBT;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.commons.io.IOUtils;

public class GameTestHarnessTestCommand {
    private static final int DEFAULT_CLEAR_RADIUS = 200;
    private static final int MAX_CLEAR_RADIUS = 1024;
    private static final int STRUCTURE_BLOCK_NEARBY_SEARCH_RADIUS = 15;
    private static final int STRUCTURE_BLOCK_FULL_SEARCH_RADIUS = 200;
    private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
    private static final int SHOW_POS_DURATION_MS = 10000;
    private static final int DEFAULT_X_SIZE = 5;
    private static final int DEFAULT_Y_SIZE = 5;
    private static final int DEFAULT_Z_SIZE = 5;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("test").then(net.minecraft.commands.CommandDispatcher.literal("runthis").executes((context) -> {
            return runNearbyTest(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("runthese").executes((context) -> {
            return runAllNearbyTests(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("runfailed").executes((context) -> {
            return runLastFailedTests(context.getSource(), false, 0, 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("onlyRequiredTests", BoolArgumentType.bool()).executes((context) -> {
            return runLastFailedTests(context.getSource(), BoolArgumentType.getBool(context, "onlyRequiredTests"), 0, 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("rotationSteps", IntegerArgumentType.integer()).executes((context) -> {
            return runLastFailedTests(context.getSource(), BoolArgumentType.getBool(context, "onlyRequiredTests"), IntegerArgumentType.getInteger(context, "rotationSteps"), 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("testsPerRow", IntegerArgumentType.integer()).executes((context) -> {
            return runLastFailedTests(context.getSource(), BoolArgumentType.getBool(context, "onlyRequiredTests"), IntegerArgumentType.getInteger(context, "rotationSteps"), IntegerArgumentType.getInteger(context, "testsPerRow"));
        }))))).then(net.minecraft.commands.CommandDispatcher.literal("run").then(net.minecraft.commands.CommandDispatcher.argument("testName", GameTestHarnessTestFunctionArgument.testFunctionArgument()).executes((context) -> {
            return runTest(context.getSource(), GameTestHarnessTestFunctionArgument.getTestFunction(context, "testName"), 0);
        }).then(net.minecraft.commands.CommandDispatcher.argument("rotationSteps", IntegerArgumentType.integer()).executes((context) -> {
            return runTest(context.getSource(), GameTestHarnessTestFunctionArgument.getTestFunction(context, "testName"), IntegerArgumentType.getInteger(context, "rotationSteps"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("runall").executes((context) -> {
            return runAllTests(context.getSource(), 0, 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("testClassName", GameTestHarnessTestClassArgument.testClassName()).executes((context) -> {
            return runAllTestsInClass(context.getSource(), GameTestHarnessTestClassArgument.getTestClassName(context, "testClassName"), 0, 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("rotationSteps", IntegerArgumentType.integer()).executes((context) -> {
            return runAllTestsInClass(context.getSource(), GameTestHarnessTestClassArgument.getTestClassName(context, "testClassName"), IntegerArgumentType.getInteger(context, "rotationSteps"), 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("testsPerRow", IntegerArgumentType.integer()).executes((context) -> {
            return runAllTestsInClass(context.getSource(), GameTestHarnessTestClassArgument.getTestClassName(context, "testClassName"), IntegerArgumentType.getInteger(context, "rotationSteps"), IntegerArgumentType.getInteger(context, "testsPerRow"));
        })))).then(net.minecraft.commands.CommandDispatcher.argument("rotationSteps", IntegerArgumentType.integer()).executes((context) -> {
            return runAllTests(context.getSource(), IntegerArgumentType.getInteger(context, "rotationSteps"), 8);
        }).then(net.minecraft.commands.CommandDispatcher.argument("testsPerRow", IntegerArgumentType.integer()).executes((context) -> {
            return runAllTests(context.getSource(), IntegerArgumentType.getInteger(context, "rotationSteps"), IntegerArgumentType.getInteger(context, "testsPerRow"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("export").then(net.minecraft.commands.CommandDispatcher.argument("testName", StringArgumentType.word()).executes((context) -> {
            return exportTestStructure(context.getSource(), StringArgumentType.getString(context, "testName"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("exportthis").executes((context) -> {
            return exportNearestTestStructure(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("import").then(net.minecraft.commands.CommandDispatcher.argument("testName", StringArgumentType.word()).executes((context) -> {
            return importTestStructure(context.getSource(), StringArgumentType.getString(context, "testName"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("pos").executes((context) -> {
            return showPos(context.getSource(), "pos");
        }).then(net.minecraft.commands.CommandDispatcher.argument("var", StringArgumentType.word()).executes((context) -> {
            return showPos(context.getSource(), StringArgumentType.getString(context, "var"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("create").then(net.minecraft.commands.CommandDispatcher.argument("testName", StringArgumentType.word()).executes((context) -> {
            return createNewStructure(context.getSource(), StringArgumentType.getString(context, "testName"), 5, 5, 5);
        }).then(net.minecraft.commands.CommandDispatcher.argument("width", IntegerArgumentType.integer()).executes((context) -> {
            return createNewStructure(context.getSource(), StringArgumentType.getString(context, "testName"), IntegerArgumentType.getInteger(context, "width"), IntegerArgumentType.getInteger(context, "width"), IntegerArgumentType.getInteger(context, "width"));
        }).then(net.minecraft.commands.CommandDispatcher.argument("height", IntegerArgumentType.integer()).then(net.minecraft.commands.CommandDispatcher.argument("depth", IntegerArgumentType.integer()).executes((context) -> {
            return createNewStructure(context.getSource(), StringArgumentType.getString(context, "testName"), IntegerArgumentType.getInteger(context, "width"), IntegerArgumentType.getInteger(context, "height"), IntegerArgumentType.getInteger(context, "depth"));
        })))))).then(net.minecraft.commands.CommandDispatcher.literal("clearall").executes((context) -> {
            return clearAllTests(context.getSource(), 200);
        }).then(net.minecraft.commands.CommandDispatcher.argument("radius", IntegerArgumentType.integer()).executes((context) -> {
            return clearAllTests(context.getSource(), IntegerArgumentType.getInteger(context, "radius"));
        }))));
    }

    private static int createNewStructure(CommandListenerWrapper source, String structure, int x, int y, int z) {
        if (x <= 48 && y <= 48 && z <= 48) {
            WorldServer serverLevel = source.getWorld();
            BlockPosition blockPos = new BlockPosition(source.getPosition());
            BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), source.getWorld().getHighestBlockYAt(HeightMap.Type.WORLD_SURFACE, blockPos).getY(), blockPos.getZ() + 3);
            GameTestHarnessStructures.createNewEmptyStructureBlock(structure.toLowerCase(), blockPos2, new BaseBlockPosition(x, y, z), EnumBlockRotation.NONE, serverLevel);

            for(int i = 0; i < x; ++i) {
                for(int j = 0; j < z; ++j) {
                    BlockPosition blockPos3 = new BlockPosition(blockPos2.getX() + i, blockPos2.getY() + 1, blockPos2.getZ() + j);
                    Block block = Blocks.POLISHED_ANDESITE;
                    ArgumentTileLocation blockInput = new ArgumentTileLocation(block.getBlockData(), Collections.emptySet(), (NBTTagCompound)null);
                    blockInput.place(serverLevel, blockPos3, 2);
                }
            }

            GameTestHarnessStructures.addCommandBlockAndButtonToStartTest(blockPos2, new BlockPosition(1, 0, -1), EnumBlockRotation.NONE, serverLevel);
            return 0;
        } else {
            throw new IllegalArgumentException("The structure must be less than 48 blocks big in each axis");
        }
    }

    private static int showPos(CommandListenerWrapper source, String variableName) throws CommandSyntaxException {
        MovingObjectPositionBlock blockHitResult = (MovingObjectPositionBlock)source.getPlayerOrException().pick(10.0D, 1.0F, false);
        BlockPosition blockPos = blockHitResult.getBlockPosition();
        WorldServer serverLevel = source.getWorld();
        Optional<BlockPosition> optional = GameTestHarnessStructures.findStructureBlockContainingPos(blockPos, 15, serverLevel);
        if (!optional.isPresent()) {
            optional = GameTestHarnessStructures.findStructureBlockContainingPos(blockPos, 200, serverLevel);
        }

        if (!optional.isPresent()) {
            source.sendFailureMessage(new ChatComponentText("Can't find a structure block that contains the targeted pos " + blockPos));
            return 0;
        } else {
            TileEntityStructure structureBlockEntity = (TileEntityStructure)serverLevel.getTileEntity(optional.get());
            BlockPosition blockPos2 = blockPos.subtract(optional.get());
            String string = blockPos2.getX() + ", " + blockPos2.getY() + ", " + blockPos2.getZ();
            String string2 = structureBlockEntity.getStructurePath();
            IChatBaseComponent component = (new ChatComponentText(string)).setChatModifier(ChatModifier.EMPTY.setBold(true).setColor(EnumChatFormat.GREEN).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatComponentText("Click to copy to clipboard"))).setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.COPY_TO_CLIPBOARD, "final BlockPos " + variableName + " = new BlockPos(" + string + ");")));
            source.sendMessage((new ChatComponentText("Position relative to " + string2 + ": ")).addSibling(component), false);
            PacketDebug.sendGameTestAddMarker(serverLevel, new BlockPosition(blockPos), string, -2147418368, 10000);
            return 1;
        }
    }

    private static int runNearbyTest(CommandListenerWrapper source) {
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        WorldServer serverLevel = source.getWorld();
        BlockPosition blockPos2 = GameTestHarnessStructures.findNearestStructureBlock(blockPos, 15, serverLevel);
        if (blockPos2 == null) {
            say(serverLevel, "Couldn't find any structure block within 15 radius", EnumChatFormat.RED);
            return 0;
        } else {
            GameTestHarnessRunner.clearMarkers(serverLevel);
            runTest(serverLevel, blockPos2, (GameTestHarnessCollector)null);
            return 1;
        }
    }

    private static int runAllNearbyTests(CommandListenerWrapper source) {
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        WorldServer serverLevel = source.getWorld();
        Collection<BlockPosition> collection = GameTestHarnessStructures.findStructureBlocks(blockPos, 200, serverLevel);
        if (collection.isEmpty()) {
            say(serverLevel, "Couldn't find any structure blocks within 200 block radius", EnumChatFormat.RED);
            return 1;
        } else {
            GameTestHarnessRunner.clearMarkers(serverLevel);
            say(source, "Running " + collection.size() + " tests...");
            GameTestHarnessCollector multipleTestTracker = new GameTestHarnessCollector();
            collection.forEach((pos) -> {
                runTest(serverLevel, pos, multipleTestTracker);
            });
            return 1;
        }
    }

    private static void runTest(WorldServer world, BlockPosition pos, @Nullable GameTestHarnessCollector tests) {
        TileEntityStructure structureBlockEntity = (TileEntityStructure)world.getTileEntity(pos);
        String string = structureBlockEntity.getStructurePath();
        GameTestHarnessTestFunction testFunction = GameTestHarnessRegistry.getTestFunction(string);
        GameTestHarnessInfo gameTestInfo = new GameTestHarnessInfo(testFunction, structureBlockEntity.getRotation(), world);
        if (tests != null) {
            tests.addTestToTrack(gameTestInfo);
            gameTestInfo.addListener(new GameTestHarnessTestCommand.TestSummaryDisplayer(world, tests));
        }

        runTestPreparation(testFunction, world);
        AxisAlignedBB aABB = GameTestHarnessStructures.getStructureBounds(structureBlockEntity);
        BlockPosition blockPos = new BlockPosition(aABB.minX, aABB.minY, aABB.minZ);
        GameTestHarnessRunner.runTest(gameTestInfo, blockPos, GameTestHarnessTicker.SINGLETON);
    }

    static void showTestSummaryIfAllDone(WorldServer world, GameTestHarnessCollector tests) {
        if (tests.isDone()) {
            say(world, "GameTest done! " + tests.getTotalCount() + " tests were run", EnumChatFormat.WHITE);
            if (tests.hasFailedRequired()) {
                say(world, tests.getFailedRequiredCount() + " required tests failed :(", EnumChatFormat.RED);
            } else {
                say(world, "All required tests passed :)", EnumChatFormat.GREEN);
            }

            if (tests.hasFailedOptional()) {
                say(world, tests.getFailedOptionalCount() + " optional tests failed", EnumChatFormat.GRAY);
            }
        }

    }

    private static int clearAllTests(CommandListenerWrapper source, int radius) {
        WorldServer serverLevel = source.getWorld();
        GameTestHarnessRunner.clearMarkers(serverLevel);
        BlockPosition blockPos = new BlockPosition(source.getPosition().x, (double)source.getWorld().getHighestBlockYAt(HeightMap.Type.WORLD_SURFACE, new BlockPosition(source.getPosition())).getY(), source.getPosition().z);
        GameTestHarnessRunner.clearAllTests(serverLevel, blockPos, GameTestHarnessTicker.SINGLETON, MathHelper.clamp(radius, 0, 1024));
        return 1;
    }

    private static int runTest(CommandListenerWrapper source, GameTestHarnessTestFunction testFunction, int rotationSteps) {
        WorldServer serverLevel = source.getWorld();
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        int i = source.getWorld().getHighestBlockYAt(HeightMap.Type.WORLD_SURFACE, blockPos).getY();
        BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), i, blockPos.getZ() + 3);
        GameTestHarnessRunner.clearMarkers(serverLevel);
        runTestPreparation(testFunction, serverLevel);
        EnumBlockRotation rotation = GameTestHarnessStructures.getRotationForRotationSteps(rotationSteps);
        GameTestHarnessInfo gameTestInfo = new GameTestHarnessInfo(testFunction, rotation, serverLevel);
        GameTestHarnessRunner.runTest(gameTestInfo, blockPos2, GameTestHarnessTicker.SINGLETON);
        return 1;
    }

    private static void runTestPreparation(GameTestHarnessTestFunction testFunction, WorldServer world) {
        Consumer<WorldServer> consumer = GameTestHarnessRegistry.getBeforeBatchFunction(testFunction.getBatchName());
        if (consumer != null) {
            consumer.accept(world);
        }

    }

    private static int runAllTests(CommandListenerWrapper source, int rotationSteps, int sizeZ) {
        GameTestHarnessRunner.clearMarkers(source.getWorld());
        Collection<GameTestHarnessTestFunction> collection = GameTestHarnessRegistry.getAllTestFunctions();
        say(source, "Running all " + collection.size() + " tests...");
        GameTestHarnessRegistry.forgetFailedTests();
        runTests(source, collection, rotationSteps, sizeZ);
        return 1;
    }

    private static int runAllTestsInClass(CommandListenerWrapper source, String testClass, int rotationSteps, int sizeZ) {
        Collection<GameTestHarnessTestFunction> collection = GameTestHarnessRegistry.getTestFunctionsForClassName(testClass);
        GameTestHarnessRunner.clearMarkers(source.getWorld());
        say(source, "Running " + collection.size() + " tests from " + testClass + "...");
        GameTestHarnessRegistry.forgetFailedTests();
        runTests(source, collection, rotationSteps, sizeZ);
        return 1;
    }

    private static int runLastFailedTests(CommandListenerWrapper source, boolean requiredOnly, int rotationSteps, int sizeZ) {
        Collection<GameTestHarnessTestFunction> collection;
        if (requiredOnly) {
            collection = GameTestHarnessRegistry.getLastFailedTests().stream().filter(GameTestHarnessTestFunction::isRequired).collect(Collectors.toList());
        } else {
            collection = GameTestHarnessRegistry.getLastFailedTests();
        }

        if (collection.isEmpty()) {
            say(source, "No failed tests to rerun");
            return 0;
        } else {
            GameTestHarnessRunner.clearMarkers(source.getWorld());
            say(source, "Rerunning " + collection.size() + " failed tests (" + (requiredOnly ? "only required tests" : "including optional tests") + ")");
            runTests(source, collection, rotationSteps, sizeZ);
            return 1;
        }
    }

    private static void runTests(CommandListenerWrapper source, Collection<GameTestHarnessTestFunction> testFunctions, int rotationSteps, int i) {
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), source.getWorld().getHighestBlockYAt(HeightMap.Type.WORLD_SURFACE, blockPos).getY(), blockPos.getZ() + 3);
        WorldServer serverLevel = source.getWorld();
        EnumBlockRotation rotation = GameTestHarnessStructures.getRotationForRotationSteps(rotationSteps);
        Collection<GameTestHarnessInfo> collection = GameTestHarnessRunner.runTests(testFunctions, blockPos2, rotation, serverLevel, GameTestHarnessTicker.SINGLETON, i);
        GameTestHarnessCollector multipleTestTracker = new GameTestHarnessCollector(collection);
        multipleTestTracker.addListener(new GameTestHarnessTestCommand.TestSummaryDisplayer(serverLevel, multipleTestTracker));
        multipleTestTracker.addFailureListener((test) -> {
            GameTestHarnessRegistry.rememberFailedTest(test.getTestFunction());
        });
    }

    private static void say(CommandListenerWrapper source, String message) {
        source.sendMessage(new ChatComponentText(message), false);
    }

    private static int exportNearestTestStructure(CommandListenerWrapper source) {
        BlockPosition blockPos = new BlockPosition(source.getPosition());
        WorldServer serverLevel = source.getWorld();
        BlockPosition blockPos2 = GameTestHarnessStructures.findNearestStructureBlock(blockPos, 15, serverLevel);
        if (blockPos2 == null) {
            say(serverLevel, "Couldn't find any structure block within 15 radius", EnumChatFormat.RED);
            return 0;
        } else {
            TileEntityStructure structureBlockEntity = (TileEntityStructure)serverLevel.getTileEntity(blockPos2);
            String string = structureBlockEntity.getStructurePath();
            return exportTestStructure(source, string);
        }
    }

    private static int exportTestStructure(CommandListenerWrapper source, String structure) {
        Path path = Paths.get(GameTestHarnessStructures.testStructuresDir);
        MinecraftKey resourceLocation = new MinecraftKey("minecraft", structure);
        Path path2 = source.getWorld().getStructureManager().createPathToStructure(resourceLocation, ".nbt");
        Path path3 = DebugReportNBT.convertStructure(path2, structure, path);
        if (path3 == null) {
            say(source, "Failed to export " + path2);
            return 1;
        } else {
            try {
                Files.createDirectories(path3.getParent());
            } catch (IOException var7) {
                say(source, "Could not create folder " + path3.getParent());
                var7.printStackTrace();
                return 1;
            }

            say(source, "Exported " + structure + " to " + path3.toAbsolutePath());
            return 0;
        }
    }

    private static int importTestStructure(CommandListenerWrapper source, String structure) {
        Path path = Paths.get(GameTestHarnessStructures.testStructuresDir, structure + ".snbt");
        MinecraftKey resourceLocation = new MinecraftKey("minecraft", structure);
        Path path2 = source.getWorld().getStructureManager().createPathToStructure(resourceLocation, ".nbt");

        try {
            BufferedReader bufferedReader = Files.newBufferedReader(path);
            String string = IOUtils.toString((Reader)bufferedReader);
            Files.createDirectories(path2.getParent());
            OutputStream outputStream = Files.newOutputStream(path2);

            try {
                NBTCompressedStreamTools.writeCompressed(GameProfileSerializer.snbtToStructure(string), outputStream);
            } catch (Throwable var11) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                    }
                }

                throw var11;
            }

            if (outputStream != null) {
                outputStream.close();
            }

            say(source, "Imported to " + path2.toAbsolutePath());
            return 0;
        } catch (CommandSyntaxException | IOException var12) {
            System.err.println("Failed to load structure " + structure);
            var12.printStackTrace();
            return 1;
        }
    }

    private static void say(WorldServer world, String message, EnumChatFormat formatting) {
        world.getPlayers((player) -> {
            return true;
        }).forEach((player) -> {
            player.sendMessage(new ChatComponentText(formatting + message), SystemUtils.NIL_UUID);
        });
    }

    static class TestSummaryDisplayer implements GameTestHarnessListener {
        private final WorldServer level;
        private final GameTestHarnessCollector tracker;

        public TestSummaryDisplayer(WorldServer world, GameTestHarnessCollector tests) {
            this.level = world;
            this.tracker = tests;
        }

        @Override
        public void testStructureLoaded(GameTestHarnessInfo test) {
        }

        @Override
        public void testPassed(GameTestHarnessInfo test) {
            GameTestHarnessTestCommand.showTestSummaryIfAllDone(this.level, this.tracker);
        }

        @Override
        public void testFailed(GameTestHarnessInfo test) {
            GameTestHarnessTestCommand.showTestSummaryIfAllDone(this.level, this.tracker);
        }
    }
}
