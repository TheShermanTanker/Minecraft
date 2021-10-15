package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.ItemStack;

public class EntityDamageSourceIndirect extends EntityDamageSource {
    @Nullable
    private final Entity owner;

    public EntityDamageSourceIndirect(String name, Entity projectile, @Nullable Entity attacker) {
        super(name, projectile);
        this.owner = attacker;
    }

    @Nullable
    @Override
    public Entity getDirectEntity() {
        return this.entity;
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return this.owner;
    }

    @Override
    public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entity) {
        IChatBaseComponent component = this.owner == null ? this.entity.getScoreboardDisplayName() : this.owner.getScoreboardDisplayName();
        ItemStack itemStack = this.owner instanceof EntityLiving ? ((EntityLiving)this.owner).getItemInMainHand() : ItemStack.EMPTY;
        String string = "death.attack." + this.msgId;
        String string2 = string + ".item";
        return !itemStack.isEmpty() && itemStack.hasName() ? new ChatMessage(string2, entity.getScoreboardDisplayName(), component, itemStack.getDisplayName()) : new ChatMessage(string, entity.getScoreboardDisplayName(), component);
    }
}
