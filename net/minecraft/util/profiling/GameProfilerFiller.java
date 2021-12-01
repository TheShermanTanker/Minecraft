package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;

public interface GameProfilerFiller {
    String ROOT = "root";

    void startTick();

    void endTick();

    void enter(String location);

    void push(Supplier<String> locationGetter);

    void exit();

    void exitEnter(String location);

    void popPush(Supplier<String> locationGetter);

    void markForCharting(EnumMetricCategory type);

    default void incrementCounter(String marker) {
        this.incrementCounter(marker, 1);
    }

    void incrementCounter(String marker, int i);

    default void incrementCounter(Supplier<String> markerGetter) {
        this.incrementCounter(markerGetter, 1);
    }

    void incrementCounter(Supplier<String> markerGetter, int i);

    static GameProfilerFiller tee(GameProfilerFiller a, GameProfilerFiller b) {
        if (a == GameProfilerDisabled.INSTANCE) {
            return b;
        } else {
            return b == GameProfilerDisabled.INSTANCE ? a : new GameProfilerFiller() {
                @Override
                public void startTick() {
                    a.startTick();
                    b.startTick();
                }

                @Override
                public void endTick() {
                    a.endTick();
                    b.endTick();
                }

                @Override
                public void enter(String location) {
                    a.enter(location);
                    b.enter(location);
                }

                @Override
                public void push(Supplier<String> locationGetter) {
                    a.push(locationGetter);
                    b.push(locationGetter);
                }

                @Override
                public void markForCharting(EnumMetricCategory type) {
                    a.markForCharting(type);
                    b.markForCharting(type);
                }

                @Override
                public void exit() {
                    a.exit();
                    b.exit();
                }

                @Override
                public void exitEnter(String location) {
                    a.exitEnter(location);
                    b.exitEnter(location);
                }

                @Override
                public void popPush(Supplier<String> locationGetter) {
                    a.popPush(locationGetter);
                    b.popPush(locationGetter);
                }

                @Override
                public void incrementCounter(String marker, int i) {
                    a.incrementCounter(marker, i);
                    b.incrementCounter(marker, i);
                }

                @Override
                public void incrementCounter(Supplier<String> markerGetter, int i) {
                    a.incrementCounter(markerGetter, i);
                    b.incrementCounter(markerGetter, i);
                }
            };
        }
    }
}
