package net.minecraft.world.entity.npc;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalInteract;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtTradingPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTradeWithPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalUseItem;
import net.minecraft.world.entity.monster.EntityEvoker;
import net.minecraft.world.entity.monster.EntityIllagerIllusioner;
import net.minecraft.world.entity.monster.EntityPillager;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.entity.monster.EntityVindicator;
import net.minecraft.world.entity.monster.EntityZoglin;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class EntityVillagerTrader extends EntityVillagerAbstract {
    private static final int NUMBER_OF_TRADE_OFFERS = 5;
    @Nullable
    private BlockPosition wanderTarget;
    private int despawnDelay;

    public EntityVillagerTrader(EntityTypes<? extends EntityVillagerTrader> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(0, new PathfinderGoalUseItem<>(this, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY), SoundEffects.WANDERING_TRADER_DISAPPEARED, (wanderingTrader) -> {
            return this.level.isNight() && !wanderingTrader.isInvisible();
        }));
        this.goalSelector.addGoal(0, new PathfinderGoalUseItem<>(this, new ItemStack(Items.MILK_BUCKET), SoundEffects.WANDERING_TRADER_REAPPEARED, (wanderingTrader) -> {
            return this.level.isDay() && wanderingTrader.isInvisible();
        }));
        this.goalSelector.addGoal(1, new PathfinderGoalTradeWithPlayer(this));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityZombie.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityEvoker.class, 12.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityVindicator.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityVex.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityPillager.class, 15.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityIllagerIllusioner.class, 12.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalAvoidTarget<>(this, EntityZoglin.class, 10.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 0.5D));
        this.goalSelector.addGoal(1, new PathfinderGoalLookAtTradingPlayer(this));
        this.goalSelector.addGoal(2, new EntityVillagerTrader.WanderToPositionGoal(this, 2.0D, 0.35D));
        this.goalSelector.addGoal(4, new PathfinderGoalMoveTowardsRestriction(this, 0.35D));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStrollLand(this, 0.35D));
        this.goalSelector.addGoal(9, new PathfinderGoalInteract(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
    }

    @Nullable
    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return null;
    }

    @Override
    public boolean isRegularVillager() {
        return false;
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == EnumHand.MAIN_HAND) {
                player.awardStat(StatisticList.TALKED_TO_VILLAGER);
            }

            if (this.getOffers().isEmpty()) {
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            } else {
                if (!this.level.isClientSide) {
                    this.setTradingPlayer(player);
                    this.openTrade(player, this.getScoreboardDisplayName(), 1);
                }

                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    protected void updateTrades() {
        VillagerTrades.IMerchantRecipeOption[] itemListings = VillagerTrades.WANDERING_TRADER_TRADES.get(1);
        VillagerTrades.IMerchantRecipeOption[] itemListings2 = VillagerTrades.WANDERING_TRADER_TRADES.get(2);
        if (itemListings != null && itemListings2 != null) {
            MerchantRecipeList merchantOffers = this.getOffers();
            this.addOffersFromItemListings(merchantOffers, itemListings, 5);
            int i = this.random.nextInt(itemListings2.length);
            VillagerTrades.IMerchantRecipeOption itemListing = itemListings2[i];
            MerchantRecipe merchantOffer = itemListing.getOffer(this, this.random);
            if (merchantOffer != null) {
                merchantOffers.add(merchantOffer);
            }

        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("DespawnDelay", this.despawnDelay);
        if (this.wanderTarget != null) {
            nbt.set("WanderTarget", GameProfileSerializer.writeBlockPos(this.wanderTarget));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("DespawnDelay", 99)) {
            this.despawnDelay = nbt.getInt("DespawnDelay");
        }

        if (nbt.hasKey("WanderTarget")) {
            this.wanderTarget = GameProfileSerializer.readBlockPos(nbt.getCompound("WanderTarget"));
        }

        this.setAgeRaw(Math.max(0, this.getAge()));
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return false;
    }

    @Override
    protected void rewardTradeXp(MerchantRecipe offer) {
        if (offer.isRewardExp()) {
            int i = 3 + this.random.nextInt(4);
            this.level.addEntity(new EntityExperienceOrb(this.level, this.locX(), this.locY() + 0.5D, this.locZ(), i));
        }

    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isTrading() ? SoundEffects.WANDERING_TRADER_TRADE : SoundEffects.WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.WANDERING_TRADER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.WANDERING_TRADER_DEATH;
    }

    @Override
    protected SoundEffect getDrinkingSound(ItemStack stack) {
        return stack.is(Items.MILK_BUCKET) ? SoundEffects.WANDERING_TRADER_DRINK_MILK : SoundEffects.WANDERING_TRADER_DRINK_POTION;
    }

    @Override
    protected SoundEffect getTradeUpdatedSound(boolean sold) {
        return sold ? SoundEffects.WANDERING_TRADER_YES : SoundEffects.WANDERING_TRADER_NO;
    }

    @Override
    public SoundEffect getTradeSound() {
        return SoundEffects.WANDERING_TRADER_YES;
    }

    public void setDespawnDelay(int delay) {
        this.despawnDelay = delay;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (!this.level.isClientSide) {
            this.maybeDespawn();
        }

    }

    private void maybeDespawn() {
        if (this.despawnDelay > 0 && !this.isTrading() && --this.despawnDelay == 0) {
            this.die();
        }

    }

    public void setWanderTarget(@Nullable BlockPosition pos) {
        this.wanderTarget = pos;
    }

    @Nullable
    BlockPosition getWanderTarget() {
        return this.wanderTarget;
    }

    class WanderToPositionGoal extends PathfinderGoal {
        final EntityVillagerTrader trader;
        final double stopDistance;
        final double speedModifier;

        WanderToPositionGoal(EntityVillagerTrader trader, double proximityDistance, double speed) {
            this.trader = trader;
            this.stopDistance = proximityDistance;
            this.speedModifier = speed;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public void stop() {
            this.trader.setWanderTarget((BlockPosition)null);
            EntityVillagerTrader.this.navigation.stop();
        }

        @Override
        public boolean canUse() {
            BlockPosition blockPos = this.trader.getWanderTarget();
            return blockPos != null && this.isTooFarAway(blockPos, this.stopDistance);
        }

        @Override
        public void tick() {
            BlockPosition blockPos = this.trader.getWanderTarget();
            if (blockPos != null && EntityVillagerTrader.this.navigation.isDone()) {
                if (this.isTooFarAway(blockPos, 10.0D)) {
                    Vec3D vec3 = (new Vec3D((double)blockPos.getX() - this.trader.locX(), (double)blockPos.getY() - this.trader.locY(), (double)blockPos.getZ() - this.trader.locZ())).normalize();
                    Vec3D vec32 = vec3.scale(10.0D).add(this.trader.locX(), this.trader.locY(), this.trader.locZ());
                    EntityVillagerTrader.this.navigation.moveTo(vec32.x, vec32.y, vec32.z, this.speedModifier);
                } else {
                    EntityVillagerTrader.this.navigation.moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), this.speedModifier);
                }
            }

        }

        private boolean isTooFarAway(BlockPosition pos, double proximityDistance) {
            return !pos.closerThan(this.trader.getPositionVector(), proximityDistance);
        }
    }
}
