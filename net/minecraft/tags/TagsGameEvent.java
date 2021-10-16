package net.minecraft.tags;

import net.minecraft.core.IRegistry;
import net.minecraft.world.level.gameevent.GameEvent;

public class TagsGameEvent {
    protected static final TagUtil<GameEvent> HELPER = TagStatic.create(IRegistry.GAME_EVENT_REGISTRY, "tags/game_events");
    public static final Tag.Named<GameEvent> VIBRATIONS = bind("vibrations");
    public static final Tag.Named<GameEvent> IGNORE_VIBRATIONS_SNEAKING = bind("ignore_vibrations_sneaking");

    private static Tag.Named<GameEvent> bind(String id) {
        return HELPER.bind(id);
    }

    public static Tags<GameEvent> getAllTags() {
        return HELPER.getAllTags();
    }
}
