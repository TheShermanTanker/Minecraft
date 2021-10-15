package net.minecraft.world.entity;

public interface IJumpable extends PlayerRideable {
    void onPlayerJump(int strength);

    boolean canJump();

    void handleStartJump(int height);

    void handleStopJump();
}
