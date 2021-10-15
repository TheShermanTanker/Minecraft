package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalStrollVillage extends PathfinderGoalRandomStroll {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;

    public PathfinderGoalStrollVillage(EntityCreature entity, double speed, boolean canDespawn) {
        super(entity, speed, 10, canDespawn);
    }

    @Override
    public boolean canUse() {
        WorldServer serverLevel = (WorldServer)this.mob.level;
        BlockPosition blockPos = this.mob.getChunkCoordinates();
        return serverLevel.isVillage(blockPos) ? false : super.canUse();
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        WorldServer serverLevel = (WorldServer)this.mob.level;
        BlockPosition blockPos = this.mob.getChunkCoordinates();
        SectionPosition sectionPos = SectionPosition.of(blockPos);
        SectionPosition sectionPos2 = BehaviorUtil.findSectionClosestToVillage(serverLevel, sectionPos, 2);
        return sectionPos2 != sectionPos ? DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3D.atBottomCenterOf(sectionPos2.center()), (double)((float)Math.PI / 2F)) : null;
    }
}
