package net.minecraft.world.entity.vehicle;

import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class EntityMinecartRideable extends EntityMinecartAbstract {
    public EntityMinecartRideable(EntityTypes<?> type, World world) {
        super(type, world);
    }

    public EntityMinecartRideable(World world, double x, double y, double z) {
        super(EntityTypes.MINECART, world, x, y, z);
    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        if (player.isSecondaryUseActive()) {
            return EnumInteractionResult.PASS;
        } else if (this.isVehicle()) {
            return EnumInteractionResult.PASS;
        } else if (!this.level.isClientSide) {
            return player.startRiding(this) ? EnumInteractionResult.CONSUME : EnumInteractionResult.PASS;
        } else {
            return EnumInteractionResult.SUCCESS;
        }
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        if (powered) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }

            if (this.getType() == 0) {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.setDamage(50.0F);
                this.velocityChanged();
            }
        }

    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.RIDEABLE;
    }
}
