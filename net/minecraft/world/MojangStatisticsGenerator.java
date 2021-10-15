package net.minecraft.world;

import com.google.common.collect.Maps;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;

public class MojangStatisticsGenerator {
    private static final String POLL_HOST = "http://snoop.minecraft.net/";
    private static final long DATA_SEND_FREQUENCY = 900000L;
    private static final int SNOOPER_VERSION = 2;
    final Map<String, Object> fixedData = Maps.newHashMap();
    final Map<String, Object> dynamicData = Maps.newHashMap();
    final String token = UUID.randomUUID().toString();
    final URL url;
    final IMojangStatistics populator;
    private final Timer timer = new Timer("Snooper Timer", true);
    final Object lock = new Object();
    private final long startupTime;
    private boolean started;
    int count;

    public MojangStatisticsGenerator(String urlPath, IMojangStatistics listener, long startTime) {
        try {
            this.url = new URL("http://snoop.minecraft.net/" + urlPath + "?version=2");
        } catch (MalformedURLException var6) {
            throw new IllegalArgumentException();
        }

        this.populator = listener;
        this.startupTime = startTime;
    }

    public void start() {
        if (!this.started) {
        }

    }

    private void populateFixedData() {
        this.setJvmArgs();
        this.setDynamicData("snooper_token", this.token);
        this.setFixedData("snooper_token", this.token);
        this.setFixedData("os_name", System.getProperty("os.name"));
        this.setFixedData("os_version", System.getProperty("os.version"));
        this.setFixedData("os_architecture", System.getProperty("os.arch"));
        this.setFixedData("java_version", System.getProperty("java.version"));
        this.setDynamicData("version", SharedConstants.getGameVersion().getId());
        this.populator.populateSnooperInitial(this);
    }

    private void setJvmArgs() {
        int[] is = new int[]{0};
        SystemUtils.getVmArguments().forEach((flag) -> {
            int var10004 = is[0];
            int var10001 = is[0];
            is[0] = var10004 + 1;
            this.setDynamicData("jvm_arg[" + var10001 + "]", flag);
        });
        this.setDynamicData("jvm_args", is[0]);
    }

    public void prepare() {
        this.setFixedData("memory_total", Runtime.getRuntime().totalMemory());
        this.setFixedData("memory_max", Runtime.getRuntime().maxMemory());
        this.setFixedData("memory_free", Runtime.getRuntime().freeMemory());
        this.setFixedData("cpu_cores", Runtime.getRuntime().availableProcessors());
        this.populator.populateSnooper(this);
    }

    public void setDynamicData(String key, Object value) {
        synchronized(this.lock) {
            this.dynamicData.put(key, value);
        }
    }

    public void setFixedData(String key, Object value) {
        synchronized(this.lock) {
            this.fixedData.put(key, value);
        }
    }

    public Map<String, String> getValues() {
        Map<String, String> map = Maps.newLinkedHashMap();
        synchronized(this.lock) {
            this.prepare();

            for(Entry<String, Object> entry : this.fixedData.entrySet()) {
                map.put(entry.getKey(), entry.getValue().toString());
            }

            for(Entry<String, Object> entry2 : this.dynamicData.entrySet()) {
                map.put(entry2.getKey(), entry2.getValue().toString());
            }

            return map;
        }
    }

    public boolean isStarted() {
        return this.started;
    }

    public void interrupt() {
        this.timer.cancel();
    }

    public String getToken() {
        return this.token;
    }

    public long getStartupTime() {
        return this.startupTime;
    }
}
