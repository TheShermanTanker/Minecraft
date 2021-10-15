package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.level.pathfinder.PathEntity;

public class BehaviorMakeLove extends Behavior<EntityVillager> {
    private static final int INTERACT_DIST_SQR = 5;
    private static final float SPEED_MODIFIER = 0.5F;
    private long birthTimestamp;

    public BehaviorMakeLove() {
        super(ImmutableMap.of(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT), 350, 350);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        return this.isBreedingPossible(entity);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return l <= this.birthTimestamp && this.isBreedingPossible(villager);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityAgeable ageableMob = villager.getBehaviorController().getMemory(MemoryModuleType.BREED_TARGET).get();
        BehaviorUtil.lockGazeAndWalkToEachOther(villager, ageableMob, 0.5F);
        serverLevel.broadcastEntityEffect(ageableMob, (byte)18);
        serverLevel.broadcastEntityEffect(villager, (byte)18);
        int i = 275 + villager.getRandom().nextInt(50);
        this.birthTimestamp = l + (long)i;
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityVillager villager2 = (EntityVillager)villager.getBehaviorController().getMemory(MemoryModuleType.BREED_TARGET).get();
        if (!(villager.distanceToSqr(villager2) > 5.0D)) {
            BehaviorUtil.lockGazeAndWalkToEachOther(villager, villager2, 0.5F);
            if (l >= this.birthTimestamp) {
                villager.eatAndDigestFood();
                villager2.eatAndDigestFood();
                this.tryToGiveBirth(serverLevel, villager, villager2);
            } else if (villager.getRandom().nextInt(35) == 0) {
                serverLevel.broadcastEntityEffect(villager2, (byte)12);
                serverLevel.broadcastEntityEffect(villager, (byte)12);
            }

        }
    }

    private void tryToGiveBirth(WorldServer world, EntityVillager first, EntityVillager second) {
        Optional<BlockPosition> optional = this.takeVacantBed(world, first);
        if (!optional.isPresent()) {
            world.broadcastEntityEffect(second, (byte)13);
            world.broadcastEntityEffect(first, (byte)13);
        } else {
            Optional<EntityVillager> optional2 = this.breed(world, first, second);
            if (optional2.isPresent()) {
                this.giveBedToChild(world, optional2.get(), optional.get());
            } else {
                world.getPoiManager().release(optional.get());
                PacketDebug.sendPoiTicketCountPacket(world, optional.get());
            }
        }

    }

    @Override
    protected void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        villager.getBehaviorController().removeMemory(MemoryModuleType.BREED_TARGET);
    }

    private boolean isBreedingPossible(EntityVillager villager) {
        BehaviorController<EntityVillager> brain = villager.getBehaviorController();
        Optional<EntityAgeable> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter((ageableMob) -> {
            return ageableMob.getEntityType() == EntityTypes.VILLAGER;
        });
        if (!optional.isPresent()) {
            return false;
        } else {
            return BehaviorUtil.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityTypes.VILLAGER) && villager.canBreed() && optional.get().canBreed();
        }
    }

    private Optional<BlockPosition> takeVacantBed(WorldServer world, EntityVillager villager) {
        return world.getPoiManager().take(VillagePlaceType.HOME.getPredicate(), (blockPos) -> {
            return this.canReach(villager, blockPos);
        }, villager.getChunkCoordinates(), 48);
    }

    private boolean canReach(EntityVillager villager, BlockPosition pos) {
        PathEntity path = villager.getNavigation().createPath(pos, VillagePlaceType.HOME.getValidRange());
        return path != null && path.canReach();
    }

    private Optional<EntityVillager> breed(WorldServer world, EntityVillager parent, EntityVillager partner) {
        EntityVillager villager = parent.getBreedOffspring(world, partner);
        if (villager == null) {
            return Optional.empty();
        } else {
            parent.setAgeRaw(6000);
            partner.setAgeRaw(6000);
            villager.setAgeRaw(-24000);
            villager.setPositionRotation(parent.locX(), parent.locY(), parent.locZ(), 0.0F, 0.0F);
            world.addAllEntities(villager);
            world.broadcastEntityEffect(villager, (byte)12);
            return Optional.of(villager);
        }
    }

    private void giveBedToChild(WorldServer world, EntityVillager child, BlockPosition pos) {
        GlobalPos globalPos = GlobalPos.create(world.getDimensionKey(), pos);
        child.getBehaviorController().setMemory(MemoryModuleType.HOME, globalPos);
    }
}
