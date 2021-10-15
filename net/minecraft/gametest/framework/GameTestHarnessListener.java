package net.minecraft.gametest.framework;

public interface GameTestHarnessListener {
    void testStructureLoaded(GameTestHarnessInfo test);

    void testPassed(GameTestHarnessInfo test);

    void testFailed(GameTestHarnessInfo test);
}
