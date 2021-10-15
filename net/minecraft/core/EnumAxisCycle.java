package net.minecraft.core;

public enum EnumAxisCycle {
    NONE {
        @Override
        public int cycle(int x, int y, int z, EnumDirection.EnumAxis axis) {
            return axis.choose(x, y, z);
        }

        @Override
        public double cycle(double x, double y, double z, EnumDirection.EnumAxis axis) {
            return axis.choose(x, y, z);
        }

        @Override
        public EnumDirection.EnumAxis cycle(EnumDirection.EnumAxis axis) {
            return axis;
        }

        @Override
        public EnumAxisCycle inverse() {
            return this;
        }
    },
    FORWARD {
        @Override
        public int cycle(int x, int y, int z, EnumDirection.EnumAxis axis) {
            return axis.choose(z, x, y);
        }

        @Override
        public double cycle(double x, double y, double z, EnumDirection.EnumAxis axis) {
            return axis.choose(z, x, y);
        }

        @Override
        public EnumDirection.EnumAxis cycle(EnumDirection.EnumAxis axis) {
            return AXIS_VALUES[Math.floorMod(axis.ordinal() + 1, 3)];
        }

        @Override
        public EnumAxisCycle inverse() {
            return BACKWARD;
        }
    },
    BACKWARD {
        @Override
        public int cycle(int x, int y, int z, EnumDirection.EnumAxis axis) {
            return axis.choose(y, z, x);
        }

        @Override
        public double cycle(double x, double y, double z, EnumDirection.EnumAxis axis) {
            return axis.choose(y, z, x);
        }

        @Override
        public EnumDirection.EnumAxis cycle(EnumDirection.EnumAxis axis) {
            return AXIS_VALUES[Math.floorMod(axis.ordinal() - 1, 3)];
        }

        @Override
        public EnumAxisCycle inverse() {
            return FORWARD;
        }
    };

    public static final EnumDirection.EnumAxis[] AXIS_VALUES = EnumDirection.EnumAxis.values();
    public static final EnumAxisCycle[] VALUES = values();

    public abstract int cycle(int x, int y, int z, EnumDirection.EnumAxis axis);

    public abstract double cycle(double x, double y, double z, EnumDirection.EnumAxis axis);

    public abstract EnumDirection.EnumAxis cycle(EnumDirection.EnumAxis axis);

    public abstract EnumAxisCycle inverse();

    public static EnumAxisCycle between(EnumDirection.EnumAxis from, EnumDirection.EnumAxis to) {
        return VALUES[Math.floorMod(to.ordinal() - from.ordinal(), 3)];
    }
}
