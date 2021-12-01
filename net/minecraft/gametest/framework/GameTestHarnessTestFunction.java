package net.minecraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.world.level.block.EnumBlockRotation;

public class GameTestHarnessTestFunction {
    private final String batchName;
    private final String testName;
    private final String structureName;
    private final boolean required;
    private final int maxAttempts;
    private final int requiredSuccesses;
    private final Consumer<GameTestHarnessHelper> function;
    private final int maxTicks;
    private final long setupTicks;
    private final EnumBlockRotation rotation;

    public GameTestHarnessTestFunction(String batchId, String structurePath, String structureName, int tickLimit, long duration, boolean required, Consumer<GameTestHarnessHelper> starter) {
        this(batchId, structurePath, structureName, EnumBlockRotation.NONE, tickLimit, duration, required, 1, 1, starter);
    }

    public GameTestHarnessTestFunction(String batchId, String structurePath, String structureName, EnumBlockRotation rotation, int tickLimit, long duration, boolean required, Consumer<GameTestHarnessHelper> starter) {
        this(batchId, structurePath, structureName, rotation, tickLimit, duration, required, 1, 1, starter);
    }

    public GameTestHarnessTestFunction(String batchId, String structurePath, String structureName, EnumBlockRotation rotation, int tickLimit, long duration, boolean required, int requiredSuccesses, int maxAttempts, Consumer<GameTestHarnessHelper> starter) {
        this.batchName = batchId;
        this.testName = structurePath;
        this.structureName = structureName;
        this.rotation = rotation;
        this.maxTicks = tickLimit;
        this.required = required;
        this.requiredSuccesses = requiredSuccesses;
        this.maxAttempts = maxAttempts;
        this.function = starter;
        this.setupTicks = duration;
    }

    public void run(GameTestHarnessHelper context) {
        this.function.accept(context);
    }

    public String getTestName() {
        return this.testName;
    }

    public String getStructureName() {
        return this.structureName;
    }

    @Override
    public String toString() {
        return this.testName;
    }

    public int getMaxTicks() {
        return this.maxTicks;
    }

    public boolean isRequired() {
        return this.required;
    }

    public String getBatchName() {
        return this.batchName;
    }

    public long getSetupTicks() {
        return this.setupTicks;
    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public boolean isFlaky() {
        return this.maxAttempts > 1;
    }

    public int getMaxAttempts() {
        return this.maxAttempts;
    }

    public int getRequiredSuccesses() {
        return this.requiredSuccesses;
    }
}
