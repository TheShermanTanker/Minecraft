package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityFox;
import net.minecraft.world.entity.animal.EntityMushroomCow;
import net.minecraft.world.entity.animal.EntityOcelot;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.entity.animal.EntityPolarBear;
import net.minecraft.world.entity.animal.EntityRabbit;
import net.minecraft.world.entity.animal.EntityTropicalFish;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.EntityWaterAnimal;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.animal.axolotl.EntityAxolotl;
import net.minecraft.world.entity.animal.goat.EntityGoat;
import net.minecraft.world.entity.monster.EntityDrowned;
import net.minecraft.world.entity.monster.EntityEndermite;
import net.minecraft.world.entity.monster.EntityGhast;
import net.minecraft.world.entity.monster.EntityGuardian;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.EntityMonsterPatrolling;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.entity.monster.EntitySilverfish;
import net.minecraft.world.entity.monster.EntitySkeletonStray;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.monster.EntityZombieHusk;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.levelgen.HeightMap;

public class EntityPositionTypes {
    private static final Map<EntityTypes<?>, EntityPositionTypes.Data> DATA_BY_TYPE = Maps.newHashMap();

    private static <T extends EntityInsentient> void register(EntityTypes<T> type, EntityPositionTypes.Surface location, HeightMap.Type heightmapType, EntityPositionTypes.SpawnPredicate<T> predicate) {
        EntityPositionTypes.Data data = DATA_BY_TYPE.put(type, new EntityPositionTypes.Data(heightmapType, location, predicate));
        if (data != null) {
            throw new IllegalStateException("Duplicate registration for type " + IRegistry.ENTITY_TYPE.getKey(type));
        }
    }

    public static EntityPositionTypes.Surface getPlacementType(EntityTypes<?> type) {
        EntityPositionTypes.Data data = DATA_BY_TYPE.get(type);
        return data == null ? EntityPositionTypes.Surface.NO_RESTRICTIONS : data.placement;
    }

    public static HeightMap.Type getHeightmapType(@Nullable EntityTypes<?> type) {
        EntityPositionTypes.Data data = DATA_BY_TYPE.get(type);
        return data == null ? HeightMap.Type.MOTION_BLOCKING_NO_LEAVES : data.heightMap;
    }

    public static <T extends Entity> boolean checkSpawnRules(EntityTypes<T> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        EntityPositionTypes.Data data = DATA_BY_TYPE.get(type);
        return data == null || data.predicate.test(type, world, spawnReason, pos, random);
    }

    static {
        register(EntityTypes.AXOLOTL, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAxolotl::checkAxolotlSpawnRules);
        register(EntityTypes.COD, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        register(EntityTypes.DOLPHIN, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        register(EntityTypes.DROWNED, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityDrowned::checkDrownedSpawnRules);
        register(EntityTypes.GUARDIAN, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGuardian::checkGuardianSpawnRules);
        register(EntityTypes.PUFFERFISH, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        register(EntityTypes.SALMON, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        register(EntityTypes.SQUID, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWaterAnimal::checkSurfaceWaterAnimalSpawnRules);
        register(EntityTypes.TROPICAL_FISH, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTropicalFish::checkTropicalFishSpawnRules);
        register(EntityTypes.BAT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBat::checkBatSpawnRules);
        register(EntityTypes.BLAZE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkAnyLightMonsterSpawnRules);
        register(EntityTypes.CAVE_SPIDER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.CHICKEN, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.COW, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.CREEPER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.DONKEY, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.ENDERMAN, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.ENDERMITE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEndermite::checkEndermiteSpawnRules);
        register(EntityTypes.ENDER_DRAGON, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.GHAST, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGhast::checkGhastSpawnRules);
        register(EntityTypes.GIANT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.GLOW_SQUID, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGlowSquid::checkGlowSquideSpawnRules);
        register(EntityTypes.GOAT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGoat::checkGoatSpawnRules);
        register(EntityTypes.HORSE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.HUSK, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityZombieHusk::checkHuskSpawnRules);
        register(EntityTypes.IRON_GOLEM, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.LLAMA, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.MAGMA_CUBE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMagmaCube::checkMagmaCubeSpawnRules);
        register(EntityTypes.MOOSHROOM, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMushroomCow::checkMushroomSpawnRules);
        register(EntityTypes.MULE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.OCELOT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING, EntityOcelot::checkOcelotSpawnRules);
        register(EntityTypes.PARROT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING, EntityParrot::checkParrotSpawnRules);
        register(EntityTypes.PIG, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.HOGLIN, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityHoglin::checkHoglinSpawnRules);
        register(EntityTypes.PIGLIN, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityPiglin::checkPiglinSpawnRules);
        register(EntityTypes.PILLAGER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonsterPatrolling::checkPatrollingMonsterSpawnRules);
        register(EntityTypes.POLAR_BEAR, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityPolarBear::checkPolarBearSpawnRules);
        register(EntityTypes.RABBIT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRabbit::checkRabbitSpawnRules);
        register(EntityTypes.SHEEP, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.SILVERFISH, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySilverfish::checkSilverfishSpawnRules);
        register(EntityTypes.SKELETON, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.SKELETON_HORSE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.SLIME, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySlime::checkSlimeSpawnRules);
        register(EntityTypes.SNOW_GOLEM, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.SPIDER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.STRAY, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySkeletonStray::checkStraySpawnRules);
        register(EntityTypes.STRIDER, EntityPositionTypes.Surface.IN_LAVA, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityStrider::checkStriderSpawnRules);
        register(EntityTypes.TURTLE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTurtle::checkTurtleSpawnRules);
        register(EntityTypes.VILLAGER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.WITCH, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.WITHER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.WITHER_SKELETON, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.WOLF, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityWolf::checkWolfSpawnRules);
        register(EntityTypes.ZOMBIE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.ZOMBIE_HORSE, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.ZOMBIFIED_PIGLIN, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityPigZombie::checkZombifiedPiglinSpawnRules);
        register(EntityTypes.ZOMBIE_VILLAGER, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.CAT, EntityPositionTypes.Surface.ON_GROUND, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.ELDER_GUARDIAN, EntityPositionTypes.Surface.IN_WATER, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGuardian::checkGuardianSpawnRules);
        register(EntityTypes.EVOKER, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.FOX, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFox::checkFoxSpawnRules);
        register(EntityTypes.ILLUSIONER, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.PANDA, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.PHANTOM, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.RAVAGER, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.SHULKER, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
        register(EntityTypes.TRADER_LLAMA, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnimal::checkAnimalSpawnRules);
        register(EntityTypes.VEX, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.VINDICATOR, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMonster::checkMonsterSpawnRules);
        register(EntityTypes.WANDERING_TRADER, EntityPositionTypes.Surface.NO_RESTRICTIONS, HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, EntityInsentient::checkMobSpawnRules);
    }

    static class Data {
        final HeightMap.Type heightMap;
        final EntityPositionTypes.Surface placement;
        final EntityPositionTypes.SpawnPredicate<?> predicate;

        public Data(HeightMap.Type heightmapType, EntityPositionTypes.Surface location, EntityPositionTypes.SpawnPredicate<?> predicate) {
            this.heightMap = heightmapType;
            this.placement = location;
            this.predicate = predicate;
        }
    }

    @FunctionalInterface
    public interface SpawnPredicate<T extends Entity> {
        boolean test(EntityTypes<T> type, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random);
    }

    public static enum Surface {
        ON_GROUND,
        IN_WATER,
        NO_RESTRICTIONS,
        IN_LAVA;
    }
}
