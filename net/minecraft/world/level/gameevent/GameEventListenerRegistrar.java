package net.minecraft.world.level.gameevent;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;

public class GameEventListenerRegistrar {
    private final GameEventListener listener;
    @Nullable
    private SectionPosition sectionPos;

    public GameEventListenerRegistrar(GameEventListener listener) {
        this.listener = listener;
    }

    public void onListenerRemoved(World world) {
        this.ifEventDispatcherExists(world, this.sectionPos, (dispatcher) -> {
            dispatcher.unregister(this.listener);
        });
    }

    public void onListenerMove(World world) {
        Optional<BlockPosition> optional = this.listener.getListenerSource().getPosition(world);
        if (optional.isPresent()) {
            long l = SectionPosition.blockToSection(optional.get().asLong());
            if (this.sectionPos == null || this.sectionPos.asLong() != l) {
                SectionPosition sectionPos = this.sectionPos;
                this.sectionPos = SectionPosition.of(l);
                this.ifEventDispatcherExists(world, sectionPos, (dispatcher) -> {
                    dispatcher.unregister(this.listener);
                });
                this.ifEventDispatcherExists(world, this.sectionPos, (dispatcher) -> {
                    dispatcher.register(this.listener);
                });
            }
        }

    }

    private void ifEventDispatcherExists(World world, @Nullable SectionPosition sectionPos, Consumer<GameEventDispatcher> action) {
        if (sectionPos != null) {
            IChunkAccess chunkAccess = world.getChunkAt(sectionPos.x(), sectionPos.z(), ChunkStatus.FULL, false);
            if (chunkAccess != null) {
                action.accept(chunkAccess.getEventDispatcher(sectionPos.y()));
            }

        }
    }
}
