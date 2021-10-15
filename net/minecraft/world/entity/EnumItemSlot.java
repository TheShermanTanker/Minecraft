package net.minecraft.world.entity;

public enum EnumItemSlot {
    MAINHAND(EnumItemSlot.Function.HAND, 0, 0, "mainhand"),
    OFFHAND(EnumItemSlot.Function.HAND, 1, 5, "offhand"),
    FEET(EnumItemSlot.Function.ARMOR, 0, 1, "feet"),
    LEGS(EnumItemSlot.Function.ARMOR, 1, 2, "legs"),
    CHEST(EnumItemSlot.Function.ARMOR, 2, 3, "chest"),
    HEAD(EnumItemSlot.Function.ARMOR, 3, 4, "head");

    private final EnumItemSlot.Function type;
    private final int index;
    private final int filterFlag;
    private final String name;

    private EnumItemSlot(EnumItemSlot.Function type, int entityId, int armorStandId, String name) {
        this.type = type;
        this.index = entityId;
        this.filterFlag = armorStandId;
        this.name = name;
    }

    public EnumItemSlot.Function getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public int getIndex(int offset) {
        return offset + this.index;
    }

    public int getSlotFlag() {
        return this.filterFlag;
    }

    public String getSlotName() {
        return this.name;
    }

    public static EnumItemSlot fromName(String name) {
        for(EnumItemSlot equipmentSlot : values()) {
            if (equipmentSlot.getSlotName().equals(name)) {
                return equipmentSlot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + name + "'");
    }

    public static EnumItemSlot byTypeAndIndex(EnumItemSlot.Function type, int index) {
        for(EnumItemSlot equipmentSlot : values()) {
            if (equipmentSlot.getType() == type && equipmentSlot.getIndex() == index) {
                return equipmentSlot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + type + "': " + index);
    }

    public static enum Function {
        HAND,
        ARMOR;
    }
}
