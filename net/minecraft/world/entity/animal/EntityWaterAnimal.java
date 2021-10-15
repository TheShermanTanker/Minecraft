package net.minecraft.world.entity.animal;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class EntityWaterAnimal extends EntityCreature {
    protected EntityWaterAnimal(EntityTypes<? extends EntityWaterAnimal> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.WATER;
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        return 1 + this.level.random.nextInt(3);
    }

    protected void handleAirSupply(int air) {
        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirTicks(air - 1);
            if (this.getAirTicks() == -20) {
                this.setAirTicks(0);
                this.damageEntity(DamageSource.DROWN, 2.0F);
            }
        } else {
            this.setAirTicks(300);
        }

    }

    @Override
    public void entityBaseTick() {
        int i = this.getAirTicks();
        super.entityBaseTick();
        this.handleAirSupply(i);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return false;
    }

    public static boolean checkUndergroundWaterCreatureSpawnRules(EntityTypes<? extends EntityLiving> entityType, WorldAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return pos.getY() < world.getSeaLevel() && pos.getY() < world.getHeight(HeightMap.Type.OCEAN_FLOOR, pos.getX(), pos.getZ()) && isDarkEnoughToSpawn(world, pos) && isBaseStoneBelow(pos, world);
    }

    public static boolean isBaseStoneBelow(BlockPosition pos, WorldAccess world) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 0; i < 5; ++i) {
            mutableBlockPos.move(EnumDirection.DOWN);
            IBlockData blockState = world.getType(mutableBlockPos);
            if (blockState.is(TagsBlock.BASE_STONE_OVERWORLD)) {
                return true;
            }

            if (!blockState.is(Blocks.WATER)) {
                return false;
            }
        }

        return false;
    }

    public static boolean isDarkEnoughToSpawn(WorldAccess world, BlockPosition pos) {
        int i = world.getLevel().isThundering() ? world.getMaxLocalRawBrightness(pos, 10) : world.getLightLevel(pos);
        return i == 0;
    }
}
