package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2I;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.BlockPosition2D;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;

public class CommandForceload {
    private static final int MAX_CHUNK_LIMIT = 256;
    private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new ChatMessage("commands.forceload.toobig", maxCount, count);
    });
    private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType((chunkPos, registryKey) -> {
        return new ChatMessage("commands.forceload.query.failure", chunkPos, registryKey);
    });
    private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(new ChatMessage("commands.forceload.added.failure"));
    private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(new ChatMessage("commands.forceload.removed.failure"));

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("forceload").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("from", ArgumentVec2I.columnPos()).executes((context) -> {
            return changeForceLoad(context.getSource(), ArgumentVec2I.getColumnPos(context, "from"), ArgumentVec2I.getColumnPos(context, "from"), true);
        }).then(net.minecraft.commands.CommandDispatcher.argument("to", ArgumentVec2I.columnPos()).executes((context) -> {
            return changeForceLoad(context.getSource(), ArgumentVec2I.getColumnPos(context, "from"), ArgumentVec2I.getColumnPos(context, "to"), true);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("from", ArgumentVec2I.columnPos()).executes((context) -> {
            return changeForceLoad(context.getSource(), ArgumentVec2I.getColumnPos(context, "from"), ArgumentVec2I.getColumnPos(context, "from"), false);
        }).then(net.minecraft.commands.CommandDispatcher.argument("to", ArgumentVec2I.columnPos()).executes((context) -> {
            return changeForceLoad(context.getSource(), ArgumentVec2I.getColumnPos(context, "from"), ArgumentVec2I.getColumnPos(context, "to"), false);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("all").executes((context) -> {
            return removeAll(context.getSource());
        }))).then(net.minecraft.commands.CommandDispatcher.literal("query").executes((context) -> {
            return listForceLoad(context.getSource());
        }).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec2I.columnPos()).executes((context) -> {
            return queryForceLoad(context.getSource(), ArgumentVec2I.getColumnPos(context, "pos"));
        }))));
    }

    private static int queryForceLoad(CommandListenerWrapper source, BlockPosition2D pos) throws CommandSyntaxException {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(SectionPosition.blockToSectionCoord(pos.x), SectionPosition.blockToSectionCoord(pos.z));
        WorldServer serverLevel = source.getWorld();
        ResourceKey<World> resourceKey = serverLevel.getDimensionKey();
        boolean bl = serverLevel.getForceLoadedChunks().contains(chunkPos.pair());
        if (bl) {
            source.sendMessage(new ChatMessage("commands.forceload.query.success", chunkPos, resourceKey.location()), false);
            return 1;
        } else {
            throw ERROR_NOT_TICKING.create(chunkPos, resourceKey.location());
        }
    }

    private static int listForceLoad(CommandListenerWrapper source) {
        WorldServer serverLevel = source.getWorld();
        ResourceKey<World> resourceKey = serverLevel.getDimensionKey();
        LongSet longSet = serverLevel.getForceLoadedChunks();
        int i = longSet.size();
        if (i > 0) {
            String string = Joiner.on(", ").join(longSet.stream().sorted().map(ChunkCoordIntPair::new).map(ChunkCoordIntPair::toString).iterator());
            if (i == 1) {
                source.sendMessage(new ChatMessage("commands.forceload.list.single", resourceKey.location(), string), false);
            } else {
                source.sendMessage(new ChatMessage("commands.forceload.list.multiple", i, resourceKey.location(), string), false);
            }
        } else {
            source.sendFailureMessage(new ChatMessage("commands.forceload.added.none", resourceKey.location()));
        }

        return i;
    }

    private static int removeAll(CommandListenerWrapper source) {
        WorldServer serverLevel = source.getWorld();
        ResourceKey<World> resourceKey = serverLevel.getDimensionKey();
        LongSet longSet = serverLevel.getForceLoadedChunks();
        longSet.forEach((l) -> {
            serverLevel.setForceLoaded(ChunkCoordIntPair.getX(l), ChunkCoordIntPair.getZ(l), false);
        });
        source.sendMessage(new ChatMessage("commands.forceload.removed.all", resourceKey.location()), true);
        return 0;
    }

    private static int changeForceLoad(CommandListenerWrapper source, BlockPosition2D from, BlockPosition2D to, boolean forceLoaded) throws CommandSyntaxException {
        int i = Math.min(from.x, to.x);
        int j = Math.min(from.z, to.z);
        int k = Math.max(from.x, to.x);
        int l = Math.max(from.z, to.z);
        if (i >= -30000000 && j >= -30000000 && k < 30000000 && l < 30000000) {
            int m = SectionPosition.blockToSectionCoord(i);
            int n = SectionPosition.blockToSectionCoord(j);
            int o = SectionPosition.blockToSectionCoord(k);
            int p = SectionPosition.blockToSectionCoord(l);
            long q = ((long)(o - m) + 1L) * ((long)(p - n) + 1L);
            if (q > 256L) {
                throw ERROR_TOO_MANY_CHUNKS.create(256, q);
            } else {
                WorldServer serverLevel = source.getWorld();
                ResourceKey<World> resourceKey = serverLevel.getDimensionKey();
                ChunkCoordIntPair chunkPos = null;
                int r = 0;

                for(int s = m; s <= o; ++s) {
                    for(int t = n; t <= p; ++t) {
                        boolean bl = serverLevel.setForceLoaded(s, t, forceLoaded);
                        if (bl) {
                            ++r;
                            if (chunkPos == null) {
                                chunkPos = new ChunkCoordIntPair(s, t);
                            }
                        }
                    }
                }

                if (r == 0) {
                    throw (forceLoaded ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
                } else {
                    if (r == 1) {
                        source.sendMessage(new ChatMessage("commands.forceload." + (forceLoaded ? "added" : "removed") + ".single", chunkPos, resourceKey.location()), true);
                    } else {
                        ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(m, n);
                        ChunkCoordIntPair chunkPos3 = new ChunkCoordIntPair(o, p);
                        source.sendMessage(new ChatMessage("commands.forceload." + (forceLoaded ? "added" : "removed") + ".multiple", r, resourceKey.location(), chunkPos2, chunkPos3), true);
                    }

                    return r;
                }
            }
        } else {
            throw ArgumentPosition.ERROR_OUT_OF_WORLD.create();
        }
    }
}
