package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerAbilities;

public enum EnumGamemode {
    SURVIVAL(0, "survival"),
    CREATIVE(1, "creative"),
    ADVENTURE(2, "adventure"),
    SPECTATOR(3, "spectator");

    public static final EnumGamemode DEFAULT_MODE = SURVIVAL;
    private static final int NOT_SET = -1;
    private final int id;
    private final String name;
    private final IChatBaseComponent shortName;
    private final IChatBaseComponent longName;

    private EnumGamemode(int id, String name) {
        this.id = id;
        this.name = name;
        this.shortName = new ChatMessage("selectWorld.gameMode." + name);
        this.longName = new ChatMessage("gameMode." + name);
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public IChatBaseComponent getLongDisplayName() {
        return this.longName;
    }

    public IChatBaseComponent getShortDisplayName() {
        return this.shortName;
    }

    public void updatePlayerAbilities(PlayerAbilities abilities) {
        if (this == CREATIVE) {
            abilities.mayfly = true;
            abilities.instabuild = true;
            abilities.invulnerable = true;
        } else if (this == SPECTATOR) {
            abilities.mayfly = true;
            abilities.instabuild = false;
            abilities.invulnerable = true;
            abilities.flying = true;
        } else {
            abilities.mayfly = false;
            abilities.instabuild = false;
            abilities.invulnerable = false;
            abilities.flying = false;
        }

        abilities.mayBuild = !this.isBlockPlacingRestricted();
    }

    public boolean isBlockPlacingRestricted() {
        return this == ADVENTURE || this == SPECTATOR;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public boolean isSurvival() {
        return this == SURVIVAL || this == ADVENTURE;
    }

    public static EnumGamemode getById(int id) {
        return byId(id, DEFAULT_MODE);
    }

    public static EnumGamemode byId(int id, EnumGamemode defaultMode) {
        for(EnumGamemode gameType : values()) {
            if (gameType.id == id) {
                return gameType;
            }
        }

        return defaultMode;
    }

    public static EnumGamemode byName(String name) {
        return byName(name, SURVIVAL);
    }

    public static EnumGamemode byName(String name, EnumGamemode defaultMode) {
        for(EnumGamemode gameType : values()) {
            if (gameType.name.equals(name)) {
                return gameType;
            }
        }

        return defaultMode;
    }

    public static int getNullableId(@Nullable EnumGamemode gameMode) {
        return gameMode != null ? gameMode.id : -1;
    }

    @Nullable
    public static EnumGamemode byNullableId(int id) {
        return id == -1 ? null : getById(id);
    }
}
