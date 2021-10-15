package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderFlying;
import net.minecraft.world.phys.Vec3D;

public class NavigationFlying extends NavigationAbstract {
    public NavigationFlying(EntityInsentient mob, World world) {
        super(mob, world);
    }

    @Override
    protected Pathfinder createPathFinder(int range) {
        this.nodeEvaluator = new PathfinderFlying();
        this.nodeEvaluator.setCanPassDoors(true);
        return new Pathfinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.canFloat() && this.isInLiquid() || !this.mob.isPassenger();
    }

    @Override
    protected Vec3D getTempMobPos() {
        return this.mob.getPositionVector();
    }

    @Override
    public PathEntity createPath(Entity entity, int distance) {
        return this.createPath(entity.getChunkCoordinates(), distance);
    }

    @Override
    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3D vec3 = this.path.getNextEntityPos(this.mob);
                if (this.mob.getBlockX() == MathHelper.floor(vec3.x) && this.mob.getBlockY() == MathHelper.floor(vec3.y) && this.mob.getBlockZ() == MathHelper.floor(vec3.z)) {
                    this.path.advance();
                }
            }

            PacketDebug.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
            if (!this.isDone()) {
                Vec3D vec32 = this.path.getNextEntityPos(this.mob);
                this.mob.getControllerMove().setWantedPosition(vec32.x, vec32.y, vec32.z, this.speedModifier);
            }
        }
    }

    @Override
    protected boolean canMoveDirectly(Vec3D origin, Vec3D target, int sizeX, int sizeY, int sizeZ) {
        int i = MathHelper.floor(origin.x);
        int j = MathHelper.floor(origin.y);
        int k = MathHelper.floor(origin.z);
        double d = target.x - origin.x;
        double e = target.y - origin.y;
        double f = target.z - origin.z;
        double g = d * d + e * e + f * f;
        if (g < 1.0E-8D) {
            return false;
        } else {
            double h = 1.0D / Math.sqrt(g);
            d = d * h;
            e = e * h;
            f = f * h;
            double l = 1.0D / Math.abs(d);
            double m = 1.0D / Math.abs(e);
            double n = 1.0D / Math.abs(f);
            double o = (double)i - origin.x;
            double p = (double)j - origin.y;
            double q = (double)k - origin.z;
            if (d >= 0.0D) {
                ++o;
            }

            if (e >= 0.0D) {
                ++p;
            }

            if (f >= 0.0D) {
                ++q;
            }

            o = o / d;
            p = p / e;
            q = q / f;
            int r = d < 0.0D ? -1 : 1;
            int s = e < 0.0D ? -1 : 1;
            int t = f < 0.0D ? -1 : 1;
            int u = MathHelper.floor(target.x);
            int v = MathHelper.floor(target.y);
            int w = MathHelper.floor(target.z);
            int x = u - i;
            int y = v - j;
            int z = w - k;

            while(x * r > 0 || y * s > 0 || z * t > 0) {
                if (o < q && o <= p) {
                    o += l;
                    i += r;
                    x = u - i;
                } else if (p < o && p <= q) {
                    p += m;
                    j += s;
                    y = v - j;
                } else {
                    q += n;
                    k += t;
                    z = w - k;
                }
            }

            return true;
        }
    }

    public void setCanOpenDoors(boolean canPathThroughDoors) {
        this.nodeEvaluator.setCanOpenDoors(canPathThroughDoors);
    }

    public boolean canPassDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.nodeEvaluator.setCanPassDoors(canEnterOpenDoors);
    }

    public boolean canOpenDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    @Override
    public boolean isStableDestination(BlockPosition pos) {
        return this.level.getType(pos).entityCanStandOn(this.level, pos, this.mob);
    }
}
