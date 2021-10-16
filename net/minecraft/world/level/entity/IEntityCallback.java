package net.minecraft.world.level.entity;

import net.minecraft.world.entity.Entity;

public interface IEntityCallback {
    IEntityCallback NULL = new IEntityCallback() {
        @Override
        public void onMove() {
        }

        @Override
        public void onRemove(Entity.RemovalReason reason) {
        }
    };

    void onMove();

    void onRemove(Entity.RemovalReason reason);
}
