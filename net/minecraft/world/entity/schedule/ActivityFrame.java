package net.minecraft.world.entity.schedule;

public class ActivityFrame {
    private final int timeStamp;
    private final float value;

    public ActivityFrame(int startTime, float priority) {
        this.timeStamp = startTime;
        this.value = priority;
    }

    public int getTimeStamp() {
        return this.timeStamp;
    }

    public float getValue() {
        return this.value;
    }
}
