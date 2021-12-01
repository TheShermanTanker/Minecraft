package net.minecraft.world.entity.animal;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
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

    public static boolean checkSurfaceWaterAnimalSpawnRules(EntityTypes<? extends EntityWaterAnimal> type, GeneratorAccess world, EnumMobSpawn reason, BlockPosition pos, Random random) {
        int i = world.getSeaLevel();
        int j = i - 13;
        return world.getFluid(pos.below()).is(TagsFluid.WATER) && world.getType(pos.above()).is(Blocks.WATER) && pos.getY() >= j && pos.getY() <= i;
    }
}
