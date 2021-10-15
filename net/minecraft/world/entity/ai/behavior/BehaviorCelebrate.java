package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemFireworks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BehaviorCelebrate extends Behavior<EntityVillager> {
    @Nullable
    private Raid currentRaid;

    public BehaviorCelebrate(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(), minRunTime, maxRunTime);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        BlockPosition blockPos = entity.getChunkCoordinates();
        this.currentRaid = world.getRaidAt(blockPos);
        return this.currentRaid != null && this.currentRaid.isVictory() && BehaviorOutside.hasNoBlocksAbove(world, entity, blockPos);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.currentRaid != null && !this.currentRaid.isStopped();
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        this.currentRaid = null;
        villager.getBehaviorController().updateActivityFromSchedule(serverLevel.getDayTime(), serverLevel.getTime());
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        Random random = villager.getRandom();
        if (random.nextInt(100) == 0) {
            villager.playCelebrateSound();
        }

        if (random.nextInt(200) == 0 && BehaviorOutside.hasNoBlocksAbove(serverLevel, villager, villager.getChunkCoordinates())) {
            EnumColor dyeColor = SystemUtils.getRandom(EnumColor.values(), random);
            int i = random.nextInt(3);
            ItemStack itemStack = this.getFirework(dyeColor, i);
            EntityFireworks fireworkRocketEntity = new EntityFireworks(villager.level, villager, villager.locX(), villager.getHeadY(), villager.locZ(), itemStack);
            villager.level.addEntity(fireworkRocketEntity);
        }

    }

    private ItemStack getFirework(EnumColor color, int flight) {
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET, 1);
        ItemStack itemStack2 = new ItemStack(Items.FIREWORK_STAR);
        NBTTagCompound compoundTag = itemStack2.getOrCreateTagElement("Explosion");
        List<Integer> list = Lists.newArrayList();
        list.add(color.getFireworksColor());
        compoundTag.putIntArray("Colors", list);
        compoundTag.setByte("Type", (byte)ItemFireworks.EffectType.BURST.getId());
        NBTTagCompound compoundTag2 = itemStack.getOrCreateTagElement("Fireworks");
        NBTTagList listTag = new NBTTagList();
        NBTTagCompound compoundTag3 = itemStack2.getTagElement("Explosion");
        if (compoundTag3 != null) {
            listTag.add(compoundTag3);
        }

        compoundTag2.setByte("Flight", (byte)flight);
        if (!listTag.isEmpty()) {
            compoundTag2.set("Explosions", listTag);
        }

        return itemStack;
    }
}
