package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeItemStack;

public class TemptingSensor extends Sensor<EntityCreature> {
    public static final int TEMPTATION_RANGE = 10;
    private static final PathfinderTargetCondition TEMPT_TARGETING = PathfinderTargetCondition.forNonCombat().range(10.0D).ignoreLineOfSight();
    private final RecipeItemStack temptations;

    public TemptingSensor(RecipeItemStack ingredient) {
        this.temptations = ingredient;
    }

    @Override
    protected void doTick(WorldServer world, EntityCreature entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        List<EntityHuman> list = world.getPlayers().stream().filter(IEntitySelector.NO_SPECTATORS).filter((playerx) -> {
            return TEMPT_TARGETING.test(entity, playerx);
        }).filter((playerx) -> {
            return entity.closerThan(playerx, 10.0D);
        }).filter(this::playerHoldingTemptation).sorted(Comparator.comparingDouble(entity::distanceToSqr)).collect(Collectors.toList());
        if (!list.isEmpty()) {
            EntityHuman player = list.get(0);
            brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, player);
        } else {
            brain.removeMemory(MemoryModuleType.TEMPTING_PLAYER);
        }

    }

    private boolean playerHoldingTemptation(EntityHuman player) {
        return this.isTemptation(player.getItemInMainHand()) || this.isTemptation(player.getItemInOffHand());
    }

    private boolean isTemptation(ItemStack stack) {
        return this.temptations.test(stack);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
    }
}
