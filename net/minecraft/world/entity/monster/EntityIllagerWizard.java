package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.level.World;

public abstract class EntityIllagerWizard extends EntityIllagerAbstract {
    private static final DataWatcherObject<Byte> DATA_SPELL_CASTING_ID = DataWatcher.defineId(EntityIllagerWizard.class, DataWatcherRegistry.BYTE);
    protected int spellCastingTickCount;
    private EntityIllagerWizard.Spell currentSpell = EntityIllagerWizard.Spell.NONE;

    protected EntityIllagerWizard(EntityTypes<? extends EntityIllagerWizard> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_SPELL_CASTING_ID, (byte)0);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.spellCastingTickCount = nbt.getInt("SpellTicks");
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("SpellTicks", this.spellCastingTickCount);
    }

    @Override
    public EntityIllagerAbstract.IllagerArmPose getArmPose() {
        if (this.isCastingSpell()) {
            return EntityIllagerAbstract.IllagerArmPose.SPELLCASTING;
        } else {
            return this.isCelebrating() ? EntityIllagerAbstract.IllagerArmPose.CELEBRATING : EntityIllagerAbstract.IllagerArmPose.CROSSED;
        }
    }

    public boolean isCastingSpell() {
        if (this.level.isClientSide) {
            return this.entityData.get(DATA_SPELL_CASTING_ID) > 0;
        } else {
            return this.spellCastingTickCount > 0;
        }
    }

    public void setSpell(EntityIllagerWizard.Spell spell) {
        this.currentSpell = spell;
        this.entityData.set(DATA_SPELL_CASTING_ID, (byte)spell.id);
    }

    public EntityIllagerWizard.Spell getSpell() {
        return !this.level.isClientSide ? this.currentSpell : EntityIllagerWizard.Spell.byId(this.entityData.get(DATA_SPELL_CASTING_ID));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.spellCastingTickCount > 0) {
            --this.spellCastingTickCount;
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide && this.isCastingSpell()) {
            EntityIllagerWizard.Spell illagerSpell = this.getSpell();
            double d = illagerSpell.spellColor[0];
            double e = illagerSpell.spellColor[1];
            double f = illagerSpell.spellColor[2];
            float g = this.yBodyRot * ((float)Math.PI / 180F) + MathHelper.cos((float)this.tickCount * 0.6662F) * 0.25F;
            float h = MathHelper.cos(g);
            float i = MathHelper.sin(g);
            this.level.addParticle(Particles.ENTITY_EFFECT, this.locX() + (double)h * 0.6D, this.locY() + 1.8D, this.locZ() + (double)i * 0.6D, d, e, f);
            this.level.addParticle(Particles.ENTITY_EFFECT, this.locX() - (double)h * 0.6D, this.locY() + 1.8D, this.locZ() - (double)i * 0.6D, d, e, f);
        }

    }

    protected int getSpellCastingTime() {
        return this.spellCastingTickCount;
    }

    protected abstract SoundEffect getSoundCastSpell();

    protected abstract class PathfinderGoalCastSpell extends PathfinderGoal {
        protected int attackWarmupDelay;
        protected int nextAttackTickCount;

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = EntityIllagerWizard.this.getGoalTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                if (EntityIllagerWizard.this.isCastingSpell()) {
                    return false;
                } else {
                    return EntityIllagerWizard.this.tickCount >= this.nextAttackTickCount;
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            EntityLiving livingEntity = EntityIllagerWizard.this.getGoalTarget();
            return livingEntity != null && livingEntity.isAlive() && this.attackWarmupDelay > 0;
        }

        @Override
        public void start() {
            this.attackWarmupDelay = this.getCastWarmupTime();
            EntityIllagerWizard.this.spellCastingTickCount = this.getCastingTime();
            this.nextAttackTickCount = EntityIllagerWizard.this.tickCount + this.getCastingInterval();
            SoundEffect soundEvent = this.getSpellPrepareSound();
            if (soundEvent != null) {
                EntityIllagerWizard.this.playSound(soundEvent, 1.0F, 1.0F);
            }

            EntityIllagerWizard.this.setSpell(this.getCastSpell());
        }

        @Override
        public void tick() {
            --this.attackWarmupDelay;
            if (this.attackWarmupDelay == 0) {
                this.performSpellCasting();
                EntityIllagerWizard.this.playSound(EntityIllagerWizard.this.getSoundCastSpell(), 1.0F, 1.0F);
            }

        }

        protected abstract void performSpellCasting();

        protected int getCastWarmupTime() {
            return 20;
        }

        protected abstract int getCastingTime();

        protected abstract int getCastingInterval();

        @Nullable
        protected abstract SoundEffect getSpellPrepareSound();

        protected abstract EntityIllagerWizard.Spell getCastSpell();
    }

    public static enum Spell {
        NONE(0, 0.0D, 0.0D, 0.0D),
        SUMMON_VEX(1, 0.7D, 0.7D, 0.8D),
        FANGS(2, 0.4D, 0.3D, 0.35D),
        WOLOLO(3, 0.7D, 0.5D, 0.2D),
        DISAPPEAR(4, 0.3D, 0.3D, 0.8D),
        BLINDNESS(5, 0.1D, 0.1D, 0.2D);

        final int id;
        final double[] spellColor;

        private Spell(int id, double particleVelocityX, double particleVelocityY, double particleVelocityZ) {
            this.id = id;
            this.spellColor = new double[]{particleVelocityX, particleVelocityY, particleVelocityZ};
        }

        public static EntityIllagerWizard.Spell byId(int id) {
            for(EntityIllagerWizard.Spell illagerSpell : values()) {
                if (id == illagerSpell.id) {
                    return illagerSpell;
                }
            }

            return NONE;
        }
    }

    protected class SpellcasterCastingSpellGoal extends PathfinderGoal {
        public SpellcasterCastingSpellGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            return EntityIllagerWizard.this.getSpellCastingTime() > 0;
        }

        @Override
        public void start() {
            super.start();
            EntityIllagerWizard.this.navigation.stop();
        }

        @Override
        public void stop() {
            super.stop();
            EntityIllagerWizard.this.setSpell(EntityIllagerWizard.Spell.NONE);
        }

        @Override
        public void tick() {
            if (EntityIllagerWizard.this.getGoalTarget() != null) {
                EntityIllagerWizard.this.getControllerLook().setLookAt(EntityIllagerWizard.this.getGoalTarget(), (float)EntityIllagerWizard.this.getMaxHeadYRot(), (float)EntityIllagerWizard.this.getMaxHeadXRot());
            }

        }
    }
}
