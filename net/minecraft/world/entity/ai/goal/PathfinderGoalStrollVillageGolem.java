package net.minecraft.world.entity.ai.goal;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceRecord;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalStrollVillageGolem extends PathfinderGoalRandomStroll {
    private static final int POI_SECTION_SCAN_RADIUS = 2;
    private static final int VILLAGER_SCAN_RADIUS = 32;
    private static final int RANDOM_POS_XY_DISTANCE = 10;
    private static final int RANDOM_POS_Y_DISTANCE = 7;

    public PathfinderGoalStrollVillageGolem(EntityCreature entity, double speed) {
        super(entity, speed, 240, false);
    }

    @Nullable
    @Override
    protected Vec3D getPosition() {
        float f = this.mob.level.random.nextFloat();
        if (this.mob.level.random.nextFloat() < 0.3F) {
            return this.getPositionTowardsAnywhere();
        } else {
            Vec3D vec3;
            if (f < 0.7F) {
                vec3 = this.getPositionTowardsVillagerWhoWantsGolem();
                if (vec3 == null) {
                    vec3 = this.getPositionTowardsPoi();
                }
            } else {
                vec3 = this.getPositionTowardsPoi();
                if (vec3 == null) {
                    vec3 = this.getPositionTowardsVillagerWhoWantsGolem();
                }
            }

            return vec3 == null ? this.getPositionTowardsAnywhere() : vec3;
        }
    }

    @Nullable
    private Vec3D getPositionTowardsAnywhere() {
        return LandRandomPos.getPos(this.mob, 10, 7);
    }

    @Nullable
    private Vec3D getPositionTowardsVillagerWhoWantsGolem() {
        WorldServer serverLevel = (WorldServer)this.mob.level;
        List<EntityVillager> list = serverLevel.getEntities(EntityTypes.VILLAGER, this.mob.getBoundingBox().inflate(32.0D), this::doesVillagerWantGolem);
        if (list.isEmpty()) {
            return null;
        } else {
            EntityVillager villager = list.get(this.mob.level.random.nextInt(list.size()));
            Vec3D vec3 = villager.getPositionVector();
            return LandRandomPos.getPosTowards(this.mob, 10, 7, vec3);
        }
    }

    @Nullable
    private Vec3D getPositionTowardsPoi() {
        SectionPosition sectionPos = this.getRandomVillageSection();
        if (sectionPos == null) {
            return null;
        } else {
            BlockPosition blockPos = this.getRandomPoiWithinSection(sectionPos);
            return blockPos == null ? null : LandRandomPos.getPosTowards(this.mob, 10, 7, Vec3D.atBottomCenterOf(blockPos));
        }
    }

    @Nullable
    private SectionPosition getRandomVillageSection() {
        WorldServer serverLevel = (WorldServer)this.mob.level;
        List<SectionPosition> list = SectionPosition.cube(SectionPosition.of(this.mob), 2).filter((sectionPos) -> {
            return serverLevel.sectionsToVillage(sectionPos) == 0;
        }).collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(serverLevel.random.nextInt(list.size()));
    }

    @Nullable
    private BlockPosition getRandomPoiWithinSection(SectionPosition pos) {
        WorldServer serverLevel = (WorldServer)this.mob.level;
        VillagePlace poiManager = serverLevel.getPoiManager();
        List<BlockPosition> list = poiManager.getInRange((poiType) -> {
            return true;
        }, pos.center(), 8, VillagePlace.Occupancy.IS_OCCUPIED).map(VillagePlaceRecord::getPos).collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(serverLevel.random.nextInt(list.size()));
    }

    private boolean doesVillagerWantGolem(EntityVillager villager) {
        return villager.wantsToSpawnGolem(this.mob.level.getTime());
    }
}
