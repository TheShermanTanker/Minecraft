package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityThrownTrident extends EntityArrow {
    private static final DataWatcherObject<Byte> ID_LOYALTY = DataWatcher.defineId(EntityThrownTrident.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Boolean> ID_FOIL = DataWatcher.defineId(EntityThrownTrident.class, DataWatcherRegistry.BOOLEAN);
    public ItemStack tridentItem = new ItemStack(Items.TRIDENT);
    private boolean dealtDamage;
    public int clientSideReturnTridentTickCount;

    public EntityThrownTrident(EntityTypes<? extends EntityThrownTrident> type, World world) {
        super(type, world);
    }

    public EntityThrownTrident(World world, EntityLiving owner, ItemStack stack) {
        super(EntityTypes.TRIDENT, owner, world);
        this.tridentItem = stack.cloneItemStack();
        this.entityData.set(ID_LOYALTY, (byte)EnchantmentManager.getLoyalty(stack));
        this.entityData.set(ID_FOIL, stack.hasFoil());
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(ID_LOYALTY, (byte)0);
        this.entityData.register(ID_FOIL, false);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity entity = this.getShooter();
        int i = this.entityData.get(ID_LOYALTY);
        if (i > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
            if (!this.isAcceptibleReturnOwner()) {
                if (!this.level.isClientSide && this.pickup == EntityArrow.PickupStatus.ALLOWED) {
                    this.spawnAtLocation(this.getItemStack(), 0.1F);
                }

                this.die();
            } else {
                this.setNoPhysics(true);
                Vec3D vec3 = entity.getEyePosition().subtract(this.getPositionVector());
                this.setPositionRaw(this.locX(), this.locY() + vec3.y * 0.015D * (double)i, this.locZ());
                if (this.level.isClientSide) {
                    this.yOld = this.locY();
                }

                double d = 0.05D * (double)i;
                this.setMot(this.getMot().scale(0.95D).add(vec3.normalize().scale(d)));
                if (this.clientSideReturnTridentTickCount == 0) {
                    this.playSound(SoundEffects.TRIDENT_RETURN, 10.0F, 1.0F);
                }

                ++this.clientSideReturnTridentTickCount;
            }
        }

        super.tick();
    }

    private boolean isAcceptibleReturnOwner() {
        Entity entity = this.getShooter();
        if (entity != null && entity.isAlive()) {
            return !(entity instanceof EntityPlayer) || !entity.isSpectator();
        } else {
            return false;
        }
    }

    @Override
    public ItemStack getItemStack() {
        return this.tridentItem.cloneItemStack();
    }

    public boolean isFoil() {
        return this.entityData.get(ID_FOIL);
    }

    @Nullable
    @Override
    protected MovingObjectPositionEntity findHitEntity(Vec3D currentPosition, Vec3D nextPosition) {
        return this.dealtDamage ? null : super.findHitEntity(currentPosition, nextPosition);
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        float f = 8.0F;
        if (entity instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)entity;
            f += EnchantmentManager.getDamageBonus(this.tridentItem, livingEntity.getMonsterType());
        }

        Entity entity2 = this.getShooter();
        DamageSource damageSource = DamageSource.trident(this, (Entity)(entity2 == null ? this : entity2));
        this.dealtDamage = true;
        SoundEffect soundEvent = SoundEffects.TRIDENT_HIT;
        if (entity.damageEntity(damageSource, f)) {
            if (entity.getEntityType() == EntityTypes.ENDERMAN) {
                return;
            }

            if (entity instanceof EntityLiving) {
                EntityLiving livingEntity2 = (EntityLiving)entity;
                if (entity2 instanceof EntityLiving) {
                    EnchantmentManager.doPostHurtEffects(livingEntity2, entity2);
                    EnchantmentManager.doPostDamageEffects((EntityLiving)entity2, livingEntity2);
                }

                this.doPostHurtEffects(livingEntity2);
            }
        }

        this.setMot(this.getMot().multiply(-0.01D, -0.1D, -0.01D));
        float g = 1.0F;
        if (this.level instanceof WorldServer && this.level.isThundering() && this.isChanneling()) {
            BlockPosition blockPos = entity.getChunkCoordinates();
            if (this.level.canSeeSky(blockPos)) {
                EntityLightning lightningBolt = EntityTypes.LIGHTNING_BOLT.create(this.level);
                lightningBolt.moveTo(Vec3D.atBottomCenterOf(blockPos));
                lightningBolt.setCause(entity2 instanceof EntityPlayer ? (EntityPlayer)entity2 : null);
                this.level.addEntity(lightningBolt);
                soundEvent = SoundEffects.TRIDENT_THUNDER;
                g = 5.0F;
            }
        }

        this.playSound(soundEvent, g, 1.0F);
    }

    public boolean isChanneling() {
        return EnchantmentManager.hasChanneling(this.tridentItem);
    }

    @Override
    protected boolean tryPickup(EntityHuman player) {
        return super.tryPickup(player) || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().pickup(this.getItemStack());
    }

    @Override
    protected SoundEffect getDefaultHitGroundSoundEvent() {
        return SoundEffects.TRIDENT_HIT_GROUND;
    }

    @Override
    public void pickup(EntityHuman player) {
        if (this.ownedBy(player) || this.getShooter() == null) {
            super.pickup(player);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("Trident", 10)) {
            this.tridentItem = ItemStack.of(nbt.getCompound("Trident"));
        }

        this.dealtDamage = nbt.getBoolean("DealtDamage");
        this.entityData.set(ID_LOYALTY, (byte)EnchantmentManager.getLoyalty(this.tridentItem));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.set("Trident", this.tridentItem.save(new NBTTagCompound()));
        nbt.setBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    public void tickDespawn() {
        int i = this.entityData.get(ID_LOYALTY);
        if (this.pickup != EntityArrow.PickupStatus.ALLOWED || i <= 0) {
            super.tickDespawn();
        }

    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
