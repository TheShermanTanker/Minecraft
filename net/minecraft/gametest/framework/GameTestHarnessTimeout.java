package net.minecraft.gametest.framework;

public class GameTestHarnessTimeout extends RuntimeException {
    public GameTestHarnessTimeout(String message) {
        super(message);
    }
}
