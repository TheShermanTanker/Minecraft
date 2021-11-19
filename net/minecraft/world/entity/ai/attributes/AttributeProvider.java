package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;

public class AttributeProvider {
    public final Map<AttributeBase, AttributeModifiable> instances;

    public AttributeProvider(Map<AttributeBase, AttributeModifiable> instances) {
        this.instances = ImmutableMap.copyOf(instances);
    }

    private AttributeModifiable getAttributeInstance(AttributeBase attribute) {
        AttributeModifiable attributeInstance = this.instances.get(attribute);
        if (attributeInstance == null) {
            throw new IllegalArgumentException("Can't find attribute " + IRegistry.ATTRIBUTE.getKey(attribute));
        } else {
            return attributeInstance;
        }
    }

    public double getValue(AttributeBase attribute) {
        return this.getAttributeInstance(attribute).getValue();
    }

    public double getBaseValue(AttributeBase attribute) {
        return this.getAttributeInstance(attribute).getBaseValue();
    }

    public double getModifierValue(AttributeBase attribute, UUID uuid) {
        AttributeModifier attributeModifier = this.getAttributeInstance(attribute).getModifier(uuid);
        if (attributeModifier == null) {
            throw new IllegalArgumentException("Can't find modifier " + uuid + " on attribute " + IRegistry.ATTRIBUTE.getKey(attribute));
        } else {
            return attributeModifier.getAmount();
        }
    }

    @Nullable
    public AttributeModifiable createInstance(Consumer<AttributeModifiable> updateCallback, AttributeBase attribute) {
        AttributeModifiable attributeInstance = this.instances.get(attribute);
        if (attributeInstance == null) {
            return null;
        } else {
            AttributeModifiable attributeInstance2 = new AttributeModifiable(attribute, updateCallback);
            attributeInstance2.replaceFrom(attributeInstance);
            return attributeInstance2;
        }
    }

    public static AttributeProvider.Builder builder() {
        return new AttributeProvider.Builder();
    }

    public boolean hasAttribute(AttributeBase type) {
        return this.instances.containsKey(type);
    }

    public boolean hasModifier(AttributeBase type, UUID uuid) {
        AttributeModifiable attributeInstance = this.instances.get(type);
        return attributeInstance != null && attributeInstance.getModifier(uuid) != null;
    }

    public static class Builder {
        private final Map<AttributeBase, AttributeModifiable> builder = Maps.newHashMap();
        private boolean instanceFrozen;

        private AttributeModifiable create(AttributeBase attribute) {
            AttributeModifiable attributeInstance = new AttributeModifiable(attribute, (attributex) -> {
                if (this.instanceFrozen) {
                    throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + IRegistry.ATTRIBUTE.getKey(attribute));
                }
            });
            this.builder.put(attribute, attributeInstance);
            return attributeInstance;
        }

        public AttributeProvider.Builder add(AttributeBase attribute) {
            this.create(attribute);
            return this;
        }

        public AttributeProvider.Builder add(AttributeBase attribute, double baseValue) {
            AttributeModifiable attributeInstance = this.create(attribute);
            attributeInstance.setValue(baseValue);
            return this;
        }

        public AttributeProvider build() {
            this.instanceFrozen = true;
            return new AttributeProvider(this.builder);
        }
    }
}
