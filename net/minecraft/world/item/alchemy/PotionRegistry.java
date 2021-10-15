package net.minecraft.world.item.alchemy;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.effect.MobEffect;

public class PotionRegistry {
    private final String name;
    private final ImmutableList<MobEffect> effects;

    public static PotionRegistry byName(String id) {
        return IRegistry.POTION.get(MinecraftKey.tryParse(id));
    }

    public PotionRegistry(MobEffect... effects) {
        this((String)null, effects);
    }

    public PotionRegistry(@Nullable String baseName, MobEffect... effects) {
        this.name = baseName;
        this.effects = ImmutableList.copyOf(effects);
    }

    public String getName(String prefix) {
        return prefix + (this.name == null ? IRegistry.POTION.getKey(this).getKey() : this.name);
    }

    public List<MobEffect> getEffects() {
        return this.effects;
    }

    public boolean hasInstantEffects() {
        if (!this.effects.isEmpty()) {
            for(MobEffect mobEffectInstance : this.effects) {
                if (mobEffectInstance.getMobEffect().isInstant()) {
                    return true;
                }
            }
        }

        return false;
    }
}
