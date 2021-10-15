package net.minecraft.world.entity.animal.horse;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.monster.EntitySkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;

public class PathfinderGoalHorseTrap extends PathfinderGoal {
    private final EntityHorseSkeleton horse;

    public PathfinderGoalHorseTrap(EntityHorseSkeleton skeletonHorse) {
        this.horse = skeletonHorse;
    }

    @Override
    public boolean canUse() {
        return this.horse.level.isPlayerNearby(this.horse.locX(), this.horse.locY(), this.horse.locZ(), 10.0D);
    }

    @Override
    public void tick() {
        WorldServer serverLevel = (WorldServer)this.horse.level;
        DifficultyDamageScaler difficultyInstance = serverLevel.getDamageScaler(this.horse.getChunkCoordinates());
        this.horse.setTrap(false);
        this.horse.setTamed(true);
        this.horse.setAgeRaw(0);
        EntityLightning lightningBolt = EntityTypes.LIGHTNING_BOLT.create(serverLevel);
        lightningBolt.teleportAndSync(this.horse.locX(), this.horse.locY(), this.horse.locZ());
        lightningBolt.setEffect(true);
        serverLevel.addEntity(lightningBolt);
        EntitySkeleton skeleton = this.createSkeleton(difficultyInstance, this.horse);
        skeleton.startRiding(this.horse);
        serverLevel.addAllEntities(skeleton);

        for(int i = 0; i < 3; ++i) {
            EntityHorseAbstract abstractHorse = this.createHorse(difficultyInstance);
            EntitySkeleton skeleton2 = this.createSkeleton(difficultyInstance, abstractHorse);
            skeleton2.startRiding(abstractHorse);
            abstractHorse.push(this.horse.getRandom().nextGaussian() * 0.5D, 0.0D, this.horse.getRandom().nextGaussian() * 0.5D);
            serverLevel.addAllEntities(abstractHorse);
        }

    }

    private EntityHorseAbstract createHorse(DifficultyDamageScaler localDifficulty) {
        EntityHorseSkeleton skeletonHorse = EntityTypes.SKELETON_HORSE.create(this.horse.level);
        skeletonHorse.prepare((WorldServer)this.horse.level, localDifficulty, EnumMobSpawn.TRIGGERED, (GroupDataEntity)null, (NBTTagCompound)null);
        skeletonHorse.setPosition(this.horse.locX(), this.horse.locY(), this.horse.locZ());
        skeletonHorse.invulnerableTime = 60;
        skeletonHorse.setPersistent();
        skeletonHorse.setTamed(true);
        skeletonHorse.setAgeRaw(0);
        return skeletonHorse;
    }

    private EntitySkeleton createSkeleton(DifficultyDamageScaler localDifficulty, EntityHorseAbstract vehicle) {
        EntitySkeleton skeleton = EntityTypes.SKELETON.create(vehicle.level);
        skeleton.prepare((WorldServer)vehicle.level, localDifficulty, EnumMobSpawn.TRIGGERED, (GroupDataEntity)null, (NBTTagCompound)null);
        skeleton.setPosition(vehicle.locX(), vehicle.locY(), vehicle.locZ());
        skeleton.invulnerableTime = 60;
        skeleton.setPersistent();
        if (skeleton.getEquipment(EnumItemSlot.HEAD).isEmpty()) {
            skeleton.setSlot(EnumItemSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }

        skeleton.setSlot(EnumItemSlot.MAINHAND, EnchantmentManager.enchantItem(skeleton.getRandom(), this.disenchant(skeleton.getItemInMainHand()), (int)(5.0F + localDifficulty.getSpecialMultiplier() * (float)skeleton.getRandom().nextInt(18)), false));
        skeleton.setSlot(EnumItemSlot.HEAD, EnchantmentManager.enchantItem(skeleton.getRandom(), this.disenchant(skeleton.getEquipment(EnumItemSlot.HEAD)), (int)(5.0F + localDifficulty.getSpecialMultiplier() * (float)skeleton.getRandom().nextInt(18)), false));
        return skeleton;
    }

    private ItemStack disenchant(ItemStack stack) {
        stack.removeTag("Enchantments");
        return stack;
    }
}
