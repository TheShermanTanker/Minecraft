package net.minecraft.world;

import java.util.UUID;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;

public abstract class BossBattle {
    private final UUID id;
    public IChatBaseComponent name;
    protected float progress;
    public BossBattle.BarColor color;
    public BossBattle.BarStyle overlay;
    protected boolean darkenScreen;
    protected boolean playBossMusic;
    protected boolean createWorldFog;

    public BossBattle(UUID uuid, IChatBaseComponent name, BossBattle.BarColor color, BossBattle.BarStyle style) {
        this.id = uuid;
        this.name = name;
        this.color = color;
        this.overlay = style;
        this.progress = 1.0F;
    }

    public UUID getId() {
        return this.id;
    }

    public IChatBaseComponent getName() {
        return this.name;
    }

    public void setName(IChatBaseComponent name) {
        this.name = name;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float percentage) {
        this.progress = percentage;
    }

    public BossBattle.BarColor getColor() {
        return this.color;
    }

    public void setColor(BossBattle.BarColor color) {
        this.color = color;
    }

    public BossBattle.BarStyle getOverlay() {
        return this.overlay;
    }

    public void setOverlay(BossBattle.BarStyle style) {
        this.overlay = style;
    }

    public boolean isDarkenSky() {
        return this.darkenScreen;
    }

    public BossBattle setDarkenSky(boolean darkenSky) {
        this.darkenScreen = darkenSky;
        return this;
    }

    public boolean isPlayMusic() {
        return this.playBossMusic;
    }

    public BossBattle setPlayMusic(boolean dragonMusic) {
        this.playBossMusic = dragonMusic;
        return this;
    }

    public BossBattle setCreateFog(boolean thickenFog) {
        this.createWorldFog = thickenFog;
        return this;
    }

    public boolean isCreateFog() {
        return this.createWorldFog;
    }

    public static enum BarColor {
        PINK("pink", EnumChatFormat.RED),
        BLUE("blue", EnumChatFormat.BLUE),
        RED("red", EnumChatFormat.DARK_RED),
        GREEN("green", EnumChatFormat.GREEN),
        YELLOW("yellow", EnumChatFormat.YELLOW),
        PURPLE("purple", EnumChatFormat.DARK_BLUE),
        WHITE("white", EnumChatFormat.WHITE);

        private final String name;
        private final EnumChatFormat formatting;

        private BarColor(String name, EnumChatFormat format) {
            this.name = name;
            this.formatting = format;
        }

        public EnumChatFormat getFormatting() {
            return this.formatting;
        }

        public String getName() {
            return this.name;
        }

        public static BossBattle.BarColor byName(String name) {
            for(BossBattle.BarColor bossBarColor : values()) {
                if (bossBarColor.name.equals(name)) {
                    return bossBarColor;
                }
            }

            return WHITE;
        }
    }

    public static enum BarStyle {
        PROGRESS("progress"),
        NOTCHED_6("notched_6"),
        NOTCHED_10("notched_10"),
        NOTCHED_12("notched_12"),
        NOTCHED_20("notched_20");

        private final String name;

        private BarStyle(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static BossBattle.BarStyle byName(String name) {
            for(BossBattle.BarStyle bossBarOverlay : values()) {
                if (bossBarOverlay.name.equals(name)) {
                    return bossBarOverlay;
                }
            }

            return PROGRESS;
        }
    }
}
