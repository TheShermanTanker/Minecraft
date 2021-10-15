package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantRecipe;

public class BehaviorTradePlayer extends Behavior<EntityVillager> {
    private static final int MAX_LOOK_TIME = 900;
    private static final int STARTING_LOOK_TIME = 40;
    @Nullable
    private ItemStack playerItemStack;
    private final List<ItemStack> displayItems = Lists.newArrayList();
    private int cycleCounter;
    private int displayIndex;
    private int lookTime;

    public BehaviorTradePlayer(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    public boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        if (!brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent()) {
            return false;
        } else {
            EntityLiving livingEntity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            return livingEntity.getEntityType() == EntityTypes.PLAYER && entity.isAlive() && livingEntity.isAlive() && !entity.isBaby() && entity.distanceToSqr(livingEntity) <= 17.0D;
        }
    }

    @Override
    public boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.checkExtraStartConditions(serverLevel, villager) && this.lookTime > 0 && villager.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    @Override
    public void start(WorldServer serverLevel, EntityVillager villager, long l) {
        super.start(serverLevel, villager, l);
        this.lookAtTarget(villager);
        this.cycleCounter = 0;
        this.displayIndex = 0;
        this.lookTime = 40;
    }

    @Override
    public void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityLiving livingEntity = this.lookAtTarget(villager);
        this.findItemsToDisplay(livingEntity, villager);
        if (!this.displayItems.isEmpty()) {
            this.displayCyclingItems(villager);
        } else {
            clearHeldItem(villager);
            this.lookTime = Math.min(this.lookTime, 40);
        }

        --this.lookTime;
    }

    @Override
    public void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        super.stop(serverLevel, villager, l);
        villager.getBehaviorController().removeMemory(MemoryModuleType.INTERACTION_TARGET);
        clearHeldItem(villager);
        this.playerItemStack = null;
    }

    private void findItemsToDisplay(EntityLiving customer, EntityVillager villager) {
        boolean bl = false;
        ItemStack itemStack = customer.getItemInMainHand();
        if (this.playerItemStack == null || !ItemStack.isSame(this.playerItemStack, itemStack)) {
            this.playerItemStack = itemStack;
            bl = true;
            this.displayItems.clear();
        }

        if (bl && !this.playerItemStack.isEmpty()) {
            this.updateDisplayItems(villager);
            if (!this.displayItems.isEmpty()) {
                this.lookTime = 900;
                this.displayFirstItem(villager);
            }
        }

    }

    private void displayFirstItem(EntityVillager villager) {
        displayAsHeldItem(villager, this.displayItems.get(0));
    }

    private void updateDisplayItems(EntityVillager villager) {
        for(MerchantRecipe merchantOffer : villager.getOffers()) {
            if (!merchantOffer.isFullyUsed() && this.playerItemStackMatchesCostOfOffer(merchantOffer)) {
                this.displayItems.add(merchantOffer.getSellingItem());
            }
        }

    }

    private boolean playerItemStackMatchesCostOfOffer(MerchantRecipe offer) {
        return ItemStack.isSame(this.playerItemStack, offer.getBuyItem1()) || ItemStack.isSame(this.playerItemStack, offer.getBuyItem2());
    }

    private static void clearHeldItem(EntityVillager villager) {
        villager.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
        villager.setDropChance(EnumItemSlot.MAINHAND, 0.085F);
    }

    private static void displayAsHeldItem(EntityVillager villager, ItemStack stack) {
        villager.setSlot(EnumItemSlot.MAINHAND, stack);
        villager.setDropChance(EnumItemSlot.MAINHAND, 0.0F);
    }

    private EntityLiving lookAtTarget(EntityVillager villager) {
        BehaviorController<?> brain = villager.getBehaviorController();
        EntityLiving livingEntity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(livingEntity, true));
        return livingEntity;
    }

    private void displayCyclingItems(EntityVillager villager) {
        if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
            ++this.displayIndex;
            this.cycleCounter = 0;
            if (this.displayIndex > this.displayItems.size() - 1) {
                this.displayIndex = 0;
            }

            displayAsHeldItem(villager, this.displayItems.get(this.displayIndex));
        }

    }
}
