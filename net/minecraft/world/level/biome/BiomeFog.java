package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.INamable;

public class BiomeFog {
    public static final Codec<BiomeFog> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("fog_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.fogColor;
        }), Codec.INT.fieldOf("water_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.waterColor;
        }), Codec.INT.fieldOf("water_fog_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.waterFogColor;
        }), Codec.INT.fieldOf("sky_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.skyColor;
        }), Codec.INT.optionalFieldOf("foliage_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.foliageColorOverride;
        }), Codec.INT.optionalFieldOf("grass_color").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.grassColorOverride;
        }), BiomeFog.GrassColor.CODEC.optionalFieldOf("grass_color_modifier", BiomeFog.GrassColor.NONE).forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.grassColorModifier;
        }), BiomeParticles.CODEC.optionalFieldOf("particle").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.ambientParticleSettings;
        }), SoundEffect.CODEC.optionalFieldOf("ambient_sound").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.ambientLoopSoundEvent;
        }), CaveSoundSettings.CODEC.optionalFieldOf("mood_sound").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.ambientMoodSettings;
        }), CaveSound.CODEC.optionalFieldOf("additions_sound").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.ambientAdditionsSettings;
        }), Music.CODEC.optionalFieldOf("music").forGetter((biomeSpecialEffects) -> {
            return biomeSpecialEffects.backgroundMusic;
        })).apply(instance, BiomeFog::new);
    });
    private final int fogColor;
    private final int waterColor;
    private final int waterFogColor;
    private final int skyColor;
    private final Optional<Integer> foliageColorOverride;
    private final Optional<Integer> grassColorOverride;
    private final BiomeFog.GrassColor grassColorModifier;
    private final Optional<BiomeParticles> ambientParticleSettings;
    private final Optional<SoundEffect> ambientLoopSoundEvent;
    private final Optional<CaveSoundSettings> ambientMoodSettings;
    private final Optional<CaveSound> ambientAdditionsSettings;
    private final Optional<Music> backgroundMusic;

    BiomeFog(int fogColor, int waterColor, int waterFogColor, int skyColor, Optional<Integer> foliageColor, Optional<Integer> grassColor, BiomeFog.GrassColor grassColorModifier, Optional<BiomeParticles> particleConfig, Optional<SoundEffect> loopSound, Optional<CaveSoundSettings> moodSound, Optional<CaveSound> additionsSound, Optional<Music> music) {
        this.fogColor = fogColor;
        this.waterColor = waterColor;
        this.waterFogColor = waterFogColor;
        this.skyColor = skyColor;
        this.foliageColorOverride = foliageColor;
        this.grassColorOverride = grassColor;
        this.grassColorModifier = grassColorModifier;
        this.ambientParticleSettings = particleConfig;
        this.ambientLoopSoundEvent = loopSound;
        this.ambientMoodSettings = moodSound;
        this.ambientAdditionsSettings = additionsSound;
        this.backgroundMusic = music;
    }

    public int getFogColor() {
        return this.fogColor;
    }

    public int getWaterColor() {
        return this.waterColor;
    }

    public int getWaterFogColor() {
        return this.waterFogColor;
    }

    public int getSkyColor() {
        return this.skyColor;
    }

    public Optional<Integer> getFoliageColorOverride() {
        return this.foliageColorOverride;
    }

    public Optional<Integer> getGrassColorOverride() {
        return this.grassColorOverride;
    }

    public BiomeFog.GrassColor getGrassColorModifier() {
        return this.grassColorModifier;
    }

    public Optional<BiomeParticles> getAmbientParticleSettings() {
        return this.ambientParticleSettings;
    }

    public Optional<SoundEffect> getAmbientLoopSoundEvent() {
        return this.ambientLoopSoundEvent;
    }

    public Optional<CaveSoundSettings> getAmbientMoodSettings() {
        return this.ambientMoodSettings;
    }

    public Optional<CaveSound> getAmbientAdditionsSettings() {
        return this.ambientAdditionsSettings;
    }

    public Optional<Music> getBackgroundMusic() {
        return this.backgroundMusic;
    }

    public static class Builder {
        private OptionalInt fogColor = OptionalInt.empty();
        private OptionalInt waterColor = OptionalInt.empty();
        private OptionalInt waterFogColor = OptionalInt.empty();
        private OptionalInt skyColor = OptionalInt.empty();
        private Optional<Integer> foliageColorOverride = Optional.empty();
        private Optional<Integer> grassColorOverride = Optional.empty();
        private BiomeFog.GrassColor grassColorModifier = BiomeFog.GrassColor.NONE;
        private Optional<BiomeParticles> ambientParticle = Optional.empty();
        private Optional<SoundEffect> ambientLoopSoundEvent = Optional.empty();
        private Optional<CaveSoundSettings> ambientMoodSettings = Optional.empty();
        private Optional<CaveSound> ambientAdditionsSettings = Optional.empty();
        private Optional<Music> backgroundMusic = Optional.empty();

        public BiomeFog.Builder fogColor(int fogColor) {
            this.fogColor = OptionalInt.of(fogColor);
            return this;
        }

        public BiomeFog.Builder waterColor(int waterColor) {
            this.waterColor = OptionalInt.of(waterColor);
            return this;
        }

        public BiomeFog.Builder waterFogColor(int waterFogColor) {
            this.waterFogColor = OptionalInt.of(waterFogColor);
            return this;
        }

        public BiomeFog.Builder skyColor(int skyColor) {
            this.skyColor = OptionalInt.of(skyColor);
            return this;
        }

        public BiomeFog.Builder foliageColorOverride(int foliageColor) {
            this.foliageColorOverride = Optional.of(foliageColor);
            return this;
        }

        public BiomeFog.Builder grassColorOverride(int grassColor) {
            this.grassColorOverride = Optional.of(grassColor);
            return this;
        }

        public BiomeFog.Builder grassColorModifier(BiomeFog.GrassColor grassColorModifier) {
            this.grassColorModifier = grassColorModifier;
            return this;
        }

        public BiomeFog.Builder ambientParticle(BiomeParticles particleConfig) {
            this.ambientParticle = Optional.of(particleConfig);
            return this;
        }

        public BiomeFog.Builder ambientLoopSound(SoundEffect sound) {
            this.ambientLoopSoundEvent = Optional.of(sound);
            return this;
        }

        public BiomeFog.Builder ambientMoodSound(CaveSoundSettings moodSound) {
            this.ambientMoodSettings = Optional.of(moodSound);
            return this;
        }

        public BiomeFog.Builder ambientAdditionsSound(CaveSound additionsSound) {
            this.ambientAdditionsSettings = Optional.of(additionsSound);
            return this;
        }

        public BiomeFog.Builder backgroundMusic(Music music) {
            this.backgroundMusic = Optional.of(music);
            return this;
        }

        public BiomeFog build() {
            return new BiomeFog(this.fogColor.orElseThrow(() -> {
                return new IllegalStateException("Missing 'fog' color.");
            }), this.waterColor.orElseThrow(() -> {
                return new IllegalStateException("Missing 'water' color.");
            }), this.waterFogColor.orElseThrow(() -> {
                return new IllegalStateException("Missing 'water fog' color.");
            }), this.skyColor.orElseThrow(() -> {
                return new IllegalStateException("Missing 'sky' color.");
            }), this.foliageColorOverride, this.grassColorOverride, this.grassColorModifier, this.ambientParticle, this.ambientLoopSoundEvent, this.ambientMoodSettings, this.ambientAdditionsSettings, this.backgroundMusic);
        }
    }

    public static enum GrassColor implements INamable {
        NONE("none") {
            @Override
            public int modifyColor(double x, double z, int color) {
                return color;
            }
        },
        DARK_FOREST("dark_forest") {
            @Override
            public int modifyColor(double x, double z, int color) {
                return (color & 16711422) + 2634762 >> 1;
            }
        },
        SWAMP("swamp") {
            @Override
            public int modifyColor(double x, double z, int color) {
                double d = BiomeBase.BIOME_INFO_NOISE.getValue(x * 0.0225D, z * 0.0225D, false);
                return d < -0.1D ? 5011004 : 6975545;
            }
        };

        private final String name;
        public static final Codec<BiomeFog.GrassColor> CODEC = INamable.fromEnum(BiomeFog.GrassColor::values, BiomeFog.GrassColor::byName);
        private static final Map<String, BiomeFog.GrassColor> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(BiomeFog.GrassColor::getName, (grassColorModifier) -> {
            return grassColorModifier;
        }));

        public abstract int modifyColor(double x, double z, int color);

        GrassColor(String string2) {
            this.name = string2;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static BiomeFog.GrassColor byName(String name) {
            return BY_NAME.get(name);
        }
    }
}
