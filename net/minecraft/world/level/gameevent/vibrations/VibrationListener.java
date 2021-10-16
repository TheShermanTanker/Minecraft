package net.minecraft.world.level.gameevent.vibrations;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsGameEvent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public class VibrationListener implements GameEventListener {
    protected final PositionSource listenerSource;
    protected final int listenerRange;
    protected final VibrationListener.VibrationListenerConfig config;
    protected Optional<GameEvent> receivingEvent = Optional.empty();
    protected int receivingDistance;
    protected int travelTimeInTicks = 0;

    public VibrationListener(PositionSource positionSource, int range, VibrationListener.VibrationListenerConfig listener) {
        this.listenerSource = positionSource;
        this.listenerRange = range;
        this.config = listener;
    }

    public void tick(World world) {
        if (this.receivingEvent.isPresent()) {
            --this.travelTimeInTicks;
            if (this.travelTimeInTicks <= 0) {
                this.travelTimeInTicks = 0;
                this.config.onSignalReceive(world, this, this.receivingEvent.get(), this.receivingDistance);
                this.receivingEvent = Optional.empty();
            }
        }

    }

    @Override
    public PositionSource getListenerSource() {
        return this.listenerSource;
    }

    @Override
    public int getListenerRadius() {
        return this.listenerRange;
    }

    @Override
    public boolean handleGameEvent(World world, GameEvent event, @Nullable Entity entity, BlockPosition pos) {
        if (!this.isValidVibration(event, entity)) {
            return false;
        } else {
            Optional<BlockPosition> optional = this.listenerSource.getPosition(world);
            if (!optional.isPresent()) {
                return false;
            } else {
                BlockPosition blockPos = optional.get();
                if (!this.config.shouldListen(world, this, pos, event, entity)) {
                    return false;
                } else if (this.isOccluded(world, pos, blockPos)) {
                    return false;
                } else {
                    this.sendSignal(world, event, pos, blockPos);
                    return true;
                }
            }
        }
    }

    private boolean isValidVibration(GameEvent event, @Nullable Entity entity) {
        if (this.receivingEvent.isPresent()) {
            return false;
        } else if (!TagsGameEvent.VIBRATIONS.isTagged(event)) {
            return false;
        } else {
            if (entity != null) {
                if (TagsGameEvent.IGNORE_VIBRATIONS_SNEAKING.isTagged(event) && entity.isSteppingCarefully()) {
                    return false;
                }

                if (entity.occludesVibrations()) {
                    return false;
                }
            }

            return entity == null || !entity.isSpectator();
        }
    }

    private void sendSignal(World world, GameEvent event, BlockPosition pos, BlockPosition sourcePos) {
        this.receivingEvent = Optional.of(event);
        if (world instanceof WorldServer) {
            this.receivingDistance = MathHelper.floor(Math.sqrt(pos.distSqr(sourcePos, false)));
            this.travelTimeInTicks = this.receivingDistance;
            ((WorldServer)world).sendVibrationParticle(new VibrationPath(pos, this.listenerSource, this.travelTimeInTicks));
        }

    }

    private boolean isOccluded(World world, BlockPosition pos, BlockPosition sourcePos) {
        return world.isBlockInLine(new ClipBlockStateContext(Vec3D.atCenterOf(pos), Vec3D.atCenterOf(sourcePos), (state) -> {
            return state.is(TagsBlock.OCCLUDES_VIBRATION_SIGNALS);
        })).getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK;
    }

    public interface VibrationListenerConfig {
        boolean shouldListen(World world, GameEventListener listener, BlockPosition pos, GameEvent event, @Nullable Entity entity);

        void onSignalReceive(World world, GameEventListener listener, GameEvent event, int distance);
    }
}
