package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public final class ProjectileHelper {
    public static MovingObjectPosition getHitResult(Entity entity, Predicate<Entity> predicate) {
        Vec3D vec3 = entity.getMot();
        World level = entity.level;
        Vec3D vec32 = entity.getPositionVector();
        Vec3D vec33 = vec32.add(vec3);
        MovingObjectPosition hitResult = level.rayTrace(new RayTrace(vec32, vec33, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, entity));
        if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
            vec33 = hitResult.getPos();
        }

        MovingObjectPosition hitResult2 = getEntityHitResult(level, entity, vec32, vec33, entity.getBoundingBox().expandTowards(entity.getMot()).inflate(1.0D), predicate);
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(Entity entity, Vec3D vec3, Vec3D vec32, AxisAlignedBB aABB, Predicate<Entity> predicate, double d) {
        World level = entity.level;
        double e = d;
        Entity entity2 = null;
        Vec3D vec33 = null;

        for(Entity entity3 : level.getEntities(entity, aABB, predicate)) {
            AxisAlignedBB aABB2 = entity3.getBoundingBox().inflate((double)entity3.getPickRadius());
            Optional<Vec3D> optional = aABB2.clip(vec3, vec32);
            if (aABB2.contains(vec3)) {
                if (e >= 0.0D) {
                    entity2 = entity3;
                    vec33 = optional.orElse(vec3);
                    e = 0.0D;
                }
            } else if (optional.isPresent()) {
                Vec3D vec34 = optional.get();
                double f = vec3.distanceSquared(vec34);
                if (f < e || e == 0.0D) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (e == 0.0D) {
                            entity2 = entity3;
                            vec33 = vec34;
                        }
                    } else {
                        entity2 = entity3;
                        vec33 = vec34;
                        e = f;
                    }
                }
            }
        }

        return entity2 == null ? null : new MovingObjectPositionEntity(entity2, vec33);
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(World world, Entity entity, Vec3D vec3, Vec3D vec32, AxisAlignedBB aABB, Predicate<Entity> predicate) {
        return getEntityHitResult(world, entity, vec3, vec32, aABB, predicate, 0.3F);
    }

    @Nullable
    public static MovingObjectPositionEntity getEntityHitResult(World level, Entity entity, Vec3D vec3, Vec3D vec32, AxisAlignedBB aABB, Predicate<Entity> predicate, float f) {
        double d = Double.MAX_VALUE;
        Entity entity2 = null;

        for(Entity entity3 : level.getEntities(entity, aABB, predicate)) {
            AxisAlignedBB aABB2 = entity3.getBoundingBox().inflate((double)f);
            Optional<Vec3D> optional = aABB2.clip(vec3, vec32);
            if (optional.isPresent()) {
                double e = vec3.distanceSquared(optional.get());
                if (e < d) {
                    entity2 = entity3;
                    d = e;
                }
            }
        }

        return entity2 == null ? null : new MovingObjectPositionEntity(entity2);
    }

    public static void rotateTowardsMovement(Entity entity, float f) {
        Vec3D vec3 = entity.getMot();
        if (vec3.lengthSqr() != 0.0D) {
            double d = vec3.horizontalDistance();
            entity.setYRot((float)(MathHelper.atan2(vec3.z, vec3.x) * (double)(180F / (float)Math.PI)) + 90.0F);
            entity.setXRot((float)(MathHelper.atan2(d, vec3.y) * (double)(180F / (float)Math.PI)) - 90.0F);

            while(entity.getXRot() - entity.xRotO < -180.0F) {
                entity.xRotO -= 360.0F;
            }

            while(entity.getXRot() - entity.xRotO >= 180.0F) {
                entity.xRotO += 360.0F;
            }

            while(entity.getYRot() - entity.yRotO < -180.0F) {
                entity.yRotO -= 360.0F;
            }

            while(entity.getYRot() - entity.yRotO >= 180.0F) {
                entity.yRotO += 360.0F;
            }

            entity.setXRot(MathHelper.lerp(f, entity.xRotO, entity.getXRot()));
            entity.setYRot(MathHelper.lerp(f, entity.yRotO, entity.getYRot()));
        }
    }

    public static EnumHand getWeaponHoldingHand(EntityLiving entity, Item item) {
        return entity.getItemInMainHand().is(item) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }

    public static EntityArrow getMobArrow(EntityLiving entity, ItemStack stack, float damageModifier) {
        ItemArrow arrowItem = (ItemArrow)(stack.getItem() instanceof ItemArrow ? stack.getItem() : Items.ARROW);
        EntityArrow abstractArrow = arrowItem.createArrow(entity.level, stack, entity);
        abstractArrow.setEnchantmentEffectsFromEntity(entity, damageModifier);
        if (stack.is(Items.TIPPED_ARROW) && abstractArrow instanceof EntityTippedArrow) {
            ((EntityTippedArrow)abstractArrow).setEffectsFromItem(stack);
        }

        return abstractArrow;
    }
}
