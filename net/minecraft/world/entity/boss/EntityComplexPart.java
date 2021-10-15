package net.minecraft.world.entity.boss;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;

public class EntityComplexPart extends Entity {
    public final EntityEnderDragon parentMob;
    public final String name;
    private final EntitySize size;

    public EntityComplexPart(EntityEnderDragon owner, String name, float width, float height) {
        super(owner.getEntityType(), owner.level);
        this.size = EntitySize.scalable(width, height);
        this.updateSize();
        this.parentMob = owner;
        this.name = name;
    }

    @Override
    protected void initDatawatcher() {
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        return this.isInvulnerable(source) ? false : this.parentMob.hurt(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public Packet<?> getPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
