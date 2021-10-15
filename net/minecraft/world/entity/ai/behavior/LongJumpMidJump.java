package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.EntityHuman;

public class LongJumpMidJump extends Behavior<EntityInsentient> {
    public static final int TIME_OUT_DURATION = 100;
    private final UniformInt timeBetweenLongJumps;
    private SoundEffect landingSound;

    public LongJumpMidJump(UniformInt cooldownRange, SoundEffect soundEvent) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), 100);
        this.timeBetweenLongJumps = cooldownRange;
        this.landingSound = soundEvent;
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        return !mob.isOnGround();
    }

    @Override
    protected void start(WorldServer serverLevel, EntityInsentient mob, long l) {
        mob.setDiscardFriction(true);
        mob.setPose(EntityPose.LONG_JUMPING);
    }

    @Override
    protected void stop(WorldServer world, EntityInsentient entity, long time) {
        if (entity.isOnGround()) {
            entity.setMot(entity.getMot().scale((double)0.1F));
            world.playSound((EntityHuman)null, entity, this.landingSound, SoundCategory.NEUTRAL, 2.0F, 1.0F);
        }

        entity.setDiscardFriction(false);
        entity.setPose(EntityPose.STANDING);
        entity.getBehaviorController().removeMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
        entity.getBehaviorController().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(world.random));
    }
}
