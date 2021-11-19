package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.entity.EntityLiving;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MobEffect implements Comparable<MobEffect> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MobEffectBase effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean noCounter;
    private boolean visible;
    private boolean showIcon;
    @Nullable
    private MobEffect hiddenEffect;

    public MobEffect(MobEffectBase type) {
        this(type, 0, 0);
    }

    public MobEffect(MobEffectBase type, int duration) {
        this(type, duration, 0);
    }

    public MobEffect(MobEffectBase type, int duration, int amplifier) {
        this(type, duration, amplifier, false, true);
    }

    public MobEffect(MobEffectBase type, int duration, int amplifier, boolean ambient, boolean visible) {
        this(type, duration, amplifier, ambient, visible, visible);
    }

    public MobEffect(MobEffectBase type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        this(type, duration, amplifier, ambient, showParticles, showIcon, (MobEffect)null);
    }

    public MobEffect(MobEffectBase type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon, @Nullable MobEffect hiddenEffect) {
        this.effect = type;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = showParticles;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
    }

    public MobEffect(MobEffect that) {
        this.effect = that.effect;
        this.setDetailsFrom(that);
    }

    void setDetailsFrom(MobEffect that) {
        this.duration = that.duration;
        this.amplifier = that.amplifier;
        this.ambient = that.ambient;
        this.visible = that.visible;
        this.showIcon = that.showIcon;
    }

    public boolean update(MobEffect that) {
        if (this.effect != that.effect) {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        boolean bl = false;
        if (that.amplifier > this.amplifier) {
            if (that.duration < this.duration) {
                MobEffect mobEffectInstance = this.hiddenEffect;
                this.hiddenEffect = new MobEffect(this);
                this.hiddenEffect.hiddenEffect = mobEffectInstance;
            }

            this.amplifier = that.amplifier;
            this.duration = that.duration;
            bl = true;
        } else if (that.duration > this.duration) {
            if (that.amplifier == this.amplifier) {
                this.duration = that.duration;
                bl = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffect(that);
            } else {
                this.hiddenEffect.update(that);
            }
        }

        if (!that.ambient && this.ambient || bl) {
            this.ambient = that.ambient;
            bl = true;
        }

        if (that.visible != this.visible) {
            this.visible = that.visible;
            bl = true;
        }

        if (that.showIcon != this.showIcon) {
            this.showIcon = that.showIcon;
            bl = true;
        }

        return bl;
    }

    public MobEffectBase getMobEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public boolean isAmbient() {
        return this.ambient;
    }

    public boolean isShowParticles() {
        return this.visible;
    }

    public boolean isShowIcon() {
        return this.showIcon;
    }

    public boolean tick(EntityLiving entity, Runnable overwriteCallback) {
        if (this.duration > 0) {
            if (this.effect.isDurationEffectTick(this.duration, this.amplifier)) {
                this.applyEffect(entity);
            }

            this.tickDownDuration();
            if (this.duration == 0 && this.hiddenEffect != null) {
                this.setDetailsFrom(this.hiddenEffect);
                this.hiddenEffect = this.hiddenEffect.hiddenEffect;
                overwriteCallback.run();
            }
        }

        return this.duration > 0;
    }

    private int tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }

        return --this.duration;
    }

    public void applyEffect(EntityLiving entity) {
        if (this.duration > 0) {
            this.effect.tick(entity, this.amplifier);
        }

    }

    public String getDescriptionId() {
        return this.effect.getDescriptionId();
    }

    @Override
    public String toString() {
        String string;
        if (this.amplifier > 0) {
            string = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.duration;
        } else {
            string = this.getDescriptionId() + ", Duration: " + this.duration;
        }

        if (!this.visible) {
            string = string + ", Particles: false";
        }

        if (!this.showIcon) {
            string = string + ", Show Icon: false";
        }

        return string;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof MobEffect)) {
            return false;
        } else {
            MobEffect mobEffectInstance = (MobEffect)object;
            return this.duration == mobEffectInstance.duration && this.amplifier == mobEffectInstance.amplifier && this.ambient == mobEffectInstance.ambient && this.effect.equals(mobEffectInstance.effect);
        }
    }

    @Override
    public int hashCode() {
        int i = this.effect.hashCode();
        i = 31 * i + this.duration;
        i = 31 * i + this.amplifier;
        return 31 * i + (this.ambient ? 1 : 0);
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.setByte("Id", (byte)MobEffectBase.getId(this.getMobEffect()));
        this.writeDetailsTo(nbt);
        return nbt;
    }

    private void writeDetailsTo(NBTTagCompound nbt) {
        nbt.setByte("Amplifier", (byte)this.getAmplifier());
        nbt.setInt("Duration", this.getDuration());
        nbt.setBoolean("Ambient", this.isAmbient());
        nbt.setBoolean("ShowParticles", this.isShowParticles());
        nbt.setBoolean("ShowIcon", this.isShowIcon());
        if (this.hiddenEffect != null) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            this.hiddenEffect.save(compoundTag);
            nbt.set("HiddenEffect", compoundTag);
        }

    }

    @Nullable
    public static MobEffect load(NBTTagCompound nbt) {
        int i = nbt.getByte("Id");
        MobEffectBase mobEffect = MobEffectBase.fromId(i);
        return mobEffect == null ? null : loadSpecifiedEffect(mobEffect, nbt);
    }

    private static MobEffect loadSpecifiedEffect(MobEffectBase type, NBTTagCompound nbt) {
        int i = nbt.getByte("Amplifier");
        int j = nbt.getInt("Duration");
        boolean bl = nbt.getBoolean("Ambient");
        boolean bl2 = true;
        if (nbt.hasKeyOfType("ShowParticles", 1)) {
            bl2 = nbt.getBoolean("ShowParticles");
        }

        boolean bl3 = bl2;
        if (nbt.hasKeyOfType("ShowIcon", 1)) {
            bl3 = nbt.getBoolean("ShowIcon");
        }

        MobEffect mobEffectInstance = null;
        if (nbt.hasKeyOfType("HiddenEffect", 10)) {
            mobEffectInstance = loadSpecifiedEffect(type, nbt.getCompound("HiddenEffect"));
        }

        return new MobEffect(type, j, i < 0 ? 0 : i, bl, bl2, bl3, mobEffectInstance);
    }

    public void setNoCounter(boolean permanent) {
        this.noCounter = permanent;
    }

    public boolean isNoCounter() {
        return this.noCounter;
    }

    @Override
    public int compareTo(MobEffect mobEffectInstance) {
        int i = 32147;
        return (this.getDuration() <= 32147 || mobEffectInstance.getDuration() <= 32147) && (!this.isAmbient() || !mobEffectInstance.isAmbient()) ? ComparisonChain.start().compare(this.isAmbient(), mobEffectInstance.isAmbient()).compare(this.getDuration(), mobEffectInstance.getDuration()).compare(this.getMobEffect().getColor(), mobEffectInstance.getMobEffect().getColor()).result() : ComparisonChain.start().compare(this.isAmbient(), mobEffectInstance.isAmbient()).compare(this.getMobEffect().getColor(), mobEffectInstance.getMobEffect().getColor()).result();
    }
}
