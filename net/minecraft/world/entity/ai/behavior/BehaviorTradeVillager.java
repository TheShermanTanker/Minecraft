package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BehaviorTradeVillager extends Behavior<EntityVillager> {
    private static final int INTERACT_DIST_SQR = 5;
    private static final float SPEED_MODIFIER = 0.5F;
    private Set<Item> trades = ImmutableSet.of();

    public BehaviorTradeVillager() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        return BehaviorUtil.targetIsValid(entity.getBehaviorController(), MemoryModuleType.INTERACTION_TARGET, EntityTypes.VILLAGER);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.checkExtraStartConditions(serverLevel, villager);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityVillager villager2 = (EntityVillager)villager.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        BehaviorUtil.lockGazeAndWalkToEachOther(villager, villager2, 0.5F);
        this.trades = figureOutWhatIAmWillingToTrade(villager, villager2);
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityVillager villager2 = (EntityVillager)villager.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (!(villager.distanceToSqr(villager2) > 5.0D)) {
            BehaviorUtil.lockGazeAndWalkToEachOther(villager, villager2, 0.5F);
            villager.gossip(serverLevel, villager2, l);
            if (villager.hasExcessFood() && (villager.getVillagerData().getProfession() == VillagerProfession.FARMER || villager2.wantsMoreFood())) {
                throwHalfStack(villager, EntityVillager.FOOD_POINTS.keySet(), villager2);
            }

            if (villager2.getVillagerData().getProfession() == VillagerProfession.FARMER && villager.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getMaxStackSize() / 2) {
                throwHalfStack(villager, ImmutableSet.of(Items.WHEAT), villager2);
            }

            if (!this.trades.isEmpty() && villager.getInventory().hasAnyOf(this.trades)) {
                throwHalfStack(villager, this.trades, villager2);
            }

        }
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        villager.getBehaviorController().removeMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private static Set<Item> figureOutWhatIAmWillingToTrade(EntityVillager villager, EntityVillager villager2) {
        ImmutableSet<Item> immutableSet = villager2.getVillagerData().getProfession().getRequestedItems();
        ImmutableSet<Item> immutableSet2 = villager.getVillagerData().getProfession().getRequestedItems();
        return immutableSet.stream().filter((item) -> {
            return !immutableSet2.contains(item);
        }).collect(Collectors.toSet());
    }

    private static void throwHalfStack(EntityVillager villager, Set<Item> validItems, EntityLiving target) {
        InventorySubcontainer simpleContainer = villager.getInventory();
        ItemStack itemStack = ItemStack.EMPTY;
        int i = 0;

        while(i < simpleContainer.getSize()) {
            ItemStack itemStack2;
            Item item;
            int j;
            label28: {
                itemStack2 = simpleContainer.getItem(i);
                if (!itemStack2.isEmpty()) {
                    item = itemStack2.getItem();
                    if (validItems.contains(item)) {
                        if (itemStack2.getCount() > itemStack2.getMaxStackSize() / 2) {
                            j = itemStack2.getCount() / 2;
                            break label28;
                        }

                        if (itemStack2.getCount() > 24) {
                            j = itemStack2.getCount() - 24;
                            break label28;
                        }
                    }
                }

                ++i;
                continue;
            }

            itemStack2.subtract(j);
            itemStack = new ItemStack(item, j);
            break;
        }

        if (!itemStack.isEmpty()) {
            BehaviorUtil.throwItem(villager, itemStack, target.getPositionVector());
        }

    }
}
