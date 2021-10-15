package net.minecraft.world.inventory;

public interface IContainerProperties {
    int getProperty(int index);

    void setProperty(int index, int value);

    int getCount();
}
