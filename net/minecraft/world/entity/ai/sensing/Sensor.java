package net.minecraft.world.entity.ai.sensing;

import java.util.Random;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;

public abstract class Sensor<E extends EntityLiving> {
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_SCAN_RATE = 20;
    protected static final int TARGETING_RANGE = 16;
    private static final PathfinderTargetCondition TARGET_CONDITIONS = PathfinderTargetCondition.forNonCombat().range(16.0D);
    private static final PathfinderTargetCondition TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = PathfinderTargetCondition.forNonCombat().range(16.0D).ignoreInvisibilityTesting();
    private static final PathfinderTargetCondition ATTACK_TARGET_CONDITIONS = PathfinderTargetCondition.forCombat().range(16.0D);
    private static final PathfinderTargetCondition ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = PathfinderTargetCondition.forCombat().range(16.0D).ignoreInvisibilityTesting();
    private static final PathfinderTargetCondition ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = PathfinderTargetCondition.forCombat().range(16.0D).ignoreLineOfSight();
    private static final PathfinderTargetCondition ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = PathfinderTargetCondition.forCombat().range(16.0D).ignoreLineOfSight().ignoreInvisibilityTesting();
    private final int scanRate;
    private long timeToTick;

    public Sensor(int senseInterval) {
        this.scanRate = senseInterval;
        this.timeToTick = (long)RANDOM.nextInt(senseInterval);
    }

    public Sensor() {
        this(20);
    }

    public final void tick(WorldServer world, E entity) {
        if (--this.timeToTick <= 0L) {
            this.timeToTick = (long)this.scanRate;
            this.doTick(world, entity);
        }

    }

    protected abstract void doTick(WorldServer world, E entity);

    public abstract Set<MemoryModuleType<?>> requires();

    public static boolean isEntityTargetable(EntityLiving entity, EntityLiving target) {
        return entity.getBehaviorController().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(entity, target) : TARGET_CONDITIONS.test(entity, target);
    }

    public static boolean isEntityAttackable(EntityLiving entity, EntityLiving target) {
        return entity.getBehaviorController().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(entity, target) : ATTACK_TARGET_CONDITIONS.test(entity, target);
    }

    public static boolean isEntityAttackableIgnoringLineOfSight(EntityLiving entity, EntityLiving target) {
        return entity.getBehaviorController().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(entity, target) : ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(entity, target);
    }
}
