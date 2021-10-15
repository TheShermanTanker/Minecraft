package net.minecraft.world.entity.decoration;

import net.minecraft.core.IRegistry;

public class Paintings {
    public static final Paintings KEBAB = register("kebab", 16, 16);
    public static final Paintings AZTEC = register("aztec", 16, 16);
    public static final Paintings ALBAN = register("alban", 16, 16);
    public static final Paintings AZTEC2 = register("aztec2", 16, 16);
    public static final Paintings BOMB = register("bomb", 16, 16);
    public static final Paintings PLANT = register("plant", 16, 16);
    public static final Paintings WASTELAND = register("wasteland", 16, 16);
    public static final Paintings POOL = register("pool", 32, 16);
    public static final Paintings COURBET = register("courbet", 32, 16);
    public static final Paintings SEA = register("sea", 32, 16);
    public static final Paintings SUNSET = register("sunset", 32, 16);
    public static final Paintings CREEBET = register("creebet", 32, 16);
    public static final Paintings WANDERER = register("wanderer", 16, 32);
    public static final Paintings GRAHAM = register("graham", 16, 32);
    public static final Paintings MATCH = register("match", 32, 32);
    public static final Paintings BUST = register("bust", 32, 32);
    public static final Paintings STAGE = register("stage", 32, 32);
    public static final Paintings VOID = register("void", 32, 32);
    public static final Paintings SKULL_AND_ROSES = register("skull_and_roses", 32, 32);
    public static final Paintings WITHER = register("wither", 32, 32);
    public static final Paintings FIGHTERS = register("fighters", 64, 32);
    public static final Paintings POINTER = register("pointer", 64, 64);
    public static final Paintings PIGSCENE = register("pigscene", 64, 64);
    public static final Paintings BURNING_SKULL = register("burning_skull", 64, 64);
    public static final Paintings SKELETON = register("skeleton", 64, 48);
    public static final Paintings DONKEY_KONG = register("donkey_kong", 64, 48);
    private final int width;
    private final int height;

    private static Paintings register(String name, int width, int height) {
        return IRegistry.register(IRegistry.MOTIVE, name, new Paintings(width, height));
    }

    public Paintings(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
