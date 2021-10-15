package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Collection;

public class GameTestHarnessTicker {
    public static final GameTestHarnessTicker SINGLETON = new GameTestHarnessTicker();
    private final Collection<GameTestHarnessInfo> testInfos = Lists.newCopyOnWriteArrayList();

    public void add(GameTestHarnessInfo test) {
        this.testInfos.add(test);
    }

    public void clear() {
        this.testInfos.clear();
    }

    public void tick() {
        this.testInfos.forEach(GameTestHarnessInfo::tick);
        this.testInfos.removeIf(GameTestHarnessInfo::isDone);
    }
}
