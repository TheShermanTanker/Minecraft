package net.minecraft.world.entity.monster;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.village.ReputationEvent;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;

public class EntityZombieVillager extends EntityZombie implements VillagerDataHolder {
    public static final DataWatcherObject<Boolean> DATA_CONVERTING_ID = DataWatcher.defineId(EntityZombieVillager.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<VillagerData> DATA_VILLAGER_DATA = DataWatcher.defineId(EntityZombieVillager.class, DataWatcherRegistry.VILLAGER_DATA);
    private static final int VILLAGER_CONVERSION_WAIT_MIN = 3600;
    private static final int VILLAGER_CONVERSION_WAIT_MAX = 6000;
    private static final int MAX_SPECIAL_BLOCKS_COUNT = 14;
    private static final int SPECIAL_BLOCK_RADIUS = 4;
    public int villagerConversionTime;
    public UUID conversionStarter;
    private NBTBase gossips;
    private NBTTagCompound tradeOffers;
    private int villagerXp;

    public EntityZombieVillager(EntityTypes<? extends EntityZombieVillager> type, World world) {
        super(type, world);
        this.setVillagerData(this.getVillagerData().withProfession(IRegistry.VILLAGER_PROFESSION.getRandom(this.random)));
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_CONVERTING_ID, false);
        this.entityData.register(DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        VillagerData.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("VillagerData", tag);
        });
        if (this.tradeOffers != null) {
            nbt.set("Offers", this.tradeOffers);
        }

        if (this.gossips != null) {
            nbt.set("Gossips", this.gossips);
        }

        nbt.setInt("ConversionTime", this.isConverting() ? this.villagerConversionTime : -1);
        if (this.conversionStarter != null) {
            nbt.putUUID("ConversionPlayer", this.conversionStarter);
        }

        nbt.setInt("Xp", this.villagerXp);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("VillagerData", 10)) {
            DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("VillagerData")));
            dataResult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
        }

        if (nbt.hasKeyOfType("Offers", 10)) {
            this.tradeOffers = nbt.getCompound("Offers");
        }

        if (nbt.hasKeyOfType("Gossips", 10)) {
            this.gossips = nbt.getList("Gossips", 10);
        }

        if (nbt.hasKeyOfType("ConversionTime", 99) && nbt.getInt("ConversionTime") > -1) {
            this.startConversion(nbt.hasUUID("ConversionPlayer") ? nbt.getUUID("ConversionPlayer") : null, nbt.getInt("ConversionTime"));
        }

        if (nbt.hasKeyOfType("Xp", 3)) {
            this.villagerXp = nbt.getInt("Xp");
        }

    }

    @Override
    public void tick() {
        if (!this.level.isClientSide && this.isAlive() && this.isConverting()) {
            int i = this.getConversionProgress();
            this.villagerConversionTime -= i;
            if (this.villagerConversionTime <= 0) {
                this.finishConversion((WorldServer)this.level);
            }
        }

        super.tick();
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.GOLDEN_APPLE)) {
            if (this.hasEffect(MobEffects.WEAKNESS)) {
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                if (!this.level.isClientSide) {
                    this.startConversion(player.getUniqueID(), this.random.nextInt(2401) + 3600);
                }

                this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                return EnumInteractionResult.SUCCESS;
            } else {
                return EnumInteractionResult.CONSUME;
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return !this.isConverting() && this.villagerXp == 0;
    }

    public boolean isConverting() {
        return this.getDataWatcher().get(DATA_CONVERTING_ID);
    }

    public void startConversion(@Nullable UUID uuid, int delay) {
        this.conversionStarter = uuid;
        this.villagerConversionTime = delay;
        this.getDataWatcher().set(DATA_CONVERTING_ID, true);
        this.removeEffect(MobEffects.WEAKNESS);
        this.addEffect(new MobEffect(MobEffects.DAMAGE_BOOST, delay, Math.min(this.level.getDifficulty().getId() - 1, 0)));
        this.level.broadcastEntityEffect(this, (byte)16);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 16) {
            if (!this.isSilent()) {
                this.level.playLocalSound(this.locX(), this.getHeadY(), this.locZ(), SoundEffects.ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
            }

        } else {
            super.handleEntityEvent(status);
        }
    }

    private void finishConversion(WorldServer world) {
        EntityVillager villager = this.convertTo(EntityTypes.VILLAGER, false);

        for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
            ItemStack itemStack = this.getEquipment(equipmentSlot);
            if (!itemStack.isEmpty()) {
                if (EnchantmentManager.hasBindingCurse(itemStack)) {
                    villager.getSlot(equipmentSlot.getIndex() + 300).set(itemStack);
                } else {
                    double d = (double)this.getEquipmentDropChance(equipmentSlot);
                    if (d > 1.0D) {
                        this.spawnAtLocation(itemStack);
                    }
                }
            }
        }

        villager.setVillagerData(this.getVillagerData());
        if (this.gossips != null) {
            villager.setGossips(this.gossips);
        }

        if (this.tradeOffers != null) {
            villager.setOffers(new MerchantRecipeList(this.tradeOffers));
        }

        villager.setExperience(this.villagerXp);
        villager.prepare(world, world.getDamageScaler(villager.getChunkCoordinates()), EnumMobSpawn.CONVERSION, (GroupDataEntity)null, (NBTTagCompound)null);
        if (this.conversionStarter != null) {
            EntityHuman player = world.getPlayerByUUID(this.conversionStarter);
            if (player instanceof EntityPlayer) {
                CriterionTriggers.CURED_ZOMBIE_VILLAGER.trigger((EntityPlayer)player, this, villager);
                world.onReputationEvent(ReputationEvent.ZOMBIE_VILLAGER_CURED, player, villager);
            }
        }

        villager.addEffect(new MobEffect(MobEffects.CONFUSION, 200, 0));
        if (!this.isSilent()) {
            world.triggerEffect((EntityHuman)null, 1027, this.getChunkCoordinates(), 0);
        }

    }

    private int getConversionProgress() {
        int i = 1;
        if (this.random.nextFloat() < 0.01F) {
            int j = 0;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int k = (int)this.locX() - 4; k < (int)this.locX() + 4 && j < 14; ++k) {
                for(int l = (int)this.locY() - 4; l < (int)this.locY() + 4 && j < 14; ++l) {
                    for(int m = (int)this.locZ() - 4; m < (int)this.locZ() + 4 && j < 14; ++m) {
                        IBlockData blockState = this.level.getType(mutableBlockPos.set(k, l, m));
                        if (blockState.is(Blocks.IRON_BARS) || blockState.getBlock() instanceof BlockBed) {
                            if (this.random.nextFloat() < 0.3F) {
                                ++i;
                            }

                            ++j;
                        }
                    }
                }
            }
        }

        return i;
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 2.0F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    public SoundEffect getSoundAmbient() {
        return SoundEffects.ZOMBIE_VILLAGER_AMBIENT;
    }

    @Override
    public SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ZOMBIE_VILLAGER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ZOMBIE_VILLAGER_DEATH;
    }

    @Override
    public SoundEffect getSoundStep() {
        return SoundEffects.ZOMBIE_VILLAGER_STEP;
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    public void setOffers(NBTTagCompound offerTag) {
        this.tradeOffers = offerTag;
    }

    public void setGossips(NBTBase gossipTag) {
        this.gossips = gossipTag;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setVillagerData(this.getVillagerData().withType(VillagerType.byBiome(world.getBiomeName(this.getChunkCoordinates()))));
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        VillagerData villagerData2 = this.getVillagerData();
        if (villagerData2.getProfession() != villagerData.getProfession()) {
            this.tradeOffers = null;
        }

        this.entityData.set(DATA_VILLAGER_DATA, villagerData);
    }

    @Override
    public VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER_DATA);
    }

    public int getVillagerXp() {
        return this.villagerXp;
    }

    public void setVillagerXp(int xp) {
        this.villagerXp = xp;
    }
}
