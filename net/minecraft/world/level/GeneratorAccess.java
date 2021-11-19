package net.minecraft.world.level;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.storage.WorldData;

public interface GeneratorAccess extends ICombinedAccess, IWorldTime {
    @Override
    default long dayTime() {
        return this.getWorldData().getDayTime();
    }

    TickList<Block> getBlockTickList();

    TickList<FluidType> getFluidTickList();

    WorldData getWorldData();

    DifficultyDamageScaler getDamageScaler(BlockPosition pos);

    @Nullable
    MinecraftServer getMinecraftServer();

    default EnumDifficulty getDifficulty() {
        return this.getWorldData().getDifficulty();
    }

    IChunkProvider getChunkProvider();

    @Override
    default boolean isChunkLoaded(int chunkX, int chunkZ) {
        return this.getChunkProvider().isLoaded(chunkX, chunkZ);
    }

    Random getRandom();

    default void update(BlockPosition pos, Block block) {
    }

    void playSound(@Nullable EntityHuman player, BlockPosition pos, SoundEffect sound, EnumSoundCategory category, float volume, float pitch);

    void addParticle(ParticleParam parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

    void triggerEffect(@Nullable EntityHuman player, int eventId, BlockPosition pos, int data);

    default int getLogicalHeight() {
        return this.getDimensionManager().getLogicalHeight();
    }

    default void triggerEffect(int eventId, BlockPosition pos, int data) {
        this.triggerEffect((EntityHuman)null, eventId, pos, data);
    }

    void gameEvent(@Nullable Entity entity, GameEvent event, BlockPosition pos);

    default void gameEvent(GameEvent event, BlockPosition pos) {
        this.gameEvent((Entity)null, event, pos);
    }

    default void gameEvent(GameEvent event, Entity emitter) {
        this.gameEvent((Entity)null, event, emitter.getChunkCoordinates());
    }

    default void gameEvent(@Nullable Entity entity, GameEvent event, Entity emitter) {
        this.gameEvent(entity, event, emitter.getChunkCoordinates());
    }
}
