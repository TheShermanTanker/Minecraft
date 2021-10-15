package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.scores.ScoreboardTeamBase;

public class CommandSpreadPlayers {
    private static final int MAX_ITERATION_COUNT = 10000;
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType((pilesCount, x, z, maxSpreadDistance) -> {
        return new ChatMessage("commands.spreadplayers.failed.teams", pilesCount, x, z, maxSpreadDistance);
    });
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType((pilesCount, x, z, maxSpreadDistance) -> {
        return new ChatMessage("commands.spreadplayers.failed.entities", pilesCount, x, z, maxSpreadDistance);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("spreadplayers").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("center", ArgumentVec2.vec2()).then(net.minecraft.commands.CommandDispatcher.argument("spreadDistance", FloatArgumentType.floatArg(0.0F)).then(net.minecraft.commands.CommandDispatcher.argument("maxRange", FloatArgumentType.floatArg(1.0F)).then(net.minecraft.commands.CommandDispatcher.argument("respectTeams", BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).executes((context) -> {
            return spreadPlayers(context.getSource(), ArgumentVec2.getVec2(context, "center"), FloatArgumentType.getFloat(context, "spreadDistance"), FloatArgumentType.getFloat(context, "maxRange"), context.getSource().getWorld().getMaxBuildHeight(), BoolArgumentType.getBool(context, "respectTeams"), ArgumentEntity.getEntities(context, "targets"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("under").then(net.minecraft.commands.CommandDispatcher.argument("maxHeight", IntegerArgumentType.integer(0)).then(net.minecraft.commands.CommandDispatcher.argument("respectTeams", BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).executes((context) -> {
            return spreadPlayers(context.getSource(), ArgumentVec2.getVec2(context, "center"), FloatArgumentType.getFloat(context, "spreadDistance"), FloatArgumentType.getFloat(context, "maxRange"), IntegerArgumentType.getInteger(context, "maxHeight"), BoolArgumentType.getBool(context, "respectTeams"), ArgumentEntity.getEntities(context, "targets"));
        })))))))));
    }

    private static int spreadPlayers(CommandListenerWrapper source, Vec2F center, float spreadDistance, float maxRange, int maxY, boolean respectTeams, Collection<? extends Entity> players) throws CommandSyntaxException {
        Random random = new Random();
        double d = (double)(center.x - maxRange);
        double e = (double)(center.y - maxRange);
        double f = (double)(center.x + maxRange);
        double g = (double)(center.y + maxRange);
        CommandSpreadPlayers.Position[] positions = createInitialPositions(random, respectTeams ? getNumberOfTeams(players) : players.size(), d, e, f, g);
        spreadPositions(center, (double)spreadDistance, source.getWorld(), random, d, e, f, g, maxY, positions, respectTeams);
        double h = setPlayerPositions(players, source.getWorld(), positions, maxY, respectTeams);
        source.sendMessage(new ChatMessage("commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"), positions.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", h)), true);
        return positions.length;
    }

    private static int getNumberOfTeams(Collection<? extends Entity> entities) {
        Set<ScoreboardTeamBase> set = Sets.newHashSet();

        for(Entity entity : entities) {
            if (entity instanceof EntityHuman) {
                set.add(entity.getScoreboardTeam());
            } else {
                set.add((ScoreboardTeamBase)null);
            }
        }

        return set.size();
    }

    private static void spreadPositions(Vec2F center, double spreadDistance, WorldServer world, Random random, double minX, double minZ, double maxX, double maxZ, int maxY, CommandSpreadPlayers.Position[] piles, boolean respectTeams) throws CommandSyntaxException {
        boolean bl = true;
        double d = (double)Float.MAX_VALUE;

        int i;
        for(i = 0; i < 10000 && bl; ++i) {
            bl = false;
            d = (double)Float.MAX_VALUE;

            for(int j = 0; j < piles.length; ++j) {
                CommandSpreadPlayers.Position position = piles[j];
                int k = 0;
                CommandSpreadPlayers.Position position2 = new CommandSpreadPlayers.Position();

                for(int l = 0; l < piles.length; ++l) {
                    if (j != l) {
                        CommandSpreadPlayers.Position position3 = piles[l];
                        double e = position.dist(position3);
                        d = Math.min(e, d);
                        if (e < spreadDistance) {
                            ++k;
                            position2.x += position3.x - position.x;
                            position2.z += position3.z - position.z;
                        }
                    }
                }

                if (k > 0) {
                    position2.x /= (double)k;
                    position2.z /= (double)k;
                    double f = position2.getLength();
                    if (f > 0.0D) {
                        position2.normalize();
                        position.moveAway(position2);
                    } else {
                        position.randomize(random, minX, minZ, maxX, maxZ);
                    }

                    bl = true;
                }

                if (position.clamp(minX, minZ, maxX, maxZ)) {
                    bl = true;
                }
            }

            if (!bl) {
                for(CommandSpreadPlayers.Position position4 : piles) {
                    if (!position4.isSafe(world, maxY)) {
                        position4.randomize(random, minX, minZ, maxX, maxZ);
                        bl = true;
                    }
                }
            }
        }

        if (d == (double)Float.MAX_VALUE) {
            d = 0.0D;
        }

        if (i >= 10000) {
            if (respectTeams) {
                throw ERROR_FAILED_TO_SPREAD_TEAMS.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            } else {
                throw ERROR_FAILED_TO_SPREAD_ENTITIES.create(piles.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", d));
            }
        }
    }

    private static double setPlayerPositions(Collection<? extends Entity> entities, WorldServer world, CommandSpreadPlayers.Position[] piles, int maxY, boolean respectTeams) {
        double d = 0.0D;
        int i = 0;
        Map<ScoreboardTeamBase, CommandSpreadPlayers.Position> map = Maps.newHashMap();

        for(Entity entity : entities) {
            CommandSpreadPlayers.Position position;
            if (respectTeams) {
                ScoreboardTeamBase team = entity instanceof EntityHuman ? entity.getScoreboardTeam() : null;
                if (!map.containsKey(team)) {
                    map.put(team, piles[i++]);
                }

                position = map.get(team);
            } else {
                position = piles[i++];
            }

            entity.enderTeleportAndLoad((double)MathHelper.floor(position.x) + 0.5D, (double)position.getSpawnY(world, maxY), (double)MathHelper.floor(position.z) + 0.5D);
            double e = Double.MAX_VALUE;

            for(CommandSpreadPlayers.Position position3 : piles) {
                if (position != position3) {
                    double f = position.dist(position3);
                    e = Math.min(f, e);
                }
            }

            d += e;
        }

        return entities.size() < 2 ? 0.0D : d / (double)entities.size();
    }

    private static CommandSpreadPlayers.Position[] createInitialPositions(Random random, int count, double minX, double minZ, double maxX, double maxZ) {
        CommandSpreadPlayers.Position[] positions = new CommandSpreadPlayers.Position[count];

        for(int i = 0; i < positions.length; ++i) {
            CommandSpreadPlayers.Position position = new CommandSpreadPlayers.Position();
            position.randomize(random, minX, minZ, maxX, maxZ);
            positions[i] = position;
        }

        return positions;
    }

    static class Position {
        double x;
        double z;

        double dist(CommandSpreadPlayers.Position other) {
            double d = this.x - other.x;
            double e = this.z - other.z;
            return Math.sqrt(d * d + e * e);
        }

        void normalize() {
            double d = this.getLength();
            this.x /= d;
            this.z /= d;
        }

        double getLength() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        public void moveAway(CommandSpreadPlayers.Position other) {
            this.x -= other.x;
            this.z -= other.z;
        }

        public boolean clamp(double minX, double minZ, double maxX, double maxZ) {
            boolean bl = false;
            if (this.x < minX) {
                this.x = minX;
                bl = true;
            } else if (this.x > maxX) {
                this.x = maxX;
                bl = true;
            }

            if (this.z < minZ) {
                this.z = minZ;
                bl = true;
            } else if (this.z > maxZ) {
                this.z = maxZ;
                bl = true;
            }

            return bl;
        }

        public int getSpawnY(IBlockAccess blockView, int maxY) {
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(this.x, (double)(maxY + 1), this.z);
            boolean bl = blockView.getType(mutableBlockPos).isAir();
            mutableBlockPos.move(EnumDirection.DOWN);

            boolean bl3;
            for(boolean bl2 = blockView.getType(mutableBlockPos).isAir(); mutableBlockPos.getY() > blockView.getMinBuildHeight(); bl2 = bl3) {
                mutableBlockPos.move(EnumDirection.DOWN);
                bl3 = blockView.getType(mutableBlockPos).isAir();
                if (!bl3 && bl2 && bl) {
                    return mutableBlockPos.getY() + 1;
                }

                bl = bl2;
            }

            return maxY + 1;
        }

        public boolean isSafe(IBlockAccess world, int maxY) {
            BlockPosition blockPos = new BlockPosition(this.x, (double)(this.getSpawnY(world, maxY) - 1), this.z);
            IBlockData blockState = world.getType(blockPos);
            Material material = blockState.getMaterial();
            return blockPos.getY() < maxY && !material.isLiquid() && material != Material.FIRE;
        }

        public void randomize(Random random, double minX, double minZ, double maxX, double maxZ) {
            this.x = MathHelper.nextDouble(random, minX, maxX);
            this.z = MathHelper.nextDouble(random, minZ, maxZ);
        }
    }
}
