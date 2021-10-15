package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.UtilColor;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;

public abstract class CommandBlockListenerAbstract implements ICommandListener {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final IChatBaseComponent DEFAULT_NAME = new ChatComponentText("@");
    private long lastExecution = -1L;
    private boolean updateLastExecution = true;
    private int successCount;
    private boolean trackOutput = true;
    @Nullable
    private IChatBaseComponent lastOutput;
    private String command = "";
    private IChatBaseComponent name = DEFAULT_NAME;

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public IChatBaseComponent getLastOutput() {
        return this.lastOutput == null ? ChatComponentText.EMPTY : this.lastOutput;
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        nbt.setString("Command", this.command);
        nbt.setInt("SuccessCount", this.successCount);
        nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        nbt.setBoolean("TrackOutput", this.trackOutput);
        if (this.lastOutput != null && this.trackOutput) {
            nbt.setString("LastOutput", IChatBaseComponent.ChatSerializer.toJson(this.lastOutput));
        }

        nbt.setBoolean("UpdateLastExecution", this.updateLastExecution);
        if (this.updateLastExecution && this.lastExecution > 0L) {
            nbt.setLong("LastExecution", this.lastExecution);
        }

        return nbt;
    }

    public void load(NBTTagCompound nbt) {
        this.command = nbt.getString("Command");
        this.successCount = nbt.getInt("SuccessCount");
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.setName(IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName")));
        }

        if (nbt.hasKeyOfType("TrackOutput", 1)) {
            this.trackOutput = nbt.getBoolean("TrackOutput");
        }

        if (nbt.hasKeyOfType("LastOutput", 8) && this.trackOutput) {
            try {
                this.lastOutput = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("LastOutput"));
            } catch (Throwable var3) {
                this.lastOutput = new ChatComponentText(var3.getMessage());
            }
        } else {
            this.lastOutput = null;
        }

        if (nbt.hasKey("UpdateLastExecution")) {
            this.updateLastExecution = nbt.getBoolean("UpdateLastExecution");
        }

        if (this.updateLastExecution && nbt.hasKey("LastExecution")) {
            this.lastExecution = nbt.getLong("LastExecution");
        } else {
            this.lastExecution = -1L;
        }

    }

    public void setCommand(String command) {
        this.command = command;
        this.successCount = 0;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean performCommand(World world) {
        if (!world.isClientSide && world.getTime() != this.lastExecution) {
            if ("Searge".equalsIgnoreCase(this.command)) {
                this.lastOutput = new ChatComponentText("#itzlipofutzli");
                this.successCount = 1;
                return true;
            } else {
                this.successCount = 0;
                MinecraftServer minecraftServer = this.getLevel().getMinecraftServer();
                if (minecraftServer.getEnableCommandBlock() && !UtilColor.isNullOrEmpty(this.command)) {
                    try {
                        this.lastOutput = null;
                        CommandListenerWrapper commandSourceStack = this.getWrapper().withCallback((commandContext, bl, i) -> {
                            if (bl) {
                                ++this.successCount;
                            }

                        });
                        minecraftServer.getCommandDispatcher().performCommand(commandSourceStack, this.command);
                    } catch (Throwable var6) {
                        CrashReport crashReport = CrashReport.forThrowable(var6, "Executing command block");
                        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Command to be executed");
                        crashReportCategory.setDetail("Command", this::getCommand);
                        crashReportCategory.setDetail("Name", () -> {
                            return this.getName().getString();
                        });
                        throw new ReportedException(crashReport);
                    }
                }

                if (this.updateLastExecution) {
                    this.lastExecution = world.getTime();
                } else {
                    this.lastExecution = -1L;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public IChatBaseComponent getName() {
        return this.name;
    }

    public void setName(@Nullable IChatBaseComponent name) {
        if (name != null) {
            this.name = name;
        } else {
            this.name = DEFAULT_NAME;
        }

    }

    @Override
    public void sendMessage(IChatBaseComponent message, UUID sender) {
        if (this.trackOutput) {
            this.lastOutput = (new ChatComponentText("[" + TIME_FORMAT.format(new Date()) + "] ")).addSibling(message);
            this.onUpdated();
        }

    }

    public abstract WorldServer getLevel();

    public abstract void onUpdated();

    public void setLastOutput(@Nullable IChatBaseComponent lastOutput) {
        this.lastOutput = lastOutput;
    }

    public void setTrackOutput(boolean trackOutput) {
        this.trackOutput = trackOutput;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public EnumInteractionResult usedBy(EntityHuman player) {
        if (!player.isCreativeAndOp()) {
            return EnumInteractionResult.PASS;
        } else {
            if (player.getCommandSenderWorld().isClientSide) {
                player.openMinecartCommandBlock(this);
            }

            return EnumInteractionResult.sidedSuccess(player.level.isClientSide);
        }
    }

    public abstract Vec3D getPosition();

    public abstract CommandListenerWrapper getWrapper();

    @Override
    public boolean shouldSendSuccess() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
    }

    @Override
    public boolean shouldSendFailure() {
        return this.trackOutput;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
    }
}
