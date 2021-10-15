package net.minecraft.world.entity.ai.behavior;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class BehaviorRandomSwim extends BehaviorStrollRandomUnconstrained {
    public static final int[][] XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

    public BehaviorRandomSwim(float speed) {
        super(speed);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return entity.isInWaterOrBubble();
    }

    @Nullable
    @Override
    protected Vec3D getTargetPos(EntityCreature entity) {
        Vec3D vec3 = null;
        Vec3D vec32 = null;

        for(int[] is : XY_DISTANCE_TIERS) {
            if (vec3 == null) {
                vec32 = BehaviorUtil.getRandomSwimmablePos(entity, is[0], is[1]);
            } else {
                vec32 = entity.getPositionVector().add(entity.getPositionVector().vectorTo(vec3).normalize().multiply((double)is[0], (double)is[1], (double)is[0]));
            }

            if (vec32 == null || entity.level.getFluid(new BlockPosition(vec32)).isEmpty()) {
                return vec3;
            }

            vec3 = vec32;
        }

        return vec32;
    }
}
