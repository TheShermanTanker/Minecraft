package net.minecraft.world.entity.ai.navigation;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderAbstract;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.Vec3D;

public abstract class NavigationAbstract {
    private static final int MAX_TIME_RECOMPUTE = 20;
    protected final EntityInsentient mob;
    protected final World level;
    @Nullable
    protected PathEntity path;
    protected double speedModifier;
    protected int tick;
    protected int lastStuckCheck;
    protected Vec3D lastStuckCheckPos = Vec3D.ZERO;
    protected BaseBlockPosition timeoutCachedNode = BaseBlockPosition.ZERO;
    protected long timeoutTimer;
    protected long lastTimeoutCheck;
    protected double timeoutLimit;
    protected float maxDistanceToWaypoint = 0.5F;
    protected boolean hasDelayedRecomputation;
    protected long timeLastRecompute;
    protected PathfinderAbstract nodeEvaluator;
    @Nullable
    private BlockPosition targetPos;
    private int reachRange;
    private float maxVisitedNodesMultiplier = 1.0F;
    public final Pathfinder pathFinder;
    private boolean isStuck;

    public NavigationAbstract(EntityInsentient mob, World world) {
        this.mob = mob;
        this.level = world;
        int i = MathHelper.floor(mob.getAttributeValue(GenericAttributes.FOLLOW_RANGE) * 16.0D);
        this.pathFinder = this.createPathFinder(i);
    }

    public void resetMaxVisitedNodesMultiplier() {
        this.maxVisitedNodesMultiplier = 1.0F;
    }

    public void setMaxVisitedNodesMultiplier(float rangeMultiplier) {
        this.maxVisitedNodesMultiplier = rangeMultiplier;
    }

    @Nullable
    public BlockPosition getTargetPos() {
        return this.targetPos;
    }

    protected abstract Pathfinder createPathFinder(int range);

    public void setSpeedModifier(double speed) {
        this.speedModifier = speed;
    }

    public boolean hasDelayedRecomputation() {
        return this.hasDelayedRecomputation;
    }

    public void recomputePath() {
        if (this.level.getTime() - this.timeLastRecompute > 20L) {
            if (this.targetPos != null) {
                this.path = null;
                this.path = this.createPath(this.targetPos, this.reachRange);
                this.timeLastRecompute = this.level.getTime();
                this.hasDelayedRecomputation = false;
            }
        } else {
            this.hasDelayedRecomputation = true;
        }

    }

    @Nullable
    public final PathEntity createPath(double x, double y, double z, int distance) {
        return this.createPath(new BlockPosition(x, y, z), distance);
    }

    @Nullable
    public PathEntity createPath(Stream<BlockPosition> positions, int distance) {
        return this.createPath(positions.collect(Collectors.toSet()), 8, false, distance);
    }

    @Nullable
    public PathEntity createPath(Set<BlockPosition> positions, int distance) {
        return this.createPath(positions, 8, false, distance);
    }

    @Nullable
    public PathEntity createPath(BlockPosition target, int distance) {
        return this.createPath(ImmutableSet.of(target), 8, false, distance);
    }

    @Nullable
    public PathEntity createPath(BlockPosition target, int minDistance, int maxDistance) {
        return this.createPath(ImmutableSet.of(target), 8, false, minDistance, (float)maxDistance);
    }

    @Nullable
    public PathEntity createPath(Entity entity, int distance) {
        return this.createPath(ImmutableSet.of(entity.getChunkCoordinates()), 16, true, distance);
    }

    @Nullable
    protected PathEntity createPath(Set<BlockPosition> positions, int range, boolean useHeadPos, int distance) {
        return this.createPath(positions, range, useHeadPos, distance, (float)this.mob.getAttributeValue(GenericAttributes.FOLLOW_RANGE));
    }

    @Nullable
    protected PathEntity createPath(Set<BlockPosition> positions, int range, boolean useHeadPos, int distance, float followRange) {
        if (positions.isEmpty()) {
            return null;
        } else if (this.mob.locY() < (double)this.level.getMinBuildHeight()) {
            return null;
        } else if (!this.canUpdatePath()) {
            return null;
        } else if (this.path != null && !this.path.isDone() && positions.contains(this.targetPos)) {
            return this.path;
        } else {
            this.level.getMethodProfiler().enter("pathfind");
            BlockPosition blockPos = useHeadPos ? this.mob.getChunkCoordinates().above() : this.mob.getChunkCoordinates();
            int i = (int)(followRange + (float)range);
            ChunkCache pathNavigationRegion = new ChunkCache(this.level, blockPos.offset(-i, -i, -i), blockPos.offset(i, i, i));
            PathEntity path = this.pathFinder.findPath(pathNavigationRegion, this.mob, positions, followRange, distance, this.maxVisitedNodesMultiplier);
            this.level.getMethodProfiler().exit();
            if (path != null && path.getTarget() != null) {
                this.targetPos = path.getTarget();
                this.reachRange = distance;
                this.resetStuckTimeout();
            }

            return path;
        }
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        return this.moveTo(this.createPath(x, y, z, 1), speed);
    }

    public boolean moveTo(Entity entity, double speed) {
        PathEntity path = this.createPath(entity, 1);
        return path != null && this.moveTo(path, speed);
    }

    public boolean moveTo(@Nullable PathEntity path, double speed) {
        if (path == null) {
            this.path = null;
            return false;
        } else {
            if (!path.sameAs(this.path)) {
                this.path = path;
            }

            if (this.isDone()) {
                return false;
            } else {
                this.trimPath();
                if (this.path.getNodeCount() <= 0) {
                    return false;
                } else {
                    this.speedModifier = speed;
                    Vec3D vec3 = this.getTempMobPos();
                    this.lastStuckCheck = this.tick;
                    this.lastStuckCheckPos = vec3;
                    return true;
                }
            }
        }
    }

    @Nullable
    public PathEntity getPath() {
        return this.path;
    }

    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3D vec3 = this.getTempMobPos();
                Vec3D vec32 = this.path.getNextEntityPos(this.mob);
                if (vec3.y > vec32.y && !this.mob.isOnGround() && MathHelper.floor(vec3.x) == MathHelper.floor(vec32.x) && MathHelper.floor(vec3.z) == MathHelper.floor(vec32.z)) {
                    this.path.advance();
                }
            }

            PacketDebug.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
            if (!this.isDone()) {
                Vec3D vec33 = this.path.getNextEntityPos(this.mob);
                this.mob.getControllerMove().setWantedPosition(vec33.x, this.getGroundY(vec33), vec33.z, this.speedModifier);
            }
        }
    }

    protected double getGroundY(Vec3D pos) {
        BlockPosition blockPos = new BlockPosition(pos);
        return this.level.getType(blockPos.below()).isAir() ? pos.y : PathfinderNormal.getFloorLevel(this.level, blockPos);
    }

    protected void followThePath() {
        Vec3D vec3 = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getWidth() > 0.75F ? this.mob.getWidth() / 2.0F : 0.75F - this.mob.getWidth() / 2.0F;
        BaseBlockPosition vec3i = this.path.getNextNodePos();
        double d = Math.abs(this.mob.locX() - ((double)vec3i.getX() + 0.5D));
        double e = Math.abs(this.mob.locY() - (double)vec3i.getY());
        double f = Math.abs(this.mob.locZ() - ((double)vec3i.getZ() + 0.5D));
        boolean bl = d < (double)this.maxDistanceToWaypoint && f < (double)this.maxDistanceToWaypoint && e < 1.0D;
        if (bl || this.mob.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(vec3)) {
            this.path.advance();
        }

        this.doStuckDetection(vec3);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3D currentPos) {
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        } else {
            Vec3D vec3 = Vec3D.atBottomCenterOf(this.path.getNextNodePos());
            if (!currentPos.closerThan(vec3, 2.0D)) {
                return false;
            } else if (this.canMoveDirectly(currentPos, this.path.getNextEntityPos(this.mob))) {
                return true;
            } else {
                Vec3D vec32 = Vec3D.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
                Vec3D vec33 = vec32.subtract(vec3);
                Vec3D vec34 = currentPos.subtract(vec3);
                return vec33.dot(vec34) > 0.0D;
            }
        }
    }

    protected void doStuckDetection(Vec3D currentPos) {
        if (this.tick - this.lastStuckCheck > 100) {
            if (currentPos.distanceSquared(this.lastStuckCheckPos) < 2.25D) {
                this.isStuck = true;
                this.stop();
            } else {
                this.isStuck = false;
            }

            this.lastStuckCheck = this.tick;
            this.lastStuckCheckPos = currentPos;
        }

        if (this.path != null && !this.path.isDone()) {
            BaseBlockPosition vec3i = this.path.getNextNodePos();
            if (vec3i.equals(this.timeoutCachedNode)) {
                this.timeoutTimer += SystemUtils.getMonotonicMillis() - this.lastTimeoutCheck;
            } else {
                this.timeoutCachedNode = vec3i;
                double d = currentPos.distanceTo(Vec3D.atBottomCenterOf(this.timeoutCachedNode));
                this.timeoutLimit = this.mob.getSpeed() > 0.0F ? d / (double)this.mob.getSpeed() * 1000.0D : 0.0D;
            }

            if (this.timeoutLimit > 0.0D && (double)this.timeoutTimer > this.timeoutLimit * 3.0D) {
                this.timeoutPath();
            }

            this.lastTimeoutCheck = SystemUtils.getMonotonicMillis();
        }

    }

    private void timeoutPath() {
        this.resetStuckTimeout();
        this.stop();
    }

    private void resetStuckTimeout() {
        this.timeoutCachedNode = BaseBlockPosition.ZERO;
        this.timeoutTimer = 0L;
        this.timeoutLimit = 0.0D;
        this.isStuck = false;
    }

    public boolean isDone() {
        return this.path == null || this.path.isDone();
    }

    public boolean isInProgress() {
        return !this.isDone();
    }

    public void stop() {
        this.path = null;
    }

    protected abstract Vec3D getTempMobPos();

    protected abstract boolean canUpdatePath();

    protected boolean isInLiquid() {
        return this.mob.isInWaterOrBubble() || this.mob.isInLava();
    }

    protected void trimPath() {
        if (this.path != null) {
            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                PathPoint node = this.path.getNode(i);
                PathPoint node2 = i + 1 < this.path.getNodeCount() ? this.path.getNode(i + 1) : null;
                IBlockData blockState = this.level.getType(new BlockPosition(node.x, node.y, node.z));
                if (blockState.is(TagsBlock.CAULDRONS)) {
                    this.path.replaceNode(i, node.cloneAndMove(node.x, node.y + 1, node.z));
                    if (node2 != null && node.y >= node2.y) {
                        this.path.replaceNode(i + 1, node.cloneAndMove(node2.x, node.y + 1, node2.z));
                    }
                }
            }

        }
    }

    protected boolean canMoveDirectly(Vec3D origin, Vec3D target) {
        return false;
    }

    public boolean isStableDestination(BlockPosition pos) {
        BlockPosition blockPos = pos.below();
        return this.level.getType(blockPos).isSolidRender(this.level, blockPos);
    }

    public PathfinderAbstract getPathFinder() {
        return this.nodeEvaluator;
    }

    public void setCanFloat(boolean canSwim) {
        this.nodeEvaluator.setCanFloat(canSwim);
    }

    public boolean canFloat() {
        return this.nodeEvaluator.canFloat();
    }

    public void recomputePath(BlockPosition pos) {
        if (this.path != null && !this.path.isDone() && this.path.getNodeCount() != 0) {
            PathPoint node = this.path.getEndNode();
            Vec3D vec3 = new Vec3D(((double)node.x + this.mob.locX()) / 2.0D, ((double)node.y + this.mob.locY()) / 2.0D, ((double)node.z + this.mob.locZ()) / 2.0D);
            if (pos.closerThan(vec3, (double)(this.path.getNodeCount() - this.path.getNextNodeIndex()))) {
                this.recomputePath();
            }

        }
    }

    public float getMaxDistanceToWaypoint() {
        return this.maxDistanceToWaypoint;
    }

    public boolean isStuck() {
        return this.isStuck;
    }
}
