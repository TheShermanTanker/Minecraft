package net.minecraft.world.level.levelgen;

public interface RandomSource {
    void setSeed(long seed);

    int nextInt();

    int nextInt(int bound);

    long nextLong();

    boolean nextBoolean();

    float nextFloat();

    double nextDouble();

    double nextGaussian();

    default void consumeCount(int count) {
        for(int i = 0; i < count; ++i) {
            this.nextInt();
        }

    }
}
