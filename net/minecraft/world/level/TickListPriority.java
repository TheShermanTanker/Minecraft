package net.minecraft.world.level;

public enum TickListPriority {
    EXTREMELY_HIGH(-3),
    VERY_HIGH(-2),
    HIGH(-1),
    NORMAL(0),
    LOW(1),
    VERY_LOW(2),
    EXTREMELY_LOW(3);

    private final int value;

    private TickListPriority(int index) {
        this.value = index;
    }

    public static TickListPriority byValue(int index) {
        for(TickListPriority tickPriority : values()) {
            if (tickPriority.value == index) {
                return tickPriority;
            }
        }

        return index < EXTREMELY_HIGH.value ? EXTREMELY_HIGH : EXTREMELY_LOW;
    }

    public int getValue() {
        return this.value;
    }
}
