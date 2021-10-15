package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public class DataPackConfiguration {
    public static final DataPackConfiguration DEFAULT = new DataPackConfiguration(ImmutableList.of("vanilla"), ImmutableList.of());
    public static final Codec<DataPackConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.STRING.listOf().fieldOf("Enabled").forGetter((dataPackConfig) -> {
            return dataPackConfig.enabled;
        }), Codec.STRING.listOf().fieldOf("Disabled").forGetter((dataPackConfig) -> {
            return dataPackConfig.disabled;
        })).apply(instance, DataPackConfiguration::new);
    });
    private final List<String> enabled;
    private final List<String> disabled;

    public DataPackConfiguration(List<String> enabled, List<String> disabled) {
        this.enabled = ImmutableList.copyOf(enabled);
        this.disabled = ImmutableList.copyOf(disabled);
    }

    public List<String> getEnabled() {
        return this.enabled;
    }

    public List<String> getDisabled() {
        return this.disabled;
    }
}
