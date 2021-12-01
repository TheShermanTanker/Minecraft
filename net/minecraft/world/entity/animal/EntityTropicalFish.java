package net.minecraft.world.entity.animal;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.Biomes;

public class EntityTropicalFish extends EntityFishSchool {
    public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
    private static final DataWatcherObject<Integer> DATA_ID_TYPE_VARIANT = DataWatcher.defineId(EntityTropicalFish.class, DataWatcherRegistry.INT);
    public static final int BASE_SMALL = 0;
    public static final int BASE_LARGE = 1;
    private static final int BASES = 2;
    private static final MinecraftKey[] BASE_TEXTURE_LOCATIONS = new MinecraftKey[]{new MinecraftKey("textures/entity/fish/tropical_a.png"), new MinecraftKey("textures/entity/fish/tropical_b.png")};
    private static final MinecraftKey[] PATTERN_A_TEXTURE_LOCATIONS = new MinecraftKey[]{new MinecraftKey("textures/entity/fish/tropical_a_pattern_1.png"), new MinecraftKey("textures/entity/fish/tropical_a_pattern_2.png"), new MinecraftKey("textures/entity/fish/tropical_a_pattern_3.png"), new MinecraftKey("textures/entity/fish/tropical_a_pattern_4.png"), new MinecraftKey("textures/entity/fish/tropical_a_pattern_5.png"), new MinecraftKey("textures/entity/fish/tropical_a_pattern_6.png")};
    private static final MinecraftKey[] PATTERN_B_TEXTURE_LOCATIONS = new MinecraftKey[]{new MinecraftKey("textures/entity/fish/tropical_b_pattern_1.png"), new MinecraftKey("textures/entity/fish/tropical_b_pattern_2.png"), new MinecraftKey("textures/entity/fish/tropical_b_pattern_3.png"), new MinecraftKey("textures/entity/fish/tropical_b_pattern_4.png"), new MinecraftKey("textures/entity/fish/tropical_b_pattern_5.png"), new MinecraftKey("textures/entity/fish/tropical_b_pattern_6.png")};
    private static final int PATTERNS = 6;
    private static final int COLORS = 15;
    public static final int[] COMMON_VARIANTS = new int[]{calculateVariant(EntityTropicalFish.Variant.STRIPEY, EnumColor.ORANGE, EnumColor.GRAY), calculateVariant(EntityTropicalFish.Variant.FLOPPER, EnumColor.GRAY, EnumColor.GRAY), calculateVariant(EntityTropicalFish.Variant.FLOPPER, EnumColor.GRAY, EnumColor.BLUE), calculateVariant(EntityTropicalFish.Variant.CLAYFISH, EnumColor.WHITE, EnumColor.GRAY), calculateVariant(EntityTropicalFish.Variant.SUNSTREAK, EnumColor.BLUE, EnumColor.GRAY), calculateVariant(EntityTropicalFish.Variant.KOB, EnumColor.ORANGE, EnumColor.WHITE), calculateVariant(EntityTropicalFish.Variant.SPOTTY, EnumColor.PINK, EnumColor.LIGHT_BLUE), calculateVariant(EntityTropicalFish.Variant.BLOCKFISH, EnumColor.PURPLE, EnumColor.YELLOW), calculateVariant(EntityTropicalFish.Variant.CLAYFISH, EnumColor.WHITE, EnumColor.RED), calculateVariant(EntityTropicalFish.Variant.SPOTTY, EnumColor.WHITE, EnumColor.YELLOW), calculateVariant(EntityTropicalFish.Variant.GLITTER, EnumColor.WHITE, EnumColor.GRAY), calculateVariant(EntityTropicalFish.Variant.CLAYFISH, EnumColor.WHITE, EnumColor.ORANGE), calculateVariant(EntityTropicalFish.Variant.DASHER, EnumColor.CYAN, EnumColor.PINK), calculateVariant(EntityTropicalFish.Variant.BRINELY, EnumColor.LIME, EnumColor.LIGHT_BLUE), calculateVariant(EntityTropicalFish.Variant.BETTY, EnumColor.RED, EnumColor.WHITE), calculateVariant(EntityTropicalFish.Variant.SNOOPER, EnumColor.GRAY, EnumColor.RED), calculateVariant(EntityTropicalFish.Variant.BLOCKFISH, EnumColor.RED, EnumColor.WHITE), calculateVariant(EntityTropicalFish.Variant.FLOPPER, EnumColor.WHITE, EnumColor.YELLOW), calculateVariant(EntityTropicalFish.Variant.KOB, EnumColor.RED, EnumColor.WHITE), calculateVariant(EntityTropicalFish.Variant.SUNSTREAK, EnumColor.GRAY, EnumColor.WHITE), calculateVariant(EntityTropicalFish.Variant.DASHER, EnumColor.CYAN, EnumColor.YELLOW), calculateVariant(EntityTropicalFish.Variant.FLOPPER, EnumColor.YELLOW, EnumColor.YELLOW)};
    private boolean isSchool = true;

    private static int calculateVariant(EntityTropicalFish.Variant variety, EnumColor baseColor, EnumColor patternColor) {
        return variety.getBase() & 255 | (variety.getIndex() & 255) << 8 | (baseColor.getColorIndex() & 255) << 16 | (patternColor.getColorIndex() & 255) << 24;
    }

    public EntityTropicalFish(EntityTypes<? extends EntityTropicalFish> type, World world) {
        super(type, world);
    }

    public static String getPredefinedName(int variant) {
        return "entity.minecraft.tropical_fish.predefined." + variant;
    }

    public static EnumColor getBaseColor(int variant) {
        return EnumColor.fromColorIndex(getBaseColorIdx(variant));
    }

    public static EnumColor getPatternColor(int variant) {
        return EnumColor.fromColorIndex(getPatternColorIdx(variant));
    }

    public static String getFishTypeName(int variant) {
        int i = getBaseVariant(variant);
        int j = getPatternVariant(variant);
        return "entity.minecraft.tropical_fish.type." + EntityTropicalFish.Variant.getPatternName(i, j);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Variant", this.getVariant());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setVariant(nbt.getInt("Variant"));
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, variant);
    }

    @Override
    public boolean isMaxGroupSizeReached(int count) {
        return !this.isSchool;
    }

    public int getVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    @Override
    public void setBucketName(ItemStack stack) {
        super.setBucketName(stack);
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        compoundTag.setInt("BucketVariantTag", this.getVariant());
    }

    @Override
    public ItemStack getBucketItem() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.TROPICAL_FISH_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEffect getSoundFlop() {
        return SoundEffects.TROPICAL_FISH_FLOP;
    }

    private static int getBaseColorIdx(int variant) {
        return (variant & 16711680) >> 16;
    }

    public float[] getBaseColor() {
        return EnumColor.fromColorIndex(getBaseColorIdx(this.getVariant())).getColor();
    }

    private static int getPatternColorIdx(int variant) {
        return (variant & -16777216) >> 24;
    }

    public float[] getPatternColor() {
        return EnumColor.fromColorIndex(getPatternColorIdx(this.getVariant())).getColor();
    }

    public static int getBaseVariant(int variant) {
        return Math.min(variant & 255, 1);
    }

    public int getBaseVariant() {
        return getBaseVariant(this.getVariant());
    }

    private static int getPatternVariant(int variant) {
        return Math.min((variant & '\uff00') >> 8, 5);
    }

    public MinecraftKey getPatternTextureLocation() {
        return getBaseVariant(this.getVariant()) == 0 ? PATTERN_A_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())] : PATTERN_B_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())];
    }

    public MinecraftKey getBaseTextureLocation() {
        return BASE_TEXTURE_LOCATIONS[getBaseVariant(this.getVariant())];
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        if (spawnReason == EnumMobSpawn.BUCKET && entityNbt != null && entityNbt.hasKeyOfType("BucketVariantTag", 3)) {
            this.setVariant(entityNbt.getInt("BucketVariantTag"));
            return entityData;
        } else {
            int i;
            int j;
            int k;
            int l;
            if (entityData instanceof EntityTropicalFish.TropicalFishGroupData) {
                EntityTropicalFish.TropicalFishGroupData tropicalFishGroupData = (EntityTropicalFish.TropicalFishGroupData)entityData;
                i = tropicalFishGroupData.base;
                j = tropicalFishGroupData.pattern;
                k = tropicalFishGroupData.baseColor;
                l = tropicalFishGroupData.patternColor;
            } else if ((double)this.random.nextFloat() < 0.9D) {
                int m = SystemUtils.getRandom(COMMON_VARIANTS, this.random);
                i = m & 255;
                j = (m & '\uff00') >> 8;
                k = (m & 16711680) >> 16;
                l = (m & -16777216) >> 24;
                entityData = new EntityTropicalFish.TropicalFishGroupData(this, i, j, k, l);
            } else {
                this.isSchool = false;
                i = this.random.nextInt(2);
                j = this.random.nextInt(6);
                k = this.random.nextInt(15);
                l = this.random.nextInt(15);
            }

            this.setVariant(i | j << 8 | k << 16 | l << 24);
            return entityData;
        }
    }

    public static boolean checkTropicalFishSpawnRules(EntityTypes<EntityTropicalFish> type, GeneratorAccess world, EnumMobSpawn reason, BlockPosition pos, Random random) {
        return world.getFluid(pos.below()).is(TagsFluid.WATER) && (Objects.equals(world.getBiomeName(pos), Optional.of(Biomes.LUSH_CAVES)) || EntityWaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, world, reason, pos, random));
    }

    static class TropicalFishGroupData extends EntityFishSchool.SchoolSpawnGroupData {
        final int base;
        final int pattern;
        final int baseColor;
        final int patternColor;

        TropicalFishGroupData(EntityTropicalFish leader, int shape, int pattern, int baseColor, int patternColor) {
            super(leader);
            this.base = shape;
            this.pattern = pattern;
            this.baseColor = baseColor;
            this.patternColor = patternColor;
        }
    }

    static enum Variant {
        KOB(0, 0),
        SUNSTREAK(0, 1),
        SNOOPER(0, 2),
        DASHER(0, 3),
        BRINELY(0, 4),
        SPOTTY(0, 5),
        FLOPPER(1, 0),
        STRIPEY(1, 1),
        GLITTER(1, 2),
        BLOCKFISH(1, 3),
        BETTY(1, 4),
        CLAYFISH(1, 5);

        private final int base;
        private final int index;
        private static final EntityTropicalFish.Variant[] VALUES = values();

        private Variant(int shape, int pattern) {
            this.base = shape;
            this.index = pattern;
        }

        public int getBase() {
            return this.base;
        }

        public int getIndex() {
            return this.index;
        }

        public static String getPatternName(int shape, int pattern) {
            return VALUES[pattern + 6 * shape].getName();
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
