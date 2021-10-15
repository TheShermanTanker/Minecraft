package net.minecraft.world;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public enum EnumDifficulty {
    PEACEFUL(0, "peaceful"),
    EASY(1, "easy"),
    NORMAL(2, "normal"),
    HARD(3, "hard");

    private static final EnumDifficulty[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(EnumDifficulty::getId)).toArray((i) -> {
        return new EnumDifficulty[i];
    });
    private final int id;
    private final String key;

    private EnumDifficulty(int id, String name) {
        this.id = id;
        this.key = name;
    }

    public int getId() {
        return this.id;
    }

    public IChatBaseComponent getDisplayName() {
        return new ChatMessage("options.difficulty." + this.key);
    }

    public static EnumDifficulty getById(int ordinal) {
        return BY_ID[ordinal % BY_ID.length];
    }

    @Nullable
    public static EnumDifficulty byName(String name) {
        for(EnumDifficulty difficulty : values()) {
            if (difficulty.key.equals(name)) {
                return difficulty;
            }
        }

        return null;
    }

    public String getKey() {
        return this.key;
    }
}
