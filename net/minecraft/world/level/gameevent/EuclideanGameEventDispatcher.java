package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class EuclideanGameEventDispatcher implements GameEventDispatcher {
    private final List<GameEventListener> listeners = Lists.newArrayList();
    private final World level;

    public EuclideanGameEventDispatcher(World world) {
        this.level = world;
    }

    @Override
    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    @Override
    public void register(GameEventListener listener) {
        this.listeners.add(listener);
        PacketDebug.sendGameEventListenerInfo(this.level, listener);
    }

    @Override
    public void unregister(GameEventListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void post(GameEvent event, @Nullable Entity entity, BlockPosition pos) {
        boolean bl = false;

        for(GameEventListener gameEventListener : this.listeners) {
            if (this.postToListener(this.level, event, entity, pos, gameEventListener)) {
                bl = true;
            }
        }

        if (bl) {
            PacketDebug.sendGameEventInfo(this.level, event, pos);
        }

    }

    private boolean postToListener(World world, GameEvent event, @Nullable Entity entity, BlockPosition pos, GameEventListener listener) {
        Optional<BlockPosition> optional = listener.getListenerSource().getPosition(world);
        if (!optional.isPresent()) {
            return false;
        } else {
            double d = optional.get().distSqr(pos, false);
            int i = listener.getListenerRadius() * listener.getListenerRadius();
            return d <= (double)i && listener.handleGameEvent(world, event, entity, pos);
        }
    }
}
