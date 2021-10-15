package net.minecraft.gametest.framework;

import net.minecraft.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameTestHarnessLogger implements GameTestHarnessITestReporter {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onTestFailed(GameTestHarnessInfo test) {
        if (test.isRequired()) {
            LOGGER.error("{} failed! {}", test.getTestName(), SystemUtils.describeError(test.getError()));
        } else {
            LOGGER.warn("(optional) {} failed. {}", test.getTestName(), SystemUtils.describeError(test.getError()));
        }

    }

    @Override
    public void onTestSuccess(GameTestHarnessInfo test) {
    }
}
