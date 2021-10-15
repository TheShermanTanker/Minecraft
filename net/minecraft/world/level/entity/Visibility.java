package net.minecraft.world.level.entity;

import net.minecraft.server.level.PlayerChunk;

public enum Visibility {
    HIDDEN(false, false),
    TRACKED(true, false),
    TICKING(true, true);

    private final boolean accessible;
    private final boolean ticking;

    private Visibility(boolean tracked, boolean tick) {
        this.accessible = tracked;
        this.ticking = tick;
    }

    public boolean isTicking() {
        return this.ticking;
    }

    public boolean isAccessible() {
        return this.accessible;
    }

    public static Visibility fromFullChunkStatus(PlayerChunk.State levelType) {
        if (levelType.isAtLeast(PlayerChunk.State.ENTITY_TICKING)) {
            return TICKING;
        } else {
            return levelType.isAtLeast(PlayerChunk.State.BORDER) ? TRACKED : HIDDEN;
        }
    }
}
