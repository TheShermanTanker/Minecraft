package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public class PathEntity {
    public final List<PathPoint> nodes;
    private PathPoint[] openSet = new PathPoint[0];
    private PathPoint[] closedSet = new PathPoint[0];
    @Nullable
    private Set<PathDestination> targetNodes;
    private int nextNodeIndex;
    private final BlockPosition target;
    private final float distToTarget;
    private final boolean reached;

    public PathEntity(List<PathPoint> nodes, BlockPosition target, boolean reachesTarget) {
        this.nodes = nodes;
        this.target = target;
        this.distToTarget = nodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
        this.reached = reachesTarget;
    }

    public void advance() {
        ++this.nextNodeIndex;
    }

    public boolean notStarted() {
        return this.nextNodeIndex <= 0;
    }

    public boolean isDone() {
        return this.nextNodeIndex >= this.nodes.size();
    }

    @Nullable
    public PathPoint getEndNode() {
        return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
    }

    public PathPoint getNode(int index) {
        return this.nodes.get(index);
    }

    public void truncateNodes(int length) {
        if (this.nodes.size() > length) {
            this.nodes.subList(length, this.nodes.size()).clear();
        }

    }

    public void replaceNode(int index, PathPoint node) {
        this.nodes.set(index, node);
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public int getNextNodeIndex() {
        return this.nextNodeIndex;
    }

    public void setNextNodeIndex(int index) {
        this.nextNodeIndex = index;
    }

    public Vec3D getEntityPosAtNode(Entity entity, int index) {
        PathPoint node = this.nodes.get(index);
        double d = (double)node.x + (double)((int)(entity.getWidth() + 1.0F)) * 0.5D;
        double e = (double)node.y;
        double f = (double)node.z + (double)((int)(entity.getWidth() + 1.0F)) * 0.5D;
        return new Vec3D(d, e, f);
    }

    public BlockPosition getNodePos(int index) {
        return this.nodes.get(index).asBlockPos();
    }

    public Vec3D getNextEntityPos(Entity entity) {
        return this.getEntityPosAtNode(entity, this.nextNodeIndex);
    }

    public BlockPosition getNextNodePos() {
        return this.nodes.get(this.nextNodeIndex).asBlockPos();
    }

    public PathPoint getNextNode() {
        return this.nodes.get(this.nextNodeIndex);
    }

    @Nullable
    public PathPoint getPreviousNode() {
        return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
    }

    public boolean sameAs(@Nullable PathEntity o) {
        if (o == null) {
            return false;
        } else if (o.nodes.size() != this.nodes.size()) {
            return false;
        } else {
            for(int i = 0; i < this.nodes.size(); ++i) {
                PathPoint node = this.nodes.get(i);
                PathPoint node2 = o.nodes.get(i);
                if (node.x != node2.x || node.y != node2.y || node.z != node2.z) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean canReach() {
        return this.reached;
    }

    @VisibleForDebug
    void setDebug(PathPoint[] debugNodes, PathPoint[] debugSecondNodes, Set<PathDestination> debugTargetNodes) {
        this.openSet = debugNodes;
        this.closedSet = debugSecondNodes;
        this.targetNodes = debugTargetNodes;
    }

    @VisibleForDebug
    public PathPoint[] getOpenSet() {
        return this.openSet;
    }

    @VisibleForDebug
    public PathPoint[] getClosedSet() {
        return this.closedSet;
    }

    public void writeToStream(PacketDataSerializer buffer) {
        if (this.targetNodes != null && !this.targetNodes.isEmpty()) {
            buffer.writeBoolean(this.reached);
            buffer.writeInt(this.nextNodeIndex);
            buffer.writeInt(this.targetNodes.size());
            this.targetNodes.forEach((target) -> {
                target.writeToStream(buffer);
            });
            buffer.writeInt(this.target.getX());
            buffer.writeInt(this.target.getY());
            buffer.writeInt(this.target.getZ());
            buffer.writeInt(this.nodes.size());

            for(PathPoint node : this.nodes) {
                node.writeToStream(buffer);
            }

            buffer.writeInt(this.openSet.length);

            for(PathPoint node2 : this.openSet) {
                node2.writeToStream(buffer);
            }

            buffer.writeInt(this.closedSet.length);

            for(PathPoint node3 : this.closedSet) {
                node3.writeToStream(buffer);
            }

        }
    }

    public static PathEntity createFromStream(PacketDataSerializer buffer) {
        boolean bl = buffer.readBoolean();
        int i = buffer.readInt();
        int j = buffer.readInt();
        Set<PathDestination> set = Sets.newHashSet();

        for(int k = 0; k < j; ++k) {
            set.add(PathDestination.createFromStream(buffer));
        }

        BlockPosition blockPos = new BlockPosition(buffer.readInt(), buffer.readInt(), buffer.readInt());
        List<PathPoint> list = Lists.newArrayList();
        int l = buffer.readInt();

        for(int m = 0; m < l; ++m) {
            list.add(PathPoint.createFromStream(buffer));
        }

        PathPoint[] nodes = new PathPoint[buffer.readInt()];

        for(int n = 0; n < nodes.length; ++n) {
            nodes[n] = PathPoint.createFromStream(buffer);
        }

        PathPoint[] nodes2 = new PathPoint[buffer.readInt()];

        for(int o = 0; o < nodes2.length; ++o) {
            nodes2[o] = PathPoint.createFromStream(buffer);
        }

        PathEntity path = new PathEntity(list, blockPos, bl);
        path.openSet = nodes;
        path.closedSet = nodes2;
        path.targetNodes = set;
        path.nextNodeIndex = i;
        return path;
    }

    @Override
    public String toString() {
        return "Path(length=" + this.nodes.size() + ")";
    }

    public BlockPosition getTarget() {
        return this.target;
    }

    public float getDistToTarget() {
        return this.distToTarget;
    }
}
