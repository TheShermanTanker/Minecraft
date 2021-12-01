package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom2;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class LongJumpToRandomPos<E extends EntityInsentient> extends Behavior<E> {
    private static final int FIND_JUMP_TRIES = 20;
    private static final int PREPARE_JUMP_DURATION = 40;
    private static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
    public static final int TIME_OUT_DURATION = 200;
    private final IntProviderUniform timeBetweenLongJumps;
    private final int maxLongJumpHeight;
    private final int maxLongJumpWidth;
    private final float maxJumpVelocity;
    private final List<LongJumpToRandomPos.PossibleJump> jumpCandidates = new ArrayList<>();
    private Optional<Vec3D> initialPosition = Optional.empty();
    private Optional<LongJumpToRandomPos.PossibleJump> chosenJump = Optional.empty();
    private int findJumpTries;
    private long prepareJumpStart;
    private Function<E, SoundEffect> getJumpSound;

    public LongJumpToRandomPos(IntProviderUniform cooldownRange, int verticalRange, int horizontalRange, float maxRange, Function<E, SoundEffect> entityToSound) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), 200);
        this.timeBetweenLongJumps = cooldownRange;
        this.maxLongJumpHeight = verticalRange;
        this.maxLongJumpWidth = horizontalRange;
        this.maxJumpVelocity = maxRange;
        this.getJumpSound = entityToSound;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityInsentient entity) {
        return entity.isOnGround() && !world.getType(entity.getChunkCoordinates()).is(Blocks.HONEY_BLOCK);
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        boolean bl = this.initialPosition.isPresent() && this.initialPosition.get().equals(mob.getPositionVector()) && this.findJumpTries > 0 && (this.chosenJump.isPresent() || !this.jumpCandidates.isEmpty());
        if (!bl && !mob.getBehaviorController().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isPresent()) {
            mob.getBehaviorController().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(serverLevel.random) / 2);
        }

        return bl;
    }

    @Override
    protected void start(WorldServer serverLevel, EntityInsentient mob, long l) {
        this.chosenJump = Optional.empty();
        this.findJumpTries = 20;
        this.jumpCandidates.clear();
        this.initialPosition = Optional.of(mob.getPositionVector());
        BlockPosition blockPos = mob.getChunkCoordinates();
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        Iterable<BlockPosition> iterable = BlockPosition.betweenClosed(i - this.maxLongJumpWidth, j - this.maxLongJumpHeight, k - this.maxLongJumpWidth, i + this.maxLongJumpWidth, j + this.maxLongJumpHeight, k + this.maxLongJumpWidth);
        NavigationAbstract pathNavigation = mob.getNavigation();

        for(BlockPosition blockPos2 : iterable) {
            double d = blockPos2.distSqr(blockPos);
            if ((i != blockPos2.getX() || k != blockPos2.getZ()) && pathNavigation.isStableDestination(blockPos2) && mob.getPathfindingMalus(PathfinderNormal.getBlockPathTypeStatic(mob.level, blockPos2.mutable())) == 0.0F) {
                Optional<Vec3D> optional = this.calculateOptimalJumpVector(mob, Vec3D.atCenterOf(blockPos2));
                optional.ifPresent((vel) -> {
                    this.jumpCandidates.add(new LongJumpToRandomPos.PossibleJump(new BlockPosition(blockPos2), vel, MathHelper.ceil(d)));
                });
            }
        }

    }

    @Override
    protected void tick(WorldServer serverLevel, E mob, long l) {
        if (this.chosenJump.isPresent()) {
            if (l - this.prepareJumpStart >= 40L) {
                mob.setYRot(mob.yBodyRot);
                mob.setDiscardFriction(true);
                Vec3D vec3 = this.chosenJump.get().getJumpVector();
                double d = vec3.length();
                double e = d + mob.getJumpBoostPower();
                mob.setMot(vec3.scale(e / d));
                mob.getBehaviorController().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
                serverLevel.playSound((EntityHuman)null, mob, this.getJumpSound.apply(mob), EnumSoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        } else {
            --this.findJumpTries;
            Optional<LongJumpToRandomPos.PossibleJump> optional = WeightedRandom2.getRandomItem(serverLevel.random, this.jumpCandidates);
            if (optional.isPresent()) {
                this.jumpCandidates.remove(optional.get());
                mob.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(optional.get().getJumpTarget()));
                NavigationAbstract pathNavigation = mob.getNavigation();
                PathEntity path = pathNavigation.createPath(optional.get().getJumpTarget(), 0, 8);
                if (path == null || !path.canReach()) {
                    this.chosenJump = optional;
                    this.prepareJumpStart = l;
                }
            }
        }

    }

    private Optional<Vec3D> calculateOptimalJumpVector(EntityInsentient entity, Vec3D pos) {
        Optional<Vec3D> optional = Optional.empty();

        for(int i = 65; i < 85; i += 5) {
            Optional<Vec3D> optional2 = this.calculateJumpVectorForAngle(entity, pos, i);
            if (!optional.isPresent() || optional2.isPresent() && optional2.get().lengthSqr() < optional.get().lengthSqr()) {
                optional = optional2;
            }
        }

        return optional;
    }

    private Optional<Vec3D> calculateJumpVectorForAngle(EntityInsentient entity, Vec3D pos, int range) {
        Vec3D vec3 = entity.getPositionVector();
        Vec3D vec32 = (new Vec3D(pos.x - vec3.x, 0.0D, pos.z - vec3.z)).normalize().scale(0.5D);
        pos = pos.subtract(vec32);
        Vec3D vec33 = pos.subtract(vec3);
        float f = (float)range * (float)Math.PI / 180.0F;
        double d = Math.atan2(vec33.z, vec33.x);
        double e = vec33.subtract(0.0D, vec33.y, 0.0D).lengthSqr();
        double g = Math.sqrt(e);
        double h = vec33.y;
        double i = Math.sin((double)(2.0F * f));
        double j = 0.08D;
        double k = Math.pow(Math.cos((double)f), 2.0D);
        double l = Math.sin((double)f);
        double m = Math.cos((double)f);
        double n = Math.sin(d);
        double o = Math.cos(d);
        double p = e * 0.08D / (g * i - 2.0D * h * k);
        if (p < 0.0D) {
            return Optional.empty();
        } else {
            double q = Math.sqrt(p);
            if (q > (double)this.maxJumpVelocity) {
                return Optional.empty();
            } else {
                double r = q * m;
                double s = q * l;
                int t = MathHelper.ceil(g / r) * 2;
                double u = 0.0D;
                Vec3D vec34 = null;

                for(int v = 0; v < t - 1; ++v) {
                    u += g / (double)t;
                    double w = l / m * u - Math.pow(u, 2.0D) * 0.08D / (2.0D * p * Math.pow(m, 2.0D));
                    double x = u * o;
                    double y = u * n;
                    Vec3D vec35 = new Vec3D(vec3.x + x, vec3.y + w, vec3.z + y);
                    if (vec34 != null && !this.isClearTransition(entity, vec34, vec35)) {
                        return Optional.empty();
                    }

                    vec34 = vec35;
                }

                return Optional.of((new Vec3D(r * o, s, r * n)).scale((double)0.95F));
            }
        }
    }

    private boolean isClearTransition(EntityInsentient entity, Vec3D startPos, Vec3D endPos) {
        EntitySize entityDimensions = entity.getDimensions(EntityPose.LONG_JUMPING);
        Vec3D vec3 = endPos.subtract(startPos);
        double d = (double)Math.min(entityDimensions.width, entityDimensions.height);
        int i = MathHelper.ceil(vec3.length() / d);
        Vec3D vec32 = vec3.normalize();
        Vec3D vec33 = startPos;

        for(int j = 0; j < i; ++j) {
            vec33 = j == i - 1 ? endPos : vec33.add(vec32.scale(d * (double)0.9F));
            AxisAlignedBB aABB = entityDimensions.makeBoundingBox(vec33);
            if (!entity.level.getCubes(entity, aABB)) {
                return false;
            }
        }

        return true;
    }

    public static class PossibleJump extends WeightedEntry.IntrusiveBase {
        private final BlockPosition jumpTarget;
        private final Vec3D jumpVector;

        public PossibleJump(BlockPosition pos, Vec3D ramVelocity, int weight) {
            super(weight);
            this.jumpTarget = pos;
            this.jumpVector = ramVelocity;
        }

        public BlockPosition getJumpTarget() {
            return this.jumpTarget;
        }

        public Vec3D getJumpVector() {
            return this.jumpVector;
        }
    }
}
