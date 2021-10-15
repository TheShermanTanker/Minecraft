package net.minecraft.world.entity.player;

import net.minecraft.nbt.NBTTagCompound;

public class PlayerAbilities {
    public boolean invulnerable;
    public boolean flying;
    public boolean mayfly;
    public boolean instabuild;
    public boolean mayBuild = true;
    public float flyingSpeed = 0.05F;
    public float walkingSpeed = 0.1F;

    public void addSaveData(NBTTagCompound nbt) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setBoolean("invulnerable", this.invulnerable);
        compoundTag.setBoolean("flying", this.flying);
        compoundTag.setBoolean("mayfly", this.mayfly);
        compoundTag.setBoolean("instabuild", this.instabuild);
        compoundTag.setBoolean("mayBuild", this.mayBuild);
        compoundTag.setFloat("flySpeed", this.flyingSpeed);
        compoundTag.setFloat("walkSpeed", this.walkingSpeed);
        nbt.set("abilities", compoundTag);
    }

    public void loadSaveData(NBTTagCompound nbt) {
        if (nbt.hasKeyOfType("abilities", 10)) {
            NBTTagCompound compoundTag = nbt.getCompound("abilities");
            this.invulnerable = compoundTag.getBoolean("invulnerable");
            this.flying = compoundTag.getBoolean("flying");
            this.mayfly = compoundTag.getBoolean("mayfly");
            this.instabuild = compoundTag.getBoolean("instabuild");
            if (compoundTag.hasKeyOfType("flySpeed", 99)) {
                this.flyingSpeed = compoundTag.getFloat("flySpeed");
                this.walkingSpeed = compoundTag.getFloat("walkSpeed");
            }

            if (compoundTag.hasKeyOfType("mayBuild", 1)) {
                this.mayBuild = compoundTag.getBoolean("mayBuild");
            }
        }

    }

    public float getFlyingSpeed() {
        return this.flyingSpeed;
    }

    public void setFlyingSpeed(float flySpeed) {
        this.flyingSpeed = flySpeed;
    }

    public float getWalkingSpeed() {
        return this.walkingSpeed;
    }

    public void setWalkingSpeed(float walkSpeed) {
        this.walkingSpeed = walkSpeed;
    }
}
