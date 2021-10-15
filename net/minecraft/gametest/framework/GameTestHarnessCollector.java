package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class GameTestHarnessCollector {
    private static final char NOT_STARTED_TEST_CHAR = ' ';
    private static final char ONGOING_TEST_CHAR = '_';
    private static final char SUCCESSFUL_TEST_CHAR = '+';
    private static final char FAILED_OPTIONAL_TEST_CHAR = 'x';
    private static final char FAILED_REQUIRED_TEST_CHAR = 'X';
    private final Collection<GameTestHarnessInfo> tests = Lists.newArrayList();
    @Nullable
    private final Collection<GameTestHarnessListener> listeners = Lists.newArrayList();

    public GameTestHarnessCollector() {
    }

    public GameTestHarnessCollector(Collection<GameTestHarnessInfo> tests) {
        this.tests.addAll(tests);
    }

    public void addTestToTrack(GameTestHarnessInfo test) {
        this.tests.add(test);
        this.listeners.forEach(test::addListener);
    }

    public void addListener(GameTestHarnessListener listener) {
        this.listeners.add(listener);
        this.tests.forEach((test) -> {
            test.addListener(listener);
        });
    }

    public void addFailureListener(Consumer<GameTestHarnessInfo> onFailed) {
        this.addListener(new GameTestHarnessListener() {
            @Override
            public void testStructureLoaded(GameTestHarnessInfo test) {
            }

            @Override
            public void testPassed(GameTestHarnessInfo test) {
            }

            @Override
            public void testFailed(GameTestHarnessInfo test) {
                onFailed.accept(test);
            }
        });
    }

    public int getFailedRequiredCount() {
        return (int)this.tests.stream().filter(GameTestHarnessInfo::hasFailed).filter(GameTestHarnessInfo::isRequired).count();
    }

    public int getFailedOptionalCount() {
        return (int)this.tests.stream().filter(GameTestHarnessInfo::hasFailed).filter(GameTestHarnessInfo::isOptional).count();
    }

    public int getDoneCount() {
        return (int)this.tests.stream().filter(GameTestHarnessInfo::isDone).count();
    }

    public boolean hasFailedRequired() {
        return this.getFailedRequiredCount() > 0;
    }

    public boolean hasFailedOptional() {
        return this.getFailedOptionalCount() > 0;
    }

    public Collection<GameTestHarnessInfo> getFailedRequired() {
        return this.tests.stream().filter(GameTestHarnessInfo::hasFailed).filter(GameTestHarnessInfo::isRequired).collect(Collectors.toList());
    }

    public Collection<GameTestHarnessInfo> getFailedOptional() {
        return this.tests.stream().filter(GameTestHarnessInfo::hasFailed).filter(GameTestHarnessInfo::isOptional).collect(Collectors.toList());
    }

    public int getTotalCount() {
        return this.tests.size();
    }

    public boolean isDone() {
        return this.getDoneCount() == this.getTotalCount();
    }

    public String getProgressBar() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('[');
        this.tests.forEach((test) -> {
            if (!test.hasStarted()) {
                stringBuffer.append(' ');
            } else if (test.hasSucceeded()) {
                stringBuffer.append('+');
            } else if (test.hasFailed()) {
                stringBuffer.append((char)(test.isRequired() ? 'X' : 'x'));
            } else {
                stringBuffer.append('_');
            }

        });
        stringBuffer.append(']');
        return stringBuffer.toString();
    }

    @Override
    public String toString() {
        return this.getProgressBar();
    }
}
