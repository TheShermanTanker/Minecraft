package net.minecraft.network.protocol.game;

import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.BossBattle;

public class PacketPlayOutBoss implements Packet<PacketListenerPlayOut> {
    private static final int FLAG_DARKEN = 1;
    private static final int FLAG_MUSIC = 2;
    private static final int FLAG_FOG = 4;
    private final UUID id;
    private final PacketPlayOutBoss.Action operation;
    static final PacketPlayOutBoss.Action REMOVE_OPERATION = new PacketPlayOutBoss.Action() {
        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.REMOVE;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.remove(uuid);
        }

        @Override
        public void write(PacketDataSerializer buf) {
        }
    };

    private PacketPlayOutBoss(UUID uuid, PacketPlayOutBoss.Action action) {
        this.id = uuid;
        this.operation = action;
    }

    public PacketPlayOutBoss(PacketDataSerializer buf) {
        this.id = buf.readUUID();
        PacketPlayOutBoss.OperationType operationType = buf.readEnum(PacketPlayOutBoss.OperationType.class);
        this.operation = operationType.reader.apply(buf);
    }

    public static PacketPlayOutBoss createAddPacket(BossBattle bar) {
        return new PacketPlayOutBoss(bar.getId(), new PacketPlayOutBoss.AddOperation(bar));
    }

    public static PacketPlayOutBoss createRemovePacket(UUID uuid) {
        return new PacketPlayOutBoss(uuid, REMOVE_OPERATION);
    }

    public static PacketPlayOutBoss createUpdateProgressPacket(BossBattle bar) {
        return new PacketPlayOutBoss(bar.getId(), new PacketPlayOutBoss.UpdateProgressOperation(bar.getProgress()));
    }

    public static PacketPlayOutBoss createUpdateNamePacket(BossBattle bar) {
        return new PacketPlayOutBoss(bar.getId(), new PacketPlayOutBoss.UpdateNameOperation(bar.getName()));
    }

    public static PacketPlayOutBoss createUpdateStylePacket(BossBattle bar) {
        return new PacketPlayOutBoss(bar.getId(), new PacketPlayOutBoss.UpdateStyleOperation(bar.getColor(), bar.getOverlay()));
    }

    public static PacketPlayOutBoss createUpdatePropertiesPacket(BossBattle bar) {
        return new PacketPlayOutBoss(bar.getId(), new PacketPlayOutBoss.UpdatePropertiesOperation(bar.isDarkenSky(), bar.isPlayMusic(), bar.isCreateFog()));
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUUID(this.id);
        buf.writeEnum(this.operation.getType());
        this.operation.write(buf);
    }

    static int encodeProperties(boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        int i = 0;
        if (darkenSky) {
            i |= 1;
        }

        if (dragonMusic) {
            i |= 2;
        }

        if (thickenFog) {
            i |= 4;
        }

        return i;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBossUpdate(this);
    }

    public void dispatch(PacketPlayOutBoss.Handler consumer) {
        this.operation.dispatch(this.id, consumer);
    }

    interface Action {
        PacketPlayOutBoss.OperationType getType();

        void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer);

        void write(PacketDataSerializer buf);
    }

    static class AddOperation implements PacketPlayOutBoss.Action {
        private final IChatBaseComponent name;
        private final float progress;
        private final BossBattle.BarColor color;
        private final BossBattle.BarStyle overlay;
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        AddOperation(BossBattle bar) {
            this.name = bar.getName();
            this.progress = bar.getProgress();
            this.color = bar.getColor();
            this.overlay = bar.getOverlay();
            this.darkenScreen = bar.isDarkenSky();
            this.playMusic = bar.isPlayMusic();
            this.createWorldFog = bar.isCreateFog();
        }

        private AddOperation(PacketDataSerializer buf) {
            this.name = buf.readComponent();
            this.progress = buf.readFloat();
            this.color = buf.readEnum(BossBattle.BarColor.class);
            this.overlay = buf.readEnum(BossBattle.BarStyle.class);
            int i = buf.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.ADD;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.add(uuid, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeComponent(this.name);
            buf.writeFloat(this.progress);
            buf.writeEnum(this.color);
            buf.writeEnum(this.overlay);
            buf.writeByte(PacketPlayOutBoss.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    public interface Handler {
        default void add(UUID uuid, IChatBaseComponent name, float percent, BossBattle.BarColor color, BossBattle.BarStyle style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        }

        default void remove(UUID uuid) {
        }

        default void updateProgress(UUID uuid, float percent) {
        }

        default void updateName(UUID uuid, IChatBaseComponent name) {
        }

        default void updateStyle(UUID id, BossBattle.BarColor color, BossBattle.BarStyle style) {
        }

        default void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        }
    }

    static enum OperationType {
        ADD(PacketPlayOutBoss.AddOperation::new),
        REMOVE((buf) -> {
            return PacketPlayOutBoss.REMOVE_OPERATION;
        }),
        UPDATE_PROGRESS(PacketPlayOutBoss.UpdateProgressOperation::new),
        UPDATE_NAME(PacketPlayOutBoss.UpdateNameOperation::new),
        UPDATE_STYLE(PacketPlayOutBoss.UpdateStyleOperation::new),
        UPDATE_PROPERTIES(PacketPlayOutBoss.UpdatePropertiesOperation::new);

        final Function<PacketDataSerializer, PacketPlayOutBoss.Action> reader;

        private OperationType(Function<PacketDataSerializer, PacketPlayOutBoss.Action> parser) {
            this.reader = parser;
        }
    }

    static class UpdateNameOperation implements PacketPlayOutBoss.Action {
        private final IChatBaseComponent name;

        UpdateNameOperation(IChatBaseComponent name) {
            this.name = name;
        }

        private UpdateNameOperation(PacketDataSerializer buf) {
            this.name = buf.readComponent();
        }

        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.UPDATE_NAME;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.updateName(uuid, this.name);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeComponent(this.name);
        }
    }

    static class UpdateProgressOperation implements PacketPlayOutBoss.Action {
        private final float progress;

        UpdateProgressOperation(float percent) {
            this.progress = percent;
        }

        private UpdateProgressOperation(PacketDataSerializer buf) {
            this.progress = buf.readFloat();
        }

        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.UPDATE_PROGRESS;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.updateProgress(uuid, this.progress);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeFloat(this.progress);
        }
    }

    static class UpdatePropertiesOperation implements PacketPlayOutBoss.Action {
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        UpdatePropertiesOperation(boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
            this.darkenScreen = darkenSky;
            this.playMusic = dragonMusic;
            this.createWorldFog = thickenFog;
        }

        private UpdatePropertiesOperation(PacketDataSerializer buf) {
            int i = buf.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.UPDATE_PROPERTIES;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.updateProperties(uuid, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeByte(PacketPlayOutBoss.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    static class UpdateStyleOperation implements PacketPlayOutBoss.Action {
        private final BossBattle.BarColor color;
        private final BossBattle.BarStyle overlay;

        UpdateStyleOperation(BossBattle.BarColor color, BossBattle.BarStyle style) {
            this.color = color;
            this.overlay = style;
        }

        private UpdateStyleOperation(PacketDataSerializer buf) {
            this.color = buf.readEnum(BossBattle.BarColor.class);
            this.overlay = buf.readEnum(BossBattle.BarStyle.class);
        }

        @Override
        public PacketPlayOutBoss.OperationType getType() {
            return PacketPlayOutBoss.OperationType.UPDATE_STYLE;
        }

        @Override
        public void dispatch(UUID uuid, PacketPlayOutBoss.Handler consumer) {
            consumer.updateStyle(uuid, this.color, this.overlay);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeEnum(this.color);
            buf.writeEnum(this.overlay);
        }
    }
}
