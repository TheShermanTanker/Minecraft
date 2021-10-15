package net.minecraft.world.level;

import com.mojang.serialization.Dynamic;
import net.minecraft.world.EnumDifficulty;

public final class WorldSettings {
    public String levelName;
    private final EnumGamemode gameType;
    public boolean hardcore;
    private final EnumDifficulty difficulty;
    private final boolean allowCommands;
    private final GameRules gameRules;
    private final DataPackConfiguration dataPackConfig;

    public WorldSettings(String name, EnumGamemode gameMode, boolean hardcore, EnumDifficulty difficulty, boolean allowCommands, GameRules gameRules, DataPackConfiguration dataPackSettings) {
        this.levelName = name;
        this.gameType = gameMode;
        this.hardcore = hardcore;
        this.difficulty = difficulty;
        this.allowCommands = allowCommands;
        this.gameRules = gameRules;
        this.dataPackConfig = dataPackSettings;
    }

    public static WorldSettings parse(Dynamic<?> dynamic, DataPackConfiguration dataPackSettings) {
        EnumGamemode gameType = EnumGamemode.getById(dynamic.get("GameType").asInt(0));
        return new WorldSettings(dynamic.get("LevelName").asString(""), gameType, dynamic.get("hardcore").asBoolean(false), dynamic.get("Difficulty").asNumber().map((number) -> {
            return EnumDifficulty.getById(number.byteValue());
        }).result().orElse(EnumDifficulty.NORMAL), dynamic.get("allowCommands").asBoolean(gameType == EnumGamemode.CREATIVE), new GameRules(dynamic.get("GameRules")), dataPackSettings);
    }

    public String getLevelName() {
        return this.levelName;
    }

    public EnumGamemode getGameType() {
        return this.gameType;
    }

    public boolean isHardcore() {
        return this.hardcore;
    }

    public EnumDifficulty getDifficulty() {
        return this.difficulty;
    }

    public boolean allowCommands() {
        return this.allowCommands;
    }

    public GameRules getGameRules() {
        return this.gameRules;
    }

    public DataPackConfiguration getDataPackConfig() {
        return this.dataPackConfig;
    }

    public WorldSettings withGameType(EnumGamemode mode) {
        return new WorldSettings(this.levelName, mode, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, this.dataPackConfig);
    }

    public WorldSettings withDifficulty(EnumDifficulty difficulty) {
        return new WorldSettings(this.levelName, this.gameType, this.hardcore, difficulty, this.allowCommands, this.gameRules, this.dataPackConfig);
    }

    public WorldSettings withDataPackConfig(DataPackConfiguration dataPackSettings) {
        return new WorldSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules, dataPackSettings);
    }

    public WorldSettings copy() {
        return new WorldSettings(this.levelName, this.gameType, this.hardcore, this.difficulty, this.allowCommands, this.gameRules.copy(), this.dataPackConfig);
    }
}
