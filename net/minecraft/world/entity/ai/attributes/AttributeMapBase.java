package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttributeMapBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<AttributeBase, AttributeModifiable> attributes = Maps.newHashMap();
    private final Set<AttributeModifiable> dirtyAttributes = Sets.newHashSet();
    private final AttributeProvider supplier;

    public AttributeMapBase(AttributeProvider defaultAttributes) {
        this.supplier = defaultAttributes;
    }

    private void onAttributeModified(AttributeModifiable instance) {
        if (instance.getAttribute().isClientSyncable()) {
            this.dirtyAttributes.add(instance);
        }

    }

    public Set<AttributeModifiable> getAttributes() {
        return this.dirtyAttributes;
    }

    public Collection<AttributeModifiable> getSyncableAttributes() {
        return this.attributes.values().stream().filter((attribute) -> {
            return attribute.getAttribute().isClientSyncable();
        }).collect(Collectors.toList());
    }

    @Nullable
    public AttributeModifiable getInstance(AttributeBase attribute) {
        return this.attributes.computeIfAbsent(attribute, (attributex) -> {
            return this.supplier.createInstance(this::onAttributeModified, attributex);
        });
    }

    public boolean hasAttribute(AttributeBase attribute) {
        return this.attributes.get(attribute) != null || this.supplier.hasAttribute(attribute);
    }

    public boolean hasModifier(AttributeBase attribute, UUID uuid) {
        AttributeModifiable attributeInstance = this.attributes.get(attribute);
        return attributeInstance != null ? attributeInstance.getModifier(uuid) != null : this.supplier.hasModifier(attribute, uuid);
    }

    public double getValue(AttributeBase attribute) {
        AttributeModifiable attributeInstance = this.attributes.get(attribute);
        return attributeInstance != null ? attributeInstance.getValue() : this.supplier.getValue(attribute);
    }

    public double getBaseValue(AttributeBase attribute) {
        AttributeModifiable attributeInstance = this.attributes.get(attribute);
        return attributeInstance != null ? attributeInstance.getBaseValue() : this.supplier.getBaseValue(attribute);
    }

    public double getModifierValue(AttributeBase attribute, UUID uuid) {
        AttributeModifiable attributeInstance = this.attributes.get(attribute);
        return attributeInstance != null ? attributeInstance.getModifier(uuid).getAmount() : this.supplier.getModifierValue(attribute, uuid);
    }

    public void removeAttributeModifiers(Multimap<AttributeBase, AttributeModifier> attributeModifiers) {
        attributeModifiers.asMap().forEach((attribute, collection) -> {
            AttributeModifiable attributeInstance = this.attributes.get(attribute);
            if (attributeInstance != null) {
                collection.forEach(attributeInstance::removeModifier);
            }

        });
    }

    public void addTransientAttributeModifiers(Multimap<AttributeBase, AttributeModifier> attributeModifiers) {
        attributeModifiers.forEach((attribute, attributeModifier) -> {
            AttributeModifiable attributeInstance = this.getInstance(attribute);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(attributeModifier);
                attributeInstance.addTransientModifier(attributeModifier);
            }

        });
    }

    public void assignValues(AttributeMapBase other) {
        other.attributes.values().forEach((attributeInstance) -> {
            AttributeModifiable attributeInstance2 = this.getInstance(attributeInstance.getAttribute());
            if (attributeInstance2 != null) {
                attributeInstance2.replaceFrom(attributeInstance);
            }

        });
    }

    public NBTTagList save() {
        NBTTagList listTag = new NBTTagList();

        for(AttributeModifiable attributeInstance : this.attributes.values()) {
            listTag.add(attributeInstance.save());
        }

        return listTag;
    }

    public void load(NBTTagList nbt) {
        for(int i = 0; i < nbt.size(); ++i) {
            NBTTagCompound compoundTag = nbt.getCompound(i);
            String string = compoundTag.getString("Name");
            SystemUtils.ifElse(IRegistry.ATTRIBUTE.getOptional(MinecraftKey.tryParse(string)), (attribute) -> {
                AttributeModifiable attributeInstance = this.getInstance(attribute);
                if (attributeInstance != null) {
                    attributeInstance.load(compoundTag);
                }

            }, () -> {
                LOGGER.warn("Ignoring unknown attribute '{}'", (Object)string);
            });
        }

    }
}
