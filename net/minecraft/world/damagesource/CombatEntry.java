package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;

public class CombatEntry {
    private final DamageSource source;
    private final int time;
    private final float damage;
    private final float health;
    @Nullable
    private final String location;
    private final float fallDistance;

    public CombatEntry(DamageSource damageSource, int entityAge, float entityOriginalHealth, float damage, @Nullable String fallDeathSuffix, float fallDistance) {
        this.source = damageSource;
        this.time = entityAge;
        this.damage = damage;
        this.health = entityOriginalHealth;
        this.location = fallDeathSuffix;
        this.fallDistance = fallDistance;
    }

    public DamageSource getSource() {
        return this.source;
    }

    public int getTime() {
        return this.time;
    }

    public float getDamage() {
        return this.damage;
    }

    public float getHealthBeforeDamage() {
        return this.health;
    }

    public float getHealthAfterDamage() {
        return this.health - this.damage;
    }

    public boolean isCombatRelated() {
        return this.source.getEntity() instanceof EntityLiving;
    }

    @Nullable
    public String getLocation() {
        return this.location;
    }

    @Nullable
    public IChatBaseComponent getAttackerName() {
        return this.getSource().getEntity() == null ? null : this.getSource().getEntity().getScoreboardDisplayName();
    }

    @Nullable
    public Entity getAttacker() {
        return this.getSource().getEntity();
    }

    public float getFallDistance() {
        return this.source == DamageSource.OUT_OF_WORLD ? Float.MAX_VALUE : this.fallDistance;
    }
}
