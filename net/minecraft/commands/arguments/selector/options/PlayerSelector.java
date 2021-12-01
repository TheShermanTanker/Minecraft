package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.critereon.CriterionConditionRange;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.AdvancementDataWorld;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.ScoreboardTeamBase;

public class PlayerSelector {
    private static final Map<String, PlayerSelector.Option> OPTIONS = Maps.newHashMap();
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType((option) -> {
        return new ChatMessage("argument.entity.options.unknown", option);
    });
    public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType((option) -> {
        return new ChatMessage("argument.entity.options.inapplicable", option);
    });
    public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.distance.negative"));
    public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.level.negative"));
    public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.limit.toosmall"));
    public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType((sortType) -> {
        return new ChatMessage("argument.entity.options.sort.irreversible", sortType);
    });
    public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType((gameMode) -> {
        return new ChatMessage("argument.entity.options.mode.invalid", gameMode);
    });
    public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType((entity) -> {
        return new ChatMessage("argument.entity.options.type.invalid", entity);
    });

    private static void register(String id, PlayerSelector.Modifier handler, Predicate<ArgumentParserSelector> condition, IChatBaseComponent description) {
        OPTIONS.put(id, new PlayerSelector.Option(handler, condition, description));
    }

    public static void bootStrap() {
        if (OPTIONS.isEmpty()) {
            register("name", (reader) -> {
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readString();
                if (reader.hasNameNotEquals() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "name");
                } else {
                    if (bl) {
                        reader.setHasNameNotEquals(true);
                    } else {
                        reader.setHasNameEquals(true);
                    }

                    reader.addPredicate((readerx) -> {
                        return readerx.getDisplayName().getString().equals(string) != bl;
                    });
                }
            }, (reader) -> {
                return !reader.hasNameEquals();
            }, new ChatMessage("argument.entity.options.name.description"));
            register("distance", (reader) -> {
                int i = reader.getReader().getCursor();
                CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromReader(reader.getReader());
                if ((doubles.getMin() == null || !(doubles.getMin() < 0.0D)) && (doubles.getMax() == null || !(doubles.getMax() < 0.0D))) {
                    reader.setDistance(doubles);
                    reader.setWorldLimited();
                } else {
                    reader.getReader().setCursor(i);
                    throw ERROR_RANGE_NEGATIVE.createWithContext(reader.getReader());
                }
            }, (reader) -> {
                return reader.getDistance().isAny();
            }, new ChatMessage("argument.entity.options.distance.description"));
            register("level", (reader) -> {
                int i = reader.getReader().getCursor();
                CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromReader(reader.getReader());
                if ((ints.getMin() == null || ints.getMin() >= 0) && (ints.getMax() == null || ints.getMax() >= 0)) {
                    reader.setLevel(ints);
                    reader.setIncludesEntities(false);
                } else {
                    reader.getReader().setCursor(i);
                    throw ERROR_LEVEL_NEGATIVE.createWithContext(reader.getReader());
                }
            }, (reader) -> {
                return reader.getLevel().isAny();
            }, new ChatMessage("argument.entity.options.level.description"));
            register("x", (reader) -> {
                reader.setWorldLimited();
                reader.setX(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getX() == null;
            }, new ChatMessage("argument.entity.options.x.description"));
            register("y", (reader) -> {
                reader.setWorldLimited();
                reader.setY(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getY() == null;
            }, new ChatMessage("argument.entity.options.y.description"));
            register("z", (reader) -> {
                reader.setWorldLimited();
                reader.setZ(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getZ() == null;
            }, new ChatMessage("argument.entity.options.z.description"));
            register("dx", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaX(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaX() == null;
            }, new ChatMessage("argument.entity.options.dx.description"));
            register("dy", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaY(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaY() == null;
            }, new ChatMessage("argument.entity.options.dy.description"));
            register("dz", (reader) -> {
                reader.setWorldLimited();
                reader.setDeltaZ(reader.getReader().readDouble());
            }, (reader) -> {
                return reader.getDeltaZ() == null;
            }, new ChatMessage("argument.entity.options.dz.description"));
            register("x_rotation", (reader) -> {
                reader.setRotX(CriterionConditionRange.fromReader(reader.getReader(), true, MathHelper::wrapDegrees));
            }, (reader) -> {
                return reader.getRotX() == CriterionConditionRange.ANY;
            }, new ChatMessage("argument.entity.options.x_rotation.description"));
            register("y_rotation", (reader) -> {
                reader.setRotY(CriterionConditionRange.fromReader(reader.getReader(), true, MathHelper::wrapDegrees));
            }, (reader) -> {
                return reader.getRotY() == CriterionConditionRange.ANY;
            }, new ChatMessage("argument.entity.options.y_rotation.description"));
            register("limit", (reader) -> {
                int i = reader.getReader().getCursor();
                int j = reader.getReader().readInt();
                if (j < 1) {
                    reader.getReader().setCursor(i);
                    throw ERROR_LIMIT_TOO_SMALL.createWithContext(reader.getReader());
                } else {
                    reader.setMaxResults(j);
                    reader.setLimited(true);
                }
            }, (reader) -> {
                return !reader.isCurrentEntity() && !reader.isLimited();
            }, new ChatMessage("argument.entity.options.limit.description"));
            register("sort", (reader) -> {
                int i = reader.getReader().getCursor();
                String string = reader.getReader().readUnquotedString();
                reader.setSuggestions((builder, consumer) -> {
                    return ICompletionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder);
                });
                BiConsumer<Vec3D, List<? extends Entity>> biConsumer;
                switch(string) {
                case "nearest":
                    biConsumer = ArgumentParserSelector.ORDER_NEAREST;
                    break;
                case "furthest":
                    biConsumer = ArgumentParserSelector.ORDER_FURTHEST;
                    break;
                case "random":
                    biConsumer = ArgumentParserSelector.ORDER_RANDOM;
                    break;
                case "arbitrary":
                    biConsumer = ArgumentParserSelector.ORDER_ARBITRARY;
                    break;
                default:
                    reader.getReader().setCursor(i);
                    throw ERROR_SORT_UNKNOWN.createWithContext(reader.getReader(), string);
                }

                reader.setOrder(biConsumer);
                reader.setSorted(true);
            }, (reader) -> {
                return !reader.isCurrentEntity() && !reader.isSorted();
            }, new ChatMessage("argument.entity.options.sort.description"));
            register("gamemode", (reader) -> {
                reader.setSuggestions((builder, consumer) -> {
                    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
                    boolean bl = !reader.hasGamemodeNotEquals();
                    boolean bl2 = true;
                    if (!string.isEmpty()) {
                        if (string.charAt(0) == '!') {
                            bl = false;
                            string = string.substring(1);
                        } else {
                            bl2 = false;
                        }
                    }

                    for(EnumGamemode gameType : EnumGamemode.values()) {
                        if (gameType.getName().toLowerCase(Locale.ROOT).startsWith(string)) {
                            if (bl2) {
                                builder.suggest("!" + gameType.getName());
                            }

                            if (bl) {
                                builder.suggest(gameType.getName());
                            }
                        }
                    }

                    return builder.buildFuture();
                });
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.hasGamemodeNotEquals() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "gamemode");
                } else {
                    String string = reader.getReader().readUnquotedString();
                    EnumGamemode gameType = EnumGamemode.byName(string, (EnumGamemode)null);
                    if (gameType == null) {
                        reader.getReader().setCursor(i);
                        throw ERROR_GAME_MODE_INVALID.createWithContext(reader.getReader(), string);
                    } else {
                        reader.setIncludesEntities(false);
                        reader.addPredicate((entity) -> {
                            if (!(entity instanceof EntityPlayer)) {
                                return false;
                            } else {
                                EnumGamemode gameType2 = ((EntityPlayer)entity).gameMode.getGameMode();
                                return bl ? gameType2 != gameType : gameType2 == gameType;
                            }
                        });
                        if (bl) {
                            reader.setHasGamemodeNotEquals(true);
                        } else {
                            reader.setHasGamemodeEquals(true);
                        }

                    }
                }
            }, (reader) -> {
                return !reader.hasGamemodeEquals();
            }, new ChatMessage("argument.entity.options.gamemode.description"));
            register("team", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate((entity) -> {
                    if (!(entity instanceof EntityLiving)) {
                        return false;
                    } else {
                        ScoreboardTeamBase team = entity.getScoreboardTeam();
                        String string2 = team == null ? "" : team.getName();
                        return string2.equals(string) != bl;
                    }
                });
                if (bl) {
                    reader.setHasTeamNotEquals(true);
                } else {
                    reader.setHasTeamEquals(true);
                }

            }, (reader) -> {
                return !reader.hasTeamEquals();
            }, new ChatMessage("argument.entity.options.team.description"));
            register("type", (reader) -> {
                reader.setSuggestions((builder, consumer) -> {
                    ICompletionProvider.suggestResource(IRegistry.ENTITY_TYPE.keySet(), builder, String.valueOf('!'));
                    ICompletionProvider.suggestResource(TagsEntity.getAllTags().getAvailableTags(), builder, "!#");
                    if (!reader.isTypeLimitedInversely()) {
                        ICompletionProvider.suggestResource(IRegistry.ENTITY_TYPE.keySet(), builder);
                        ICompletionProvider.suggestResource(TagsEntity.getAllTags().getAvailableTags(), builder, String.valueOf('#'));
                    }

                    return builder.buildFuture();
                });
                int i = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.isTypeLimitedInversely() && !bl) {
                    reader.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), "type");
                } else {
                    if (bl) {
                        reader.setTypeLimitedInversely();
                    }

                    if (reader.isTag()) {
                        MinecraftKey resourceLocation = MinecraftKey.read(reader.getReader());
                        reader.addPredicate((entity) -> {
                            return entity.getEntityType().is(entity.getMinecraftServer().getTagRegistry().getOrEmpty(IRegistry.ENTITY_TYPE_REGISTRY).getTagOrEmpty(resourceLocation)) != bl;
                        });
                    } else {
                        MinecraftKey resourceLocation2 = MinecraftKey.read(reader.getReader());
                        EntityTypes<?> entityType = IRegistry.ENTITY_TYPE.getOptional(resourceLocation2).orElseThrow(() -> {
                            reader.getReader().setCursor(i);
                            return ERROR_ENTITY_TYPE_INVALID.createWithContext(reader.getReader(), resourceLocation2.toString());
                        });
                        if (Objects.equals(EntityTypes.PLAYER, entityType) && !bl) {
                            reader.setIncludesEntities(false);
                        }

                        reader.addPredicate((entity) -> {
                            return Objects.equals(entityType, entity.getEntityType()) != bl;
                        });
                        if (!bl) {
                            reader.limitToType(entityType);
                        }
                    }

                }
            }, (reader) -> {
                return !reader.isTypeLimited();
            }, new ChatMessage("argument.entity.options.type.description"));
            register("tag", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate((entity) -> {
                    if ("".equals(string)) {
                        return entity.getScoreboardTags().isEmpty() != bl;
                    } else {
                        return entity.getScoreboardTags().contains(string) != bl;
                    }
                });
            }, (reader) -> {
                return true;
            }, new ChatMessage("argument.entity.options.tag.description"));
            register("nbt", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                NBTTagCompound compoundTag = (new MojangsonParser(reader.getReader())).readStruct();
                reader.addPredicate((entity) -> {
                    NBTTagCompound compoundTag2 = entity.save(new NBTTagCompound());
                    if (entity instanceof EntityPlayer) {
                        ItemStack itemStack = ((EntityPlayer)entity).getInventory().getItemInHand();
                        if (!itemStack.isEmpty()) {
                            compoundTag2.set("SelectedItem", itemStack.save(new NBTTagCompound()));
                        }
                    }

                    return GameProfileSerializer.compareNbt(compoundTag, compoundTag2, true) != bl;
                });
            }, (reader) -> {
                return true;
            }, new ChatMessage("argument.entity.options.nbt.description"));
            register("scores", (reader) -> {
                StringReader stringReader = reader.getReader();
                Map<String, CriterionConditionValue.IntegerRange> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while(stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    String string = stringReader.readUnquotedString();
                    stringReader.skipWhitespace();
                    stringReader.expect('=');
                    stringReader.skipWhitespace();
                    CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromReader(stringReader);
                    map.put(string, ints);
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate((entity) -> {
                        Scoreboard scoreboard = entity.getMinecraftServer().getScoreboard();
                        String string = entity.getName();

                        for(Entry<String, CriterionConditionValue.IntegerRange> entry : map.entrySet()) {
                            ScoreboardObjective objective = scoreboard.getObjective(entry.getKey());
                            if (objective == null) {
                                return false;
                            }

                            if (!scoreboard.hasPlayerScore(string, objective)) {
                                return false;
                            }

                            ScoreboardScore score = scoreboard.getPlayerScoreForObjective(string, objective);
                            int i = score.getScore();
                            if (!entry.getValue().matches(i)) {
                                return false;
                            }
                        }

                        return true;
                    });
                }

                reader.setHasScores(true);
            }, (reader) -> {
                return !reader.hasScores();
            }, new ChatMessage("argument.entity.options.scores.description"));
            register("advancements", (reader) -> {
                StringReader stringReader = reader.getReader();
                Map<MinecraftKey, Predicate<AdvancementProgress>> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while(stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    MinecraftKey resourceLocation = MinecraftKey.read(stringReader);
                    stringReader.skipWhitespace();
                    stringReader.expect('=');
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == '{') {
                        Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
                        stringReader.skipWhitespace();
                        stringReader.expect('{');
                        stringReader.skipWhitespace();

                        while(stringReader.canRead() && stringReader.peek() != '}') {
                            stringReader.skipWhitespace();
                            String string = stringReader.readUnquotedString();
                            stringReader.skipWhitespace();
                            stringReader.expect('=');
                            stringReader.skipWhitespace();
                            boolean bl = stringReader.readBoolean();
                            map2.put(string, (criterionProgress) -> {
                                return criterionProgress.isDone() == bl;
                            });
                            stringReader.skipWhitespace();
                            if (stringReader.canRead() && stringReader.peek() == ',') {
                                stringReader.skip();
                            }
                        }

                        stringReader.skipWhitespace();
                        stringReader.expect('}');
                        stringReader.skipWhitespace();
                        map.put(resourceLocation, (advancementProgress) -> {
                            for(Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                                CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                                if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                                    return false;
                                }
                            }

                            return true;
                        });
                    } else {
                        boolean bl2 = stringReader.readBoolean();
                        map.put(resourceLocation, (advancementProgress) -> {
                            return advancementProgress.isDone() == bl2;
                        });
                    }

                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate((entity) -> {
                        if (!(entity instanceof EntityPlayer)) {
                            return false;
                        } else {
                            EntityPlayer serverPlayer = (EntityPlayer)entity;
                            AdvancementDataPlayer playerAdvancements = serverPlayer.getAdvancementData();
                            AdvancementDataWorld serverAdvancementManager = serverPlayer.getMinecraftServer().getAdvancementData();

                            for(Entry<MinecraftKey, Predicate<AdvancementProgress>> entry : map.entrySet()) {
                                Advancement advancement = serverAdvancementManager.getAdvancement(entry.getKey());
                                if (advancement == null || !entry.getValue().test(playerAdvancements.getProgress(advancement))) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    });
                    reader.setIncludesEntities(false);
                }

                reader.setHasAdvancements(true);
            }, (reader) -> {
                return !reader.hasAdvancements();
            }, new ChatMessage("argument.entity.options.advancements.description"));
            register("predicate", (reader) -> {
                boolean bl = reader.shouldInvertValue();
                MinecraftKey resourceLocation = MinecraftKey.read(reader.getReader());
                reader.addPredicate((entity) -> {
                    if (!(entity.level instanceof WorldServer)) {
                        return false;
                    } else {
                        WorldServer serverLevel = (WorldServer)entity.level;
                        LootItemCondition lootItemCondition = serverLevel.getMinecraftServer().getLootPredicateManager().get(resourceLocation);
                        if (lootItemCondition == null) {
                            return false;
                        } else {
                            LootTableInfo lootContext = (new LootTableInfo.Builder(serverLevel)).set(LootContextParameters.THIS_ENTITY, entity).set(LootContextParameters.ORIGIN, entity.getPositionVector()).build(LootContextParameterSets.SELECTOR);
                            return bl ^ lootItemCondition.test(lootContext);
                        }
                    }
                });
            }, (reader) -> {
                return true;
            }, new ChatMessage("argument.entity.options.predicate.description"));
        }
    }

    public static PlayerSelector.Modifier get(ArgumentParserSelector reader, String option, int restoreCursor) throws CommandSyntaxException {
        PlayerSelector.Option option2 = OPTIONS.get(option);
        if (option2 != null) {
            if (option2.predicate.test(reader)) {
                return option2.modifier;
            } else {
                throw ERROR_INAPPLICABLE_OPTION.createWithContext(reader.getReader(), option);
            }
        } else {
            reader.getReader().setCursor(restoreCursor);
            throw ERROR_UNKNOWN_OPTION.createWithContext(reader.getReader(), option);
        }
    }

    public static void suggestNames(ArgumentParserSelector reader, SuggestionsBuilder suggestionBuilder) {
        String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(Entry<String, PlayerSelector.Option> entry : OPTIONS.entrySet()) {
            if ((entry.getValue()).predicate.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionBuilder.suggest((String)entry.getKey() + "=", (entry.getValue()).description);
            }
        }

    }

    public interface Modifier {
        void handle(ArgumentParserSelector reader) throws CommandSyntaxException;
    }

    static class Option {
        public final PlayerSelector.Modifier modifier;
        public final Predicate<ArgumentParserSelector> predicate;
        public final IChatBaseComponent description;

        Option(PlayerSelector.Modifier handler, Predicate<ArgumentParserSelector> condition, IChatBaseComponent description) {
            this.modifier = handler;
            this.predicate = condition;
            this.description = description;
        }
    }
}
