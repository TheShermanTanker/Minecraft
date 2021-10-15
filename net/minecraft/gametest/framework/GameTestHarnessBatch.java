package net.minecraft.gametest.framework;

import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;

public class GameTestHarnessBatch {
    public static final String DEFAULT_BATCH_NAME = "defaultBatch";
    private final String name;
    private final Collection<GameTestHarnessTestFunction> testFunctions;
    @Nullable
    private final Consumer<WorldServer> beforeBatchFunction;
    @Nullable
    private final Consumer<WorldServer> afterBatchFunction;

    public GameTestHarnessBatch(String id, Collection<GameTestHarnessTestFunction> testFunctions, @Nullable Consumer<WorldServer> beforeBatchConsumer, @Nullable Consumer<WorldServer> afterBatchConsumer) {
        if (testFunctions.isEmpty()) {
            throw new IllegalArgumentException("A GameTestBatch must include at least one TestFunction!");
        } else {
            this.name = id;
            this.testFunctions = testFunctions;
            this.beforeBatchFunction = beforeBatchConsumer;
            this.afterBatchFunction = afterBatchConsumer;
        }
    }

    public String getName() {
        return this.name;
    }

    public Collection<GameTestHarnessTestFunction> getTestFunctions() {
        return this.testFunctions;
    }

    public void runBeforeBatchFunction(WorldServer world) {
        if (this.beforeBatchFunction != null) {
            this.beforeBatchFunction.accept(world);
        }

    }

    public void runAfterBatchFunction(WorldServer world) {
        if (this.afterBatchFunction != null) {
            this.afterBatchFunction.accept(world);
        }

    }
}
