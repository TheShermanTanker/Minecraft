package net.minecraft.world.level.block.piston;

import net.minecraft.core.EnumDirection;
import net.minecraft.world.phys.AxisAlignedBB;

public class PistonUtil {
    public static AxisAlignedBB getMovementArea(AxisAlignedBB box, EnumDirection direction, double length) {
        double d = length * (double)direction.getAxisDirection().getStep();
        double e = Math.min(d, 0.0D);
        double f = Math.max(d, 0.0D);
        switch(direction) {
        case WEST:
            return new AxisAlignedBB(box.minX + e, box.minY, box.minZ, box.minX + f, box.maxY, box.maxZ);
        case EAST:
            return new AxisAlignedBB(box.maxX + e, box.minY, box.minZ, box.maxX + f, box.maxY, box.maxZ);
        case DOWN:
            return new AxisAlignedBB(box.minX, box.minY + e, box.minZ, box.maxX, box.minY + f, box.maxZ);
        case UP:
        default:
            return new AxisAlignedBB(box.minX, box.maxY + e, box.minZ, box.maxX, box.maxY + f, box.maxZ);
        case NORTH:
            return new AxisAlignedBB(box.minX, box.minY, box.minZ + e, box.maxX, box.maxY, box.minZ + f);
        case SOUTH:
            return new AxisAlignedBB(box.minX, box.minY, box.maxZ + e, box.maxX, box.maxY, box.maxZ + f);
        }
    }
}
