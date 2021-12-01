package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;

public class BehaviorOutside extends Behavior<EntityLiving> {
    private final float speedModifier;

    public BehaviorOutside(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        Optional<Vec3D> optional = Optional.ofNullable(this.getOutdoorPosition(world, entity));
        if (optional.isPresent()) {
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, optional.map((pos) -> {
                return new MemoryTarget(pos, this.speedModifier, 0);
            }));
        }

    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return !world.canSeeSky(entity.getChunkCoordinates());
    }

    @Nullable
    private Vec3D getOutdoorPosition(WorldServer world, EntityLiving entity) {
        Random random = entity.getRandom();
        BlockPosition blockPos = entity.getChunkCoordinates();

        for(int i = 0; i < 10; ++i) {
            BlockPosition blockPos2 = blockPos.offset(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
            if (hasNoBlocksAbove(world, entity, blockPos2)) {
                return Vec3D.atBottomCenterOf(blockPos2);
            }
        }

        return null;
    }

    public static boolean hasNoBlocksAbove(WorldServer world, EntityLiving entity, BlockPosition pos) {
        return world.canSeeSky(pos) && (double)world.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, pos).getY() <= entity.locY();
    }
}
