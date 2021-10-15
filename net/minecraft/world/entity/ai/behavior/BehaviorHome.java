package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;

public class BehaviorHome extends Behavior<EntityLiving> {
    private final float speedModifier;
    private final int radius;
    private final int closeEnoughDist;
    private Optional<BlockPosition> currentPos = Optional.empty();

    public BehaviorHome(int maxDistance, float walkSpeed, int preferredDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.REGISTERED, MemoryModuleType.HIDING_PLACE, MemoryStatus.REGISTERED));
        this.radius = maxDistance;
        this.speedModifier = walkSpeed;
        this.closeEnoughDist = preferredDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        Optional<BlockPosition> optional = world.getPoiManager().find((poiType) -> {
            return poiType == VillagePlaceType.HOME;
        }, (blockPos) -> {
            return true;
        }, entity.getChunkCoordinates(), this.closeEnoughDist + 1, VillagePlace.Occupancy.ANY);
        if (optional.isPresent() && optional.get().closerThan(entity.getPositionVector(), (double)this.closeEnoughDist)) {
            this.currentPos = optional;
        } else {
            this.currentPos = Optional.empty();
        }

        return true;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Optional<BlockPosition> optional = this.currentPos;
        if (!optional.isPresent()) {
            optional = world.getPoiManager().getRandom((poiType) -> {
                return poiType == VillagePlaceType.HOME;
            }, (blockPos) -> {
                return true;
            }, VillagePlace.Occupancy.ANY, entity.getChunkCoordinates(), this.radius, entity.getRandom());
            if (!optional.isPresent()) {
                Optional<GlobalPos> optional2 = brain.getMemory(MemoryModuleType.HOME);
                if (optional2.isPresent()) {
                    optional = Optional.of(optional2.get().getBlockPosition());
                }
            }
        }

        if (optional.isPresent()) {
            brain.removeMemory(MemoryModuleType.PATH);
            brain.removeMemory(MemoryModuleType.LOOK_TARGET);
            brain.removeMemory(MemoryModuleType.BREED_TARGET);
            brain.removeMemory(MemoryModuleType.INTERACTION_TARGET);
            brain.setMemory(MemoryModuleType.HIDING_PLACE, GlobalPos.create(world.getDimensionKey(), optional.get()));
            if (!optional.get().closerThan(entity.getPositionVector(), (double)this.closeEnoughDist)) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(optional.get(), this.speedModifier, this.closeEnoughDist));
            }
        }

    }
}
