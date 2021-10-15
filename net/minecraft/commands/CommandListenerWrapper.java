package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class CommandListenerWrapper implements ICompletionProvider {
    public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(new ChatMessage("permissions.requires.player"));
    public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(new ChatMessage("permissions.requires.entity"));
    public final ICommandListener source;
    private final Vec3D worldPosition;
    private final WorldServer level;
    private final int permissionLevel;
    private final String textName;
    private final IChatBaseComponent displayName;
    private final MinecraftServer server;
    private final boolean silent;
    @Nullable
    private final Entity entity;
    private final ResultConsumer<CommandListenerWrapper> consumer;
    private final ArgumentAnchor.Anchor anchor;
    private final Vec2F rotation;

    public CommandListenerWrapper(ICommandListener output, Vec3D pos, Vec2F rot, WorldServer world, int level, String name, IChatBaseComponent displayName, MinecraftServer server, @Nullable Entity entity) {
        this(output, pos, rot, world, level, name, displayName, server, entity, false, (context, success, result) -> {
        }, ArgumentAnchor.Anchor.FEET);
    }

    protected CommandListenerWrapper(ICommandListener output, Vec3D pos, Vec2F rot, WorldServer world, int level, String name, IChatBaseComponent displayName, MinecraftServer server, @Nullable Entity entity, boolean silent, ResultConsumer<CommandListenerWrapper> consumer, ArgumentAnchor.Anchor entityAnchor) {
        this.source = output;
        this.worldPosition = pos;
        this.level = world;
        this.silent = silent;
        this.entity = entity;
        this.permissionLevel = level;
        this.textName = name;
        this.displayName = displayName;
        this.server = server;
        this.consumer = consumer;
        this.anchor = entityAnchor;
        this.rotation = rot;
    }

    public CommandListenerWrapper withSource(ICommandListener output) {
        return this.source == output ? this : new CommandListenerWrapper(output, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withEntity(Entity entity) {
        return this.entity == entity ? this : new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, entity.getDisplayName().getString(), entity.getScoreboardDisplayName(), this.server, entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withPosition(Vec3D position) {
        return this.worldPosition.equals(position) ? this : new CommandListenerWrapper(this.source, position, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withRotation(Vec2F rotation) {
        return this.rotation.equals(rotation) ? this : new CommandListenerWrapper(this.source, this.worldPosition, rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withCallback(ResultConsumer<CommandListenerWrapper> consumer) {
        return this.consumer.equals(consumer) ? this : new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, consumer, this.anchor);
    }

    public CommandListenerWrapper withCallback(ResultConsumer<CommandListenerWrapper> consumer, BinaryOperator<ResultConsumer<CommandListenerWrapper>> merger) {
        ResultConsumer<CommandListenerWrapper> resultConsumer = merger.apply(this.consumer, consumer);
        return this.withCallback(resultConsumer);
    }

    public CommandListenerWrapper withSuppressedOutput() {
        return !this.silent && !this.source.alwaysAccepts() ? new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, true, this.consumer, this.anchor) : this;
    }

    public CommandListenerWrapper withPermission(int level) {
        return level == this.permissionLevel ? this : new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, level, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withMaximumPermission(int level) {
        return level <= this.permissionLevel ? this : new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, level, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
    }

    public CommandListenerWrapper withAnchor(ArgumentAnchor.Anchor anchor) {
        return anchor == this.anchor ? this : new CommandListenerWrapper(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, anchor);
    }

    public CommandListenerWrapper withLevel(WorldServer world) {
        if (world == this.level) {
            return this;
        } else {
            double d = DimensionManager.getTeleportationScale(this.level.getDimensionManager(), world.getDimensionManager());
            Vec3D vec3 = new Vec3D(this.worldPosition.x * d, this.worldPosition.y, this.worldPosition.z * d);
            return new CommandListenerWrapper(this.source, vec3, this.rotation, world, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
        }
    }

    public CommandListenerWrapper facing(Entity entity, ArgumentAnchor.Anchor anchor) {
        return this.facing(anchor.apply(entity));
    }

    public CommandListenerWrapper facing(Vec3D position) {
        Vec3D vec3 = this.anchor.apply(this);
        double d = position.x - vec3.x;
        double e = position.y - vec3.y;
        double f = position.z - vec3.z;
        double g = Math.sqrt(d * d + f * f);
        float h = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * (double)(180F / (float)Math.PI))));
        float i = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F);
        return this.withRotation(new Vec2F(h, i));
    }

    public IChatBaseComponent getScoreboardDisplayName() {
        return this.displayName;
    }

    public String getName() {
        return this.textName;
    }

    @Override
    public boolean hasPermission(int level) {
        return this.permissionLevel >= level;
    }

    public Vec3D getPosition() {
        return this.worldPosition;
    }

    public WorldServer getWorld() {
        return this.level;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    public Entity getEntityOrException() throws CommandSyntaxException {
        if (this.entity == null) {
            throw ERROR_NOT_ENTITY.create();
        } else {
            return this.entity;
        }
    }

    public EntityPlayer getPlayerOrException() throws CommandSyntaxException {
        if (!(this.entity instanceof EntityPlayer)) {
            throw ERROR_NOT_PLAYER.create();
        } else {
            return (EntityPlayer)this.entity;
        }
    }

    public Vec2F getRotation() {
        return this.rotation;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public ArgumentAnchor.Anchor getAnchor() {
        return this.anchor;
    }

    public void sendMessage(IChatBaseComponent message, boolean broadcastToOps) {
        if (this.source.shouldSendSuccess() && !this.silent) {
            this.source.sendMessage(message, SystemUtils.NIL_UUID);
        }

        if (broadcastToOps && this.source.shouldBroadcastCommands() && !this.silent) {
            this.sendAdminMessage(message);
        }

    }

    private void sendAdminMessage(IChatBaseComponent message) {
        IChatBaseComponent component = (new ChatMessage("chat.type.admin", this.getScoreboardDisplayName(), message)).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC});
        if (this.server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            for(EntityPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
                if (serverPlayer != this.source && this.server.getPlayerList().isOp(serverPlayer.getProfile())) {
                    serverPlayer.sendMessage(component, SystemUtils.NIL_UUID);
                }
            }
        }

        if (this.source != this.server && this.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
            this.server.sendMessage(component, SystemUtils.NIL_UUID);
        }

    }

    public void sendFailureMessage(IChatBaseComponent message) {
        if (this.source.shouldSendFailure() && !this.silent) {
            this.source.sendMessage((new ChatComponentText("")).addSibling(message).withStyle(EnumChatFormat.RED), SystemUtils.NIL_UUID);
        }

    }

    public void onCommandComplete(CommandContext<CommandListenerWrapper> context, boolean success, int result) {
        if (this.consumer != null) {
            this.consumer.onCommandComplete(context, success, result);
        }

    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return Lists.newArrayList(this.server.getPlayers());
    }

    @Override
    public Collection<String> getAllTeams() {
        return this.server.getScoreboard().getTeamNames();
    }

    @Override
    public Collection<MinecraftKey> getAvailableSoundEvents() {
        return IRegistry.SOUND_EVENT.keySet();
    }

    @Override
    public Stream<MinecraftKey> getRecipeNames() {
        return this.server.getCraftingManager().getRecipeIds();
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<ICompletionProvider> context, SuggestionsBuilder builder) {
        return null;
    }

    @Override
    public Set<ResourceKey<World>> levels() {
        return this.server.levelKeys();
    }

    @Override
    public IRegistryCustom registryAccess() {
        return this.server.getCustomRegistry();
    }
}
