package net.minecraft.util.thread;

public abstract class IAsyncTaskHandlerReentrant<R extends Runnable> extends IAsyncTaskHandler<R> {
    private int reentrantCount;

    public IAsyncTaskHandlerReentrant(String name) {
        super(name);
    }

    @Override
    public boolean scheduleExecutables() {
        return this.isEntered() || super.scheduleExecutables();
    }

    protected boolean isEntered() {
        return this.reentrantCount != 0;
    }

    @Override
    public void executeTask(R task) {
        ++this.reentrantCount;

        try {
            super.executeTask(task);
        } finally {
            --this.reentrantCount;
        }

    }
}
