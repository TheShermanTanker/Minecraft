package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class AttributeModifiable {
    private final AttributeBase attribute;
    private final Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation = Maps.newEnumMap(AttributeModifier.Operation.class);
    private final Map<UUID, AttributeModifier> modifierById = new Object2ObjectArrayMap<>();
    private final Set<AttributeModifier> permanentModifiers = new ObjectArraySet<>();
    private double baseValue;
    private boolean dirty = true;
    private double cachedValue;
    private final Consumer<AttributeModifiable> onDirty;

    public AttributeModifiable(AttributeBase type, Consumer<AttributeModifiable> updateCallback) {
        this.attribute = type;
        this.onDirty = updateCallback;
        this.baseValue = type.getDefault();
    }

    public AttributeBase getAttribute() {
        return this.attribute;
    }

    public double getBaseValue() {
        return this.baseValue;
    }

    public void setValue(double baseValue) {
        if (baseValue != this.baseValue) {
            this.baseValue = baseValue;
            this.setDirty();
        }
    }

    public Set<AttributeModifier> getModifiers(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.computeIfAbsent(operation, (operationx) -> {
            return Sets.newHashSet();
        });
    }

    public Set<AttributeModifier> getModifiers() {
        return ImmutableSet.copyOf(this.modifierById.values());
    }

    @Nullable
    public AttributeModifier getModifier(UUID uuid) {
        return this.modifierById.get(uuid);
    }

    public boolean hasModifier(AttributeModifier modifier) {
        return this.modifierById.get(modifier.getUniqueId()) != null;
    }

    private void addModifier(AttributeModifier modifier) {
        AttributeModifier attributeModifier = this.modifierById.putIfAbsent(modifier.getUniqueId(), modifier);
        if (attributeModifier != null) {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        } else {
            this.getModifiers(modifier.getOperation()).add(modifier);
            this.setDirty();
        }
    }

    public void addTransientModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
    }

    public void addPermanentModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
        this.permanentModifiers.add(modifier);
    }

    protected void setDirty() {
        this.dirty = true;
        this.onDirty.accept(this);
    }

    public void removeModifier(AttributeModifier modifier) {
        this.getModifiers(modifier.getOperation()).remove(modifier);
        this.modifierById.remove(modifier.getUniqueId());
        this.permanentModifiers.remove(modifier);
        this.setDirty();
    }

    public void removeModifier(UUID uuid) {
        AttributeModifier attributeModifier = this.getModifier(uuid);
        if (attributeModifier != null) {
            this.removeModifier(attributeModifier);
        }

    }

    public boolean removePermanentModifier(UUID uuid) {
        AttributeModifier attributeModifier = this.getModifier(uuid);
        if (attributeModifier != null && this.permanentModifiers.contains(attributeModifier)) {
            this.removeModifier(attributeModifier);
            return true;
        } else {
            return false;
        }
    }

    public void removeModifiers() {
        for(AttributeModifier attributeModifier : this.getModifiers()) {
            this.removeModifier(attributeModifier);
        }

    }

    public double getValue() {
        if (this.dirty) {
            this.cachedValue = this.calculateValue();
            this.dirty = false;
        }

        return this.cachedValue;
    }

    private double calculateValue() {
        double d = this.getBaseValue();

        for(AttributeModifier attributeModifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADDITION)) {
            d += attributeModifier.getAmount();
        }

        double e = d;

        for(AttributeModifier attributeModifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_BASE)) {
            e += d * attributeModifier2.getAmount();
        }

        for(AttributeModifier attributeModifier3 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            e *= 1.0D + attributeModifier3.getAmount();
        }

        return this.attribute.sanitizeValue(e);
    }

    private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.getOrDefault(operation, Collections.emptySet());
    }

    public void replaceFrom(AttributeModifiable other) {
        this.baseValue = other.baseValue;
        this.modifierById.clear();
        this.modifierById.putAll(other.modifierById);
        this.permanentModifiers.clear();
        this.permanentModifiers.addAll(other.permanentModifiers);
        this.modifiersByOperation.clear();
        other.modifiersByOperation.forEach((operation, modifiers) -> {
            this.getModifiers(operation).addAll(modifiers);
        });
        this.setDirty();
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", IRegistry.ATTRIBUTE.getKey(this.attribute).toString());
        compoundTag.setDouble("Base", this.baseValue);
        if (!this.permanentModifiers.isEmpty()) {
            NBTTagList listTag = new NBTTagList();

            for(AttributeModifier attributeModifier : this.permanentModifiers) {
                listTag.add(attributeModifier.save());
            }

            compoundTag.set("Modifiers", listTag);
        }

        return compoundTag;
    }

    public void load(NBTTagCompound nbt) {
        this.baseValue = nbt.getDouble("Base");
        if (nbt.hasKeyOfType("Modifiers", 9)) {
            NBTTagList listTag = nbt.getList("Modifiers", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                AttributeModifier attributeModifier = AttributeModifier.load(listTag.getCompound(i));
                if (attributeModifier != null) {
                    this.modifierById.put(attributeModifier.getUniqueId(), attributeModifier);
                    this.getModifiers(attributeModifier.getOperation()).add(attributeModifier);
                    this.permanentModifiers.add(attributeModifier);
                }
            }
        }

        this.setDirty();
    }
}
