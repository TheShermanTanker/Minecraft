package net.minecraft.world.entity.animal;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBreed;
import net.minecraft.world.entity.ai.goal.PathfinderGoalEatTile;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalTempt;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTables;

public class EntitySheep extends EntityAnimal implements IShearable {
    private static final int EAT_ANIMATION_TICKS = 40;
    private static final DataWatcherObject<Byte> DATA_WOOL_ID = DataWatcher.defineId(EntitySheep.class, DataWatcherRegistry.BYTE);
    private static final Map<EnumColor, IMaterial> ITEM_BY_DYE = SystemUtils.make(Maps.newEnumMap(EnumColor.class), (map) -> {
        map.put(EnumColor.WHITE, Blocks.WHITE_WOOL);
        map.put(EnumColor.ORANGE, Blocks.ORANGE_WOOL);
        map.put(EnumColor.MAGENTA, Blocks.MAGENTA_WOOL);
        map.put(EnumColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
        map.put(EnumColor.YELLOW, Blocks.YELLOW_WOOL);
        map.put(EnumColor.LIME, Blocks.LIME_WOOL);
        map.put(EnumColor.PINK, Blocks.PINK_WOOL);
        map.put(EnumColor.GRAY, Blocks.GRAY_WOOL);
        map.put(EnumColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
        map.put(EnumColor.CYAN, Blocks.CYAN_WOOL);
        map.put(EnumColor.PURPLE, Blocks.PURPLE_WOOL);
        map.put(EnumColor.BLUE, Blocks.BLUE_WOOL);
        map.put(EnumColor.BROWN, Blocks.BROWN_WOOL);
        map.put(EnumColor.GREEN, Blocks.GREEN_WOOL);
        map.put(EnumColor.RED, Blocks.RED_WOOL);
        map.put(EnumColor.BLACK, Blocks.BLACK_WOOL);
    });
    private static final Map<EnumColor, float[]> COLORARRAY_BY_COLOR = Maps.newEnumMap(Arrays.stream(EnumColor.values()).collect(Collectors.toMap((dyeColor) -> {
        return dyeColor;
    }, EntitySheep::createSheepColor)));
    private int eatAnimationTick;
    private PathfinderGoalEatTile eatBlockGoal;

    private static float[] createSheepColor(EnumColor color) {
        if (color == EnumColor.WHITE) {
            return new float[]{0.9019608F, 0.9019608F, 0.9019608F};
        } else {
            float[] fs = color.getColor();
            float f = 0.75F;
            return new float[]{fs[0] * 0.75F, fs[1] * 0.75F, fs[2] * 0.75F};
        }
    }

    public static float[] getColorArray(EnumColor dyeColor) {
        return COLORARRAY_BY_COLOR.get(dyeColor);
    }

    public EntitySheep(EntityTypes<? extends EntitySheep> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.eatBlockGoal = new PathfinderGoalEatTile(this);
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 1.25D));
        this.goalSelector.addGoal(2, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.addGoal(3, new PathfinderGoalTempt(this, 1.1D, RecipeItemStack.of(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new PathfinderGoalFollowParent(this, 1.1D));
        this.goalSelector.addGoal(5, this.eatBlockGoal);
        this.goalSelector.addGoal(6, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.goalSelector.addGoal(7, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
    }

    @Override
    protected void mobTick() {
        this.eatAnimationTick = this.eatBlockGoal.getEatAnimationTick();
        super.mobTick();
    }

    @Override
    public void movementTick() {
        if (this.level.isClientSide) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        }

        super.movementTick();
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 8.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.23F);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_WOOL_ID, (byte)0);
    }

    @Override
    public MinecraftKey getDefaultLootTable() {
        if (this.isSheared()) {
            return this.getEntityType().getDefaultLootTable();
        } else {
            switch(this.getColor()) {
            case WHITE:
            default:
                return LootTables.SHEEP_WHITE;
            case ORANGE:
                return LootTables.SHEEP_ORANGE;
            case MAGENTA:
                return LootTables.SHEEP_MAGENTA;
            case LIGHT_BLUE:
                return LootTables.SHEEP_LIGHT_BLUE;
            case YELLOW:
                return LootTables.SHEEP_YELLOW;
            case LIME:
                return LootTables.SHEEP_LIME;
            case PINK:
                return LootTables.SHEEP_PINK;
            case GRAY:
                return LootTables.SHEEP_GRAY;
            case LIGHT_GRAY:
                return LootTables.SHEEP_LIGHT_GRAY;
            case CYAN:
                return LootTables.SHEEP_CYAN;
            case PURPLE:
                return LootTables.SHEEP_PURPLE;
            case BLUE:
                return LootTables.SHEEP_BLUE;
            case BROWN:
                return LootTables.SHEEP_BROWN;
            case GREEN:
                return LootTables.SHEEP_GREEN;
            case RED:
                return LootTables.SHEEP_RED;
            case BLACK:
                return LootTables.SHEEP_BLACK;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 10) {
            this.eatAnimationTick = 40;
        } else {
            super.handleEntityEvent(status);
        }

    }

    public float getHeadEatPositionScale(float delta) {
        if (this.eatAnimationTick <= 0) {
            return 0.0F;
        } else if (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36) {
            return 1.0F;
        } else {
            return this.eatAnimationTick < 4 ? ((float)this.eatAnimationTick - delta) / 4.0F : -((float)(this.eatAnimationTick - 40) - delta) / 4.0F;
        }
    }

    public float getHeadEatAngleScale(float delta) {
        if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
            float f = ((float)(this.eatAnimationTick - 4) - delta) / 32.0F;
            return ((float)Math.PI / 5F) + 0.21991149F * MathHelper.sin(f * 28.7F);
        } else {
            return this.eatAnimationTick > 0 ? ((float)Math.PI / 5F) : this.getXRot() * ((float)Math.PI / 180F);
        }
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.SHEARS)) {
            if (!this.level.isClientSide && this.canShear()) {
                this.shear(EnumSoundCategory.PLAYERS);
                this.gameEvent(GameEvent.SHEAR, player);
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
                return EnumInteractionResult.SUCCESS;
            } else {
                return EnumInteractionResult.CONSUME;
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void shear(EnumSoundCategory shearedSoundCategory) {
        this.level.playSound((EntityHuman)null, this, SoundEffects.SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        this.setSheared(true);
        int i = 1 + this.random.nextInt(3);

        for(int j = 0; j < i; ++j) {
            EntityItem itemEntity = this.spawnAtLocation(ITEM_BY_DYE.get(this.getColor()), 1);
            if (itemEntity != null) {
                itemEntity.setMot(itemEntity.getMot().add((double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(this.random.nextFloat() * 0.05F), (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
            }
        }

    }

    @Override
    public boolean canShear() {
        return this.isAlive() && !this.isSheared() && !this.isBaby();
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("Sheared", this.isSheared());
        nbt.setByte("Color", (byte)this.getColor().getColorIndex());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setSheared(nbt.getBoolean("Sheared"));
        this.setColor(EnumColor.fromColorIndex(nbt.getByte("Color")));
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SHEEP_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SHEEP_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.SHEEP_STEP, 0.15F, 1.0F);
    }

    public EnumColor getColor() {
        return EnumColor.fromColorIndex(this.entityData.get(DATA_WOOL_ID) & 15);
    }

    public void setColor(EnumColor color) {
        byte b = this.entityData.get(DATA_WOOL_ID);
        this.entityData.set(DATA_WOOL_ID, (byte)(b & 240 | color.getColorIndex() & 15));
    }

    public boolean isSheared() {
        return (this.entityData.get(DATA_WOOL_ID) & 16) != 0;
    }

    public void setSheared(boolean sheared) {
        byte b = this.entityData.get(DATA_WOOL_ID);
        if (sheared) {
            this.entityData.set(DATA_WOOL_ID, (byte)(b | 16));
        } else {
            this.entityData.set(DATA_WOOL_ID, (byte)(b & -17));
        }

    }

    public static EnumColor getRandomSheepColor(Random random) {
        int i = random.nextInt(100);
        if (i < 5) {
            return EnumColor.BLACK;
        } else if (i < 10) {
            return EnumColor.GRAY;
        } else if (i < 15) {
            return EnumColor.LIGHT_GRAY;
        } else if (i < 18) {
            return EnumColor.BROWN;
        } else {
            return random.nextInt(500) == 0 ? EnumColor.PINK : EnumColor.WHITE;
        }
    }

    @Override
    public EntitySheep getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntitySheep sheep = (EntitySheep)ageableMob;
        EntitySheep sheep2 = EntityTypes.SHEEP.create(serverLevel);
        sheep2.setColor(this.getOffspringColor(this, sheep));
        return sheep2;
    }

    @Override
    public void blockEaten() {
        this.setSheared(false);
        if (this.isBaby()) {
            this.setAge(60);
        }

    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setColor(getRandomSheepColor(world.getRandom()));
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    private EnumColor getOffspringColor(EntityAnimal firstParent, EntityAnimal secondParent) {
        EnumColor dyeColor = ((EntitySheep)firstParent).getColor();
        EnumColor dyeColor2 = ((EntitySheep)secondParent).getColor();
        InventoryCrafting craftingContainer = makeContainer(dyeColor, dyeColor2);
        return this.level.getCraftingManager().craft(Recipes.CRAFTING, craftingContainer, this.level).map((recipe) -> {
            return recipe.assemble(craftingContainer);
        }).map(ItemStack::getItem).filter(ItemDye.class::isInstance).map(ItemDye.class::cast).map(ItemDye::getDyeColor).orElseGet(() -> {
            return this.level.random.nextBoolean() ? dyeColor : dyeColor2;
        });
    }

    private static InventoryCrafting makeContainer(EnumColor firstColor, EnumColor secondColor) {
        InventoryCrafting craftingContainer = new InventoryCrafting(new Container((Containers)null, -1) {
            @Override
            public boolean canUse(EntityHuman player) {
                return false;
            }
        }, 2, 1);
        craftingContainer.setItem(0, new ItemStack(ItemDye.byColor(firstColor)));
        craftingContainer.setItem(1, new ItemStack(ItemDye.byColor(secondColor)));
        return craftingContainer;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.95F * dimensions.height;
    }
}
