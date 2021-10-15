package net.minecraft.world.entity.animal;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class EntityPufferFish extends EntityFish {
    private static final DataWatcherObject<Integer> PUFF_STATE = DataWatcher.defineId(EntityPufferFish.class, DataWatcherRegistry.INT);
    int inflateCounter;
    int deflateTimer;
    private static final Predicate<EntityLiving> SCARY_MOB = (entity) -> {
        if (entity instanceof EntityHuman && ((EntityHuman)entity).isCreative()) {
            return false;
        } else {
            return entity.getEntityType() == EntityTypes.AXOLOTL || entity.getMonsterType() != EnumMonsterType.WATER;
        }
    };
    static final PathfinderTargetCondition targetingConditions = PathfinderTargetCondition.forNonCombat().ignoreInvisibilityTesting().ignoreLineOfSight().selector(SCARY_MOB);
    public static final int STATE_SMALL = 0;
    public static final int STATE_MID = 1;
    public static final int STATE_FULL = 2;

    public EntityPufferFish(EntityTypes<? extends EntityPufferFish> type, World world) {
        super(type, world);
        this.updateSize();
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(PUFF_STATE, 0);
    }

    public int getPuffState() {
        return this.entityData.get(PUFF_STATE);
    }

    public void setPuffState(int puffState) {
        this.entityData.set(PUFF_STATE, puffState);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (PUFF_STATE.equals(data)) {
            this.updateSize();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("PuffState", this.getPuffState());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setPuffState(nbt.getInt("PuffState"));
    }

    @Override
    public ItemStack getBucketItem() {
        return new ItemStack(Items.PUFFERFISH_BUCKET);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(1, new EntityPufferFish.PufferfishPuffGoal(this));
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide && this.isAlive() && this.doAITick()) {
            if (this.inflateCounter > 0) {
                if (this.getPuffState() == 0) {
                    this.playSound(SoundEffects.PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getVoicePitch());
                    this.setPuffState(1);
                } else if (this.inflateCounter > 40 && this.getPuffState() == 1) {
                    this.playSound(SoundEffects.PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getVoicePitch());
                    this.setPuffState(2);
                }

                ++this.inflateCounter;
            } else if (this.getPuffState() != 0) {
                if (this.deflateTimer > 60 && this.getPuffState() == 2) {
                    this.playSound(SoundEffects.PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getVoicePitch());
                    this.setPuffState(1);
                } else if (this.deflateTimer > 100 && this.getPuffState() == 1) {
                    this.playSound(SoundEffects.PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getVoicePitch());
                    this.setPuffState(0);
                }

                ++this.deflateTimer;
            }
        }

        super.tick();
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.isAlive() && this.getPuffState() > 0) {
            for(EntityInsentient mob : this.level.getEntitiesOfClass(EntityInsentient.class, this.getBoundingBox().inflate(0.3D), (entity) -> {
                return targetingConditions.test(this, entity);
            })) {
                if (mob.isAlive()) {
                    this.touch(mob);
                }
            }
        }

    }

    private void touch(EntityInsentient mob) {
        int i = this.getPuffState();
        if (mob.damageEntity(DamageSource.mobAttack(this), (float)(1 + i))) {
            mob.addEffect(new MobEffect(MobEffects.POISON, 60 * i, 0), this);
            this.playSound(SoundEffects.PUFFER_FISH_STING, 1.0F, 1.0F);
        }

    }

    @Override
    public void pickup(EntityHuman player) {
        int i = this.getPuffState();
        if (player instanceof EntityPlayer && i > 0 && player.damageEntity(DamageSource.mobAttack(this), (float)(1 + i))) {
            if (!this.isSilent()) {
                ((EntityPlayer)player).connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.PUFFER_FISH_STING, 0.0F));
            }

            player.addEffect(new MobEffect(MobEffects.POISON, 60 * i, 0), this);
        }

    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.PUFFER_FISH_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PUFFER_FISH_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PUFFER_FISH_HURT;
    }

    @Override
    protected SoundEffect getSoundFlop() {
        return SoundEffects.PUFFER_FISH_FLOP;
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return super.getDimensions(pose).scale(getScale(this.getPuffState()));
    }

    private static float getScale(int puffState) {
        switch(puffState) {
        case 0:
            return 0.5F;
        case 1:
            return 0.7F;
        default:
            return 1.0F;
        }
    }

    static class PufferfishPuffGoal extends PathfinderGoal {
        private final EntityPufferFish fish;

        public PufferfishPuffGoal(EntityPufferFish pufferfish) {
            this.fish = pufferfish;
        }

        @Override
        public boolean canUse() {
            List<EntityLiving> list = this.fish.level.getEntitiesOfClass(EntityLiving.class, this.fish.getBoundingBox().inflate(2.0D), (livingEntity) -> {
                return EntityPufferFish.targetingConditions.test(this.fish, livingEntity);
            });
            return !list.isEmpty();
        }

        @Override
        public void start() {
            this.fish.inflateCounter = 1;
            this.fish.deflateTimer = 0;
        }

        @Override
        public void stop() {
            this.fish.inflateCounter = 0;
        }
    }
}
