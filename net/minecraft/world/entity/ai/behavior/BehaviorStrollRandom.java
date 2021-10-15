package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class BehaviorStrollRandom extends Behavior<EntityCreature> {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private final float speedModifier;
    private final int maxXyDist;
    private final int maxYDist;

    public BehaviorStrollRandom(float walkSpeed) {
        this(walkSpeed, 10, 7);
    }

    public BehaviorStrollRandom(float walkSpeed, int maxHorizontalDistance, int maxVerticalDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = walkSpeed;
        this.maxXyDist = maxHorizontalDistance;
        this.maxYDist = maxVerticalDistance;
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        BlockPosition blockPos = entity.getChunkCoordinates();
        if (world.isVillage(blockPos)) {
            this.setRandomPos(entity);
        } else {
            SectionPosition sectionPos = SectionPosition.of(blockPos);
            SectionPosition sectionPos2 = BehaviorUtil.findSectionClosestToVillage(world, sectionPos, 2);
            if (sectionPos2 != sectionPos) {
                this.setTargetedPos(entity, sectionPos2);
            } else {
                this.setRandomPos(entity);
            }
        }

    }

    private void setTargetedPos(EntityCreature entity, SectionPosition pos) {
        Optional<Vec3D> optional = Optional.ofNullable(DefaultRandomPos.getPosTowards(entity, this.maxXyDist, this.maxYDist, Vec3D.atBottomCenterOf(pos.center()), (double)((float)Math.PI / 2F)));
        entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
            return new MemoryTarget(vec3, this.speedModifier, 0);
        }));
    }

    private void setRandomPos(EntityCreature entity) {
        Optional<Vec3D> optional = Optional.ofNullable(LandRandomPos.getPos(entity, this.maxXyDist, this.maxYDist));
        entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
            return new MemoryTarget(vec3, this.speedModifier, 0);
        }));
    }
}
