package net.minecraft.world.level.entity;

import net.minecraft.world.entity.Entity;

public interface EntityWorldCallback {
    EntityWorldCallback NULL = new EntityWorldCallback() {
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
