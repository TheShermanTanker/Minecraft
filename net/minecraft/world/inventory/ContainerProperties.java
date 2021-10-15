package net.minecraft.world.inventory;

public class ContainerProperties implements IContainerProperties {
    private final int[] ints;

    public ContainerProperties(int size) {
        this.ints = new int[size];
    }

    @Override
    public int getProperty(int index) {
        return this.ints[index];
    }

    @Override
    public void setProperty(int index, int value) {
        this.ints[index] = value;
    }

    @Override
    public int getCount() {
        return this.ints.length;
    }
}
