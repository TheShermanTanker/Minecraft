package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.ArgumentParticle;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.material.EnumPistonReaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityAreaEffectCloud extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TIME_BETWEEN_APPLICATIONS = 5;
    private static final DataWatcherObject<Float> DATA_RADIUS = DataWatcher.defineId(EntityAreaEffectCloud.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_COLOR = DataWatcher.defineId(EntityAreaEffectCloud.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_WAITING = DataWatcher.defineId(EntityAreaEffectCloud.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<ParticleParam> DATA_PARTICLE = DataWatcher.defineId(EntityAreaEffectCloud.class, DataWatcherRegistry.PARTICLE);
    private static final float MAX_RADIUS = 32.0F;
    private PotionRegistry potion = Potions.EMPTY;
    public List<MobEffect> effects = Lists.newArrayList();
    private final Map<Entity, Integer> victims = Maps.newHashMap();
    private int duration = 600;
    public int waitTime = 20;
    public int reapplicationDelay = 20;
    private boolean fixedColor;
    public int durationOnUse;
    public float radiusOnUse;
    public float radiusPerTick;
    @Nullable
    private EntityLiving owner;
    @Nullable
    private UUID ownerUUID;

    public EntityAreaEffectCloud(EntityTypes<? extends EntityAreaEffectCloud> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.setRadius(3.0F);
    }

    public EntityAreaEffectCloud(World world, double x, double y, double z) {
        this(EntityTypes.AREA_EFFECT_CLOUD, world);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_COLOR, 0);
        this.getDataWatcher().register(DATA_RADIUS, 0.5F);
        this.getDataWatcher().register(DATA_WAITING, false);
        this.getDataWatcher().register(DATA_PARTICLE, Particles.ENTITY_EFFECT);
    }

    public void setRadius(float radius) {
        if (!this.level.isClientSide) {
            this.getDataWatcher().set(DATA_RADIUS, MathHelper.clamp(radius, 0.0F, 32.0F));
        }

    }

    @Override
    public void updateSize() {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        super.updateSize();
        this.setPosition(d, e, f);
    }

    public float getRadius() {
        return this.getDataWatcher().get(DATA_RADIUS);
    }

    public void setPotion(PotionRegistry potion) {
        this.potion = potion;
        if (!this.fixedColor) {
            this.updateColor();
        }

    }

    private void updateColor() {
        if (this.potion == Potions.EMPTY && this.effects.isEmpty()) {
            this.getDataWatcher().set(DATA_COLOR, 0);
        } else {
            this.getDataWatcher().set(DATA_COLOR, PotionUtil.getColor(PotionUtil.getAllEffects(this.potion, this.effects)));
        }

    }

    public void addEffect(MobEffect effect) {
        this.effects.add(effect);
        if (!this.fixedColor) {
            this.updateColor();
        }

    }

    public int getColor() {
        return this.getDataWatcher().get(DATA_COLOR);
    }

    public void setColor(int rgb) {
        this.fixedColor = true;
        this.getDataWatcher().set(DATA_COLOR, rgb);
    }

    public ParticleParam getParticle() {
        return this.getDataWatcher().get(DATA_PARTICLE);
    }

    public void setParticle(ParticleParam particle) {
        this.getDataWatcher().set(DATA_PARTICLE, particle);
    }

    protected void setWaiting(boolean waiting) {
        this.getDataWatcher().set(DATA_WAITING, waiting);
    }

    public boolean isWaiting() {
        return this.getDataWatcher().get(DATA_WAITING);
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public void tick() {
        super.tick();
        boolean bl = this.isWaiting();
        float f = this.getRadius();
        if (this.level.isClientSide) {
            if (bl && this.random.nextBoolean()) {
                return;
            }

            ParticleParam particleOptions = this.getParticle();
            int i;
            float g;
            if (bl) {
                i = 2;
                g = 0.2F;
            } else {
                i = MathHelper.ceil((float)Math.PI * f * f);
                g = f;
            }

            for(int k = 0; k < i; ++k) {
                float l = this.random.nextFloat() * ((float)Math.PI * 2F);
                float m = MathHelper.sqrt(this.random.nextFloat()) * g;
                double d = this.locX() + (double)(MathHelper.cos(l) * m);
                double e = this.locY();
                double n = this.locZ() + (double)(MathHelper.sin(l) * m);
                double s;
                double t;
                double u;
                if (particleOptions.getParticle() != Particles.ENTITY_EFFECT) {
                    if (bl) {
                        s = 0.0D;
                        t = 0.0D;
                        u = 0.0D;
                    } else {
                        s = (0.5D - this.random.nextDouble()) * 0.15D;
                        t = (double)0.01F;
                        u = (0.5D - this.random.nextDouble()) * 0.15D;
                    }
                } else {
                    int o = bl && this.random.nextBoolean() ? 16777215 : this.getColor();
                    s = (double)((float)(o >> 16 & 255) / 255.0F);
                    t = (double)((float)(o >> 8 & 255) / 255.0F);
                    u = (double)((float)(o & 255) / 255.0F);
                }

                this.level.addAlwaysVisibleParticle(particleOptions, d, e, n, s, t, u);
            }
        } else {
            if (this.tickCount >= this.waitTime + this.duration) {
                this.die();
                return;
            }

            boolean bl2 = this.tickCount < this.waitTime;
            if (bl != bl2) {
                this.setWaiting(bl2);
            }

            if (bl2) {
                return;
            }

            if (this.radiusPerTick != 0.0F) {
                f += this.radiusPerTick;
                if (f < 0.5F) {
                    this.die();
                    return;
                }

                this.setRadius(f);
            }

            if (this.tickCount % 5 == 0) {
                this.victims.entrySet().removeIf((entry) -> {
                    return this.tickCount >= entry.getValue();
                });
                List<MobEffect> list = Lists.newArrayList();

                for(MobEffect mobEffectInstance : this.potion.getEffects()) {
                    list.add(new MobEffect(mobEffectInstance.getMobEffect(), mobEffectInstance.getDuration() / 4, mobEffectInstance.getAmplifier(), mobEffectInstance.isAmbient(), mobEffectInstance.isShowParticles()));
                }

                list.addAll(this.effects);
                if (list.isEmpty()) {
                    this.victims.clear();
                } else {
                    List<EntityLiving> list2 = this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox());
                    if (!list2.isEmpty()) {
                        for(EntityLiving livingEntity : list2) {
                            if (!this.victims.containsKey(livingEntity) && livingEntity.isAffectedByPotions()) {
                                double y = livingEntity.locX() - this.locX();
                                double z = livingEntity.locZ() - this.locZ();
                                double aa = y * y + z * z;
                                if (aa <= (double)(f * f)) {
                                    this.victims.put(livingEntity, this.tickCount + this.reapplicationDelay);

                                    for(MobEffect mobEffectInstance2 : list) {
                                        if (mobEffectInstance2.getMobEffect().isInstant()) {
                                            mobEffectInstance2.getMobEffect().applyInstantEffect(this, this.getSource(), livingEntity, mobEffectInstance2.getAmplifier(), 0.5D);
                                        } else {
                                            livingEntity.addEffect(new MobEffect(mobEffectInstance2), this);
                                        }
                                    }

                                    if (this.radiusOnUse != 0.0F) {
                                        f += this.radiusOnUse;
                                        if (f < 0.5F) {
                                            this.die();
                                            return;
                                        }

                                        this.setRadius(f);
                                    }

                                    if (this.durationOnUse != 0) {
                                        this.duration += this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.die();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public float getRadiusOnUse() {
        return this.radiusOnUse;
    }

    public void setRadiusOnUse(float radius) {
        this.radiusOnUse = radius;
    }

    public float getRadiusPerTick() {
        return this.radiusPerTick;
    }

    public void setRadiusPerTick(float growth) {
        this.radiusPerTick = growth;
    }

    public int getDurationOnUse() {
        return this.durationOnUse;
    }

    public void setDurationOnUse(int durationOnUse) {
        this.durationOnUse = durationOnUse;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int ticks) {
        this.waitTime = ticks;
    }

    public void setSource(@Nullable EntityLiving owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUniqueID();
    }

    @Nullable
    public EntityLiving getSource() {
        if (this.owner == null && this.ownerUUID != null && this.level instanceof WorldServer) {
            Entity entity = ((WorldServer)this.level).getEntity(this.ownerUUID);
            if (entity instanceof EntityLiving) {
                this.owner = (EntityLiving)entity;
            }
        }

        return this.owner;
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        this.tickCount = nbt.getInt("Age");
        this.duration = nbt.getInt("Duration");
        this.waitTime = nbt.getInt("WaitTime");
        this.reapplicationDelay = nbt.getInt("ReapplicationDelay");
        this.durationOnUse = nbt.getInt("DurationOnUse");
        this.radiusOnUse = nbt.getFloat("RadiusOnUse");
        this.radiusPerTick = nbt.getFloat("RadiusPerTick");
        this.setRadius(nbt.getFloat("Radius"));
        if (nbt.hasUUID("Owner")) {
            this.ownerUUID = nbt.getUUID("Owner");
        }

        if (nbt.hasKeyOfType("Particle", 8)) {
            try {
                this.setParticle(ArgumentParticle.readParticle(new StringReader(nbt.getString("Particle"))));
            } catch (CommandSyntaxException var5) {
                LOGGER.warn("Couldn't load custom particle {}", nbt.getString("Particle"), var5);
            }
        }

        if (nbt.hasKeyOfType("Color", 99)) {
            this.setColor(nbt.getInt("Color"));
        }

        if (nbt.hasKeyOfType("Potion", 8)) {
            this.setPotion(PotionUtil.getPotion(nbt));
        }

        if (nbt.hasKeyOfType("Effects", 9)) {
            NBTTagList listTag = nbt.getList("Effects", 10);
            this.effects.clear();

            for(int i = 0; i < listTag.size(); ++i) {
                MobEffect mobEffectInstance = MobEffect.load(listTag.getCompound(i));
                if (mobEffectInstance != null) {
                    this.addEffect(mobEffectInstance);
                }
            }
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.setInt("Age", this.tickCount);
        nbt.setInt("Duration", this.duration);
        nbt.setInt("WaitTime", this.waitTime);
        nbt.setInt("ReapplicationDelay", this.reapplicationDelay);
        nbt.setInt("DurationOnUse", this.durationOnUse);
        nbt.setFloat("RadiusOnUse", this.radiusOnUse);
        nbt.setFloat("RadiusPerTick", this.radiusPerTick);
        nbt.setFloat("Radius", this.getRadius());
        nbt.setString("Particle", this.getParticle().writeToString());
        if (this.ownerUUID != null) {
            nbt.putUUID("Owner", this.ownerUUID);
        }

        if (this.fixedColor) {
            nbt.setInt("Color", this.getColor());
        }

        if (this.potion != Potions.EMPTY) {
            nbt.setString("Potion", IRegistry.POTION.getKey(this.potion).toString());
        }

        if (!this.effects.isEmpty()) {
            NBTTagList listTag = new NBTTagList();

            for(MobEffect mobEffectInstance : this.effects) {
                listTag.add(mobEffectInstance.save(new NBTTagCompound()));
            }

            nbt.set("Effects", listTag);
        }

    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_RADIUS.equals(data)) {
            this.updateSize();
        }

        super.onSyncedDataUpdated(data);
    }

    public PotionRegistry getPotion() {
        return this.potion;
    }

    @Override
    public EnumPistonReaction getPushReaction() {
        return EnumPistonReaction.IGNORE;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return EntitySize.scalable(this.getRadius() * 2.0F, 0.5F);
    }
}
