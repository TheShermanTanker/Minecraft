package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.ChunkCache;
import net.minecraft.world.level.IBlockAccess;

public abstract class PathfinderAbstract {
    protected ChunkCache level;
    protected EntityInsentient mob;
    protected final Int2ObjectMap<PathPoint> nodes = new Int2ObjectOpenHashMap<>();
    protected int entityWidth;
    protected int entityHeight;
    protected int entityDepth;
    protected boolean canPassDoors;
    protected boolean canOpenDoors;
    protected boolean canFloat;

    public void prepare(ChunkCache cachedWorld, EntityInsentient entity) {
        this.level = cachedWorld;
        this.mob = entity;
        this.nodes.clear();
        this.entityWidth = MathHelper.floor(entity.getWidth() + 1.0F);
        this.entityHeight = MathHelper.floor(entity.getHeight() + 1.0F);
        this.entityDepth = MathHelper.floor(entity.getWidth() + 1.0F);
    }

    public void done() {
        this.level = null;
        this.mob = null;
    }

    protected PathPoint getNode(BlockPosition pos) {
        return this.getNode(pos.getX(), pos.getY(), pos.getZ());
    }

    protected PathPoint getNode(int x, int y, int z) {
        return this.nodes.computeIfAbsent(PathPoint.createHash(x, y, z), (l) -> {
            return new PathPoint(x, y, z);
        });
    }

    public abstract PathPoint getStart();

    public abstract PathDestination getGoal(double x, double y, double z);

    public abstract int getNeighbors(PathPoint[] successors, PathPoint node);

    public abstract PathType getBlockPathType(IBlockAccess world, int x, int y, int z, EntityInsentient mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors);

    public abstract PathType getBlockPathType(IBlockAccess world, int x, int y, int z);

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.canPassDoors = canEnterOpenDoors;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }

    public void setCanFloat(boolean canSwim) {
        this.canFloat = canSwim;
    }

    public boolean canPassDoors() {
        return this.canPassDoors;
    }

    public boolean canOpenDoors() {
        return this.canOpenDoors;
    }

    public boolean canFloat() {
        return this.canFloat;
    }
}
