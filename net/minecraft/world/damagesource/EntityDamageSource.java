package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;

public class EntityDamageSource extends DamageSource {
    protected final Entity entity;
    private boolean isThorns;

    public EntityDamageSource(String name, Entity source) {
        super(name);
        this.entity = source;
    }

    public EntityDamageSource setThorns() {
        this.isThorns = true;
        return this;
    }

    public boolean isThorns() {
        return this.isThorns;
    }

    @Override
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public IChatBaseComponent getLocalizedDeathMessage(EntityLiving entity) {
        ItemStack itemStack = this.entity instanceof EntityLiving ? ((EntityLiving)this.entity).getItemInMainHand() : ItemStack.EMPTY;
        String string = "death.attack." + this.msgId;
        return !itemStack.isEmpty() && itemStack.hasName() ? new ChatMessage(string + ".item", entity.getScoreboardDisplayName(), this.entity.getScoreboardDisplayName(), itemStack.getDisplayName()) : new ChatMessage(string, entity.getScoreboardDisplayName(), this.entity.getScoreboardDisplayName());
    }

    @Override
    public boolean scalesWithDifficulty() {
        return this.entity instanceof EntityLiving && !(this.entity instanceof EntityHuman);
    }

    @Nullable
    @Override
    public Vec3D getSourcePosition() {
        return this.entity.getPositionVector();
    }

    @Override
    public String toString() {
        return "EntityDamageSource (" + this.entity + ")";
    }
}
