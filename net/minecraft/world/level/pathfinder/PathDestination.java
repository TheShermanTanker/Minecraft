package net.minecraft.world.level.pathfinder;

import net.minecraft.network.PacketDataSerializer;

public class PathDestination extends PathPoint {
    private float bestHeuristic = Float.MAX_VALUE;
    private PathPoint bestNode;
    private boolean reached;

    public PathDestination(PathPoint node) {
        super(node.x, node.y, node.z);
    }

    public PathDestination(int x, int y, int z) {
        super(x, y, z);
    }

    public void updateBest(float distance, PathPoint node) {
        if (distance < this.bestHeuristic) {
            this.bestHeuristic = distance;
            this.bestNode = node;
        }

    }

    public PathPoint getBestNode() {
        return this.bestNode;
    }

    public void setReached() {
        this.reached = true;
    }

    public boolean isReached() {
        return this.reached;
    }

    public static PathDestination createFromStream(PacketDataSerializer buffer) {
        PathDestination target = new PathDestination(buffer.readInt(), buffer.readInt(), buffer.readInt());
        target.walkedDistance = buffer.readFloat();
        target.costMalus = buffer.readFloat();
        target.closed = buffer.readBoolean();
        target.type = PathType.values()[buffer.readInt()];
        target.f = buffer.readFloat();
        return target;
    }
}
