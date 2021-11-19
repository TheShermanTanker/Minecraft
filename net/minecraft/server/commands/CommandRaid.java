package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChatComponent;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.entity.raid.PersistentRaid;
import net.minecraft.world.entity.raid.Raid;

public class CommandRaid {
    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("raid").requires((source) -> {
            return source.hasPermission(3);
        }).then(net.minecraft.commands.CommandDispatcher.literal("start").then(net.minecraft.commands.CommandDispatcher.argument("omenlvl", IntegerArgumentType.integer(0)).executes((context) -> {
            return start(context.getSource(), IntegerArgumentType.getInteger(context, "omenlvl"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((context) -> {
            return stop(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("check").executes((context) -> {
            return check(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("sound").then(net.minecraft.commands.CommandDispatcher.argument("type", ArgumentChatComponent.textComponent()).executes((context) -> {
            return playSound(context.getSource(), ArgumentChatComponent.getComponent(context, "type"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("spawnleader").executes((context) -> {
            return spawnLeader(context.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.literal("setomen").then(net.minecraft.commands.CommandDispatcher.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return setBadOmenLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"));
        }))).then(net.minecraft.commands.CommandDispatcher.literal("glow").executes((context) -> {
            return glow(context.getSource());
        })));
    }

    private static int glow(CommandListenerWrapper source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            for(EntityRaider raider : raid.getAllRaiders()) {
                raider.addEffect(new MobEffect(MobEffectList.GLOWING, 1000, 1));
            }
        }

        return 1;
    }

    private static int setBadOmenLevel(CommandListenerWrapper source, int level) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            int i = raid.getMaxBadOmenLevel();
            if (level > i) {
                source.sendFailureMessage(new ChatComponentText("Sorry, the max bad omen level you can set is " + i));
            } else {
                int j = raid.getBadOmenLevel();
                raid.setBadOmenLevel(level);
                source.sendMessage(new ChatComponentText("Changed village's bad omen level from " + j + " to " + level), false);
            }
        } else {
            source.sendFailureMessage(new ChatComponentText("No raid found here"));
        }

        return 1;
    }

    private static int spawnLeader(CommandListenerWrapper source) {
        source.sendMessage(new ChatComponentText("Spawned a raid captain"), false);
        EntityRaider raider = EntityTypes.PILLAGER.create(source.getWorld());
        raider.setPatrolLeader(true);
        raider.setSlot(EnumItemSlot.HEAD, Raid.getLeaderBannerInstance());
        raider.setPosition(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        raider.prepare(source.getWorld(), source.getWorld().getDamageScaler(new BlockPosition(source.getPosition())), EnumMobSpawn.COMMAND, (GroupDataEntity)null, (NBTTagCompound)null);
        source.getWorld().addAllEntities(raider);
        return 1;
    }

    private static int playSound(CommandListenerWrapper source, IChatBaseComponent type) {
        if (type != null && type.getString().equals("local")) {
            source.getWorld().playSound((EntityHuman)null, new BlockPosition(source.getPosition().add(5.0D, 0.0D, 0.0D)), SoundEffects.RAID_HORN, EnumSoundCategory.NEUTRAL, 2.0F, 1.0F);
        }

        return 1;
    }

    private static int start(CommandListenerWrapper source, int level) throws CommandSyntaxException {
        EntityPlayer serverPlayer = source.getPlayerOrException();
        BlockPosition blockPos = serverPlayer.getChunkCoordinates();
        if (serverPlayer.getWorldServer().isRaided(blockPos)) {
            source.sendFailureMessage(new ChatComponentText("Raid already started close by"));
            return -1;
        } else {
            PersistentRaid raids = serverPlayer.getWorldServer().getPersistentRaid();
            Raid raid = raids.createOrExtendRaid(serverPlayer);
            if (raid != null) {
                raid.setBadOmenLevel(level);
                raids.setDirty();
                source.sendMessage(new ChatComponentText("Created a raid in your local village"), false);
            } else {
                source.sendFailureMessage(new ChatComponentText("Failed to create a raid in your local village"));
            }

            return 1;
        }
    }

    private static int stop(CommandListenerWrapper source) throws CommandSyntaxException {
        EntityPlayer serverPlayer = source.getPlayerOrException();
        BlockPosition blockPos = serverPlayer.getChunkCoordinates();
        Raid raid = serverPlayer.getWorldServer().getRaidAt(blockPos);
        if (raid != null) {
            raid.stop();
            source.sendMessage(new ChatComponentText("Stopped raid"), false);
            return 1;
        } else {
            source.sendFailureMessage(new ChatComponentText("No raid here"));
            return -1;
        }
    }

    private static int check(CommandListenerWrapper source) throws CommandSyntaxException {
        Raid raid = getRaid(source.getPlayerOrException());
        if (raid != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found a started raid! ");
            source.sendMessage(new ChatComponentText(stringBuilder.toString()), false);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Num groups spawned: ");
            stringBuilder.append(raid.getGroupsSpawned());
            stringBuilder.append(" Bad omen level: ");
            stringBuilder.append(raid.getBadOmenLevel());
            stringBuilder.append(" Num mobs: ");
            stringBuilder.append(raid.getTotalRaidersAlive());
            stringBuilder.append(" Raid health: ");
            stringBuilder.append(raid.sumMobHealth());
            stringBuilder.append(" / ");
            stringBuilder.append(raid.getTotalHealth());
            source.sendMessage(new ChatComponentText(stringBuilder.toString()), false);
            return 1;
        } else {
            source.sendFailureMessage(new ChatComponentText("Found no started raids"));
            return 0;
        }
    }

    @Nullable
    private static Raid getRaid(EntityPlayer player) {
        return player.getWorldServer().getRaidAt(player.getChunkCoordinates());
    }
}
