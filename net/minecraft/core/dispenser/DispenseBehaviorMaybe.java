package net.minecraft.core.dispenser;

import net.minecraft.core.ISourceBlock;

public abstract class DispenseBehaviorMaybe extends DispenseBehaviorItem {
    private boolean success = true;

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    protected void playSound(ISourceBlock pointer) {
        pointer.getWorld().triggerEffect(this.isSuccess() ? 1000 : 1001, pointer.getBlockPosition(), 0);
    }
}
