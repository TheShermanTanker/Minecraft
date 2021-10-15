package net.minecraft.world.entity.projectile;

import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;

public class EntityThrownExpBottle extends EntityProjectileThrowable {
    public EntityThrownExpBottle(EntityTypes<? extends EntityThrownExpBottle> type, World world) {
        super(type, world);
    }

    public EntityThrownExpBottle(World world, EntityLiving owner) {
        super(EntityTypes.EXPERIENCE_BOTTLE, owner, world);
    }

    public EntityThrownExpBottle(World world, double x, double y, double z) {
        super(EntityTypes.EXPERIENCE_BOTTLE, x, y, z, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EXPERIENCE_BOTTLE;
    }

    @Override
    protected float getGravity() {
        return 0.07F;
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (this.level instanceof WorldServer) {
            this.level.triggerEffect(2002, this.getChunkCoordinates(), PotionUtil.getColor(Potions.WATER));
            int i = 3 + this.level.random.nextInt(5) + this.level.random.nextInt(5);
            EntityExperienceOrb.award((WorldServer)this.level, this.getPositionVector(), i);
            this.die();
        }

    }
}
