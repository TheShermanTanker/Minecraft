package net.minecraft.commands.synchronization;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.commands.arguments.ArgumentAngle;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.commands.arguments.ArgumentChatFormat;
import net.minecraft.commands.arguments.ArgumentCriterionValue;
import net.minecraft.commands.arguments.ArgumentDimension;
import net.minecraft.commands.arguments.ArgumentEnchantment;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentEntitySummon;
import net.minecraft.commands.arguments.ArgumentInventorySlot;
import net.minecraft.commands.arguments.ArgumentMathOperation;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.ArgumentMobEffect;
import net.minecraft.commands.arguments.ArgumentNBTBase;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.commands.arguments.ArgumentNBTTag;
import net.minecraft.commands.arguments.ArgumentParticle;
import net.minecraft.commands.arguments.ArgumentProfile;
import net.minecraft.commands.arguments.ArgumentScoreboardCriteria;
import net.minecraft.commands.arguments.ArgumentScoreboardObjective;
import net.minecraft.commands.arguments.ArgumentScoreboardSlot;
import net.minecraft.commands.arguments.ArgumentScoreboardTeam;
import net.minecraft.commands.arguments.ArgumentScoreholder;
import net.minecraft.commands.arguments.ArgumentTime;
import net.minecraft.commands.arguments.ArgumentUUID;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.blocks.ArgumentTile;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.coordinates.ArgumentRotation;
import net.minecraft.commands.arguments.coordinates.ArgumentRotationAxis;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2I;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.arguments.item.ArgumentItemPredicate;
import net.minecraft.commands.arguments.item.ArgumentItemStack;
import net.minecraft.commands.arguments.item.ArgumentTag;
import net.minecraft.commands.synchronization.brigadier.ArgumentSerializers;
import net.minecraft.gametest.framework.GameTestHarnessTestClassArgument;
import net.minecraft.gametest.framework.GameTestHarnessTestFunctionArgument;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArgumentRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, ArgumentRegistry.Entry<?>> BY_CLASS = Maps.newHashMap();
    private static final Map<MinecraftKey, ArgumentRegistry.Entry<?>> BY_NAME = Maps.newHashMap();

    public static <T extends ArgumentType<?>> void register(String id, Class<T> class_, ArgumentSerializer<T> argumentSerializer) {
        MinecraftKey resourceLocation = new MinecraftKey(id);
        if (BY_CLASS.containsKey(class_)) {
            throw new IllegalArgumentException("Class " + class_.getName() + " already has a serializer!");
        } else if (BY_NAME.containsKey(resourceLocation)) {
            throw new IllegalArgumentException("'" + resourceLocation + "' is already a registered serializer!");
        } else {
            ArgumentRegistry.Entry<T> entry = new ArgumentRegistry.Entry<>(class_, argumentSerializer, resourceLocation);
            BY_CLASS.put(class_, entry);
            BY_NAME.put(resourceLocation, entry);
        }
    }

    public static void bootStrap() {
        ArgumentSerializers.bootstrap();
        register("entity", ArgumentEntity.class, new ArgumentEntity.Serializer());
        register("game_profile", ArgumentProfile.class, new ArgumentSerializerVoid<>(ArgumentProfile::gameProfile));
        register("block_pos", ArgumentPosition.class, new ArgumentSerializerVoid<>(ArgumentPosition::blockPos));
        register("column_pos", ArgumentVec2I.class, new ArgumentSerializerVoid<>(ArgumentVec2I::columnPos));
        register("vec3", ArgumentVec3.class, new ArgumentSerializerVoid<>(ArgumentVec3::vec3));
        register("vec2", ArgumentVec2.class, new ArgumentSerializerVoid<>(ArgumentVec2::vec2));
        register("block_state", ArgumentTile.class, new ArgumentSerializerVoid<>(ArgumentTile::block));
        register("block_predicate", ArgumentBlockPredicate.class, new ArgumentSerializerVoid<>(ArgumentBlockPredicate::blockPredicate));
        register("item_stack", ArgumentItemStack.class, new ArgumentSerializerVoid<>(ArgumentItemStack::item));
        register("item_predicate", ArgumentItemPredicate.class, new ArgumentSerializerVoid<>(ArgumentItemPredicate::itemPredicate));
        register("color", ArgumentChatFormat.class, new ArgumentSerializerVoid<>(ArgumentChatFormat::color));
        register("component", ArgumentChatComponent.class, new ArgumentSerializerVoid<>(ArgumentChatComponent::textComponent));
        register("message", ArgumentChat.class, new ArgumentSerializerVoid<>(ArgumentChat::message));
        register("nbt_compound_tag", ArgumentNBTTag.class, new ArgumentSerializerVoid<>(ArgumentNBTTag::compoundTag));
        register("nbt_tag", ArgumentNBTBase.class, new ArgumentSerializerVoid<>(ArgumentNBTBase::nbtTag));
        register("nbt_path", ArgumentNBTKey.class, new ArgumentSerializerVoid<>(ArgumentNBTKey::nbtPath));
        register("objective", ArgumentScoreboardObjective.class, new ArgumentSerializerVoid<>(ArgumentScoreboardObjective::objective));
        register("objective_criteria", ArgumentScoreboardCriteria.class, new ArgumentSerializerVoid<>(ArgumentScoreboardCriteria::criteria));
        register("operation", ArgumentMathOperation.class, new ArgumentSerializerVoid<>(ArgumentMathOperation::operation));
        register("particle", ArgumentParticle.class, new ArgumentSerializerVoid<>(ArgumentParticle::particle));
        register("angle", ArgumentAngle.class, new ArgumentSerializerVoid<>(ArgumentAngle::angle));
        register("rotation", ArgumentRotation.class, new ArgumentSerializerVoid<>(ArgumentRotation::rotation));
        register("scoreboard_slot", ArgumentScoreboardSlot.class, new ArgumentSerializerVoid<>(ArgumentScoreboardSlot::displaySlot));
        register("score_holder", ArgumentScoreholder.class, new ArgumentScoreholder.Serializer());
        register("swizzle", ArgumentRotationAxis.class, new ArgumentSerializerVoid<>(ArgumentRotationAxis::swizzle));
        register("team", ArgumentScoreboardTeam.class, new ArgumentSerializerVoid<>(ArgumentScoreboardTeam::team));
        register("item_slot", ArgumentInventorySlot.class, new ArgumentSerializerVoid<>(ArgumentInventorySlot::slot));
        register("resource_location", ArgumentMinecraftKeyRegistered.class, new ArgumentSerializerVoid<>(ArgumentMinecraftKeyRegistered::id));
        register("mob_effect", ArgumentMobEffect.class, new ArgumentSerializerVoid<>(ArgumentMobEffect::effect));
        register("function", ArgumentTag.class, new ArgumentSerializerVoid<>(ArgumentTag::functions));
        register("entity_anchor", ArgumentAnchor.class, new ArgumentSerializerVoid<>(ArgumentAnchor::anchor));
        register("int_range", ArgumentCriterionValue.Ints.class, new ArgumentSerializerVoid<>(ArgumentCriterionValue::intRange));
        register("float_range", ArgumentCriterionValue.Floats.class, new ArgumentSerializerVoid<>(ArgumentCriterionValue::floatRange));
        register("item_enchantment", ArgumentEnchantment.class, new ArgumentSerializerVoid<>(ArgumentEnchantment::enchantment));
        register("entity_summon", ArgumentEntitySummon.class, new ArgumentSerializerVoid<>(ArgumentEntitySummon::id));
        register("dimension", ArgumentDimension.class, new ArgumentSerializerVoid<>(ArgumentDimension::dimension));
        register("time", ArgumentTime.class, new ArgumentSerializerVoid<>(ArgumentTime::time));
        register("uuid", ArgumentUUID.class, new ArgumentSerializerVoid<>(ArgumentUUID::uuid));
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            register("test_argument", GameTestHarnessTestFunctionArgument.class, new ArgumentSerializerVoid<>(GameTestHarnessTestFunctionArgument::testFunctionArgument));
            register("test_class", GameTestHarnessTestClassArgument.class, new ArgumentSerializerVoid<>(GameTestHarnessTestClassArgument::testClassName));
        }

    }

    @Nullable
    private static ArgumentRegistry.Entry<?> get(MinecraftKey id) {
        return BY_NAME.get(id);
    }

    @Nullable
    private static ArgumentRegistry.Entry<?> get(ArgumentType<?> argumentType) {
        return BY_CLASS.get(argumentType.getClass());
    }

    public static <T extends ArgumentType<?>> void serialize(PacketDataSerializer friendlyByteBuf, T argumentType) {
        ArgumentRegistry.Entry<T> entry = get(argumentType);
        if (entry == null) {
            LOGGER.error("Could not serialize {} ({}) - will not be sent to client!", argumentType, argumentType.getClass());
            friendlyByteBuf.writeResourceLocation(new MinecraftKey(""));
        } else {
            friendlyByteBuf.writeResourceLocation(entry.name);
            entry.serializer.serializeToNetwork(argumentType, friendlyByteBuf);
        }
    }

    @Nullable
    public static ArgumentType<?> deserialize(PacketDataSerializer buf) {
        MinecraftKey resourceLocation = buf.readResourceLocation();
        ArgumentRegistry.Entry<?> entry = get(resourceLocation);
        if (entry == null) {
            LOGGER.error("Could not deserialize {}", (Object)resourceLocation);
            return null;
        } else {
            return entry.serializer.deserializeFromNetwork(buf);
        }
    }

    private static <T extends ArgumentType<?>> void serializeToJson(JsonObject jsonObject, T argumentType) {
        ArgumentRegistry.Entry<T> entry = get(argumentType);
        if (entry == null) {
            LOGGER.error("Could not serialize argument {} ({})!", argumentType, argumentType.getClass());
            jsonObject.addProperty("type", "unknown");
        } else {
            jsonObject.addProperty("type", "argument");
            jsonObject.addProperty("parser", entry.name.toString());
            JsonObject jsonObject2 = new JsonObject();
            entry.serializer.serializeToJson(argumentType, jsonObject2);
            if (jsonObject2.size() > 0) {
                jsonObject.add("properties", jsonObject2);
            }
        }

    }

    public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> commandDispatcher, CommandNode<S> commandNode) {
        JsonObject jsonObject = new JsonObject();
        if (commandNode instanceof RootCommandNode) {
            jsonObject.addProperty("type", "root");
        } else if (commandNode instanceof LiteralCommandNode) {
            jsonObject.addProperty("type", "literal");
        } else if (commandNode instanceof ArgumentCommandNode) {
            serializeToJson(jsonObject, ((ArgumentCommandNode)commandNode).getType());
        } else {
            LOGGER.error("Could not serialize node {} ({})!", commandNode, commandNode.getClass());
            jsonObject.addProperty("type", "unknown");
        }

        JsonObject jsonObject2 = new JsonObject();

        for(CommandNode<S> commandNode2 : commandNode.getChildren()) {
            jsonObject2.add(commandNode2.getName(), serializeNodeToJson(commandDispatcher, commandNode2));
        }

        if (jsonObject2.size() > 0) {
            jsonObject.add("children", jsonObject2);
        }

        if (commandNode.getCommand() != null) {
            jsonObject.addProperty("executable", true);
        }

        if (commandNode.getRedirect() != null) {
            Collection<String> collection = commandDispatcher.getPath(commandNode.getRedirect());
            if (!collection.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(String string : collection) {
                    jsonArray.add(string);
                }

                jsonObject.add("redirect", jsonArray);
            }
        }

        return jsonObject;
    }

    public static boolean isTypeRegistered(ArgumentType<?> argumentType) {
        return get(argumentType) != null;
    }

    public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> node) {
        Set<CommandNode<T>> set = Sets.newIdentityHashSet();
        Set<ArgumentType<?>> set2 = Sets.newHashSet();
        findUsedArgumentTypes(node, set2, set);
        return set2;
    }

    private static <T> void findUsedArgumentTypes(CommandNode<T> node, Set<ArgumentType<?>> argumentTypes, Set<CommandNode<T>> ignoredNodes) {
        if (ignoredNodes.add(node)) {
            if (node instanceof ArgumentCommandNode) {
                argumentTypes.add(((ArgumentCommandNode)node).getType());
            }

            node.getChildren().forEach((nodex) -> {
                findUsedArgumentTypes(nodex, argumentTypes, ignoredNodes);
            });
            CommandNode<T> commandNode = node.getRedirect();
            if (commandNode != null) {
                findUsedArgumentTypes(commandNode, argumentTypes, ignoredNodes);
            }

        }
    }

    static class Entry<T extends ArgumentType<?>> {
        public final Class<T> clazz;
        public final ArgumentSerializer<T> serializer;
        public final MinecraftKey name;

        Entry(Class<T> class_, ArgumentSerializer<T> argumentSerializer, MinecraftKey resourceLocation) {
            this.clazz = class_;
            this.serializer = argumentSerializer;
            this.name = resourceLocation;
        }
    }
}
