package net.minecraft.world.entity.ai.attributes;

public class AttributeBase {
    public static final int MAX_NAME_LENGTH = 64;
    private final double defaultValue;
    private boolean syncable;
    private final String descriptionId;

    protected AttributeBase(String translationKey, double fallback) {
        this.defaultValue = fallback;
        this.descriptionId = translationKey;
    }

    public double getDefault() {
        return this.defaultValue;
    }

    public boolean isClientSyncable() {
        return this.syncable;
    }

    public AttributeBase setSyncable(boolean tracked) {
        this.syncable = tracked;
        return this;
    }

    public double sanitizeValue(double value) {
        return value;
    }

    public String getName() {
        return this.descriptionId;
    }
}
