package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.commands.arguments.ArgumentCriterionValue;
import net.minecraft.commands.arguments.ArgumentDimension;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.commands.arguments.ArgumentScoreboardObjective;
import net.minecraft.commands.arguments.ArgumentScoreholder;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.coordinates.ArgumentRotation;
import net.minecraft.commands.arguments.coordinates.ArgumentRotationAxis;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.bossevents.BossBattleCustom;
import net.minecraft.server.commands.data.CommandData;
import net.minecraft.server.commands.data.CommandDataAccessor;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;

public class CommandExecute {
    private static final int MAX_TEST_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new ChatMessage("commands.execute.blocks.toobig", maxCount, count);
    });
    private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.execute.conditional.fail"));
    private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType((count) -> {
        return new ChatMessage("commands.execute.conditional.fail_count", count);
    });
    private static final BinaryOperator<ResultConsumer<CommandListenerWrapper>> CALLBACK_CHAINER = (resultConsumer, resultConsumer2) -> {
        return (context, success, result) -> {
            resultConsumer.onCommandComplete(context, success, result);
            resultConsumer2.onCommandComplete(context, success, result);
        };
    };
    private static final SuggestionProvider<CommandListenerWrapper> SUGGEST_PREDICATE = (context, builder) -> {
        LootPredicateManager predicateManager = context.getSource().getServer().getLootPredicateManager();
        return ICompletionProvider.suggestResource(predicateManager.getKeys(), builder);
    };

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        LiteralCommandNode<CommandListenerWrapper> literalCommandNode = dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("execute").requires((source) -> {
            return source.hasPermission(2);
        }));
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("execute").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.literal("run").redirect(dispatcher.getRoot())).then(addConditionals(literalCommandNode, net.minecraft.commands.CommandDispatcher.literal("if"), true)).then(addConditionals(literalCommandNode, net.minecraft.commands.CommandDispatcher.literal("unless"), false)).then(net.minecraft.commands.CommandDispatcher.literal("as").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).fork(literalCommandNode, (context) -> {
            List<CommandListenerWrapper> list = Lists.newArrayList();

            for(Entity entity : ArgumentEntity.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withEntity(entity));
            }

            return list;
        }))).then(net.minecraft.commands.CommandDispatcher.literal("at").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).fork(literalCommandNode, (context) -> {
            List<CommandListenerWrapper> list = Lists.newArrayList();

            for(Entity entity : ArgumentEntity.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withLevel((WorldServer)entity.level).withPosition(entity.getPositionVector()).withRotation(entity.getRotationVector()));
            }

            return list;
        }))).then(net.minecraft.commands.CommandDispatcher.literal("store").then(wrapStores(literalCommandNode, net.minecraft.commands.CommandDispatcher.literal("result"), true)).then(wrapStores(literalCommandNode, net.minecraft.commands.CommandDispatcher.literal("success"), false))).then(net.minecraft.commands.CommandDispatcher.literal("positioned").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withPosition(ArgumentVec3.getVec3(context, "pos")).withAnchor(ArgumentAnchor.Anchor.FEET);
        })).then(net.minecraft.commands.CommandDispatcher.literal("as").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).fork(literalCommandNode, (context) -> {
            List<CommandListenerWrapper> list = Lists.newArrayList();

            for(Entity entity : ArgumentEntity.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withPosition(entity.getPositionVector()));
            }

            return list;
        })))).then(net.minecraft.commands.CommandDispatcher.literal("rotated").then(net.minecraft.commands.CommandDispatcher.argument("rot", ArgumentRotation.rotation()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withRotation(ArgumentRotation.getRotation(context, "rot").getRotation(context.getSource()));
        })).then(net.minecraft.commands.CommandDispatcher.literal("as").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).fork(literalCommandNode, (context) -> {
            List<CommandListenerWrapper> list = Lists.newArrayList();

            for(Entity entity : ArgumentEntity.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withRotation(entity.getRotationVector()));
            }

            return list;
        })))).then(net.minecraft.commands.CommandDispatcher.literal("facing").then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.multipleEntities()).then(net.minecraft.commands.CommandDispatcher.argument("anchor", ArgumentAnchor.anchor()).fork(literalCommandNode, (context) -> {
            List<CommandListenerWrapper> list = Lists.newArrayList();
            ArgumentAnchor.Anchor anchor = ArgumentAnchor.getAnchor(context, "anchor");

            for(Entity entity : ArgumentEntity.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().facing(entity, anchor));
            }

            return list;
        })))).then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).redirect(literalCommandNode, (context) -> {
            return context.getSource().facing(ArgumentVec3.getVec3(context, "pos"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("align").then(net.minecraft.commands.CommandDispatcher.argument("axes", ArgumentRotationAxis.swizzle()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withPosition(context.getSource().getPosition().align(ArgumentRotationAxis.getSwizzle(context, "axes")));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("anchored").then(net.minecraft.commands.CommandDispatcher.argument("anchor", ArgumentAnchor.anchor()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withAnchor(ArgumentAnchor.getAnchor(context, "anchor"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("in").then(net.minecraft.commands.CommandDispatcher.argument("dimension", ArgumentDimension.dimension()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withLevel(ArgumentDimension.getDimension(context, "dimension"));
        }))));
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> wrapStores(LiteralCommandNode<CommandListenerWrapper> node, LiteralArgumentBuilder<CommandListenerWrapper> builder, boolean requestResult) {
        builder.then(net.minecraft.commands.CommandDispatcher.literal("score").then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentScoreholder.scoreHolders()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("objective", ArgumentScoreboardObjective.objective()).redirect(node, (context) -> {
            return storeValue(context.getSource(), ArgumentScoreholder.getNamesWithDefaultWildcard(context, "targets"), ArgumentScoreboardObjective.getObjective(context, "objective"), requestResult);
        }))));
        builder.then(net.minecraft.commands.CommandDispatcher.literal("bossbar").then(net.minecraft.commands.CommandDispatcher.argument("id", ArgumentMinecraftKeyRegistered.id()).suggests(CommandBossBar.SUGGEST_BOSS_BAR).then(net.minecraft.commands.CommandDispatcher.literal("value").redirect(node, (context) -> {
            return storeValue(context.getSource(), CommandBossBar.getBossBar(context), true, requestResult);
        })).then(net.minecraft.commands.CommandDispatcher.literal("max").redirect(node, (context) -> {
            return storeValue(context.getSource(), CommandBossBar.getBossBar(context), false, requestResult);
        }))));

        for(CommandData.DataProvider dataProvider : CommandData.TARGET_PROVIDERS) {
            dataProvider.wrap(builder, (builderx) -> {
                return builderx.then(net.minecraft.commands.CommandDispatcher.argument("path", ArgumentNBTKey.nbtPath()).then(net.minecraft.commands.CommandDispatcher.literal("int").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagInt.valueOf((int)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("float").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagFloat.valueOf((float)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("short").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagShort.valueOf((short)((int)((double)result * DoubleArgumentType.getDouble(context, "scale"))));
                    }, requestResult);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("long").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagLong.valueOf((long)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("double").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagDouble.valueOf((double)result * DoubleArgumentType.getDouble(context, "scale"));
                    }, requestResult);
                }))).then(net.minecraft.commands.CommandDispatcher.literal("byte").then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"), (result) -> {
                        return NBTTagByte.valueOf((byte)((int)((double)result * DoubleArgumentType.getDouble(context, "scale"))));
                    }, requestResult);
                }))));
            });
        }

        return builder;
    }

    private static CommandListenerWrapper storeValue(CommandListenerWrapper source, Collection<String> targets, ScoreboardObjective objective, boolean requestResult) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        return source.withCallback((context, success, result) -> {
            for(String string : targets) {
                ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
                int i = requestResult ? result : (success ? 1 : 0);
                score.setScore(i);
            }

        }, CALLBACK_CHAINER);
    }

    private static CommandListenerWrapper storeValue(CommandListenerWrapper source, BossBattleCustom bossBar, boolean storeInValue, boolean requestResult) {
        return source.withCallback((context, success, result) -> {
            int i = requestResult ? result : (success ? 1 : 0);
            if (storeInValue) {
                bossBar.setValue(i);
            } else {
                bossBar.setMax(i);
            }

        }, CALLBACK_CHAINER);
    }

    private static CommandListenerWrapper storeData(CommandListenerWrapper source, CommandDataAccessor object, ArgumentNBTKey.NbtPath path, IntFunction<NBTBase> nbtSetter, boolean requestResult) {
        return source.withCallback((context, success, result) -> {
            try {
                NBTTagCompound compoundTag = object.getData();
                int i = requestResult ? result : (success ? 1 : 0);
                path.set(compoundTag, () -> {
                    return nbtSetter.apply(i);
                });
                object.setData(compoundTag);
            } catch (CommandSyntaxException var9) {
            }

        }, CALLBACK_CHAINER);
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> addConditionals(CommandNode<CommandListenerWrapper> root, LiteralArgumentBuilder<CommandListenerWrapper> argumentBuilder, boolean positive) {
        argumentBuilder.then(net.minecraft.commands.CommandDispatcher.literal("block").then(net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentPosition.blockPos()).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("block", ArgumentBlockPredicate.blockPredicate()), positive, (context) -> {
            return ArgumentBlockPredicate.getBlockPredicate(context, "block").test(new ShapeDetectorBlock(context.getSource().getWorld(), ArgumentPosition.getLoadedBlockPos(context, "pos"), true));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("score").then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(net.minecraft.commands.CommandDispatcher.argument("targetObjective", ArgumentScoreboardObjective.objective()).then(net.minecraft.commands.CommandDispatcher.literal("=").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()), positive, (context) -> {
            return checkScore(context, Integer::equals);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("<").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a < b;
            });
        })))).then(net.minecraft.commands.CommandDispatcher.literal("<=").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a <= b;
            });
        })))).then(net.minecraft.commands.CommandDispatcher.literal(">").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a > b;
            });
        })))).then(net.minecraft.commands.CommandDispatcher.literal(">=").then(net.minecraft.commands.CommandDispatcher.argument("source", ArgumentScoreholder.scoreHolder()).suggests(ArgumentScoreholder.SUGGEST_SCORE_HOLDERS).then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("sourceObjective", ArgumentScoreboardObjective.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a >= b;
            });
        })))).then(net.minecraft.commands.CommandDispatcher.literal("matches").then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("range", ArgumentCriterionValue.intRange()), positive, (context) -> {
            return checkScore(context, ArgumentCriterionValue.Ints.getRange(context, "range"));
        })))))).then(net.minecraft.commands.CommandDispatcher.literal("blocks").then(net.minecraft.commands.CommandDispatcher.argument("start", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("end", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentPosition.blockPos()).then(addIfBlocksConditional(root, net.minecraft.commands.CommandDispatcher.literal("all"), positive, false)).then(addIfBlocksConditional(root, net.minecraft.commands.CommandDispatcher.literal("masked"), positive, true)))))).then(net.minecraft.commands.CommandDispatcher.literal("entity").then(net.minecraft.commands.CommandDispatcher.argument("entities", ArgumentEntity.multipleEntities()).fork(root, (context) -> {
            return expect(context, positive, !ArgumentEntity.getOptionalEntities(context, "entities").isEmpty());
        }).executes(createNumericConditionalHandler(positive, (context) -> {
            return ArgumentEntity.getOptionalEntities(context, "entities").size();
        })))).then(net.minecraft.commands.CommandDispatcher.literal("predicate").then(addConditional(root, net.minecraft.commands.CommandDispatcher.argument("predicate", ArgumentMinecraftKeyRegistered.id()).suggests(SUGGEST_PREDICATE), positive, (context) -> {
            return checkCustomPredicate(context.getSource(), ArgumentMinecraftKeyRegistered.getPredicate(context, "predicate"));
        })));

        for(CommandData.DataProvider dataProvider : CommandData.SOURCE_PROVIDERS) {
            argumentBuilder.then(dataProvider.wrap(net.minecraft.commands.CommandDispatcher.literal("data"), (builder) -> {
                return builder.then(net.minecraft.commands.CommandDispatcher.argument("path", ArgumentNBTKey.nbtPath()).fork(root, (commandContext) -> {
                    return expect(commandContext, positive, checkMatchingData(dataProvider.access(commandContext), ArgumentNBTKey.getPath(commandContext, "path")) > 0);
                }).executes(createNumericConditionalHandler(positive, (context) -> {
                    return checkMatchingData(dataProvider.access(context), ArgumentNBTKey.getPath(context, "path"));
                })));
            }));
        }

        return argumentBuilder;
    }

    private static Command<CommandListenerWrapper> createNumericConditionalHandler(boolean positive, CommandExecute.CommandNumericPredicate condition) {
        return positive ? (context) -> {
            int i = condition.test(context);
            if (i > 0) {
                context.getSource().sendMessage(new ChatMessage("commands.execute.conditional.pass_count", i), false);
                return i;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        } : (context) -> {
            int i = condition.test(context);
            if (i == 0) {
                context.getSource().sendMessage(new ChatMessage("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
            }
        };
    }

    private static int checkMatchingData(CommandDataAccessor object, ArgumentNBTKey.NbtPath path) throws CommandSyntaxException {
        return path.countMatching(object.getData());
    }

    private static boolean checkScore(CommandContext<CommandListenerWrapper> context, BiPredicate<Integer, Integer> condition) throws CommandSyntaxException {
        String string = ArgumentScoreholder.getName(context, "target");
        ScoreboardObjective objective = ArgumentScoreboardObjective.getObjective(context, "targetObjective");
        String string2 = ArgumentScoreholder.getName(context, "source");
        ScoreboardObjective objective2 = ArgumentScoreboardObjective.getObjective(context, "sourceObjective");
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        if (scoreboard.hasPlayerScore(string, objective) && scoreboard.hasPlayerScore(string2, objective2)) {
            ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
            ScoreboardScore score2 = scoreboard.getPlayerScoreForObjective(string2, objective2);
            return condition.test(score.getScore(), score2.getScore());
        } else {
            return false;
        }
    }

    private static boolean checkScore(CommandContext<CommandListenerWrapper> context, CriterionConditionValue.IntegerRange range) throws CommandSyntaxException {
        String string = ArgumentScoreholder.getName(context, "target");
        ScoreboardObjective objective = ArgumentScoreboardObjective.getObjective(context, "targetObjective");
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        return !scoreboard.hasPlayerScore(string, objective) ? false : range.matches(scoreboard.getPlayerScoreForObjective(string, objective).getScore());
    }

    private static boolean checkCustomPredicate(CommandListenerWrapper source, LootItemCondition condition) {
        WorldServer serverLevel = source.getWorld();
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(serverLevel)).set(LootContextParameters.ORIGIN, source.getPosition()).setOptional(LootContextParameters.THIS_ENTITY, source.getEntity());
        return condition.test(builder.build(LootContextParameterSets.COMMAND));
    }

    private static Collection<CommandListenerWrapper> expect(CommandContext<CommandListenerWrapper> context, boolean positive, boolean value) {
        return (Collection<CommandListenerWrapper>)(value == positive ? Collections.singleton(context.getSource()) : Collections.emptyList());
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> addConditional(CommandNode<CommandListenerWrapper> root, ArgumentBuilder<CommandListenerWrapper, ?> builder, boolean positive, CommandExecute.CommandPredicate condition) {
        return builder.fork(root, (context) -> {
            return expect(context, positive, condition.test(context));
        }).executes((context) -> {
            if (positive == condition.test(context)) {
                context.getSource().sendMessage(new ChatMessage("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        });
    }

    private static ArgumentBuilder<CommandListenerWrapper, ?> addIfBlocksConditional(CommandNode<CommandListenerWrapper> root, ArgumentBuilder<CommandListenerWrapper, ?> builder, boolean positive, boolean masked) {
        return builder.fork(root, (context) -> {
            return expect(context, positive, checkRegions(context, masked).isPresent());
        }).executes(positive ? (context) -> {
            return checkIfRegions(context, masked);
        } : (context) -> {
            return checkUnlessRegions(context, masked);
        });
    }

    private static int checkIfRegions(CommandContext<CommandListenerWrapper> context, boolean masked) throws CommandSyntaxException {
        OptionalInt optionalInt = checkRegions(context, masked);
        if (optionalInt.isPresent()) {
            context.getSource().sendMessage(new ChatMessage("commands.execute.conditional.pass_count", optionalInt.getAsInt()), false);
            return optionalInt.getAsInt();
        } else {
            throw ERROR_CONDITIONAL_FAILED.create();
        }
    }

    private static int checkUnlessRegions(CommandContext<CommandListenerWrapper> context, boolean masked) throws CommandSyntaxException {
        OptionalInt optionalInt = checkRegions(context, masked);
        if (optionalInt.isPresent()) {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalInt.getAsInt());
        } else {
            context.getSource().sendMessage(new ChatMessage("commands.execute.conditional.pass"), false);
            return 1;
        }
    }

    private static OptionalInt checkRegions(CommandContext<CommandListenerWrapper> context, boolean masked) throws CommandSyntaxException {
        return checkRegions(context.getSource().getWorld(), ArgumentPosition.getLoadedBlockPos(context, "start"), ArgumentPosition.getLoadedBlockPos(context, "end"), ArgumentPosition.getLoadedBlockPos(context, "destination"), masked);
    }

    private static OptionalInt checkRegions(WorldServer world, BlockPosition start, BlockPosition end, BlockPosition destination, boolean masked) throws CommandSyntaxException {
        StructureBoundingBox boundingBox = StructureBoundingBox.fromCorners(start, end);
        StructureBoundingBox boundingBox2 = StructureBoundingBox.fromCorners(destination, destination.offset(boundingBox.getLength()));
        BlockPosition blockPos = new BlockPosition(boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ());
        int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
        if (i > 32768) {
            throw ERROR_AREA_TOO_LARGE.create(32768, i);
        } else {
            int j = 0;

            for(int k = boundingBox.minZ(); k <= boundingBox.maxZ(); ++k) {
                for(int l = boundingBox.minY(); l <= boundingBox.maxY(); ++l) {
                    for(int m = boundingBox.minX(); m <= boundingBox.maxX(); ++m) {
                        BlockPosition blockPos2 = new BlockPosition(m, l, k);
                        BlockPosition blockPos3 = blockPos2.offset(blockPos);
                        IBlockData blockState = world.getType(blockPos2);
                        if (!masked || !blockState.is(Blocks.AIR)) {
                            if (blockState != world.getType(blockPos3)) {
                                return OptionalInt.empty();
                            }

                            TileEntity blockEntity = world.getTileEntity(blockPos2);
                            TileEntity blockEntity2 = world.getTileEntity(blockPos3);
                            if (blockEntity != null) {
                                if (blockEntity2 == null) {
                                    return OptionalInt.empty();
                                }

                                if (blockEntity2.getTileType() != blockEntity.getTileType()) {
                                    return OptionalInt.empty();
                                }

                                NBTTagCompound compoundTag = blockEntity.saveWithoutMetadata();
                                NBTTagCompound compoundTag2 = blockEntity2.saveWithoutMetadata();
                                if (!compoundTag.equals(compoundTag2)) {
                                    return OptionalInt.empty();
                                }
                            }

                            ++j;
                        }
                    }
                }
            }

            return OptionalInt.of(j);
        }
    }

    @FunctionalInterface
    interface CommandNumericPredicate {
        int test(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface CommandPredicate {
        boolean test(CommandContext<CommandListenerWrapper> context) throws CommandSyntaxException;
    }
}
