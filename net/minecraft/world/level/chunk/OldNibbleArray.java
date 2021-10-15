package net.minecraft.world.level.chunk;

public class OldNibbleArray {
    public final byte[] data;
    private final int depthBits;
    private final int depthBitsPlusFour;

    public OldNibbleArray(byte[] data, int yCoordinateBits) {
        this.data = data;
        this.depthBits = yCoordinateBits;
        this.depthBitsPlusFour = yCoordinateBits + 4;
    }

    public int get(int x, int y, int z) {
        int i = x << this.depthBitsPlusFour | z << this.depthBits | y;
        int j = i >> 1;
        int k = i & 1;
        return k == 0 ? this.data[j] & 15 : this.data[j] >> 4 & 15;
    }
}
