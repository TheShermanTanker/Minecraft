package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockLectern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestHarnessListener {
    private final GameTestHarnessInfo originalTestInfo;
    private final GameTestHarnessTicker testTicker;
    private final BlockPosition structurePos;
    int attempts;
    int successes;

    public ReportGameListener(GameTestHarnessInfo test, GameTestHarnessTicker testManager, BlockPosition pos) {
        this.originalTestInfo = test;
        this.testTicker = testManager;
        this.structurePos = pos;
        this.attempts = 0;
        this.successes = 0;
    }

    @Override
    public void testStructureLoaded(GameTestHarnessInfo test) {
        spawnBeacon(this.originalTestInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
        ++this.attempts;
    }

    @Override
    public void testPassed(GameTestHarnessInfo test) {
        ++this.successes;
        if (!test.isFlaky()) {
            reportPassed(test, test.getTestName() + " passed!");
        } else {
            if (this.successes >= test.requiredSuccesses()) {
                reportPassed(test, test + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(this.originalTestInfo.getLevel(), EnumChatFormat.GREEN, "Flaky test " + this.originalTestInfo + " succeeded, attempt: " + this.attempts + " successes: " + this.successes);
                this.rerunTest();
            }

        }
    }

    @Override
    public void testFailed(GameTestHarnessInfo test) {
        if (!test.isFlaky()) {
            reportFailure(test, test.getError());
        } else {
            GameTestHarnessTestFunction testFunction = this.originalTestInfo.getTestFunction();
            String string = "Flaky test " + this.originalTestInfo + " failed, attempt: " + this.attempts + "/" + testFunction.getMaxAttempts();
            if (testFunction.getRequiredSuccesses() > 1) {
                string = string + ", successes: " + this.successes + " (" + testFunction.getRequiredSuccesses() + " required)";
            }

            say(this.originalTestInfo.getLevel(), EnumChatFormat.YELLOW, string);
            if (test.maxAttempts() - this.attempts + this.successes >= test.requiredSuccesses()) {
                this.rerunTest();
            } else {
                reportFailure(test, new ExhaustedAttemptsException(this.attempts, this.successes, test));
            }

        }
    }

    public static void reportPassed(GameTestHarnessInfo test, String output) {
        spawnBeacon(test, Blocks.LIME_STAINED_GLASS);
        visualizePassedTest(test, output);
    }

    private static void visualizePassedTest(GameTestHarnessInfo test, String output) {
        say(test.getLevel(), EnumChatFormat.GREEN, output);
        GlobalTestReporter.onTestSuccess(test);
    }

    protected static void reportFailure(GameTestHarnessInfo test, Throwable output) {
        spawnBeacon(test, test.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
        spawnLectern(test, SystemUtils.describeError(output));
        visualizeFailedTest(test, output);
    }

    protected static void visualizeFailedTest(GameTestHarnessInfo test, Throwable output) {
        String string = output.getMessage() + (output.getCause() == null ? "" : " cause: " + SystemUtils.describeError(output.getCause()));
        String string2 = (test.isRequired() ? "" : "(optional) ") + test.getTestName() + " failed! " + string;
        say(test.getLevel(), test.isRequired() ? EnumChatFormat.RED : EnumChatFormat.YELLOW, string2);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(output), output);
        if (throwable instanceof GameTestHarnessAssertionPosition) {
            GameTestHarnessAssertionPosition gameTestAssertPosException = (GameTestHarnessAssertionPosition)throwable;
            showRedBox(test.getLevel(), gameTestAssertPosException.getAbsolutePos(), gameTestAssertPosException.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(test);
    }

    private void rerunTest() {
        this.originalTestInfo.clearStructure();
        GameTestHarnessInfo gameTestInfo = new GameTestHarnessInfo(this.originalTestInfo.getTestFunction(), this.originalTestInfo.getRotation(), this.originalTestInfo.getLevel());
        gameTestInfo.startExecution();
        this.testTicker.add(gameTestInfo);
        gameTestInfo.addListener(this);
        gameTestInfo.spawnStructure(this.structurePos, 2);
    }

    protected static void spawnBeacon(GameTestHarnessInfo test, Block block) {
        WorldServer serverLevel = test.getLevel();
        BlockPosition blockPos = test.getStructureBlockPos();
        BlockPosition blockPos2 = new BlockPosition(-1, -1, -1);
        BlockPosition blockPos3 = DefinedStructure.transform(blockPos.offset(blockPos2), EnumBlockMirror.NONE, test.getRotation(), blockPos);
        serverLevel.setTypeUpdate(blockPos3, Blocks.BEACON.getBlockData().rotate(test.getRotation()));
        BlockPosition blockPos4 = blockPos3.offset(0, 1, 0);
        serverLevel.setTypeUpdate(blockPos4, block.getBlockData());

        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                BlockPosition blockPos5 = blockPos3.offset(i, -1, j);
                serverLevel.setTypeUpdate(blockPos5, Blocks.IRON_BLOCK.getBlockData());
            }
        }

    }

    private static void spawnLectern(GameTestHarnessInfo test, String output) {
        WorldServer serverLevel = test.getLevel();
        BlockPosition blockPos = test.getStructureBlockPos();
        BlockPosition blockPos2 = new BlockPosition(-1, 1, -1);
        BlockPosition blockPos3 = DefinedStructure.transform(blockPos.offset(blockPos2), EnumBlockMirror.NONE, test.getRotation(), blockPos);
        serverLevel.setTypeUpdate(blockPos3, Blocks.LECTERN.getBlockData().rotate(test.getRotation()));
        IBlockData blockState = serverLevel.getType(blockPos3);
        ItemStack itemStack = createBook(test.getTestName(), test.isRequired(), output);
        BlockLectern.tryPlaceBook((EntityHuman)null, serverLevel, blockPos3, blockState, itemStack);
    }

    private static ItemStack createBook(String text, boolean required, String output) {
        ItemStack itemStack = new ItemStack(Items.WRITABLE_BOOK);
        NBTTagList listTag = new NBTTagList();
        StringBuffer stringBuffer = new StringBuffer();
        Arrays.stream(text.split("\\.")).forEach((line) -> {
            stringBuffer.append(line).append('\n');
        });
        if (!required) {
            stringBuffer.append("(optional)\n");
        }

        stringBuffer.append("-------------------\n");
        listTag.add(NBTTagString.valueOf(stringBuffer + output));
        itemStack.addTagElement("pages", listTag);
        return itemStack;
    }

    protected static void say(WorldServer world, EnumChatFormat formatting, String message) {
        world.getPlayers((player) -> {
            return true;
        }).forEach((player) -> {
            player.sendMessage((new ChatComponentText(message)).withStyle(formatting), SystemUtils.NIL_UUID);
        });
    }

    private static void showRedBox(WorldServer world, BlockPosition pos, String message) {
        PacketDebug.sendGameTestAddMarker(world, pos, message, -2130771968, Integer.MAX_VALUE);
    }
}
