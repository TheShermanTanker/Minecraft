package net.minecraft.world.entity;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.util.MathHelper;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.entity.animal.EntityCod;
import net.minecraft.world.entity.animal.EntityCow;
import net.minecraft.world.entity.animal.EntityDolphin;
import net.minecraft.world.entity.animal.EntityFox;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityMushroomCow;
import net.minecraft.world.entity.animal.EntityOcelot;
import net.minecraft.world.entity.animal.EntityPanda;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.entity.animal.EntityPig;
import net.minecraft.world.entity.animal.EntityPolarBear;
import net.minecraft.world.entity.animal.EntityPufferFish;
import net.minecraft.world.entity.animal.EntityRabbit;
import net.minecraft.world.entity.animal.EntitySalmon;
import net.minecraft.world.entity.animal.EntitySheep;
import net.minecraft.world.entity.animal.EntitySnowman;
import net.minecraft.world.entity.animal.EntitySquid;
import net.minecraft.world.entity.animal.EntityTropicalFish;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.animal.axolotl.EntityAxolotl;
import net.minecraft.world.entity.animal.goat.EntityGoat;
import net.minecraft.world.entity.animal.horse.EntityHorse;
import net.minecraft.world.entity.animal.horse.EntityHorseDonkey;
import net.minecraft.world.entity.animal.horse.EntityHorseMule;
import net.minecraft.world.entity.animal.horse.EntityHorseSkeleton;
import net.minecraft.world.entity.animal.horse.EntityHorseZombie;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.animal.horse.EntityLlamaTrader;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.decoration.EntityLeash;
import net.minecraft.world.entity.decoration.EntityPainting;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.monster.EntityBlaze;
import net.minecraft.world.entity.monster.EntityCaveSpider;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.monster.EntityDrowned;
import net.minecraft.world.entity.monster.EntityEnderman;
import net.minecraft.world.entity.monster.EntityEndermite;
import net.minecraft.world.entity.monster.EntityEvoker;
import net.minecraft.world.entity.monster.EntityGhast;
import net.minecraft.world.entity.monster.EntityGiantZombie;
import net.minecraft.world.entity.monster.EntityGuardian;
import net.minecraft.world.entity.monster.EntityGuardianElder;
import net.minecraft.world.entity.monster.EntityIllagerIllusioner;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import net.minecraft.world.entity.monster.EntityPhantom;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.entity.monster.EntityPillager;
import net.minecraft.world.entity.monster.EntityRavager;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.monster.EntitySilverfish;
import net.minecraft.world.entity.monster.EntitySkeleton;
import net.minecraft.world.entity.monster.EntitySkeletonStray;
import net.minecraft.world.entity.monster.EntitySkeletonWither;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.entity.monster.EntitySpider;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.entity.monster.EntityVindicator;
import net.minecraft.world.entity.monster.EntityWitch;
import net.minecraft.world.entity.monster.EntityZoglin;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.monster.EntityZombieHusk;
import net.minecraft.world.entity.monster.EntityZombieVillager;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglinBrute;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.EntityVillagerTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityDragonFireball;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.entity.projectile.EntityEnderPearl;
import net.minecraft.world.entity.projectile.EntityEnderSignal;
import net.minecraft.world.entity.projectile.EntityEvokerFangs;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.entity.projectile.EntityLargeFireball;
import net.minecraft.world.entity.projectile.EntityLlamaSpit;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.entity.projectile.EntityShulkerBullet;
import net.minecraft.world.entity.projectile.EntitySmallFireball;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.entity.projectile.EntityThrownExpBottle;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.entity.vehicle.EntityMinecartChest;
import net.minecraft.world.entity.vehicle.EntityMinecartCommandBlock;
import net.minecraft.world.entity.vehicle.EntityMinecartFurnace;
import net.minecraft.world.entity.vehicle.EntityMinecartHopper;
import net.minecraft.world.entity.vehicle.EntityMinecartMobSpawner;
import net.minecraft.world.entity.vehicle.EntityMinecartRideable;
import net.minecraft.world.entity.vehicle.EntityMinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityTypes<T extends Entity> implements EntityTypeTest<Entity, T> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String ENTITY_TAG = "EntityTag";
    private static final float MAGIC_HORSE_WIDTH = 1.3964844F;
    public static final EntityTypes<EntityAreaEffectCloud> AREA_EFFECT_CLOUD = register("area_effect_cloud", EntityTypes.Builder.<EntityAreaEffectCloud>of(EntityAreaEffectCloud::new, EnumCreatureType.MISC).fireImmune().sized(6.0F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityArmorStand> ARMOR_STAND = register("armor_stand", EntityTypes.Builder.<EntityArmorStand>of(EntityArmorStand::new, EnumCreatureType.MISC).sized(0.5F, 1.975F).trackingRange(10));
    public static final EntityTypes<EntityTippedArrow> ARROW = register("arrow", EntityTypes.Builder.<EntityTippedArrow>of(EntityTippedArrow::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<EntityAxolotl> AXOLOTL = register("axolotl", EntityTypes.Builder.<EntityAxolotl>of(EntityAxolotl::new, EnumCreatureType.AXOLOTLS).sized(0.75F, 0.42F).trackingRange(10));
    public static final EntityTypes<EntityBat> BAT = register("bat", EntityTypes.Builder.<EntityBat>of(EntityBat::new, EnumCreatureType.AMBIENT).sized(0.5F, 0.9F).trackingRange(5));
    public static final EntityTypes<EntityBee> BEE = register("bee", EntityTypes.Builder.<EntityBee>of(EntityBee::new, EnumCreatureType.CREATURE).sized(0.7F, 0.6F).trackingRange(8));
    public static final EntityTypes<EntityBlaze> BLAZE = register("blaze", EntityTypes.Builder.<EntityBlaze>of(EntityBlaze::new, EnumCreatureType.MONSTER).fireImmune().sized(0.6F, 1.8F).trackingRange(8));
    public static final EntityTypes<EntityBoat> BOAT = register("boat", EntityTypes.Builder.<EntityBoat>of(EntityBoat::new, EnumCreatureType.MISC).sized(1.375F, 0.5625F).trackingRange(10));
    public static final EntityTypes<EntityCat> CAT = register("cat", EntityTypes.Builder.<EntityCat>of(EntityCat::new, EnumCreatureType.CREATURE).sized(0.6F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityCaveSpider> CAVE_SPIDER = register("cave_spider", EntityTypes.Builder.<EntityCaveSpider>of(EntityCaveSpider::new, EnumCreatureType.MONSTER).sized(0.7F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityChicken> CHICKEN = register("chicken", EntityTypes.Builder.<EntityChicken>of(EntityChicken::new, EnumCreatureType.CREATURE).sized(0.4F, 0.7F).trackingRange(10));
    public static final EntityTypes<EntityCod> COD = register("cod", EntityTypes.Builder.<EntityCod>of(EntityCod::new, EnumCreatureType.WATER_AMBIENT).sized(0.5F, 0.3F).trackingRange(4));
    public static final EntityTypes<EntityCow> COW = register("cow", EntityTypes.Builder.<EntityCow>of(EntityCow::new, EnumCreatureType.CREATURE).sized(0.9F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityCreeper> CREEPER = register("creeper", EntityTypes.Builder.<EntityCreeper>of(EntityCreeper::new, EnumCreatureType.MONSTER).sized(0.6F, 1.7F).trackingRange(8));
    public static final EntityTypes<EntityDolphin> DOLPHIN = register("dolphin", EntityTypes.Builder.<EntityDolphin>of(EntityDolphin::new, EnumCreatureType.WATER_CREATURE).sized(0.9F, 0.6F));
    public static final EntityTypes<EntityHorseDonkey> DONKEY = register("donkey", EntityTypes.Builder.<EntityHorseDonkey>of(EntityHorseDonkey::new, EnumCreatureType.CREATURE).sized(1.3964844F, 1.5F).trackingRange(10));
    public static final EntityTypes<EntityDragonFireball> DRAGON_FIREBALL = register("dragon_fireball", EntityTypes.Builder.<EntityDragonFireball>of(EntityDragonFireball::new, EnumCreatureType.MISC).sized(1.0F, 1.0F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityDrowned> DROWNED = register("drowned", EntityTypes.Builder.<EntityDrowned>of(EntityDrowned::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityGuardianElder> ELDER_GUARDIAN = register("elder_guardian", EntityTypes.Builder.<EntityGuardianElder>of(EntityGuardianElder::new, EnumCreatureType.MONSTER).sized(1.9975F, 1.9975F).trackingRange(10));
    public static final EntityTypes<EntityEnderCrystal> END_CRYSTAL = register("end_crystal", EntityTypes.Builder.<EntityEnderCrystal>of(EntityEnderCrystal::new, EnumCreatureType.MISC).sized(2.0F, 2.0F).trackingRange(16).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityEnderDragon> ENDER_DRAGON = register("ender_dragon", EntityTypes.Builder.<EntityEnderDragon>of(EntityEnderDragon::new, EnumCreatureType.MONSTER).fireImmune().sized(16.0F, 8.0F).trackingRange(10));
    public static final EntityTypes<EntityEnderman> ENDERMAN = register("enderman", EntityTypes.Builder.<EntityEnderman>of(EntityEnderman::new, EnumCreatureType.MONSTER).sized(0.6F, 2.9F).trackingRange(8));
    public static final EntityTypes<EntityEndermite> ENDERMITE = register("endermite", EntityTypes.Builder.<EntityEndermite>of(EntityEndermite::new, EnumCreatureType.MONSTER).sized(0.4F, 0.3F).trackingRange(8));
    public static final EntityTypes<EntityEvoker> EVOKER = register("evoker", EntityTypes.Builder.<EntityEvoker>of(EntityEvoker::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityEvokerFangs> EVOKER_FANGS = register("evoker_fangs", EntityTypes.Builder.<EntityEvokerFangs>of(EntityEvokerFangs::new, EnumCreatureType.MISC).sized(0.5F, 0.8F).trackingRange(6).updateInterval(2));
    public static final EntityTypes<EntityExperienceOrb> EXPERIENCE_ORB = register("experience_orb", EntityTypes.Builder.<EntityExperienceOrb>of(EntityExperienceOrb::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(6).updateInterval(20));
    public static final EntityTypes<EntityEnderSignal> EYE_OF_ENDER = register("eye_of_ender", EntityTypes.Builder.<EntityEnderSignal>of(EntityEnderSignal::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(4));
    public static final EntityTypes<EntityFallingBlock> FALLING_BLOCK = register("falling_block", EntityTypes.Builder.<EntityFallingBlock>of(EntityFallingBlock::new, EnumCreatureType.MISC).sized(0.98F, 0.98F).trackingRange(10).updateInterval(20));
    public static final EntityTypes<EntityFireworks> FIREWORK_ROCKET = register("firework_rocket", EntityTypes.Builder.<EntityFireworks>of(EntityFireworks::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityFox> FOX = register("fox", EntityTypes.Builder.<EntityFox>of(EntityFox::new, EnumCreatureType.CREATURE).sized(0.6F, 0.7F).trackingRange(8).immuneTo(Blocks.SWEET_BERRY_BUSH));
    public static final EntityTypes<EntityGhast> GHAST = register("ghast", EntityTypes.Builder.<EntityGhast>of(EntityGhast::new, EnumCreatureType.MONSTER).fireImmune().sized(4.0F, 4.0F).trackingRange(10));
    public static final EntityTypes<EntityGiantZombie> GIANT = register("giant", EntityTypes.Builder.<EntityGiantZombie>of(EntityGiantZombie::new, EnumCreatureType.MONSTER).sized(3.6F, 12.0F).trackingRange(10));
    public static final EntityTypes<GlowItemFrame> GLOW_ITEM_FRAME = register("glow_item_frame", EntityTypes.Builder.<GlowItemFrame>of(GlowItemFrame::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityGlowSquid> GLOW_SQUID = register("glow_squid", EntityTypes.Builder.<EntityGlowSquid>of(EntityGlowSquid::new, EnumCreatureType.UNDERGROUND_WATER_CREATURE).sized(0.8F, 0.8F).trackingRange(10));
    public static final EntityTypes<EntityGoat> GOAT = register("goat", EntityTypes.Builder.<EntityGoat>of(EntityGoat::new, EnumCreatureType.CREATURE).sized(0.9F, 1.3F).trackingRange(10));
    public static final EntityTypes<EntityGuardian> GUARDIAN = register("guardian", EntityTypes.Builder.<EntityGuardian>of(EntityGuardian::new, EnumCreatureType.MONSTER).sized(0.85F, 0.85F).trackingRange(8));
    public static final EntityTypes<EntityHoglin> HOGLIN = register("hoglin", EntityTypes.Builder.<EntityHoglin>of(EntityHoglin::new, EnumCreatureType.MONSTER).sized(1.3964844F, 1.4F).trackingRange(8));
    public static final EntityTypes<EntityHorse> HORSE = register("horse", EntityTypes.Builder.<EntityHorse>of(EntityHorse::new, EnumCreatureType.CREATURE).sized(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntityZombieHusk> HUSK = register("husk", EntityTypes.Builder.<EntityZombieHusk>of(EntityZombieHusk::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityIllagerIllusioner> ILLUSIONER = register("illusioner", EntityTypes.Builder.<EntityIllagerIllusioner>of(EntityIllagerIllusioner::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityIronGolem> IRON_GOLEM = register("iron_golem", EntityTypes.Builder.<EntityIronGolem>of(EntityIronGolem::new, EnumCreatureType.MISC).sized(1.4F, 2.7F).trackingRange(10));
    public static final EntityTypes<EntityItem> ITEM = register("item", EntityTypes.Builder.<EntityItem>of(EntityItem::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(6).updateInterval(20));
    public static final EntityTypes<EntityItemFrame> ITEM_FRAME = register("item_frame", EntityTypes.Builder.<EntityItemFrame>of(EntityItemFrame::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLargeFireball> FIREBALL = register("fireball", EntityTypes.Builder.<EntityLargeFireball>of(EntityLargeFireball::new, EnumCreatureType.MISC).sized(1.0F, 1.0F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityLeash> LEASH_KNOT = register("leash_knot", EntityTypes.Builder.<EntityLeash>of(EntityLeash::new, EnumCreatureType.MISC).noSave().sized(0.375F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLightning> LIGHTNING_BOLT = register("lightning_bolt", EntityTypes.Builder.<EntityLightning>of(EntityLightning::new, EnumCreatureType.MISC).noSave().sized(0.0F, 0.0F).trackingRange(16).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLlama> LLAMA = register("llama", EntityTypes.Builder.<EntityLlama>of(EntityLlama::new, EnumCreatureType.CREATURE).sized(0.9F, 1.87F).trackingRange(10));
    public static final EntityTypes<EntityLlamaSpit> LLAMA_SPIT = register("llama_spit", EntityTypes.Builder.<EntityLlamaSpit>of(EntityLlamaSpit::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityMagmaCube> MAGMA_CUBE = register("magma_cube", EntityTypes.Builder.<EntityMagmaCube>of(EntityMagmaCube::new, EnumCreatureType.MONSTER).fireImmune().sized(2.04F, 2.04F).trackingRange(8));
    public static final EntityTypes<EntityMarker> MARKER = register("marker", EntityTypes.Builder.<EntityMarker>of(EntityMarker::new, EnumCreatureType.MISC).sized(0.0F, 0.0F).trackingRange(0));
    public static final EntityTypes<EntityMinecartRideable> MINECART = register("minecart", EntityTypes.Builder.<EntityMinecartRideable>of(EntityMinecartRideable::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartChest> CHEST_MINECART = register("chest_minecart", EntityTypes.Builder.<EntityMinecartChest>of(EntityMinecartChest::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartCommandBlock> COMMAND_BLOCK_MINECART = register("command_block_minecart", EntityTypes.Builder.<EntityMinecartCommandBlock>of(EntityMinecartCommandBlock::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartFurnace> FURNACE_MINECART = register("furnace_minecart", EntityTypes.Builder.<EntityMinecartFurnace>of(EntityMinecartFurnace::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartHopper> HOPPER_MINECART = register("hopper_minecart", EntityTypes.Builder.<EntityMinecartHopper>of(EntityMinecartHopper::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartMobSpawner> SPAWNER_MINECART = register("spawner_minecart", EntityTypes.Builder.<EntityMinecartMobSpawner>of(EntityMinecartMobSpawner::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartTNT> TNT_MINECART = register("tnt_minecart", EntityTypes.Builder.<EntityMinecartTNT>of(EntityMinecartTNT::new, EnumCreatureType.MISC).sized(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityHorseMule> MULE = register("mule", EntityTypes.Builder.<EntityHorseMule>of(EntityHorseMule::new, EnumCreatureType.CREATURE).sized(1.3964844F, 1.6F).trackingRange(8));
    public static final EntityTypes<EntityMushroomCow> MOOSHROOM = register("mooshroom", EntityTypes.Builder.<EntityMushroomCow>of(EntityMushroomCow::new, EnumCreatureType.CREATURE).sized(0.9F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityOcelot> OCELOT = register("ocelot", EntityTypes.Builder.<EntityOcelot>of(EntityOcelot::new, EnumCreatureType.CREATURE).sized(0.6F, 0.7F).trackingRange(10));
    public static final EntityTypes<EntityPainting> PAINTING = register("painting", EntityTypes.Builder.<EntityPainting>of(EntityPainting::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityPanda> PANDA = register("panda", EntityTypes.Builder.<EntityPanda>of(EntityPanda::new, EnumCreatureType.CREATURE).sized(1.3F, 1.25F).trackingRange(10));
    public static final EntityTypes<EntityParrot> PARROT = register("parrot", EntityTypes.Builder.<EntityParrot>of(EntityParrot::new, EnumCreatureType.CREATURE).sized(0.5F, 0.9F).trackingRange(8));
    public static final EntityTypes<EntityPhantom> PHANTOM = register("phantom", EntityTypes.Builder.<EntityPhantom>of(EntityPhantom::new, EnumCreatureType.MONSTER).sized(0.9F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityPig> PIG = register("pig", EntityTypes.Builder.<EntityPig>of(EntityPig::new, EnumCreatureType.CREATURE).sized(0.9F, 0.9F).trackingRange(10));
    public static final EntityTypes<EntityPiglin> PIGLIN = register("piglin", EntityTypes.Builder.<EntityPiglin>of(EntityPiglin::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPiglinBrute> PIGLIN_BRUTE = register("piglin_brute", EntityTypes.Builder.<EntityPiglinBrute>of(EntityPiglinBrute::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPillager> PILLAGER = register("pillager", EntityTypes.Builder.<EntityPillager>of(EntityPillager::new, EnumCreatureType.MONSTER).canSpawnFarFromPlayer().sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPolarBear> POLAR_BEAR = register("polar_bear", EntityTypes.Builder.<EntityPolarBear>of(EntityPolarBear::new, EnumCreatureType.CREATURE).immuneTo(Blocks.POWDER_SNOW).sized(1.4F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityTNTPrimed> TNT = register("tnt", EntityTypes.Builder.<EntityTNTPrimed>of(EntityTNTPrimed::new, EnumCreatureType.MISC).fireImmune().sized(0.98F, 0.98F).trackingRange(10).updateInterval(10));
    public static final EntityTypes<EntityPufferFish> PUFFERFISH = register("pufferfish", EntityTypes.Builder.<EntityPufferFish>of(EntityPufferFish::new, EnumCreatureType.WATER_AMBIENT).sized(0.7F, 0.7F).trackingRange(4));
    public static final EntityTypes<EntityRabbit> RABBIT = register("rabbit", EntityTypes.Builder.<EntityRabbit>of(EntityRabbit::new, EnumCreatureType.CREATURE).sized(0.4F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityRavager> RAVAGER = register("ravager", EntityTypes.Builder.<EntityRavager>of(EntityRavager::new, EnumCreatureType.MONSTER).sized(1.95F, 2.2F).trackingRange(10));
    public static final EntityTypes<EntitySalmon> SALMON = register("salmon", EntityTypes.Builder.<EntitySalmon>of(EntitySalmon::new, EnumCreatureType.WATER_AMBIENT).sized(0.7F, 0.4F).trackingRange(4));
    public static final EntityTypes<EntitySheep> SHEEP = register("sheep", EntityTypes.Builder.<EntitySheep>of(EntitySheep::new, EnumCreatureType.CREATURE).sized(0.9F, 1.3F).trackingRange(10));
    public static final EntityTypes<EntityShulker> SHULKER = register("shulker", EntityTypes.Builder.<EntityShulker>of(EntityShulker::new, EnumCreatureType.MONSTER).fireImmune().canSpawnFarFromPlayer().sized(1.0F, 1.0F).trackingRange(10));
    public static final EntityTypes<EntityShulkerBullet> SHULKER_BULLET = register("shulker_bullet", EntityTypes.Builder.<EntityShulkerBullet>of(EntityShulkerBullet::new, EnumCreatureType.MISC).sized(0.3125F, 0.3125F).trackingRange(8));
    public static final EntityTypes<EntitySilverfish> SILVERFISH = register("silverfish", EntityTypes.Builder.<EntitySilverfish>of(EntitySilverfish::new, EnumCreatureType.MONSTER).sized(0.4F, 0.3F).trackingRange(8));
    public static final EntityTypes<EntitySkeleton> SKELETON = register("skeleton", EntityTypes.Builder.<EntitySkeleton>of(EntitySkeleton::new, EnumCreatureType.MONSTER).sized(0.6F, 1.99F).trackingRange(8));
    public static final EntityTypes<EntityHorseSkeleton> SKELETON_HORSE = register("skeleton_horse", EntityTypes.Builder.<EntityHorseSkeleton>of(EntityHorseSkeleton::new, EnumCreatureType.CREATURE).sized(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntitySlime> SLIME = register("slime", EntityTypes.Builder.<EntitySlime>of(EntitySlime::new, EnumCreatureType.MONSTER).sized(2.04F, 2.04F).trackingRange(10));
    public static final EntityTypes<EntitySmallFireball> SMALL_FIREBALL = register("small_fireball", EntityTypes.Builder.<EntitySmallFireball>of(EntitySmallFireball::new, EnumCreatureType.MISC).sized(0.3125F, 0.3125F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntitySnowman> SNOW_GOLEM = register("snow_golem", EntityTypes.Builder.<EntitySnowman>of(EntitySnowman::new, EnumCreatureType.MISC).immuneTo(Blocks.POWDER_SNOW).sized(0.7F, 1.9F).trackingRange(8));
    public static final EntityTypes<EntitySnowball> SNOWBALL = register("snowball", EntityTypes.Builder.<EntitySnowball>of(EntitySnowball::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntitySpectralArrow> SPECTRAL_ARROW = register("spectral_arrow", EntityTypes.Builder.<EntitySpectralArrow>of(EntitySpectralArrow::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<EntitySpider> SPIDER = register("spider", EntityTypes.Builder.<EntitySpider>of(EntitySpider::new, EnumCreatureType.MONSTER).sized(1.4F, 0.9F).trackingRange(8));
    public static final EntityTypes<EntitySquid> SQUID = register("squid", EntityTypes.Builder.<EntitySquid>of(EntitySquid::new, EnumCreatureType.WATER_CREATURE).sized(0.8F, 0.8F).trackingRange(8));
    public static final EntityTypes<EntitySkeletonStray> STRAY = register("stray", EntityTypes.Builder.<EntitySkeletonStray>of(EntitySkeletonStray::new, EnumCreatureType.MONSTER).sized(0.6F, 1.99F).immuneTo(Blocks.POWDER_SNOW).trackingRange(8));
    public static final EntityTypes<EntityStrider> STRIDER = register("strider", EntityTypes.Builder.<EntityStrider>of(EntityStrider::new, EnumCreatureType.CREATURE).fireImmune().sized(0.9F, 1.7F).trackingRange(10));
    public static final EntityTypes<EntityEgg> EGG = register("egg", EntityTypes.Builder.<EntityEgg>of(EntityEgg::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityEnderPearl> ENDER_PEARL = register("ender_pearl", EntityTypes.Builder.<EntityEnderPearl>of(EntityEnderPearl::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityThrownExpBottle> EXPERIENCE_BOTTLE = register("experience_bottle", EntityTypes.Builder.<EntityThrownExpBottle>of(EntityThrownExpBottle::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityPotion> POTION = register("potion", EntityTypes.Builder.<EntityPotion>of(EntityPotion::new, EnumCreatureType.MISC).sized(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityThrownTrident> TRIDENT = register("trident", EntityTypes.Builder.<EntityThrownTrident>of(EntityThrownTrident::new, EnumCreatureType.MISC).sized(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<EntityLlamaTrader> TRADER_LLAMA = register("trader_llama", EntityTypes.Builder.<EntityLlamaTrader>of(EntityLlamaTrader::new, EnumCreatureType.CREATURE).sized(0.9F, 1.87F).trackingRange(10));
    public static final EntityTypes<EntityTropicalFish> TROPICAL_FISH = register("tropical_fish", EntityTypes.Builder.<EntityTropicalFish>of(EntityTropicalFish::new, EnumCreatureType.WATER_AMBIENT).sized(0.5F, 0.4F).trackingRange(4));
    public static final EntityTypes<EntityTurtle> TURTLE = register("turtle", EntityTypes.Builder.<EntityTurtle>of(EntityTurtle::new, EnumCreatureType.CREATURE).sized(1.2F, 0.4F).trackingRange(10));
    public static final EntityTypes<EntityVex> VEX = register("vex", EntityTypes.Builder.<EntityVex>of(EntityVex::new, EnumCreatureType.MONSTER).fireImmune().sized(0.4F, 0.8F).trackingRange(8));
    public static final EntityTypes<EntityVillager> VILLAGER = register("villager", EntityTypes.Builder.<EntityVillager>of(EntityVillager::new, EnumCreatureType.MISC).sized(0.6F, 1.95F).trackingRange(10));
    public static final EntityTypes<EntityVindicator> VINDICATOR = register("vindicator", EntityTypes.Builder.<EntityVindicator>of(EntityVindicator::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityVillagerTrader> WANDERING_TRADER = register("wandering_trader", EntityTypes.Builder.<EntityVillagerTrader>of(EntityVillagerTrader::new, EnumCreatureType.CREATURE).sized(0.6F, 1.95F).trackingRange(10));
    public static final EntityTypes<EntityWitch> WITCH = register("witch", EntityTypes.Builder.<EntityWitch>of(EntityWitch::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityWither> WITHER = register("wither", EntityTypes.Builder.<EntityWither>of(EntityWither::new, EnumCreatureType.MONSTER).fireImmune().immuneTo(Blocks.WITHER_ROSE).sized(0.9F, 3.5F).trackingRange(10));
    public static final EntityTypes<EntitySkeletonWither> WITHER_SKELETON = register("wither_skeleton", EntityTypes.Builder.<EntitySkeletonWither>of(EntitySkeletonWither::new, EnumCreatureType.MONSTER).fireImmune().immuneTo(Blocks.WITHER_ROSE).sized(0.7F, 2.4F).trackingRange(8));
    public static final EntityTypes<EntityWitherSkull> WITHER_SKULL = register("wither_skull", EntityTypes.Builder.<EntityWitherSkull>of(EntityWitherSkull::new, EnumCreatureType.MISC).sized(0.3125F, 0.3125F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityWolf> WOLF = register("wolf", EntityTypes.Builder.<EntityWolf>of(EntityWolf::new, EnumCreatureType.CREATURE).sized(0.6F, 0.85F).trackingRange(10));
    public static final EntityTypes<EntityZoglin> ZOGLIN = register("zoglin", EntityTypes.Builder.<EntityZoglin>of(EntityZoglin::new, EnumCreatureType.MONSTER).fireImmune().sized(1.3964844F, 1.4F).trackingRange(8));
    public static final EntityTypes<EntityZombie> ZOMBIE = register("zombie", EntityTypes.Builder.<EntityZombie>of(EntityZombie::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityHorseZombie> ZOMBIE_HORSE = register("zombie_horse", EntityTypes.Builder.<EntityHorseZombie>of(EntityHorseZombie::new, EnumCreatureType.CREATURE).sized(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntityZombieVillager> ZOMBIE_VILLAGER = register("zombie_villager", EntityTypes.Builder.<EntityZombieVillager>of(EntityZombieVillager::new, EnumCreatureType.MONSTER).sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPigZombie> ZOMBIFIED_PIGLIN = register("zombified_piglin", EntityTypes.Builder.<EntityPigZombie>of(EntityPigZombie::new, EnumCreatureType.MONSTER).fireImmune().sized(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityHuman> PLAYER = register("player", EntityTypes.Builder.<EntityHuman>createNothing(EnumCreatureType.MISC).noSave().noSummon().sized(0.6F, 1.8F).trackingRange(32).updateInterval(2));
    public static final EntityTypes<EntityFishingHook> FISHING_BOBBER = register("fishing_bobber", EntityTypes.Builder.<EntityFishingHook>of(EntityFishingHook::new, EnumCreatureType.MISC).noSave().noSummon().sized(0.25F, 0.25F).trackingRange(4).updateInterval(5));
    private final EntityTypes.EntityFactory<T> factory;
    private final EnumCreatureType category;
    private final ImmutableSet<Block> immuneTo;
    private final boolean serialize;
    private final boolean summon;
    private final boolean fireImmune;
    private final boolean canSpawnFarFromPlayer;
    private final int clientTrackingRange;
    private final int updateInterval;
    @Nullable
    private String descriptionId;
    @Nullable
    private IChatBaseComponent description;
    @Nullable
    private MinecraftKey lootTable;
    private final EntitySize dimensions;

    private static <T extends Entity> EntityTypes<T> register(String id, EntityTypes.Builder<T> type) {
        return IRegistry.register(IRegistry.ENTITY_TYPE, id, type.build(id));
    }

    public static MinecraftKey getName(EntityTypes<?> type) {
        return IRegistry.ENTITY_TYPE.getKey(type);
    }

    public static Optional<EntityTypes<?>> byString(String id) {
        return IRegistry.ENTITY_TYPE.getOptional(MinecraftKey.tryParse(id));
    }

    public EntityTypes(EntityTypes.EntityFactory<T> factory, EnumCreatureType spawnGroup, boolean saveable, boolean summonable, boolean fireImmune, boolean spawnableFarFromPlayer, ImmutableSet<Block> canSpawnInside, EntitySize dimensions, int maxTrackDistance, int trackTickInterval) {
        this.factory = factory;
        this.category = spawnGroup;
        this.canSpawnFarFromPlayer = spawnableFarFromPlayer;
        this.serialize = saveable;
        this.summon = summonable;
        this.fireImmune = fireImmune;
        this.immuneTo = canSpawnInside;
        this.dimensions = dimensions;
        this.clientTrackingRange = maxTrackDistance;
        this.updateInterval = trackTickInterval;
    }

    @Nullable
    public Entity spawnCreature(WorldServer world, @Nullable ItemStack stack, @Nullable EntityHuman player, BlockPosition pos, EnumMobSpawn spawnReason, boolean alignPosition, boolean invertY) {
        return this.spawnCreature(world, stack == null ? null : stack.getTag(), stack != null && stack.hasName() ? stack.getName() : null, player, pos, spawnReason, alignPosition, invertY);
    }

    @Nullable
    public T spawnCreature(WorldServer world, @Nullable NBTTagCompound itemNbt, @Nullable IChatBaseComponent name, @Nullable EntityHuman player, BlockPosition pos, EnumMobSpawn spawnReason, boolean alignPosition, boolean invertY) {
        T entity = this.createCreature(world, itemNbt, name, player, pos, spawnReason, alignPosition, invertY);
        if (entity != null) {
            world.addAllEntities(entity);
        }

        return entity;
    }

    @Nullable
    public T createCreature(WorldServer world, @Nullable NBTTagCompound itemNbt, @Nullable IChatBaseComponent name, @Nullable EntityHuman player, BlockPosition pos, EnumMobSpawn spawnReason, boolean alignPosition, boolean invertY) {
        T entity = this.create(world);
        if (entity == null) {
            return (T)null;
        } else {
            double d;
            if (alignPosition) {
                entity.setPosition((double)pos.getX() + 0.5D, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D);
                d = getYOffset(world, pos, invertY, entity.getBoundingBox());
            } else {
                d = 0.0D;
            }

            entity.setPositionRotation((double)pos.getX() + 0.5D, (double)pos.getY() + d, (double)pos.getZ() + 0.5D, MathHelper.wrapDegrees(world.random.nextFloat() * 360.0F), 0.0F);
            if (entity instanceof EntityInsentient) {
                EntityInsentient mob = (EntityInsentient)entity;
                mob.yHeadRot = mob.getYRot();
                mob.yBodyRot = mob.getYRot();
                mob.prepare(world, world.getDamageScaler(mob.getChunkCoordinates()), spawnReason, (GroupDataEntity)null, itemNbt);
                mob.playAmbientSound();
            }

            if (name != null && entity instanceof EntityLiving) {
                entity.setCustomName(name);
            }

            updateCustomEntityTag(world, player, entity, itemNbt);
            return entity;
        }
    }

    protected static double getYOffset(IWorldReader world, BlockPosition pos, boolean invertY, AxisAlignedBB boundingBox) {
        AxisAlignedBB aABB = new AxisAlignedBB(pos);
        if (invertY) {
            aABB = aABB.expandTowards(0.0D, -1.0D, 0.0D);
        }

        Iterable<VoxelShape> iterable = world.getCollisions((Entity)null, aABB);
        return 1.0D + VoxelShapes.collide(EnumDirection.EnumAxis.Y, boundingBox, iterable, invertY ? -2.0D : -1.0D);
    }

    public static void updateCustomEntityTag(World world, @Nullable EntityHuman player, @Nullable Entity entity, @Nullable NBTTagCompound itemNbt) {
        if (itemNbt != null && itemNbt.hasKeyOfType("EntityTag", 10)) {
            MinecraftServer minecraftServer = world.getMinecraftServer();
            if (minecraftServer != null && entity != null) {
                if (world.isClientSide || !entity.onlyOpCanSetNbt() || player != null && minecraftServer.getPlayerList().isOp(player.getProfile())) {
                    NBTTagCompound compoundTag = entity.save(new NBTTagCompound());
                    UUID uUID = entity.getUniqueID();
                    compoundTag.merge(itemNbt.getCompound("EntityTag"));
                    entity.setUUID(uUID);
                    entity.load(compoundTag);
                }
            }
        }
    }

    public boolean canSerialize() {
        return this.serialize;
    }

    public boolean canSummon() {
        return this.summon;
    }

    public boolean fireImmune() {
        return this.fireImmune;
    }

    public boolean canSpawnFarFromPlayer() {
        return this.canSpawnFarFromPlayer;
    }

    public EnumCreatureType getCategory() {
        return this.category;
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.makeDescriptionId("entity", IRegistry.ENTITY_TYPE.getKey(this));
        }

        return this.descriptionId;
    }

    public IChatBaseComponent getDescription() {
        if (this.description == null) {
            this.description = new ChatMessage(this.getDescriptionId());
        }

        return this.description;
    }

    @Override
    public String toString() {
        return this.getDescriptionId();
    }

    public String toShortString() {
        int i = this.getDescriptionId().lastIndexOf(46);
        return i == -1 ? this.getDescriptionId() : this.getDescriptionId().substring(i + 1);
    }

    public MinecraftKey getDefaultLootTable() {
        if (this.lootTable == null) {
            MinecraftKey resourceLocation = IRegistry.ENTITY_TYPE.getKey(this);
            this.lootTable = new MinecraftKey(resourceLocation.getNamespace(), "entities/" + resourceLocation.getKey());
        }

        return this.lootTable;
    }

    public float getWidth() {
        return this.dimensions.width;
    }

    public float getHeight() {
        return this.dimensions.height;
    }

    @Nullable
    public T create(World world) {
        return this.factory.create(this, world);
    }

    @Nullable
    public static Entity create(int type, World world) {
        return create(world, IRegistry.ENTITY_TYPE.fromId(type));
    }

    public static Optional<Entity> create(NBTTagCompound nbt, World world) {
        return SystemUtils.ifElse(by(nbt).map((entityType) -> {
            return entityType.create(world);
        }), (entity) -> {
            entity.load(nbt);
        }, () -> {
            LOGGER.warn("Skipping Entity with id {}", (Object)nbt.getString("id"));
        });
    }

    @Nullable
    private static Entity create(World world, @Nullable EntityTypes<?> type) {
        return type == null ? null : type.create(world);
    }

    public AxisAlignedBB getAABB(double feetX, double feetY, double feetZ) {
        float f = this.getWidth() / 2.0F;
        return new AxisAlignedBB(feetX - (double)f, feetY, feetZ - (double)f, feetX + (double)f, feetY + (double)this.getHeight(), feetZ + (double)f);
    }

    public boolean isBlockDangerous(IBlockData state) {
        if (this.immuneTo.contains(state.getBlock())) {
            return false;
        } else if (!this.fireImmune && PathfinderNormal.isBurningBlock(state)) {
            return true;
        } else {
            return state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW);
        }
    }

    public EntitySize getDimensions() {
        return this.dimensions;
    }

    public static Optional<EntityTypes<?>> by(NBTTagCompound nbt) {
        return IRegistry.ENTITY_TYPE.getOptional(new MinecraftKey(nbt.getString("id")));
    }

    @Nullable
    public static Entity loadEntityRecursive(NBTTagCompound nbt, World world, Function<Entity, Entity> entityProcessor) {
        return loadStaticEntity(nbt, world).map(entityProcessor).map((entity) -> {
            if (nbt.hasKeyOfType("Passengers", 9)) {
                NBTTagList listTag = nbt.getList("Passengers", 10);

                for(int i = 0; i < listTag.size(); ++i) {
                    Entity entity2 = loadEntityRecursive(listTag.getCompound(i), world, entityProcessor);
                    if (entity2 != null) {
                        entity2.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }).orElse((Entity)null);
    }

    public static Stream<Entity> loadEntitiesRecursive(List<? extends NBTBase> entityNbtList, World world) {
        final Spliterator<? extends NBTBase> spliterator = entityNbtList.spliterator();
        return StreamSupport.stream(new Spliterator<Entity>() {
            @Override
            public boolean tryAdvance(Consumer<? super Entity> consumer) {
                return spliterator.tryAdvance((tag) -> {
                    EntityTypes.loadEntityRecursive((NBTTagCompound)tag, world, (entity) -> {
                        consumer.accept(entity);
                        return entity;
                    });
                });
            }

            @Override
            public Spliterator<Entity> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return (long)entityNbtList.size();
            }

            @Override
            public int characteristics() {
                return 1297;
            }
        }, false);
    }

    private static Optional<Entity> loadStaticEntity(NBTTagCompound nbt, World world) {
        try {
            return create(nbt, world);
        } catch (RuntimeException var3) {
            LOGGER.warn("Exception loading entity: ", (Throwable)var3);
            return Optional.empty();
        }
    }

    public int getChunkRange() {
        return this.clientTrackingRange;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }

    public boolean isDeltaTracking() {
        return this != PLAYER && this != LLAMA_SPIT && this != WITHER && this != BAT && this != ITEM_FRAME && this != GLOW_ITEM_FRAME && this != LEASH_KNOT && this != PAINTING && this != END_CRYSTAL && this != EVOKER_FANGS;
    }

    public boolean is(Tag<EntityTypes<?>> tag) {
        return tag.isTagged(this);
    }

    @Nullable
    @Override
    public T tryCast(Entity obj) {
        return (T)(obj.getEntityType() == this ? obj : null);
    }

    @Override
    public Class<? extends Entity> getBaseClass() {
        return Entity.class;
    }

    public static class Builder<T extends Entity> {
        private final EntityTypes.EntityFactory<T> factory;
        private final EnumCreatureType category;
        private ImmutableSet<Block> immuneTo = ImmutableSet.of();
        private boolean serialize = true;
        private boolean summon = true;
        private boolean fireImmune;
        private boolean canSpawnFarFromPlayer;
        private int clientTrackingRange = 5;
        private int updateInterval = 3;
        private EntitySize dimensions = EntitySize.scalable(0.6F, 1.8F);

        private Builder(EntityTypes.EntityFactory<T> factory, EnumCreatureType spawnGroup) {
            this.factory = factory;
            this.category = spawnGroup;
            this.canSpawnFarFromPlayer = spawnGroup == EnumCreatureType.CREATURE || spawnGroup == EnumCreatureType.MISC;
        }

        public static <T extends Entity> EntityTypes.Builder<T> of(EntityTypes.EntityFactory<T> factory, EnumCreatureType spawnGroup) {
            return new EntityTypes.Builder<>(factory, spawnGroup);
        }

        public static <T extends Entity> EntityTypes.Builder<T> createNothing(EnumCreatureType spawnGroup) {
            return new EntityTypes.Builder<>((entityType, level) -> {
                return (T)null;
            }, spawnGroup);
        }

        public EntityTypes.Builder<T> sized(float width, float height) {
            this.dimensions = EntitySize.scalable(width, height);
            return this;
        }

        public EntityTypes.Builder<T> noSummon() {
            this.summon = false;
            return this;
        }

        public EntityTypes.Builder<T> noSave() {
            this.serialize = false;
            return this;
        }

        public EntityTypes.Builder<T> fireImmune() {
            this.fireImmune = true;
            return this;
        }

        public EntityTypes.Builder<T> immuneTo(Block... blocks) {
            this.immuneTo = ImmutableSet.copyOf(blocks);
            return this;
        }

        public EntityTypes.Builder<T> canSpawnFarFromPlayer() {
            this.canSpawnFarFromPlayer = true;
            return this;
        }

        public EntityTypes.Builder<T> trackingRange(int maxTrackingRange) {
            this.clientTrackingRange = maxTrackingRange;
            return this;
        }

        public EntityTypes.Builder<T> updateInterval(int trackingTickInterval) {
            this.updateInterval = trackingTickInterval;
            return this;
        }

        public EntityTypes<T> build(String id) {
            if (this.serialize) {
                SystemUtils.fetchChoiceType(DataConverterTypes.ENTITY_TREE, id);
            }

            return new EntityTypes<>(this.factory, this.category, this.serialize, this.summon, this.fireImmune, this.canSpawnFarFromPlayer, this.immuneTo, this.dimensions, this.clientTrackingRange, this.updateInterval);
        }
    }

    public interface EntityFactory<T extends Entity> {
        T create(EntityTypes<T> type, World world);
    }
}
