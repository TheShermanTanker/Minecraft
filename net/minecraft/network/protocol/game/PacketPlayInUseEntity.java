package net.minecraft.network.protocol.game;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayInUseEntity implements Packet<PacketListenerPlayIn> {
    private final int entityId;
    private final PacketPlayInUseEntity.EnumEntityUseAction action;
    private final boolean usingSecondaryAction;
    static final PacketPlayInUseEntity.EnumEntityUseAction ATTACK_ACTION = new PacketPlayInUseEntity.EnumEntityUseAction() {
        @Override
        public PacketPlayInUseEntity.ActionType getType() {
            return PacketPlayInUseEntity.ActionType.ATTACK;
        }

        @Override
        public void dispatch(PacketPlayInUseEntity.Handler handler) {
            handler.onAttack();
        }

        @Override
        public void write(PacketDataSerializer buf) {
        }
    };

    private PacketPlayInUseEntity(int entityId, boolean playerSneaking, PacketPlayInUseEntity.EnumEntityUseAction type) {
        this.entityId = entityId;
        this.action = type;
        this.usingSecondaryAction = playerSneaking;
    }

    public static PacketPlayInUseEntity createAttackPacket(Entity entity, boolean playerSneaking) {
        return new PacketPlayInUseEntity(entity.getId(), playerSneaking, ATTACK_ACTION);
    }

    public static PacketPlayInUseEntity createInteractionPacket(Entity entity, boolean playerSneaking, EnumHand hand) {
        return new PacketPlayInUseEntity(entity.getId(), playerSneaking, new PacketPlayInUseEntity.InteractionAction(hand));
    }

    public static PacketPlayInUseEntity createInteractionPacket(Entity entity, boolean playerSneaking, EnumHand hand, Vec3D pos) {
        return new PacketPlayInUseEntity(entity.getId(), playerSneaking, new PacketPlayInUseEntity.InteractionAtLocationAction(hand, pos));
    }

    public PacketPlayInUseEntity(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        PacketPlayInUseEntity.ActionType actionType = buf.readEnum(PacketPlayInUseEntity.ActionType.class);
        this.action = actionType.reader.apply(buf);
        this.usingSecondaryAction = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeEnum(this.action.getType());
        this.action.write(buf);
        buf.writeBoolean(this.usingSecondaryAction);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleInteract(this);
    }

    @Nullable
    public Entity getTarget(WorldServer world) {
        return world.getEntityOrPart(this.entityId);
    }

    public boolean isUsingSecondaryAction() {
        return this.usingSecondaryAction;
    }

    public void dispatch(PacketPlayInUseEntity.Handler handler) {
        this.action.dispatch(handler);
    }

    public static enum ActionType {
        INTERACT(PacketPlayInUseEntity.InteractionAction::new),
        ATTACK((friendlyByteBuf) -> {
            return PacketPlayInUseEntity.ATTACK_ACTION;
        }),
        INTERACT_AT(PacketPlayInUseEntity.InteractionAtLocationAction::new);

        final Function<PacketDataSerializer, PacketPlayInUseEntity.EnumEntityUseAction> reader;

        private ActionType(Function<PacketDataSerializer, PacketPlayInUseEntity.EnumEntityUseAction> handlerGetter) {
            this.reader = handlerGetter;
        }
    }

    interface EnumEntityUseAction {
        PacketPlayInUseEntity.ActionType getType();

        void dispatch(PacketPlayInUseEntity.Handler handler);

        void write(PacketDataSerializer buf);
    }

    public interface Handler {
        void onInteraction(EnumHand hand);

        void onInteraction(EnumHand hand, Vec3D pos);

        void onAttack();
    }

    static class InteractionAction implements PacketPlayInUseEntity.EnumEntityUseAction {
        private final EnumHand hand;

        InteractionAction(EnumHand hand) {
            this.hand = hand;
        }

        private InteractionAction(PacketDataSerializer buf) {
            this.hand = buf.readEnum(EnumHand.class);
        }

        @Override
        public PacketPlayInUseEntity.ActionType getType() {
            return PacketPlayInUseEntity.ActionType.INTERACT;
        }

        @Override
        public void dispatch(PacketPlayInUseEntity.Handler handler) {
            handler.onInteraction(this.hand);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeEnum(this.hand);
        }
    }

    static class InteractionAtLocationAction implements PacketPlayInUseEntity.EnumEntityUseAction {
        private final EnumHand hand;
        private final Vec3D location;

        InteractionAtLocationAction(EnumHand hand, Vec3D pos) {
            this.hand = hand;
            this.location = pos;
        }

        private InteractionAtLocationAction(PacketDataSerializer buf) {
            this.location = new Vec3D((double)buf.readFloat(), (double)buf.readFloat(), (double)buf.readFloat());
            this.hand = buf.readEnum(EnumHand.class);
        }

        @Override
        public PacketPlayInUseEntity.ActionType getType() {
            return PacketPlayInUseEntity.ActionType.INTERACT_AT;
        }

        @Override
        public void dispatch(PacketPlayInUseEntity.Handler handler) {
            handler.onInteraction(this.hand, this.location);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeFloat((float)this.location.x);
            buf.writeFloat((float)this.location.y);
            buf.writeFloat((float)this.location.z);
            buf.writeEnum(this.hand);
        }
    }
}
