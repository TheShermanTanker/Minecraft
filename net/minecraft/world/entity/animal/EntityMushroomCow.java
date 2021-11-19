package net.minecraft.world.entity.animal;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSuspiciousStew;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFlowers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.tuple.Pair;

public class EntityMushroomCow extends EntityCow implements IShearable {
    private static final DataWatcherObject<String> DATA_TYPE = DataWatcher.defineId(EntityMushroomCow.class, DataWatcherRegistry.STRING);
    private static final int MUTATE_CHANCE = 1024;
    private MobEffectBase effect;
    private int effectDuration;
    private UUID lastLightningBoltUUID;

    public EntityMushroomCow(EntityTypes<? extends EntityMushroomCow> type, World world) {
        super(type, world);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return world.getType(pos.below()).is(Blocks.MYCELIUM) ? 10.0F : world.getBrightness(pos) - 0.5F;
    }

    public static boolean checkMushroomSpawnRules(EntityTypes<EntityMushroomCow> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getType(pos.below()).is(Blocks.MYCELIUM) && world.getLightLevel(pos, 0) > 8;
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        UUID uUID = lightning.getUniqueID();
        if (!uUID.equals(this.lastLightningBoltUUID)) {
            this.setVariant(this.getVariant() == EntityMushroomCow.Type.RED ? EntityMushroomCow.Type.BROWN : EntityMushroomCow.Type.RED);
            this.lastLightningBoltUUID = uUID;
            this.playSound(SoundEffects.MOOSHROOM_CONVERT, 2.0F, 1.0F);
        }

    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_TYPE, EntityMushroomCow.Type.RED.type);
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.BOWL) && !this.isBaby()) {
            boolean bl = false;
            ItemStack itemStack2;
            if (this.effect != null) {
                bl = true;
                itemStack2 = new ItemStack(Items.SUSPICIOUS_STEW);
                ItemSuspiciousStew.saveMobEffect(itemStack2, this.effect, this.effectDuration);
                this.effect = null;
                this.effectDuration = 0;
            } else {
                itemStack2 = new ItemStack(Items.MUSHROOM_STEW);
            }

            ItemStack itemStack4 = ItemLiquidUtil.createFilledResult(itemStack, player, itemStack2, false);
            player.setItemInHand(hand, itemStack4);
            SoundEffect soundEvent;
            if (bl) {
                soundEvent = SoundEffects.MOOSHROOM_MILK_SUSPICIOUSLY;
            } else {
                soundEvent = SoundEffects.MOOSHROOM_MILK;
            }

            this.playSound(soundEvent, 1.0F, 1.0F);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (itemStack.is(Items.SHEARS) && this.canShear()) {
            this.shear(EnumSoundCategory.PLAYERS);
            this.gameEvent(GameEvent.SHEAR, player);
            if (!this.level.isClientSide) {
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (this.getVariant() == EntityMushroomCow.Type.BROWN && itemStack.is(TagsItem.SMALL_FLOWERS)) {
            if (this.effect != null) {
                for(int i = 0; i < 2; ++i) {
                    this.level.addParticle(Particles.SMOKE, this.locX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.locZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                }
            } else {
                Optional<Pair<MobEffectBase, Integer>> optional = this.getEffectFromItemStack(itemStack);
                if (!optional.isPresent()) {
                    return EnumInteractionResult.PASS;
                }

                Pair<MobEffectBase, Integer> pair = optional.get();
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }

                for(int j = 0; j < 4; ++j) {
                    this.level.addParticle(Particles.EFFECT, this.locX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.locZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                }

                this.effect = pair.getLeft();
                this.effectDuration = pair.getRight();
                this.playSound(SoundEffects.MOOSHROOM_EAT, 2.0F, 1.0F);
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void shear(EnumSoundCategory shearedSoundCategory) {
        this.level.playSound((EntityHuman)null, this, SoundEffects.MOOSHROOM_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        if (!this.level.isClientSide()) {
            ((WorldServer)this.level).sendParticles(Particles.EXPLOSION, this.locX(), this.getY(0.5D), this.locZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.die();
            EntityCow cow = EntityTypes.COW.create(this.level);
            cow.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
            cow.setHealth(this.getHealth());
            cow.yBodyRot = this.yBodyRot;
            if (this.hasCustomName()) {
                cow.setCustomName(this.getCustomName());
                cow.setCustomNameVisible(this.getCustomNameVisible());
            }

            if (this.isPersistent()) {
                cow.setPersistent();
            }

            cow.setInvulnerable(this.isInvulnerable());
            this.level.addEntity(cow);

            for(int i = 0; i < 5; ++i) {
                this.level.addEntity(new EntityItem(this.level, this.locX(), this.getY(1.0D), this.locZ(), new ItemStack(this.getVariant().blockState.getBlock())));
            }
        }

    }

    @Override
    public boolean canShear() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setString("Type", this.getVariant().type);
        if (this.effect != null) {
            nbt.setByte("EffectId", (byte)MobEffectBase.getId(this.effect));
            nbt.setInt("EffectDuration", this.effectDuration);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setVariant(EntityMushroomCow.Type.byType(nbt.getString("Type")));
        if (nbt.hasKeyOfType("EffectId", 1)) {
            this.effect = MobEffectBase.fromId(nbt.getByte("EffectId"));
        }

        if (nbt.hasKeyOfType("EffectDuration", 3)) {
            this.effectDuration = nbt.getInt("EffectDuration");
        }

    }

    private Optional<Pair<MobEffectBase, Integer>> getEffectFromItemStack(ItemStack flower) {
        Item item = flower.getItem();
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock)item).getBlock();
            if (block instanceof BlockFlowers) {
                BlockFlowers flowerBlock = (BlockFlowers)block;
                return Optional.of(Pair.of(flowerBlock.getSuspiciousStewEffect(), flowerBlock.getEffectDuration()));
            }
        }

        return Optional.empty();
    }

    public void setVariant(EntityMushroomCow.Type type) {
        this.entityData.set(DATA_TYPE, type.type);
    }

    public EntityMushroomCow.Type getVariant() {
        return EntityMushroomCow.Type.byType(this.entityData.get(DATA_TYPE));
    }

    @Override
    public EntityMushroomCow getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityMushroomCow mushroomCow = EntityTypes.MOOSHROOM.create(serverLevel);
        mushroomCow.setVariant(this.getOffspringType((EntityMushroomCow)ageableMob));
        return mushroomCow;
    }

    private EntityMushroomCow.Type getOffspringType(EntityMushroomCow mooshroom) {
        EntityMushroomCow.Type mushroomType = this.getVariant();
        EntityMushroomCow.Type mushroomType2 = mooshroom.getVariant();
        EntityMushroomCow.Type mushroomType3;
        if (mushroomType == mushroomType2 && this.random.nextInt(1024) == 0) {
            mushroomType3 = mushroomType == EntityMushroomCow.Type.BROWN ? EntityMushroomCow.Type.RED : EntityMushroomCow.Type.BROWN;
        } else {
            mushroomType3 = this.random.nextBoolean() ? mushroomType : mushroomType2;
        }

        return mushroomType3;
    }

    public static enum Type {
        RED("red", Blocks.RED_MUSHROOM.getBlockData()),
        BROWN("brown", Blocks.BROWN_MUSHROOM.getBlockData());

        final String type;
        final IBlockData blockState;

        private Type(String name, IBlockData mushroom) {
            this.type = name;
            this.blockState = mushroom;
        }

        public IBlockData getBlockState() {
            return this.blockState;
        }

        static EntityMushroomCow.Type byType(String name) {
            for(EntityMushroomCow.Type mushroomType : values()) {
                if (mushroomType.type.equals(name)) {
                    return mushroomType;
                }
            }

            return RED;
        }
    }
}
