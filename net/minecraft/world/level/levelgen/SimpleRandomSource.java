package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Pair;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ThreadingDetector;

public class SimpleRandomSource implements RandomSource {
    private static final int MODULUS_BITS = 48;
    private static final long MODULUS_MASK = 281474976710655L;
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private static final float FLOAT_MULTIPLIER = 5.9604645E-8F;
    private static final double DOUBLE_MULTIPLIER = (double)1.110223E-16F;
    private final AtomicLong seed = new AtomicLong();
    private double nextNextGaussian;
    private boolean haveNextNextGaussian;

    public SimpleRandomSource(long seed) {
        this.setSeed(seed);
    }

    @Override
    public void setSeed(long seed) {
        if (!this.seed.compareAndSet(this.seed.get(), (seed ^ 25214903917L) & 281474976710655L)) {
            throw ThreadingDetector.makeThreadingException("SimpleRandomSource", (DebugBuffer<Pair<Thread, StackTraceElement[]>>)null);
        }
    }

    private int next(int bits) {
        long l = this.seed.get();
        long m = l * 25214903917L + 11L & 281474976710655L;
        if (!this.seed.compareAndSet(l, m)) {
            throw ThreadingDetector.makeThreadingException("SimpleRandomSource", (DebugBuffer<Pair<Thread, StackTraceElement[]>>)null);
        } else {
            return (int)(m >> 48 - bits);
        }
    }

    @Override
    public int nextInt() {
        return this.next(32);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        } else if ((bound & bound - 1) == 0) {
            return (int)((long)bound * (long)this.next(31) >> 31);
        } else {
            int i;
            int j;
            do {
                i = this.next(31);
                j = i % bound;
            } while(i - j + (bound - 1) < 0);

            return j;
        }
    }

    @Override
    public long nextLong() {
        int i = this.next(32);
        int j = this.next(32);
        long l = (long)i << 32;
        return l + (long)j;
    }

    @Override
    public boolean nextBoolean() {
        return this.next(1) != 0;
    }

    @Override
    public float nextFloat() {
        return (float)this.next(24) * 5.9604645E-8F;
    }

    @Override
    public double nextDouble() {
        int i = this.next(26);
        int j = this.next(27);
        long l = ((long)i << 27) + (long)j;
        return (double)l * (double)1.110223E-16F;
    }

    @Override
    public double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        } else {
            while(true) {
                double d = 2.0D * this.nextDouble() - 1.0D;
                double e = 2.0D * this.nextDouble() - 1.0D;
                double f = MathHelper.square(d) + MathHelper.square(e);
                if (!(f >= 1.0D)) {
                    if (f != 0.0D) {
                        double g = Math.sqrt(-2.0D * Math.log(f) / f);
                        this.nextNextGaussian = e * g;
                        this.haveNextNextGaussian = true;
                        return d * g;
                    }
                }
            }
        }
    }
}
