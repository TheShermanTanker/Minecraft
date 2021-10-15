package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class Behaviors {
    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getCorePackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(0, new BehaviorSwim(0.8F)), Pair.of(0, new BehaviorInteractDoor()), Pair.of(0, new BehaviorLook(45, 90)), Pair.of(0, new BehaviorPanic()), Pair.of(0, new BehaviorWake()), Pair.of(0, new BehaviorBellAlert()), Pair.of(0, new BehaviorRaid()), Pair.of(0, new BehaviorPositionValidate(profession.getJobPoiType(), MemoryModuleType.JOB_SITE)), Pair.of(0, new BehaviorPositionValidate(profession.getJobPoiType(), MemoryModuleType.POTENTIAL_JOB_SITE)), Pair.of(1, new BehavorMove()), Pair.of(2, new BehaviorBetterJob(profession)), Pair.of(3, new BehaviorInteractPlayer(speed)), Pair.of(5, new BehaviorFindAdmirableItem(speed, false, 4)), Pair.of(6, new BehaviorFindPosition(profession.getJobPoiType(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty())), Pair.of(7, new BehaviorPotentialJobSite(speed)), Pair.of(8, new BehaviorLeaveJob(speed)), Pair.of(10, new BehaviorFindPosition(VillagePlaceType.HOME, MemoryModuleType.HOME, false, Optional.of((byte)14))), Pair.of(10, new BehaviorFindPosition(VillagePlaceType.MEETING, MemoryModuleType.MEETING_POINT, true, Optional.of((byte)14))), Pair.of(10, new BehaviorCareer()), Pair.of(10, new BehaviorProfession()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getWorkPackage(VillagerProfession profession, float speed) {
        BehaviorWork workAtPoi;
        if (profession == VillagerProfession.FARMER) {
            workAtPoi = new BehaviorWorkComposter();
        } else {
            workAtPoi = new BehaviorWork();
        }

        return ImmutableList.of(getMinimalLookBehavior(), Pair.of(5, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(workAtPoi, 7), Pair.of(new BehaviorStrollPosition(MemoryModuleType.JOB_SITE, 0.4F, 4), 2), Pair.of(new BehaviorStrollPlace(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5), Pair.of(new BehaviorStrollPlaceList(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5), Pair.of(new BehaviorFarm(), profession == VillagerProfession.FARMER ? 2 : 5), Pair.of(new BehaviorBonemeal(), profession == VillagerProfession.FARMER ? 4 : 7)))), Pair.of(10, new BehaviorTradePlayer(400, 1600)), Pair.of(10, new BehaviorLookInteract(EntityTypes.PLAYER, 4)), Pair.of(2, new BehaviorWalkAwayBlock(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)), Pair.of(3, new BehaviorVillageHeroGift(100)), Pair.of(99, new BehaviorSchedule()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getPlayPackage(float speed) {
        return ImmutableList.of(Pair.of(0, new BehavorMove(80, 120)), getFullLookBehavior(), Pair.of(5, new BehaviorPlay()), Pair.of(5, new BehaviorGateSingle<>(ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_ABSENT), ImmutableList.of(Pair.of(BehaviorInteract.of(EntityTypes.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2), Pair.of(BehaviorInteract.of(EntityTypes.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1), Pair.of(new BehaviorStrollRandom(speed), 1), Pair.of(new BehaviorLookWalk(speed, 2), 1), Pair.of(new BehaviorBedJump(speed), 2), Pair.of(new BehaviorNop(20, 40), 2)))), Pair.of(99, new BehaviorSchedule()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getRestPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(2, new BehaviorWalkAwayBlock(MemoryModuleType.HOME, speed, 1, 150, 1200)), Pair.of(3, new BehaviorPositionValidate(VillagePlaceType.HOME, MemoryModuleType.HOME)), Pair.of(3, new BehaviorSleep()), Pair.of(5, new BehaviorGateSingle<>(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT), ImmutableList.of(Pair.of(new BehaviorWalkHome(speed), 1), Pair.of(new BehaviorStrollInside(speed), 4), Pair.of(new BehaviorNearestVillage(speed, 4), 2), Pair.of(new BehaviorNop(20, 40), 2)))), getMinimalLookBehavior(), Pair.of(99, new BehaviorSchedule()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getMeetPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(2, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorStrollPosition(MemoryModuleType.MEETING_POINT, 0.4F, 40), 2), Pair.of(new BehaviorBell(), 2)))), Pair.of(10, new BehaviorTradePlayer(400, 1600)), Pair.of(10, new BehaviorLookInteract(EntityTypes.PLAYER, 4)), Pair.of(2, new BehaviorWalkAwayBlock(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)), Pair.of(3, new BehaviorVillageHeroGift(100)), Pair.of(3, new BehaviorPositionValidate(VillagePlaceType.MEETING, MemoryModuleType.MEETING_POINT)), Pair.of(3, new BehaviorGate<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), BehaviorGate.Order.ORDERED, BehaviorGate.Execution.RUN_ONE, ImmutableList.of(Pair.of(new BehaviorTradeVillager(), 1)))), getFullLookBehavior(), Pair.of(99, new BehaviorSchedule()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getIdlePackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(2, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(BehaviorInteract.of(EntityTypes.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2), Pair.of(new BehaviorInteract<>(EntityTypes.VILLAGER, 8, EntityAgeable::canBreed, EntityAgeable::canBreed, MemoryModuleType.BREED_TARGET, speed, 2), 1), Pair.of(BehaviorInteract.of(EntityTypes.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1), Pair.of(new BehaviorStrollRandom(speed), 1), Pair.of(new BehaviorLookWalk(speed, 2), 1), Pair.of(new BehaviorBedJump(speed), 1), Pair.of(new BehaviorNop(30, 60), 1)))), Pair.of(3, new BehaviorVillageHeroGift(100)), Pair.of(3, new BehaviorLookInteract(EntityTypes.PLAYER, 4)), Pair.of(3, new BehaviorTradePlayer(400, 1600)), Pair.of(3, new BehaviorGate<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), BehaviorGate.Order.ORDERED, BehaviorGate.Execution.RUN_ONE, ImmutableList.of(Pair.of(new BehaviorTradeVillager(), 1)))), Pair.of(3, new BehaviorGate<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), BehaviorGate.Order.ORDERED, BehaviorGate.Execution.RUN_ONE, ImmutableList.of(Pair.of(new BehaviorMakeLove(), 1)))), getFullLookBehavior(), Pair.of(99, new BehaviorSchedule()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getPanicPackage(VillagerProfession profession, float speed) {
        float f = speed * 1.5F;
        return ImmutableList.of(Pair.of(0, new BehaviorCooldown()), Pair.of(1, BehaviorWalkAway.entity(MemoryModuleType.NEAREST_HOSTILE, f, 6, false)), Pair.of(1, BehaviorWalkAway.entity(MemoryModuleType.HURT_BY_ENTITY, f, 6, false)), Pair.of(3, new BehaviorStrollRandom(f, 2, 2)), getMinimalLookBehavior());
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getPreRaidPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(0, new BehaviorBellRing()), Pair.of(0, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorWalkAwayBlock(MemoryModuleType.MEETING_POINT, speed * 1.5F, 2, 150, 200), 6), Pair.of(new BehaviorStrollRandom(speed * 1.5F), 2)))), getMinimalLookBehavior(), Pair.of(99, new BehaviorRaidReset()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getRaidPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(Pair.of(0, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorOutsideCelebrate(speed), 5), Pair.of(new BehaviorVictory(speed * 1.1F), 2)))), Pair.of(0, new BehaviorCelebrate(600, 600)), Pair.of(2, new BehaviorHomeRaid(24, speed * 1.4F)), getMinimalLookBehavior(), Pair.of(99, new BehaviorRaidReset()));
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super EntityVillager>>> getHidePackage(VillagerProfession profession, float speed) {
        int i = 2;
        return ImmutableList.of(Pair.of(0, new BehaviorHide(15, 3)), Pair.of(1, new BehaviorHome(32, speed * 1.25F, 2)), getMinimalLookBehavior());
    }

    private static Pair<Integer, Behavior<EntityLiving>> getFullLookBehavior() {
        return Pair.of(5, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorLookTarget(EntityTypes.CAT, 8.0F), 8), Pair.of(new BehaviorLookTarget(EntityTypes.VILLAGER, 8.0F), 2), Pair.of(new BehaviorLookTarget(EntityTypes.PLAYER, 8.0F), 2), Pair.of(new BehaviorLookTarget(EnumCreatureType.CREATURE, 8.0F), 1), Pair.of(new BehaviorLookTarget(EnumCreatureType.WATER_CREATURE, 8.0F), 1), Pair.of(new BehaviorLookTarget(EnumCreatureType.UNDERGROUND_WATER_CREATURE, 8.0F), 1), Pair.of(new BehaviorLookTarget(EnumCreatureType.WATER_AMBIENT, 8.0F), 1), Pair.of(new BehaviorLookTarget(EnumCreatureType.MONSTER, 8.0F), 1), Pair.of(new BehaviorNop(30, 60), 2))));
    }

    private static Pair<Integer, Behavior<EntityLiving>> getMinimalLookBehavior() {
        return Pair.of(5, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorLookTarget(EntityTypes.VILLAGER, 8.0F), 2), Pair.of(new BehaviorLookTarget(EntityTypes.PLAYER, 8.0F), 2), Pair.of(new BehaviorNop(30, 60), 8))));
    }
}
