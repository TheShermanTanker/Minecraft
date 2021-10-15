package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class BehaviorVillageHeroGift extends Behavior<EntityVillager> {
    private static final int THROW_GIFT_AT_DISTANCE = 5;
    private static final int MIN_TIME_BETWEEN_GIFTS = 600;
    private static final int MAX_TIME_BETWEEN_GIFTS = 6600;
    private static final int TIME_TO_DELAY_FOR_HEAD_TO_FINISH_TURNING = 20;
    private static final Map<VillagerProfession, MinecraftKey> GIFTS = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put(VillagerProfession.ARMORER, LootTables.ARMORER_GIFT);
        hashMap.put(VillagerProfession.BUTCHER, LootTables.BUTCHER_GIFT);
        hashMap.put(VillagerProfession.CARTOGRAPHER, LootTables.CARTOGRAPHER_GIFT);
        hashMap.put(VillagerProfession.CLERIC, LootTables.CLERIC_GIFT);
        hashMap.put(VillagerProfession.FARMER, LootTables.FARMER_GIFT);
        hashMap.put(VillagerProfession.FISHERMAN, LootTables.FISHERMAN_GIFT);
        hashMap.put(VillagerProfession.FLETCHER, LootTables.FLETCHER_GIFT);
        hashMap.put(VillagerProfession.LEATHERWORKER, LootTables.LEATHERWORKER_GIFT);
        hashMap.put(VillagerProfession.LIBRARIAN, LootTables.LIBRARIAN_GIFT);
        hashMap.put(VillagerProfession.MASON, LootTables.MASON_GIFT);
        hashMap.put(VillagerProfession.SHEPHERD, LootTables.SHEPHERD_GIFT);
        hashMap.put(VillagerProfession.TOOLSMITH, LootTables.TOOLSMITH_GIFT);
        hashMap.put(VillagerProfession.WEAPONSMITH, LootTables.WEAPONSMITH_GIFT);
    });
    private static final float SPEED_MODIFIER = 0.5F;
    private int timeUntilNextGift = 600;
    private boolean giftGivenDuringThisRun;
    private long timeSinceStart;

    public BehaviorVillageHeroGift(int delay) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryStatus.VALUE_PRESENT), delay);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        if (!this.isHeroVisible(entity)) {
            return false;
        } else if (this.timeUntilNextGift > 0) {
            --this.timeUntilNextGift;
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        this.giftGivenDuringThisRun = false;
        this.timeSinceStart = time;
        EntityHuman player = this.getNearestTargetableHero(entity).get();
        entity.getBehaviorController().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
        BehaviorUtil.lookAtEntity(entity, player);
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityVillager entity, long time) {
        return this.isHeroVisible(entity) && !this.giftGivenDuringThisRun;
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        EntityHuman player = this.getNearestTargetableHero(villager).get();
        BehaviorUtil.lookAtEntity(villager, player);
        if (this.isWithinThrowingDistance(villager, player)) {
            if (l - this.timeSinceStart > 20L) {
                this.throwGift(villager, player);
                this.giftGivenDuringThisRun = true;
            }
        } else {
            BehaviorUtil.setWalkAndLookTargetMemories(villager, player, 0.5F, 5);
        }

    }

    @Override
    protected void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        this.timeUntilNextGift = calculateTimeUntilNextGift(serverLevel);
        villager.getBehaviorController().removeMemory(MemoryModuleType.INTERACTION_TARGET);
        villager.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        villager.getBehaviorController().removeMemory(MemoryModuleType.LOOK_TARGET);
    }

    private void throwGift(EntityVillager villager, EntityLiving recipient) {
        for(ItemStack itemStack : this.getItemToThrow(villager)) {
            BehaviorUtil.throwItem(villager, itemStack, recipient.getPositionVector());
        }

    }

    private List<ItemStack> getItemToThrow(EntityVillager villager) {
        if (villager.isBaby()) {
            return ImmutableList.of(new ItemStack(Items.POPPY));
        } else {
            VillagerProfession villagerProfession = villager.getVillagerData().getProfession();
            if (GIFTS.containsKey(villagerProfession)) {
                LootTable lootTable = villager.level.getMinecraftServer().getLootTableRegistry().getLootTable(GIFTS.get(villagerProfession));
                LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)villager.level)).set(LootContextParameters.ORIGIN, villager.getPositionVector()).set(LootContextParameters.THIS_ENTITY, villager).withRandom(villager.getRandom());
                return lootTable.populateLoot(builder.build(LootContextParameterSets.GIFT));
            } else {
                return ImmutableList.of(new ItemStack(Items.WHEAT_SEEDS));
            }
        }
    }

    private boolean isHeroVisible(EntityVillager villager) {
        return this.getNearestTargetableHero(villager).isPresent();
    }

    private Optional<EntityHuman> getNearestTargetableHero(EntityVillager villager) {
        return villager.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(this::isHero);
    }

    private boolean isHero(EntityHuman player) {
        return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
    }

    private boolean isWithinThrowingDistance(EntityVillager villager, EntityHuman player) {
        BlockPosition blockPos = player.getChunkCoordinates();
        BlockPosition blockPos2 = villager.getChunkCoordinates();
        return blockPos2.closerThan(blockPos, 5.0D);
    }

    private static int calculateTimeUntilNextGift(WorldServer world) {
        return 600 + world.random.nextInt(6001);
    }
}
