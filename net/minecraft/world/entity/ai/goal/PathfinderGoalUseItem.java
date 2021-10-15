package net.minecraft.world.entity.ai.goal;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;

public class PathfinderGoalUseItem<T extends EntityInsentient> extends PathfinderGoal {
    private final T mob;
    private final ItemStack item;
    private final Predicate<? super T> canUseSelector;
    private final SoundEffect finishUsingSound;

    public PathfinderGoalUseItem(T actor, ItemStack item, @Nullable SoundEffect sound, Predicate<? super T> condition) {
        this.mob = actor;
        this.item = item;
        this.finishUsingSound = sound;
        this.canUseSelector = condition;
    }

    @Override
    public boolean canUse() {
        return this.canUseSelector.test(this.mob);
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isHandRaised();
    }

    @Override
    public void start() {
        this.mob.setSlot(EnumItemSlot.MAINHAND, this.item.cloneItemStack());
        this.mob.startUsingItem(EnumHand.MAIN_HAND);
    }

    @Override
    public void stop() {
        this.mob.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
        if (this.finishUsingSound != null) {
            this.mob.playSound(this.finishUsingSound, 1.0F, this.mob.getRandom().nextFloat() * 0.2F + 0.9F);
        }

    }
}
