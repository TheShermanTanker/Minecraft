package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Unit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResetChunksCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("resetchunks").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return resetChunks(context.getSource(), 0, true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("range", IntegerArgumentType.integer(0, 5)).executes((context) -> {
            return resetChunks(context.getSource(), IntegerArgumentType.getInteger(context, "range"), true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("skipOldChunks", BoolArgumentType.bool()).executes((commandContext) -> {
            return resetChunks(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "range"), BoolArgumentType.getBool(commandContext, "skipOldChunks"));
        }))));
    }

    private static int resetChunks(CommandListenerWrapper source, int radius, boolean skipOldChunks) {
        WorldServer serverLevel = source.getWorld();
        ChunkProviderServer serverChunkCache = serverLevel.getChunkSource();
        serverChunkCache.chunkMap.debugReloadGenerator();
        Vec3D vec3 = source.getPosition();
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(new BlockPosition(vec3));
        int i = chunkPos.z - radius;
        int j = chunkPos.z + radius;
        int k = chunkPos.x - radius;
        int l = chunkPos.x + radius;

        for(int m = i; m <= j; ++m) {
            for(int n = k; n <= l; ++n) {
                ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(n, m);
                Chunk levelChunk = serverChunkCache.getChunkAt(n, m, false);
                if (levelChunk != null && (!skipOldChunks || !levelChunk.isOldNoiseGeneration())) {
                    for(BlockPosition blockPos : BlockPosition.betweenClosed(chunkPos2.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPos2.getMinBlockZ(), chunkPos2.getMaxBlockX(), serverLevel.getMaxBuildHeight() - 1, chunkPos2.getMaxBlockZ())) {
                        serverLevel.setTypeAndData(blockPos, Blocks.AIR.getBlockData(), 16);
                    }
                }
            }
        }

        ThreadedMailbox<Runnable> processorMailbox = ThreadedMailbox.create(SystemUtils.backgroundExecutor(), "worldgen-resetchunks");
        long o = System.currentTimeMillis();
        int p = (radius * 2 + 1) * (radius * 2 + 1);

        for(ChunkStatus chunkStatus : ImmutableList.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS, ChunkStatus.LIQUID_CARVERS, ChunkStatus.FEATURES)) {
            long q = System.currentTimeMillis();
            CompletableFuture<Unit> completableFuture = CompletableFuture.supplyAsync(() -> {
                return Unit.INSTANCE;
            }, processorMailbox::tell);

            for(int r = chunkPos.z - radius; r <= chunkPos.z + radius; ++r) {
                for(int s = chunkPos.x - radius; s <= chunkPos.x + radius; ++s) {
                    ChunkCoordIntPair chunkPos3 = new ChunkCoordIntPair(s, r);
                    Chunk levelChunk2 = serverChunkCache.getChunkAt(s, r, false);
                    if (levelChunk2 != null && (!skipOldChunks || !levelChunk2.isOldNoiseGeneration())) {
                        List<IChunkAccess> list = Lists.newArrayList();
                        int t = Math.max(1, chunkStatus.getRange());

                        for(int u = chunkPos3.z - t; u <= chunkPos3.z + t; ++u) {
                            for(int v = chunkPos3.x - t; v <= chunkPos3.x + t; ++v) {
                                IChunkAccess chunkAccess = serverChunkCache.getChunkAt(v, u, chunkStatus.getParent(), true);
                                IChunkAccess chunkAccess2;
                                if (chunkAccess instanceof ProtoChunkExtension) {
                                    chunkAccess2 = new ProtoChunkExtension(((ProtoChunkExtension)chunkAccess).getWrapped(), true);
                                } else if (chunkAccess instanceof Chunk) {
                                    chunkAccess2 = new ProtoChunkExtension((Chunk)chunkAccess, true);
                                } else {
                                    chunkAccess2 = chunkAccess;
                                }

                                list.add(chunkAccess2);
                            }
                        }

                        completableFuture = completableFuture.thenComposeAsync((unit) -> {
                            return chunkStatus.generate(processorMailbox::tell, serverLevel, serverChunkCache.getChunkGenerator(), serverLevel.getStructureManager(), serverChunkCache.getLightEngine(), (chunk) -> {
                                throw new UnsupportedOperationException("Not creating full chunks here");
                            }, list, true).thenApply((either) -> {
                                if (chunkStatus == ChunkStatus.NOISE) {
                                    either.left().ifPresent((chunk) -> {
                                        HeightMap.primeHeightmaps(chunk, ChunkStatus.POST_FEATURES);
                                    });
                                }

                                return Unit.INSTANCE;
                            });
                        }, processorMailbox::tell);
                    }
                }
            }

            source.getServer().awaitTasks(completableFuture::isDone);
            LOGGER.debug(chunkStatus.getName() + " took " + (System.currentTimeMillis() - q) + " ms");
        }

        long w = System.currentTimeMillis();

        for(int x = chunkPos.z - radius; x <= chunkPos.z + radius; ++x) {
            for(int y = chunkPos.x - radius; y <= chunkPos.x + radius; ++y) {
                ChunkCoordIntPair chunkPos4 = new ChunkCoordIntPair(y, x);
                Chunk levelChunk3 = serverChunkCache.getChunkAt(y, x, false);
                if (levelChunk3 != null && (!skipOldChunks || !levelChunk3.isOldNoiseGeneration())) {
                    for(BlockPosition blockPos2 : BlockPosition.betweenClosed(chunkPos4.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPos4.getMinBlockZ(), chunkPos4.getMaxBlockX(), serverLevel.getMaxBuildHeight() - 1, chunkPos4.getMaxBlockZ())) {
                        serverChunkCache.flagDirty(blockPos2);
                    }
                }
            }
        }

        LOGGER.debug("blockChanged took " + (System.currentTimeMillis() - w) + " ms");
        long z = System.currentTimeMillis() - o;
        source.sendMessage(new ChatComponentText(String.format("%d chunks have been reset. This took %d ms for %d chunks, or %02f ms per chunk", p, z, p, (float)z / (float)p)), true);
        return 1;
    }
}
