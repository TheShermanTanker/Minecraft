package net.minecraft.server.rcon;

import java.util.UUID;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class RemoteControlCommandListener implements ICommandListener {
    private static final String RCON = "Rcon";
    private static final IChatBaseComponent RCON_COMPONENT = new ChatComponentText("Rcon");
    private final StringBuffer buffer = new StringBuffer();
    private final MinecraftServer server;

    public RemoteControlCommandListener(MinecraftServer server) {
        this.server = server;
    }

    public void clearMessages() {
        this.buffer.setLength(0);
    }

    public String getMessages() {
        return this.buffer.toString();
    }

    public CommandListenerWrapper getWrapper() {
        WorldServer serverLevel = this.server.overworld();
        return new CommandListenerWrapper(this, Vec3D.atLowerCornerOf(serverLevel.getSpawn()), Vec2F.ZERO, serverLevel, 4, "Rcon", RCON_COMPONENT, this.server, (Entity)null);
    }

    @Override
    public void sendMessage(IChatBaseComponent message, UUID sender) {
        this.buffer.append(message.getString());
    }

    @Override
    public boolean shouldSendSuccess() {
        return true;
    }

    @Override
    public boolean shouldSendFailure() {
        return true;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return this.server.shouldRconBroadcast();
    }
}
