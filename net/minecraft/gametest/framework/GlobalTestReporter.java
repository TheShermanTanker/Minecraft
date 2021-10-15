package net.minecraft.gametest.framework;

public class GlobalTestReporter {
    private static GameTestHarnessITestReporter DELEGATE = new GameTestHarnessLogger();

    public static void replaceWith(GameTestHarnessITestReporter listener) {
        DELEGATE = listener;
    }

    public static void onTestFailed(GameTestHarnessInfo test) {
        DELEGATE.onTestFailed(test);
    }

    public static void onTestSuccess(GameTestHarnessInfo test) {
        DELEGATE.onTestSuccess(test);
    }

    public static void finish() {
        DELEGATE.finish();
    }
}
