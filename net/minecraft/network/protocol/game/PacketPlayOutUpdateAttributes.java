package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PacketPlayOutUpdateAttributes implements Packet<PacketListenerPlayOut> {
    private final int entityId;
    private final List<PacketPlayOutUpdateAttributes.AttributeSnapshot> attributes;

    public PacketPlayOutUpdateAttributes(int entityId, Collection<AttributeModifiable> attributes) {
        this.entityId = entityId;
        this.attributes = Lists.newArrayList();

        for(AttributeModifiable attributeInstance : attributes) {
            this.attributes.add(new PacketPlayOutUpdateAttributes.AttributeSnapshot(attributeInstance.getAttribute(), attributeInstance.getBaseValue(), attributeInstance.getModifiers()));
        }

    }

    public PacketPlayOutUpdateAttributes(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        this.attributes = buf.readList((bufx) -> {
            MinecraftKey resourceLocation = bufx.readResourceLocation();
            AttributeBase attribute = IRegistry.ATTRIBUTE.get(resourceLocation);
            double d = bufx.readDouble();
            List<AttributeModifier> list = bufx.readList((modifiers) -> {
                return new AttributeModifier(modifiers.readUUID(), "Unknown synced attribute modifier", modifiers.readDouble(), AttributeModifier.Operation.fromValue(modifiers.readByte()));
            });
            return new PacketPlayOutUpdateAttributes.AttributeSnapshot(attribute, d, list);
        });
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeCollection(this.attributes, (bufx, attribute) -> {
            bufx.writeResourceLocation(IRegistry.ATTRIBUTE.getKey(attribute.getAttribute()));
            bufx.writeDouble(attribute.getBase());
            bufx.writeCollection(attribute.getModifiers(), (buf, modifier) -> {
                buf.writeUUID(modifier.getUniqueId());
                buf.writeDouble(modifier.getAmount());
                buf.writeByte(modifier.getOperation().toValue());
            });
        });
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleUpdateAttributes(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public List<PacketPlayOutUpdateAttributes.AttributeSnapshot> getValues() {
        return this.attributes;
    }

    public static class AttributeSnapshot {
        private final AttributeBase attribute;
        private final double base;
        private final Collection<AttributeModifier> modifiers;

        public AttributeSnapshot(AttributeBase attribute, double baseValue, Collection<AttributeModifier> modifiers) {
            this.attribute = attribute;
            this.base = baseValue;
            this.modifiers = modifiers;
        }

        public AttributeBase getAttribute() {
            return this.attribute;
        }

        public double getBase() {
            return this.base;
        }

        public Collection<AttributeModifier> getModifiers() {
            return this.modifiers;
        }
    }
}
