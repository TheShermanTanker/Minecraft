package net.minecraft.gametest.framework;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import net.minecraft.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TeamcityTestReporter implements GameTestHarnessITestReporter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Escaper ESCAPER = Escapers.builder().addEscape('\'', "|'").addEscape('\n', "|n").addEscape('\r', "|r").addEscape('|', "||").addEscape('[', "|[").addEscape(']', "|]").build();

    @Override
    public void onTestFailed(GameTestHarnessInfo test) {
        String string = ESCAPER.escape(test.getTestName());
        String string2 = ESCAPER.escape(test.getError().getMessage());
        String string3 = ESCAPER.escape(SystemUtils.describeError(test.getError()));
        LOGGER.info("##teamcity[testStarted name='{}']", (Object)string);
        if (test.isRequired()) {
            LOGGER.info("##teamcity[testFailed name='{}' message='{}' details='{}']", string, string2, string3);
        } else {
            LOGGER.info("##teamcity[testIgnored name='{}' message='{}' details='{}']", string, string2, string3);
        }

        LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", string, test.getRunTime());
    }

    @Override
    public void onTestSuccess(GameTestHarnessInfo test) {
        String string = ESCAPER.escape(test.getTestName());
        LOGGER.info("##teamcity[testStarted name='{}']", (Object)string);
        LOGGER.info("##teamcity[testFinished name='{}' duration='{}']", string, test.getRunTime());
    }
}
