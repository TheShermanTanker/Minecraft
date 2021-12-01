package net.minecraft.server.level;

import net.minecraft.world.level.chunk.Chunk;

record ServerChunkCache$ChunkAndHolder(Chunk chunk, PlayerChunk holder) {
    ServerChunkCache$ChunkAndHolder(Chunk levelChunk, PlayerChunk chunkHolder) {
        this.chunk = levelChunk;
        this.holder = chunkHolder;
    }

    public Chunk chunk() {
        return this.chunk;
    }

    public PlayerChunk holder() {
        return this.holder;
    }
}
