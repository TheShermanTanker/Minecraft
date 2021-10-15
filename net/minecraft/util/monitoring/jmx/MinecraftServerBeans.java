package net.minecraft.util.monitoring.jmx;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MinecraftServerBeans implements DynamicMBean {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final MBeanInfo mBeanInfo;
    private final Map<String, MinecraftServerBeans.AttributeDescription> attributeDescriptionByName = Stream.of(new MinecraftServerBeans.AttributeDescription("tickTimes", this::getTickTimes, "Historical tick times (ms)", long[].class), new MinecraftServerBeans.AttributeDescription("averageTickTime", this::getAverageTickTime, "Current average tick time (ms)", Long.TYPE)).collect(Collectors.toMap((attributeDescription) -> {
        return attributeDescription.name;
    }, Function.identity()));

    private MinecraftServerBeans(MinecraftServer server) {
        this.server = server;
        MBeanAttributeInfo[] mBeanAttributeInfos = this.attributeDescriptionByName.values().stream().map(MinecraftServerBeans.AttributeDescription::asMBeanAttributeInfo).toArray((i) -> {
            return new MBeanAttributeInfo[i];
        });
        this.mBeanInfo = new MBeanInfo(MinecraftServerBeans.class.getSimpleName(), "metrics for dedicated server", mBeanAttributeInfos, (MBeanConstructorInfo[])null, (MBeanOperationInfo[])null, new MBeanNotificationInfo[0]);
    }

    public static void registerJmxMonitoring(MinecraftServer server) {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new MinecraftServerBeans(server), new ObjectName("net.minecraft.server:type=Server"));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException var2) {
            LOGGER.warn("Failed to initialise server as JMX bean", (Throwable)var2);
        }

    }

    private float getAverageTickTime() {
        return this.server.getAverageTickTime();
    }

    private long[] getTickTimes() {
        return this.server.tickTimes;
    }

    @Nullable
    @Override
    public Object getAttribute(String string) {
        MinecraftServerBeans.AttributeDescription attributeDescription = this.attributeDescriptionByName.get(string);
        return attributeDescription == null ? null : attributeDescription.getter.get();
    }

    @Override
    public void setAttribute(Attribute attribute) {
    }

    @Override
    public AttributeList getAttributes(String[] strings) {
        List<Attribute> list = Arrays.stream(strings).map(this.attributeDescriptionByName::get).filter(Objects::nonNull).map((attributeDescription) -> {
            return new Attribute(attributeDescription.name, attributeDescription.getter.get());
        }).collect(Collectors.toList());
        return new AttributeList(list);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributeList) {
        return new AttributeList();
    }

    @Nullable
    @Override
    public Object invoke(String string, Object[] objects, String[] strings) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return this.mBeanInfo;
    }

    static final class AttributeDescription {
        final String name;
        final Supplier<Object> getter;
        private final String description;
        private final Class<?> type;

        AttributeDescription(String name, Supplier<Object> getter, String description, Class<?> type) {
            this.name = name;
            this.getter = getter;
            this.description = description;
            this.type = type;
        }

        private MBeanAttributeInfo asMBeanAttributeInfo() {
            return new MBeanAttributeInfo(this.name, this.type.getSimpleName(), this.description, true, false, false);
        }
    }
}
