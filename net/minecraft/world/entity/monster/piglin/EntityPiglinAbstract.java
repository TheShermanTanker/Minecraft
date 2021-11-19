package net.minecraft.world.entity.monster.piglin;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.util.PathfinderGoalUtil;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.item.ItemToolMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class EntityPiglinAbstract extends EntityMonster {
    protected static final DataWatcherObject<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = DataWatcher.defineId(EntityPiglinAbstract.class, DataWatcherRegistry.BOOLEAN);
    protected static final int CONVERSION_TIME = 300;
    public int timeInOverworld;

    public EntityPiglinAbstract(EntityTypes<? extends EntityPiglinAbstract> type, World world) {
        super(type, world);
        this.setCanPickupLoot(true);
        this.applyOpenDoorsAbility();
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    private void applyOpenDoorsAbility() {
        if (PathfinderGoalUtil.hasGroundPathNavigation(this)) {
            ((Navigation)this.getNavigation()).setCanOpenDoors(true);
        }

    }

    protected abstract boolean canHunt();

    public void setImmuneToZombification(boolean immuneToZombification) {
        this.getDataWatcher().set(DATA_IMMUNE_TO_ZOMBIFICATION, immuneToZombification);
    }

    public boolean isImmuneToZombification() {
        return this.getDataWatcher().get(DATA_IMMUNE_TO_ZOMBIFICATION);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_IMMUNE_TO_ZOMBIFICATION, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.isImmuneToZombification()) {
            nbt.setBoolean("IsImmuneToZombification", true);
        }

        nbt.setInt("TimeInOverworld", this.timeInOverworld);
    }

    @Override
    public double getMyRidingOffset() {
        return this.isBaby() ? -0.05D : -0.45D;
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setImmuneToZombification(nbt.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = nbt.getInt("TimeInOverworld");
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.isConverting()) {
            ++this.timeInOverworld;
        } else {
            this.timeInOverworld = 0;
        }

        if (this.timeInOverworld > 300) {
            this.playConvertedSound();
            this.finishConversion((WorldServer)this.level);
        }

    }

    public boolean isConverting() {
        return !this.level.getDimensionManager().isPiglinSafe() && !this.isImmuneToZombification() && !this.isNoAI();
    }

    protected void finishConversion(WorldServer world) {
        EntityPigZombie zombifiedPiglin = this.convertTo(EntityTypes.ZOMBIFIED_PIGLIN, true);
        if (zombifiedPiglin != null) {
            zombifiedPiglin.addEffect(new MobEffect(MobEffectList.CONFUSION, 200, 0));
        }

    }

    public boolean isAdult() {
        return !this.isBaby();
    }

    public abstract EntityPiglinArmPose getArmPose();

    @Nullable
    @Override
    public EntityLiving getGoalTarget() {
        return this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse((EntityLiving)null);
    }

    protected boolean isHoldingMeleeWeapon() {
        return this.getItemInMainHand().getItem() instanceof ItemToolMaterial;
    }

    @Override
    public void playAmbientSound() {
        if (PiglinAI.isIdle(this)) {
            super.playAmbientSound();
        }

    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendEntityBrain(this);
    }

    protected abstract void playConvertedSound();
}
