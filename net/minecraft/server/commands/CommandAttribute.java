package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.UUID;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.ArgumentMinecraftKeyRegistered;
import net.minecraft.commands.arguments.ArgumentUUID;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CommandAttribute {
    private static final SuggestionProvider<CommandListenerWrapper> AVAILABLE_ATTRIBUTES = (context, builder) -> {
        return ICompletionProvider.suggestResource(IRegistry.ATTRIBUTE.keySet(), builder);
    };
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((name) -> {
        return new ChatMessage("commands.attribute.failed.entity", name);
    });
    private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType((entityName, attributeName) -> {
        return new ChatMessage("commands.attribute.failed.no_attribute", entityName, attributeName);
    });
    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType((entityName, attributeName, uuid) -> {
        return new ChatMessage("commands.attribute.failed.no_modifier", attributeName, entityName, uuid);
    });
    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType((entityName, attributeName, uuid) -> {
        return new ChatMessage("commands.attribute.failed.modifier_already_present", uuid, attributeName, entityName);
    });

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("attribute").requires((source) -> {
            return source.hasPermission(2);
        }).then(net.minecraft.commands.CommandDispatcher.argument("target", ArgumentEntity.entity()).then(net.minecraft.commands.CommandDispatcher.argument("attribute", ArgumentMinecraftKeyRegistered.id()).suggests(AVAILABLE_ATTRIBUTES).then(net.minecraft.commands.CommandDispatcher.literal("get").executes((context) -> {
            return getAttributeValue(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), 1.0D);
        }).then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeValue(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "scale"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("base").then(net.minecraft.commands.CommandDispatcher.literal("set").then(net.minecraft.commands.CommandDispatcher.argument("value", DoubleArgumentType.doubleArg()).executes((context) -> {
            return setAttributeBase(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "value"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("get").executes((context) -> {
            return getAttributeBase(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), 1.0D);
        }).then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeBase(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "scale"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("modifier").then(net.minecraft.commands.CommandDispatcher.literal("add").then(net.minecraft.commands.CommandDispatcher.argument("uuid", ArgumentUUID.uuid()).then(net.minecraft.commands.CommandDispatcher.argument("name", StringArgumentType.string()).then(net.minecraft.commands.CommandDispatcher.argument("value", DoubleArgumentType.doubleArg()).then(net.minecraft.commands.CommandDispatcher.literal("add").executes((context) -> {
            return addModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.ADDITION);
        })).then(net.minecraft.commands.CommandDispatcher.literal("multiply").executes((context) -> {
            return addModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        })).then(net.minecraft.commands.CommandDispatcher.literal("multiply_base").executes((context) -> {
            return addModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.MULTIPLY_BASE);
        })))))).then(net.minecraft.commands.CommandDispatcher.literal("remove").then(net.minecraft.commands.CommandDispatcher.argument("uuid", ArgumentUUID.uuid()).executes((context) -> {
            return removeModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("value").then(net.minecraft.commands.CommandDispatcher.literal("get").then(net.minecraft.commands.CommandDispatcher.argument("uuid", ArgumentUUID.uuid()).executes((context) -> {
            return getAttributeModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"), 1.0D);
        }).then(net.minecraft.commands.CommandDispatcher.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeModifier(context.getSource(), ArgumentEntity.getEntity(context, "target"), ArgumentMinecraftKeyRegistered.getAttribute(context, "attribute"), ArgumentUUID.getUuid(context, "uuid"), DoubleArgumentType.getDouble(context, "scale"));
        })))))))));
    }

    private static AttributeModifiable getAttributeInstance(Entity entity, AttributeBase attribute) throws CommandSyntaxException {
        AttributeModifiable attributeInstance = getLivingEntity(entity).getAttributeMap().getInstance(attribute);
        if (attributeInstance == null) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getDisplayName(), new ChatMessage(attribute.getName()));
        } else {
            return attributeInstance;
        }
    }

    private static EntityLiving getLivingEntity(Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof EntityLiving)) {
            throw ERROR_NOT_LIVING_ENTITY.create(entity.getDisplayName());
        } else {
            return (EntityLiving)entity;
        }
    }

    private static EntityLiving getEntityWithAttribute(Entity entity, AttributeBase attribute) throws CommandSyntaxException {
        EntityLiving livingEntity = getLivingEntity(entity);
        if (!livingEntity.getAttributeMap().hasAttribute(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getDisplayName(), new ChatMessage(attribute.getName()));
        } else {
            return livingEntity;
        }
    }

    private static int getAttributeValue(CommandListenerWrapper source, Entity target, AttributeBase attribute, double multiplier) throws CommandSyntaxException {
        EntityLiving livingEntity = getEntityWithAttribute(target, attribute);
        double d = livingEntity.getAttributeValue(attribute);
        source.sendMessage(new ChatMessage("commands.attribute.value.get.success", new ChatMessage(attribute.getName()), target.getDisplayName(), d), false);
        return (int)(d * multiplier);
    }

    private static int getAttributeBase(CommandListenerWrapper source, Entity target, AttributeBase attribute, double multiplier) throws CommandSyntaxException {
        EntityLiving livingEntity = getEntityWithAttribute(target, attribute);
        double d = livingEntity.getAttributeBaseValue(attribute);
        source.sendMessage(new ChatMessage("commands.attribute.base_value.get.success", new ChatMessage(attribute.getName()), target.getDisplayName(), d), false);
        return (int)(d * multiplier);
    }

    private static int getAttributeModifier(CommandListenerWrapper source, Entity target, AttributeBase attribute, UUID uuid, double multiplier) throws CommandSyntaxException {
        EntityLiving livingEntity = getEntityWithAttribute(target, attribute);
        AttributeMapBase attributeMap = livingEntity.getAttributeMap();
        if (!attributeMap.hasModifier(attribute, uuid)) {
            throw ERROR_NO_SUCH_MODIFIER.create(target.getDisplayName(), new ChatMessage(attribute.getName()), uuid);
        } else {
            double d = attributeMap.getModifierValue(attribute, uuid);
            source.sendMessage(new ChatMessage("commands.attribute.modifier.value.get.success", uuid, new ChatMessage(attribute.getName()), target.getDisplayName(), d), false);
            return (int)(d * multiplier);
        }
    }

    private static int setAttributeBase(CommandListenerWrapper source, Entity target, AttributeBase attribute, double value) throws CommandSyntaxException {
        getAttributeInstance(target, attribute).setValue(value);
        source.sendMessage(new ChatMessage("commands.attribute.base_value.set.success", new ChatMessage(attribute.getName()), target.getDisplayName(), value), false);
        return 1;
    }

    private static int addModifier(CommandListenerWrapper source, Entity target, AttributeBase attribute, UUID uuid, String name, double value, AttributeModifier.Operation operation) throws CommandSyntaxException {
        AttributeModifiable attributeInstance = getAttributeInstance(target, attribute);
        AttributeModifier attributeModifier = new AttributeModifier(uuid, name, value, operation);
        if (attributeInstance.hasModifier(attributeModifier)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(target.getDisplayName(), new ChatMessage(attribute.getName()), uuid);
        } else {
            attributeInstance.addPermanentModifier(attributeModifier);
            source.sendMessage(new ChatMessage("commands.attribute.modifier.add.success", uuid, new ChatMessage(attribute.getName()), target.getDisplayName()), false);
            return 1;
        }
    }

    private static int removeModifier(CommandListenerWrapper source, Entity target, AttributeBase attribute, UUID uuid) throws CommandSyntaxException {
        AttributeModifiable attributeInstance = getAttributeInstance(target, attribute);
        if (attributeInstance.removePermanentModifier(uuid)) {
            source.sendMessage(new ChatMessage("commands.attribute.modifier.remove.success", uuid, new ChatMessage(attribute.getName()), target.getDisplayName()), false);
            return 1;
        } else {
            throw ERROR_NO_SUCH_MODIFIER.create(target.getDisplayName(), new ChatMessage(attribute.getName()), uuid);
        }
    }
}
