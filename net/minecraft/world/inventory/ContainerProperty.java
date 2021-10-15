package net.minecraft.world.inventory;

public abstract class ContainerProperty {
    private int prevValue;

    public static ContainerProperty forContainer(IContainerProperties delegate, int index) {
        return new ContainerProperty() {
            @Override
            public int get() {
                return delegate.getProperty(index);
            }

            @Override
            public void set(int value) {
                delegate.setProperty(index, value);
            }
        };
    }

    public static ContainerProperty shared(int[] array, int index) {
        return new ContainerProperty() {
            @Override
            public int get() {
                return array[index];
            }

            @Override
            public void set(int value) {
                array[index] = value;
            }
        };
    }

    public static ContainerProperty standalone() {
        return new ContainerProperty() {
            private int value;

            @Override
            public int get() {
                return this.value;
            }

            @Override
            public void set(int value) {
                this.value = value;
            }
        };
    }

    public abstract int get();

    public abstract void set(int value);

    public boolean checkAndClearUpdateFlag() {
        int i = this.get();
        boolean bl = i != this.prevValue;
        this.prevValue = i;
        return bl;
    }
}
