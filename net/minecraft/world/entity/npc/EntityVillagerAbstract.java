package net.minecraft.world.entity.npc;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.IMerchant;
import net.minecraft.world.item.trading.MerchantRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityVillagerAbstract extends EntityAgeable implements InventoryCarrier, NPC, IMerchant {
    private static final DataWatcherObject<Integer> DATA_UNHAPPY_COUNTER = DataWatcher.defineId(EntityVillagerAbstract.class, DataWatcherRegistry.INT);
    public static final int VILLAGER_SLOT_OFFSET = 300;
    private static final int VILLAGER_INVENTORY_SIZE = 8;
    @Nullable
    private EntityHuman tradingPlayer;
    @Nullable
    protected MerchantRecipeList offers;
    private final InventorySubcontainer inventory = new InventorySubcontainer(8);

    public EntityVillagerAbstract(EntityTypes<? extends EntityVillagerAbstract> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(false);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    public int getUnhappyCounter() {
        return this.entityData.get(DATA_UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int ticks) {
        this.entityData.set(DATA_UNHAPPY_COUNTER, ticks);
    }

    @Override
    public int getExperience() {
        return 0;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return this.isBaby() ? 0.81F : 1.62F;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_UNHAPPY_COUNTER, 0);
    }

    @Override
    public void setTradingPlayer(@Nullable EntityHuman customer) {
        this.tradingPlayer = customer;
    }

    @Nullable
    @Override
    public EntityHuman getTrader() {
        return this.tradingPlayer;
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public MerchantRecipeList getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantRecipeList();
            this.updateTrades();
        }

        return this.offers;
    }

    @Override
    public void overrideOffers(@Nullable MerchantRecipeList offers) {
    }

    @Override
    public void setForcedExperience(int experience) {
    }

    @Override
    public void notifyTrade(MerchantRecipe offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.rewardTradeXp(offer);
        if (this.tradingPlayer instanceof EntityPlayer) {
            CriterionTriggers.TRADE.trigger((EntityPlayer)this.tradingPlayer, this, offer.getSellingItem());
        }

    }

    protected abstract void rewardTradeXp(MerchantRecipe offer);

    @Override
    public boolean isRegularVillager() {
        return true;
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level.isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(this.getTradeUpdatedSound(!stack.isEmpty()), this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public SoundEffect getTradeSound() {
        return SoundEffects.VILLAGER_YES;
    }

    protected SoundEffect getTradeUpdatedSound(boolean sold) {
        return sold ? SoundEffects.VILLAGER_YES : SoundEffects.VILLAGER_NO;
    }

    public void playCelebrateSound() {
        this.playSound(SoundEffects.VILLAGER_CELEBRATE, this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        MerchantRecipeList merchantOffers = this.getOffers();
        if (!merchantOffers.isEmpty()) {
            nbt.set("Offers", merchantOffers.createTag());
        }

        nbt.set("Inventory", this.inventory.createTag());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("Offers", 10)) {
            this.offers = new MerchantRecipeList(nbt.getCompound("Offers"));
        }

        this.inventory.fromTag(nbt.getList("Inventory", 10));
    }

    @Nullable
    @Override
    public Entity changeDimension(WorldServer destination) {
        this.stopTrading();
        return super.changeDimension(destination);
    }

    protected void stopTrading() {
        this.setTradingPlayer((EntityHuman)null);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.stopTrading();
    }

    protected void addParticlesAroundSelf(ParticleParam parameters) {
        for(int i = 0; i < 5; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(parameters, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return false;
    }

    @Override
    public InventorySubcontainer getInventory() {
        return this.inventory;
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        int i = mappedIndex - 300;
        return i >= 0 && i < this.inventory.getSize() ? SlotAccess.forContainer(this.inventory, i) : super.getSlot(mappedIndex);
    }

    @Override
    public World getWorld() {
        return this.level;
    }

    protected abstract void updateTrades();

    protected void addOffersFromItemListings(MerchantRecipeList recipeList, VillagerTrades.IMerchantRecipeOption[] pool, int count) {
        Set<Integer> set = Sets.newHashSet();
        if (pool.length > count) {
            while(set.size() < count) {
                set.add(this.random.nextInt(pool.length));
            }
        } else {
            for(int i = 0; i < pool.length; ++i) {
                set.add(i);
            }
        }

        for(Integer integer : set) {
            VillagerTrades.IMerchantRecipeOption itemListing = pool[integer];
            MerchantRecipe merchantOffer = itemListing.getOffer(this, this.random);
            if (merchantOffer != null) {
                recipeList.add(merchantOffer);
            }
        }

    }

    @Override
    public Vec3D getRopeHoldPosition(float f) {
        float g = MathHelper.lerp(f, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
        Vec3D vec3 = new Vec3D(0.0D, this.getBoundingBox().getYsize() - 1.0D, 0.2D);
        return this.getPosition(f).add(vec3.yRot(-g));
    }
}
