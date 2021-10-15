package net.minecraft.gametest.framework;

public interface GameTestHarnessITestReporter {
    void onTestFailed(GameTestHarnessInfo test);

    void onTestSuccess(GameTestHarnessInfo test);

    default void finish() {
    }
}
