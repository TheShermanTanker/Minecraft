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
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.entity.options.unknown", object);
    });
    public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.entity.options.inapplicable", object);
    });
    public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.distance.negative"));
    public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.level.negative"));
    public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(new ChatMessage("argument.entity.options.limit.toosmall"));
    public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.entity.options.sort.irreversible", object);
    });
    public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.entity.options.mode.invalid", object);
    });
    public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.entity.options.type.invalid", object);
    });

    private static void register(String id, PlayerSelector.Modifier handler, Predicate<ArgumentParserSelector> condition, IChatBaseComponent description) {
        OPTIONS.put(id, new PlayerSelector.Option(handler, condition, description));
    }

    public static void bootStrap() {
        if (OPTIONS.isEmpty()) {
            register("name", (entitySelectorParser) -> {
                int i = entitySelectorParser.getReader().getCursor();
                boolean bl = entitySelectorParser.shouldInvertValue();
                String string = entitySelectorParser.getReader().readString();
                if (entitySelectorParser.hasNameNotEquals() && !bl) {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(entitySelectorParser.getReader(), "name");
                } else {
                    if (bl) {
                        entitySelectorParser.setHasNameNotEquals(true);
                    } else {
                        entitySelectorParser.setHasNameEquals(true);
                    }

                    entitySelectorParser.addPredicate((entity) -> {
                        return entity.getDisplayName().getString().equals(string) != bl;
                    });
                }
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.hasNameEquals();
            }, new ChatMessage("argument.entity.options.name.description"));
            register("distance", (entitySelectorParser) -> {
                int i = entitySelectorParser.getReader().getCursor();
                CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromReader(entitySelectorParser.getReader());
                if ((doubles.getMin() == null || !(doubles.getMin() < 0.0D)) && (doubles.getMax() == null || !(doubles.getMax() < 0.0D))) {
                    entitySelectorParser.setDistance(doubles);
                    entitySelectorParser.setWorldLimited();
                } else {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_RANGE_NEGATIVE.createWithContext(entitySelectorParser.getReader());
                }
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getDistance().isAny();
            }, new ChatMessage("argument.entity.options.distance.description"));
            register("level", (entitySelectorParser) -> {
                int i = entitySelectorParser.getReader().getCursor();
                CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromReader(entitySelectorParser.getReader());
                if ((ints.getMin() == null || ints.getMin() >= 0) && (ints.getMax() == null || ints.getMax() >= 0)) {
                    entitySelectorParser.setLevel(ints);
                    entitySelectorParser.setIncludesEntities(false);
                } else {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_LEVEL_NEGATIVE.createWithContext(entitySelectorParser.getReader());
                }
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getLevel().isAny();
            }, new ChatMessage("argument.entity.options.level.description"));
            register("x", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setX(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getX() == null;
            }, new ChatMessage("argument.entity.options.x.description"));
            register("y", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setY(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getY() == null;
            }, new ChatMessage("argument.entity.options.y.description"));
            register("z", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setZ(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getZ() == null;
            }, new ChatMessage("argument.entity.options.z.description"));
            register("dx", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setDeltaX(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getDeltaX() == null;
            }, new ChatMessage("argument.entity.options.dx.description"));
            register("dy", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setDeltaY(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getDeltaY() == null;
            }, new ChatMessage("argument.entity.options.dy.description"));
            register("dz", (entitySelectorParser) -> {
                entitySelectorParser.setWorldLimited();
                entitySelectorParser.setDeltaZ(entitySelectorParser.getReader().readDouble());
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getDeltaZ() == null;
            }, new ChatMessage("argument.entity.options.dz.description"));
            register("x_rotation", (entitySelectorParser) -> {
                entitySelectorParser.setRotX(CriterionConditionRange.fromReader(entitySelectorParser.getReader(), true, MathHelper::wrapDegrees));
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getRotX() == CriterionConditionRange.ANY;
            }, new ChatMessage("argument.entity.options.x_rotation.description"));
            register("y_rotation", (entitySelectorParser) -> {
                entitySelectorParser.setRotY(CriterionConditionRange.fromReader(entitySelectorParser.getReader(), true, MathHelper::wrapDegrees));
            }, (entitySelectorParser) -> {
                return entitySelectorParser.getRotY() == CriterionConditionRange.ANY;
            }, new ChatMessage("argument.entity.options.y_rotation.description"));
            register("limit", (entitySelectorParser) -> {
                int i = entitySelectorParser.getReader().getCursor();
                int j = entitySelectorParser.getReader().readInt();
                if (j < 1) {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_LIMIT_TOO_SMALL.createWithContext(entitySelectorParser.getReader());
                } else {
                    entitySelectorParser.setMaxResults(j);
                    entitySelectorParser.setLimited(true);
                }
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.isCurrentEntity() && !entitySelectorParser.isLimited();
            }, new ChatMessage("argument.entity.options.limit.description"));
            register("sort", (entitySelectorParser) -> {
                int i = entitySelectorParser.getReader().getCursor();
                String string = entitySelectorParser.getReader().readUnquotedString();
                entitySelectorParser.setSuggestions((suggestionsBuilder, consumer) -> {
                    return ICompletionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), suggestionsBuilder);
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
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_SORT_UNKNOWN.createWithContext(entitySelectorParser.getReader(), string);
                }

                entitySelectorParser.setOrder(biConsumer);
                entitySelectorParser.setSorted(true);
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.isCurrentEntity() && !entitySelectorParser.isSorted();
            }, new ChatMessage("argument.entity.options.sort.description"));
            register("gamemode", (entitySelectorParser) -> {
                entitySelectorParser.setSuggestions((suggestionsBuilder, consumer) -> {
                    String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
                    boolean bl = !entitySelectorParser.hasGamemodeNotEquals();
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
                                suggestionsBuilder.suggest("!" + gameType.getName());
                            }

                            if (bl) {
                                suggestionsBuilder.suggest(gameType.getName());
                            }
                        }
                    }

                    return suggestionsBuilder.buildFuture();
                });
                int i = entitySelectorParser.getReader().getCursor();
                boolean bl = entitySelectorParser.shouldInvertValue();
                if (entitySelectorParser.hasGamemodeNotEquals() && !bl) {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(entitySelectorParser.getReader(), "gamemode");
                } else {
                    String string = entitySelectorParser.getReader().readUnquotedString();
                    EnumGamemode gameType = EnumGamemode.byName(string, (EnumGamemode)null);
                    if (gameType == null) {
                        entitySelectorParser.getReader().setCursor(i);
                        throw ERROR_GAME_MODE_INVALID.createWithContext(entitySelectorParser.getReader(), string);
                    } else {
                        entitySelectorParser.setIncludesEntities(false);
                        entitySelectorParser.addPredicate((entity) -> {
                            if (!(entity instanceof EntityPlayer)) {
                                return false;
                            } else {
                                EnumGamemode gameType2 = ((EntityPlayer)entity).gameMode.getGameMode();
                                return bl ? gameType2 != gameType : gameType2 == gameType;
                            }
                        });
                        if (bl) {
                            entitySelectorParser.setHasGamemodeNotEquals(true);
                        } else {
                            entitySelectorParser.setHasGamemodeEquals(true);
                        }

                    }
                }
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.hasGamemodeEquals();
            }, new ChatMessage("argument.entity.options.gamemode.description"));
            register("team", (entitySelectorParser) -> {
                boolean bl = entitySelectorParser.shouldInvertValue();
                String string = entitySelectorParser.getReader().readUnquotedString();
                entitySelectorParser.addPredicate((entity) -> {
                    if (!(entity instanceof EntityLiving)) {
                        return false;
                    } else {
                        ScoreboardTeamBase team = entity.getScoreboardTeam();
                        String string2 = team == null ? "" : team.getName();
                        return string2.equals(string) != bl;
                    }
                });
                if (bl) {
                    entitySelectorParser.setHasTeamNotEquals(true);
                } else {
                    entitySelectorParser.setHasTeamEquals(true);
                }

            }, (entitySelectorParser) -> {
                return !entitySelectorParser.hasTeamEquals();
            }, new ChatMessage("argument.entity.options.team.description"));
            register("type", (entitySelectorParser) -> {
                entitySelectorParser.setSuggestions((suggestionsBuilder, consumer) -> {
                    ICompletionProvider.suggestResource(IRegistry.ENTITY_TYPE.keySet(), suggestionsBuilder, String.valueOf('!'));
                    ICompletionProvider.suggestResource(TagsEntity.getAllTags().getAvailableTags(), suggestionsBuilder, "!#");
                    if (!entitySelectorParser.isTypeLimitedInversely()) {
                        ICompletionProvider.suggestResource(IRegistry.ENTITY_TYPE.keySet(), suggestionsBuilder);
                        ICompletionProvider.suggestResource(TagsEntity.getAllTags().getAvailableTags(), suggestionsBuilder, String.valueOf('#'));
                    }

                    return suggestionsBuilder.buildFuture();
                });
                int i = entitySelectorParser.getReader().getCursor();
                boolean bl = entitySelectorParser.shouldInvertValue();
                if (entitySelectorParser.isTypeLimitedInversely() && !bl) {
                    entitySelectorParser.getReader().setCursor(i);
                    throw ERROR_INAPPLICABLE_OPTION.createWithContext(entitySelectorParser.getReader(), "type");
                } else {
                    if (bl) {
                        entitySelectorParser.setTypeLimitedInversely();
                    }

                    if (entitySelectorParser.isTag()) {
                        MinecraftKey resourceLocation = MinecraftKey.read(entitySelectorParser.getReader());
                        entitySelectorParser.addPredicate((entity) -> {
                            return entity.getEntityType().is(entity.getMinecraftServer().getTagRegistry().getOrEmpty(IRegistry.ENTITY_TYPE_REGISTRY).getTagOrEmpty(resourceLocation)) != bl;
                        });
                    } else {
                        MinecraftKey resourceLocation2 = MinecraftKey.read(entitySelectorParser.getReader());
                        EntityTypes<?> entityType = IRegistry.ENTITY_TYPE.getOptional(resourceLocation2).orElseThrow(() -> {
                            entitySelectorParser.getReader().setCursor(i);
                            return ERROR_ENTITY_TYPE_INVALID.createWithContext(entitySelectorParser.getReader(), resourceLocation2.toString());
                        });
                        if (Objects.equals(EntityTypes.PLAYER, entityType) && !bl) {
                            entitySelectorParser.setIncludesEntities(false);
                        }

                        entitySelectorParser.addPredicate((entity) -> {
                            return Objects.equals(entityType, entity.getEntityType()) != bl;
                        });
                        if (!bl) {
                            entitySelectorParser.limitToType(entityType);
                        }
                    }

                }
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.isTypeLimited();
            }, new ChatMessage("argument.entity.options.type.description"));
            register("tag", (entitySelectorParser) -> {
                boolean bl = entitySelectorParser.shouldInvertValue();
                String string = entitySelectorParser.getReader().readUnquotedString();
                entitySelectorParser.addPredicate((entity) -> {
                    if ("".equals(string)) {
                        return entity.getScoreboardTags().isEmpty() != bl;
                    } else {
                        return entity.getScoreboardTags().contains(string) != bl;
                    }
                });
            }, (entitySelectorParser) -> {
                return true;
            }, new ChatMessage("argument.entity.options.tag.description"));
            register("nbt", (entitySelectorParser) -> {
                boolean bl = entitySelectorParser.shouldInvertValue();
                NBTTagCompound compoundTag = (new MojangsonParser(entitySelectorParser.getReader())).readStruct();
                entitySelectorParser.addPredicate((entity) -> {
                    NBTTagCompound compoundTag2 = entity.save(new NBTTagCompound());
                    if (entity instanceof EntityPlayer) {
                        ItemStack itemStack = ((EntityPlayer)entity).getInventory().getItemInHand();
                        if (!itemStack.isEmpty()) {
                            compoundTag2.set("SelectedItem", itemStack.save(new NBTTagCompound()));
                        }
                    }

                    return GameProfileSerializer.compareNbt(compoundTag, compoundTag2, true) != bl;
                });
            }, (entitySelectorParser) -> {
                return true;
            }, new ChatMessage("argument.entity.options.nbt.description"));
            register("scores", (entitySelectorParser) -> {
                StringReader stringReader = entitySelectorParser.getReader();
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
                    entitySelectorParser.addPredicate((entity) -> {
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

                entitySelectorParser.setHasScores(true);
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.hasScores();
            }, new ChatMessage("argument.entity.options.scores.description"));
            register("advancements", (entitySelectorParser) -> {
                StringReader stringReader = entitySelectorParser.getReader();
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
                    entitySelectorParser.addPredicate((entity) -> {
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
                    entitySelectorParser.setIncludesEntities(false);
                }

                entitySelectorParser.setHasAdvancements(true);
            }, (entitySelectorParser) -> {
                return !entitySelectorParser.hasAdvancements();
            }, new ChatMessage("argument.entity.options.advancements.description"));
            register("predicate", (entitySelectorParser) -> {
                boolean bl = entitySelectorParser.shouldInvertValue();
                MinecraftKey resourceLocation = MinecraftKey.read(entitySelectorParser.getReader());
                entitySelectorParser.addPredicate((entity) -> {
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
            }, (entitySelectorParser) -> {
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

        Option(PlayerSelector.Modifier modifier, Predicate<ArgumentParserSelector> predicate, IChatBaseComponent component) {
            this.modifier = modifier;
            this.predicate = predicate;
            this.description = component;
        }
    }
}
