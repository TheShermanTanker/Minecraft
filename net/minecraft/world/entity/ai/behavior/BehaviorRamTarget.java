package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;

public class BehaviorRamTarget<E extends EntityCreature> extends Behavior<E> {
    public static final int TIME_OUT_DURATION = 200;
    public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
    private final Function<E, UniformInt> getTimeBetweenRams;
    private final PathfinderTargetCondition ramTargeting;
    private final float speed;
    private final ToDoubleFunction<E> getKnockbackForce;
    private Vec3D ramDirection;
    private final Function<E, SoundEffect> getImpactSound;

    public BehaviorRamTarget(Function<E, UniformInt> cooldownRangeFactory, PathfinderTargetCondition targetPredicate, float speed, ToDoubleFunction<E> strengthMultiplierFactory, Function<E, SoundEffect> soundFactory) {
        super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
        this.getTimeBetweenRams = cooldownRangeFactory;
        this.ramTargeting = targetPredicate;
        this.speed = speed;
        this.getKnockbackForce = strengthMultiplierFactory;
        this.getImpactSound = soundFactory;
        this.ramDirection = Vec3D.ZERO;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        return pathfinderMob.getBehaviorController().hasMemory(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        BlockPosition blockPos = pathfinderMob.getChunkCoordinates();
        BehaviorController<?> brain = pathfinderMob.getBehaviorController();
        Vec3D vec3 = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
        this.ramDirection = (new Vec3D((double)blockPos.getX() - vec3.getX(), 0.0D, (double)blockPos.getZ() - vec3.getZ())).normalize();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, this.speed, 0));
    }

    @Override
    protected void tick(WorldServer serverLevel, E pathfinderMob, long l) {
        List<EntityLiving> list = serverLevel.getNearbyEntities(EntityLiving.class, this.ramTargeting, pathfinderMob, pathfinderMob.getBoundingBox());
        BehaviorController<?> brain = pathfinderMob.getBehaviorController();
        if (!list.isEmpty()) {
            EntityLiving livingEntity = list.get(0);
            livingEntity.damageEntity(DamageSource.mobAttack(pathfinderMob).setNoAggro(), (float)pathfinderMob.getAttributeValue(GenericAttributes.ATTACK_DAMAGE));
            int i = pathfinderMob.hasEffect(MobEffects.MOVEMENT_SPEED) ? pathfinderMob.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
            int j = pathfinderMob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? pathfinderMob.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1 : 0;
            float f = 0.25F * (float)(i - j);
            float g = MathHelper.clamp(pathfinderMob.getSpeed() * 1.65F, 0.2F, 3.0F) + f;
            float h = livingEntity.applyBlockingModifier(DamageSource.mobAttack(pathfinderMob)) ? 0.5F : 1.0F;
            livingEntity.knockback((double)(h * g) * this.getKnockbackForce.applyAsDouble(pathfinderMob), this.ramDirection.getX(), this.ramDirection.getZ());
            this.finishRam(serverLevel, pathfinderMob);
            serverLevel.playSound((EntityHuman)null, pathfinderMob, this.getImpactSound.apply(pathfinderMob), SoundCategory.HOSTILE, 1.0F, 1.0F);
        } else {
            Optional<MemoryTarget> optional = brain.getMemory(MemoryModuleType.WALK_TARGET);
            Optional<Vec3D> optional2 = brain.getMemory(MemoryModuleType.RAM_TARGET);
            boolean bl = !optional.isPresent() || !optional2.isPresent() || optional.get().getTarget().currentPosition().distanceTo(optional2.get()) < 0.25D;
            if (bl) {
                this.finishRam(serverLevel, pathfinderMob);
            }
        }

    }

    protected void finishRam(WorldServer world, E entity) {
        world.broadcastEntityEffect(entity, (byte)59);
        entity.getBehaviorController().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(entity).sample(world.random));
        entity.getBehaviorController().removeMemory(MemoryModuleType.RAM_TARGET);
    }
}
