package net.minecraft.world.entity.animal;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityTameableAnimal;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;

public abstract class EntityPerchable extends EntityTameableAnimal {
    private static final int RIDE_COOLDOWN = 100;
    private int rideCooldownCounter;

    protected EntityPerchable(EntityTypes<? extends EntityPerchable> type, World world) {
        super(type, world);
    }

    public boolean setEntityOnShoulder(EntityPlayer player) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("id", this.getSaveID());
        this.save(compoundTag);
        if (player.setEntityOnShoulder(compoundTag)) {
            this.die();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void tick() {
        ++this.rideCooldownCounter;
        super.tick();
    }

    public boolean canSitOnShoulder() {
        return this.rideCooldownCounter > 100;
    }
}
