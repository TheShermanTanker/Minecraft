package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPosition;

public class NaturalSpawnerPotentials {
    private final List<NaturalSpawnerPotentials.PointCharge> charges = Lists.newArrayList();

    public void addCharge(BlockPosition pos, double mass) {
        if (mass != 0.0D) {
            this.charges.add(new NaturalSpawnerPotentials.PointCharge(pos, mass));
        }

    }

    public double getPotentialEnergyChange(BlockPosition pos, double mass) {
        if (mass == 0.0D) {
            return 0.0D;
        } else {
            double d = 0.0D;

            for(NaturalSpawnerPotentials.PointCharge pointCharge : this.charges) {
                d += pointCharge.getPotentialChange(pos);
            }

            return d * mass;
        }
    }

    static class PointCharge {
        private final BlockPosition pos;
        private final double charge;

        public PointCharge(BlockPosition pos, double mass) {
            this.pos = pos;
            this.charge = mass;
        }

        public double getPotentialChange(BlockPosition pos) {
            double d = this.pos.distSqr(pos);
            return d == 0.0D ? Double.POSITIVE_INFINITY : this.charge / Math.sqrt(d);
        }
    }
}
