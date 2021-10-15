package net.minecraft.world.entity.monster;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3fa;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.item.ItemCrossbow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3D;

public interface ICrossbow extends IRangedEntity {
    void setChargingCrossbow(boolean charging);

    void shootCrossbowProjectile(EntityLiving target, ItemStack crossbow, IProjectile projectile, float multiShotSpray);

    @Nullable
    EntityLiving getGoalTarget();

    void onCrossbowAttackPerformed();

    default void performCrossbowAttack(EntityLiving entity, float speed) {
        EnumHand interactionHand = ProjectileHelper.getWeaponHoldingHand(entity, Items.CROSSBOW);
        ItemStack itemStack = entity.getItemInHand(interactionHand);
        if (entity.isHolding(Items.CROSSBOW)) {
            ItemCrossbow.performShooting(entity.level, entity, interactionHand, itemStack, speed, (float)(14 - entity.level.getDifficulty().getId() * 4));
        }

        this.onCrossbowAttackPerformed();
    }

    default void shootCrossbowProjectile(EntityLiving entity, EntityLiving target, IProjectile projectile, float multishotSpray, float speed) {
        double d = target.locX() - entity.locX();
        double e = target.locZ() - entity.locZ();
        double f = Math.sqrt(d * d + e * e);
        double g = target.getY(0.3333333333333333D) - projectile.locY() + f * (double)0.2F;
        Vector3fa vector3f = this.getProjectileShotVector(entity, new Vec3D(d, g, e), multishotSpray);
        projectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, (float)(14 - entity.level.getDifficulty().getId() * 4));
        entity.playSound(SoundEffects.CROSSBOW_SHOOT, 1.0F, 1.0F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    default Vector3fa getProjectileShotVector(EntityLiving entity, Vec3D positionDelta, float multishotSpray) {
        Vec3D vec3 = positionDelta.normalize();
        Vec3D vec32 = vec3.cross(new Vec3D(0.0D, 1.0D, 0.0D));
        if (vec32.lengthSqr() <= 1.0E-7D) {
            vec32 = vec3.cross(entity.getUpVector(1.0F));
        }

        Quaternion quaternion = new Quaternion(new Vector3fa(vec32), 90.0F, true);
        Vector3fa vector3f = new Vector3fa(vec3);
        vector3f.transform(quaternion);
        Quaternion quaternion2 = new Quaternion(vector3f, multishotSpray, true);
        Vector3fa vector3f2 = new Vector3fa(vec3);
        vector3f2.transform(quaternion2);
        return vector3f2;
    }
}
