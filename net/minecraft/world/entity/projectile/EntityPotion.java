package net.minecraft.world.entity.projectile;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.axolotl.EntityAxolotl;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockCandleAbstract;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntityPotion extends EntityProjectileThrowable implements ItemSupplier {
    public static final double SPLASH_RANGE = 4.0D;
    private static final double SPLASH_RANGE_SQ = 16.0D;
    public static final Predicate<EntityLiving> WATER_SENSITIVE = EntityLiving::isSensitiveToWater;

    public EntityPotion(EntityTypes<? extends EntityPotion> type, World world) {
        super(type, world);
    }

    public EntityPotion(World world, EntityLiving owner) {
        super(EntityTypes.POTION, owner, world);
    }

    public EntityPotion(World world, double x, double y, double z) {
        super(EntityTypes.POTION, x, y, z, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level.isClientSide) {
            ItemStack itemStack = this.getSuppliedItem();
            PotionRegistry potion = PotionUtil.getPotion(itemStack);
            List<MobEffect> list = PotionUtil.getEffects(itemStack);
            boolean bl = potion == Potions.WATER && list.isEmpty();
            EnumDirection direction = blockHitResult.getDirection();
            BlockPosition blockPos = blockHitResult.getBlockPosition();
            BlockPosition blockPos2 = blockPos.relative(direction);
            if (bl) {
                this.dowseFire(blockPos2);
                this.dowseFire(blockPos2.relative(direction.opposite()));

                for(EnumDirection direction2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                    this.dowseFire(blockPos2.relative(direction2));
                }
            }

        }
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            ItemStack itemStack = this.getSuppliedItem();
            PotionRegistry potion = PotionUtil.getPotion(itemStack);
            List<MobEffect> list = PotionUtil.getEffects(itemStack);
            boolean bl = potion == Potions.WATER && list.isEmpty();
            if (bl) {
                this.splash();
            } else if (!list.isEmpty()) {
                if (this.isLingering()) {
                    this.makeAreaOfEffectCloud(itemStack, potion);
                } else {
                    this.applySplash(list, hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.ENTITY ? ((MovingObjectPositionEntity)hitResult).getEntity() : null);
                }
            }

            int i = potion.hasInstantEffects() ? 2007 : 2002;
            this.level.triggerEffect(i, this.getChunkCoordinates(), PotionUtil.getColor(itemStack));
            this.die();
        }
    }

    private void splash() {
        AxisAlignedBB aABB = this.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<EntityLiving> list = this.level.getEntitiesOfClass(EntityLiving.class, aABB, WATER_SENSITIVE);
        if (!list.isEmpty()) {
            for(EntityLiving livingEntity : list) {
                double d = this.distanceToSqr(livingEntity);
                if (d < 16.0D && livingEntity.isSensitiveToWater()) {
                    livingEntity.damageEntity(DamageSource.indirectMagic(this, this.getShooter()), 1.0F);
                }
            }
        }

        for(EntityAxolotl axolotl : this.level.getEntitiesOfClass(EntityAxolotl.class, aABB)) {
            axolotl.rehydrate();
        }

    }

    private void applySplash(List<MobEffect> statusEffects, @Nullable Entity entity) {
        AxisAlignedBB aABB = this.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<EntityLiving> list = this.level.getEntitiesOfClass(EntityLiving.class, aABB);
        if (!list.isEmpty()) {
            Entity entity2 = this.getEffectSource();

            for(EntityLiving livingEntity : list) {
                if (livingEntity.isAffectedByPotions()) {
                    double d = this.distanceToSqr(livingEntity);
                    if (d < 16.0D) {
                        double e = 1.0D - Math.sqrt(d) / 4.0D;
                        if (livingEntity == entity) {
                            e = 1.0D;
                        }

                        for(MobEffect mobEffectInstance : statusEffects) {
                            MobEffectBase mobEffect = mobEffectInstance.getMobEffect();
                            if (mobEffect.isInstant()) {
                                mobEffect.applyInstantEffect(this, this.getShooter(), livingEntity, mobEffectInstance.getAmplifier(), e);
                            } else {
                                int i = (int)(e * (double)mobEffectInstance.getDuration() + 0.5D);
                                if (i > 20) {
                                    livingEntity.addEffect(new MobEffect(mobEffect, i, mobEffectInstance.getAmplifier(), mobEffectInstance.isAmbient(), mobEffectInstance.isShowParticles()), entity2);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void makeAreaOfEffectCloud(ItemStack stack, PotionRegistry potion) {
        EntityAreaEffectCloud areaEffectCloud = new EntityAreaEffectCloud(this.level, this.locX(), this.locY(), this.locZ());
        Entity entity = this.getShooter();
        if (entity instanceof EntityLiving) {
            areaEffectCloud.setSource((EntityLiving)entity);
        }

        areaEffectCloud.setRadius(3.0F);
        areaEffectCloud.setRadiusOnUse(-0.5F);
        areaEffectCloud.setWaitTime(10);
        areaEffectCloud.setRadiusPerTick(-areaEffectCloud.getRadius() / (float)areaEffectCloud.getDuration());
        areaEffectCloud.setPotion(potion);

        for(MobEffect mobEffectInstance : PotionUtil.getCustomEffects(stack)) {
            areaEffectCloud.addEffect(new MobEffect(mobEffectInstance));
        }

        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.hasKeyOfType("CustomPotionColor", 99)) {
            areaEffectCloud.setColor(compoundTag.getInt("CustomPotionColor"));
        }

        this.level.addEntity(areaEffectCloud);
    }

    public boolean isLingering() {
        return this.getSuppliedItem().is(Items.LINGERING_POTION);
    }

    private void dowseFire(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);
        if (blockState.is(TagsBlock.FIRE)) {
            this.level.removeBlock(pos, false);
        } else if (BlockCandleAbstract.isLit(blockState)) {
            BlockCandleAbstract.extinguish((EntityHuman)null, blockState, this.level, pos);
        } else if (BlockCampfire.isLitCampfire(blockState)) {
            this.level.triggerEffect((EntityHuman)null, 1009, pos, 0);
            BlockCampfire.dowse(this.getShooter(), this.level, pos, blockState);
            this.level.setTypeUpdate(pos, blockState.set(BlockCampfire.LIT, Boolean.valueOf(false)));
        }

    }
}
