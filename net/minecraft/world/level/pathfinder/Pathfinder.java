package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;

public class Pathfinder {
    private static final float FUDGING = 1.5F;
    private final PathPoint[] neighbors = new PathPoint[32];
    private final int maxVisitedNodes;
    public final PathfinderAbstract nodeEvaluator;
    private static final boolean DEBUG = false;
    private final Path openSet = new Path();

    public Pathfinder(PathfinderAbstract pathNodeMaker, int range) {
        this.nodeEvaluator = pathNodeMaker;
        this.maxVisitedNodes = range;
    }

    @Nullable
    public PathEntity findPath(ChunkCache world, EntityInsentient mob, Set<BlockPosition> positions, float followRange, int distance, float rangeMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(world, mob);
        PathPoint node = this.nodeEvaluator.getStart();
        Map<PathDestination, BlockPosition> map = positions.stream().collect(Collectors.toMap((pos) -> {
            return this.nodeEvaluator.getGoal((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        }, Function.identity()));
        PathEntity path = this.findPath(world.getProfiler(), node, map, followRange, distance, rangeMultiplier);
        this.nodeEvaluator.done();
        return path;
    }

    @Nullable
    private PathEntity findPath(GameProfilerFiller profiler, PathPoint startNode, Map<PathDestination, BlockPosition> positions, float followRange, int distance, float rangeMultiplier) {
        profiler.enter("find_path");
        profiler.markForCharting(EnumMetricCategory.PATH_FINDING);
        Set<PathDestination> set = positions.keySet();
        startNode.g = 0.0F;
        startNode.h = this.getBestH(startNode, set);
        startNode.f = startNode.h;
        this.openSet.clear();
        this.openSet.insert(startNode);
        Set<PathPoint> set2 = ImmutableSet.of();
        int i = 0;
        Set<PathDestination> set3 = Sets.newHashSetWithExpectedSize(set.size());
        int j = (int)((float)this.maxVisitedNodes * rangeMultiplier);

        while(!this.openSet.isEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            PathPoint node = this.openSet.pop();
            node.closed = true;

            for(PathDestination target : set) {
                if (node.distanceManhattan(target) <= (float)distance) {
                    target.setReached();
                    set3.add(target);
                }
            }

            if (!set3.isEmpty()) {
                break;
            }

            if (!(node.distanceTo(startNode) >= followRange)) {
                int k = this.nodeEvaluator.getNeighbors(this.neighbors, node);

                for(int l = 0; l < k; ++l) {
                    PathPoint node2 = this.neighbors[l];
                    float f = node.distanceTo(node2);
                    node2.walkedDistance = node.walkedDistance + f;
                    float g = node.g + f + node2.costMalus;
                    if (node2.walkedDistance < followRange && (!node2.inOpenSet() || g < node2.g)) {
                        node2.cameFrom = node;
                        node2.g = g;
                        node2.h = this.getBestH(node2, set) * 1.5F;
                        if (node2.inOpenSet()) {
                            this.openSet.changeCost(node2, node2.g + node2.h);
                        } else {
                            node2.f = node2.g + node2.h;
                            this.openSet.insert(node2);
                        }
                    }
                }
            }
        }

        Optional<PathEntity> optional = !set3.isEmpty() ? set3.stream().map((target) -> {
            return this.reconstructPath(target.getBestNode(), positions.get(target), true);
        }).min(Comparator.comparingInt(PathEntity::getNodeCount)) : set.stream().map((target) -> {
            return this.reconstructPath(target.getBestNode(), positions.get(target), false);
        }).min(Comparator.comparingDouble(PathEntity::getDistToTarget).thenComparingInt(PathEntity::getNodeCount));
        profiler.exit();
        return !optional.isPresent() ? null : optional.get();
    }

    private float getBestH(PathPoint node, Set<PathDestination> targets) {
        float f = Float.MAX_VALUE;

        for(PathDestination target : targets) {
            float g = node.distanceTo(target);
            target.updateBest(g, node);
            f = Math.min(g, f);
        }

        return f;
    }

    private PathEntity reconstructPath(PathPoint endNode, BlockPosition target, boolean reachesTarget) {
        List<PathPoint> list = Lists.newArrayList();
        PathPoint node = endNode;
        list.add(0, endNode);

        while(node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new PathEntity(list, target, reachesTarget);
    }
}
