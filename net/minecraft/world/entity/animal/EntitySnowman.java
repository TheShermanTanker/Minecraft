package net.minecraft.world.entity.animal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalArrowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.monster.IRangedEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

public class EntitySnowman extends EntityGolem implements IShearable, IRangedEntity {
    private static final DataWatcherObject<Byte> DATA_PUMPKIN_ID = DataWatcher.defineId(EntitySnowman.class, DataWatcherRegistry.BYTE);
    private static final byte PUMPKIN_FLAG = 16;
    private static final float EYE_HEIGHT = 1.7F;

    public EntitySnowman(EntityTypes<? extends EntitySnowman> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalArrowAttack(this, 1.25D, 20, 10.0F));
        this.goalSelector.addGoal(2, new PathfinderGoalRandomStrollLand(this, 1.0D, 1.0000001E-5F));
        this.goalSelector.addGoal(3, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(4, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityInsentient.class, 10, true, false, (livingEntity) -> {
            return livingEntity instanceof IMonster;
        }));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 4.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.2F);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_PUMPKIN_ID, (byte)16);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("Pumpkin", this.hasPumpkin());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("Pumpkin")) {
            this.setHasPumpkin(nbt.getBoolean("Pumpkin"));
        }

    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (!this.level.isClientSide) {
            int i = MathHelper.floor(this.locX());
            int j = MathHelper.floor(this.locY());
            int k = MathHelper.floor(this.locZ());
            if (this.level.getBiome(new BlockPosition(i, 0, k)).getAdjustedTemperature(new BlockPosition(i, j, k)) > 1.0F) {
                this.damageEntity(DamageSource.ON_FIRE, 1.0F);
            }

            if (!this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return;
            }

            IBlockData blockState = Blocks.SNOW.getBlockData();

            for(int l = 0; l < 4; ++l) {
                i = MathHelper.floor(this.locX() + (double)((float)(l % 2 * 2 - 1) * 0.25F));
                j = MathHelper.floor(this.locY());
                k = MathHelper.floor(this.locZ() + (double)((float)(l / 2 % 2 * 2 - 1) * 0.25F));
                BlockPosition blockPos = new BlockPosition(i, j, k);
                if (this.level.getType(blockPos).isAir() && this.level.getBiome(blockPos).getAdjustedTemperature(blockPos) < 0.8F && blockState.canPlace(this.level, blockPos)) {
                    this.level.setTypeUpdate(blockPos, blockState);
                }
            }
        }

    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        EntitySnowball snowball = new EntitySnowball(this.level, this);
        double d = target.getHeadY() - (double)1.1F;
        double e = target.locX() - this.locX();
        double f = d - snowball.locY();
        double g = target.locZ() - this.locZ();
        double h = Math.sqrt(e * e + g * g) * (double)0.2F;
        snowball.shoot(e, f + h, g, 1.6F, 12.0F);
        this.playSound(SoundEffects.SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addEntity(snowball);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 1.7F;
    }

    @Override
    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.SHEARS) && this.canShear()) {
            this.shear(EnumSoundCategory.PLAYERS);
            this.gameEvent(GameEvent.SHEAR, player);
            if (!this.level.isClientSide) {
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public void shear(EnumSoundCategory shearedSoundCategory) {
        this.level.playSound((EntityHuman)null, this, SoundEffects.SNOW_GOLEM_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        if (!this.level.isClientSide()) {
            this.setHasPumpkin(false);
            this.spawnAtLocation(new ItemStack(Items.CARVED_PUMPKIN), 1.7F);
        }

    }

    @Override
    public boolean canShear() {
        return this.isAlive() && this.hasPumpkin();
    }

    public boolean hasPumpkin() {
        return (this.entityData.get(DATA_PUMPKIN_ID) & 16) != 0;
    }

    public void setHasPumpkin(boolean hasPumpkin) {
        byte b = this.entityData.get(DATA_PUMPKIN_ID);
        if (hasPumpkin) {
            this.entityData.set(DATA_PUMPKIN_ID, (byte)(b | 16));
        } else {
            this.entityData.set(DATA_PUMPKIN_ID, (byte)(b & -17));
        }

    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SNOW_GOLEM_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SNOW_GOLEM_HURT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SNOW_GOLEM_DEATH;
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)(0.75F * this.getHeadHeight()), (double)(this.getWidth() * 0.4F));
    }
}
