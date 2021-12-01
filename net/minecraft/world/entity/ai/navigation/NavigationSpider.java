package net.minecraft.world.entity.ai.navigation;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathEntity;

public class NavigationSpider extends Navigation {
    @Nullable
    private BlockPosition pathToPosition;

    public NavigationSpider(EntityInsentient mob, World world) {
        super(mob, world);
    }

    @Override
    public PathEntity createPath(BlockPosition target, int distance) {
        this.pathToPosition = target;
        return super.createPath(target, distance);
    }

    @Override
    public PathEntity createPath(Entity entity, int distance) {
        this.pathToPosition = entity.getChunkCoordinates();
        return super.createPath(entity, distance);
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        PathEntity path = this.createPath(entity, 0);
        if (path != null) {
            return this.moveTo(path, speed);
        } else {
            this.pathToPosition = entity.getChunkCoordinates();
            this.speedModifier = speed;
            return true;
        }
    }

    @Override
    public void tick() {
        if (!this.isDone()) {
            super.tick();
        } else {
            if (this.pathToPosition != null) {
                if (!this.pathToPosition.closerThan(this.mob.getPositionVector(), (double)this.mob.getWidth()) && (!(this.mob.locY() > (double)this.pathToPosition.getY()) || !(new BlockPosition((double)this.pathToPosition.getX(), this.mob.locY(), (double)this.pathToPosition.getZ())).closerThan(this.mob.getPositionVector(), (double)this.mob.getWidth()))) {
                    this.mob.getControllerMove().setWantedPosition((double)this.pathToPosition.getX(), (double)this.pathToPosition.getY(), (double)this.pathToPosition.getZ(), this.speedModifier);
                } else {
                    this.pathToPosition = null;
                }
            }

        }
    }
}
