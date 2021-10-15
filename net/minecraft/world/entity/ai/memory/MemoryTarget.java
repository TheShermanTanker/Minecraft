package net.minecraft.world.entity.ai.memory;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BehaviorPosition;
import net.minecraft.world.entity.ai.behavior.BehaviorPositionEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorTarget;
import net.minecraft.world.phys.Vec3D;

public class MemoryTarget {
    private final BehaviorPosition target;
    private final float speedModifier;
    private final int closeEnoughDist;

    public MemoryTarget(BlockPosition pos, float speed, int completionRange) {
        this(new BehaviorTarget(pos), speed, completionRange);
    }

    public MemoryTarget(Vec3D pos, float speed, int completionRange) {
        this(new BehaviorTarget(new BlockPosition(pos)), speed, completionRange);
    }

    public MemoryTarget(Entity entity, float speed, int completionRange) {
        this(new BehaviorPositionEntity(entity, false), speed, completionRange);
    }

    public MemoryTarget(BehaviorPosition lookTarget, float speed, int completionRange) {
        this.target = lookTarget;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
    }

    public BehaviorPosition getTarget() {
        return this.target;
    }

    public float getSpeedModifier() {
        return this.speedModifier;
    }

    public int getCloseEnoughDist() {
        return this.closeEnoughDist;
    }
}
