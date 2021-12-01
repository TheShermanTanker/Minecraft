package net.minecraft.world.level.levelgen;

public record TerrainInfo(double offset, double factor, double jaggedness) {
    public TerrainInfo(double d, double e, double f) {
        this.offset = d;
        this.factor = e;
        this.jaggedness = f;
    }

    public double offset() {
        return this.offset;
    }

    public double factor() {
        return this.factor;
    }

    public double jaggedness() {
        return this.jaggedness;
    }
}
