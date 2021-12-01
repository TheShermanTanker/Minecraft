package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public class ItemBoat extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = IEntitySelector.NO_SPECTATORS.and(Entity::isInteractable);
    private final EntityBoat.EnumBoatType type;

    public ItemBoat(EntityBoat.EnumBoatType type, Item.Info settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        MovingObjectPosition hitResult = getPlayerPOVHitResult(world, user, RayTrace.FluidCollisionOption.ANY);
        if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
            return InteractionResultWrapper.pass(itemStack);
        } else {
            Vec3D vec3 = user.getViewVector(1.0F);
            double d = 5.0D;
            List<Entity> list = world.getEntities(user, user.getBoundingBox().expandTowards(vec3.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vec3D vec32 = user.getEyePosition();

                for(Entity entity : list) {
                    AxisAlignedBB aABB = entity.getBoundingBox().inflate((double)entity.getPickRadius());
                    if (aABB.contains(vec32)) {
                        return InteractionResultWrapper.pass(itemStack);
                    }
                }
            }

            if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                EntityBoat boat = new EntityBoat(world, hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
                boat.setType(this.type);
                boat.setYRot(user.getYRot());
                if (!world.getCubes(boat, boat.getBoundingBox())) {
                    return InteractionResultWrapper.fail(itemStack);
                } else {
                    if (!world.isClientSide) {
                        world.addEntity(boat);
                        world.gameEvent(user, GameEvent.ENTITY_PLACE, new BlockPosition(hitResult.getPos()));
                        if (!user.getAbilities().instabuild) {
                            itemStack.subtract(1);
                        }
                    }

                    user.awardStat(StatisticList.ITEM_USED.get(this));
                    return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
                }
            } else {
                return InteractionResultWrapper.pass(itemStack);
            }
        }
    }
}
