package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.FormattedString;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class TileEntitySign extends TileEntity {
    public static final int LINES = 4;
    private static final String[] RAW_TEXT_FIELD_NAMES = new String[]{"Text1", "Text2", "Text3", "Text4"};
    private static final String[] FILTERED_TEXT_FIELD_NAMES = new String[]{"FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4"};
    public final IChatBaseComponent[] messages = new IChatBaseComponent[]{ChatComponentText.EMPTY, ChatComponentText.EMPTY, ChatComponentText.EMPTY, ChatComponentText.EMPTY};
    private final IChatBaseComponent[] filteredMessages = new IChatBaseComponent[]{ChatComponentText.EMPTY, ChatComponentText.EMPTY, ChatComponentText.EMPTY, ChatComponentText.EMPTY};
    public boolean isEditable = true;
    @Nullable
    private UUID playerWhoMayEdit;
    @Nullable
    private FormattedString[] renderMessages;
    private boolean renderMessagedFiltered;
    private EnumColor color = EnumColor.BLACK;
    private boolean hasGlowingText;

    public TileEntitySign(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SIGN, pos, state);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);

        for(int i = 0; i < 4; ++i) {
            IChatBaseComponent component = this.messages[i];
            String string = IChatBaseComponent.ChatSerializer.toJson(component);
            nbt.setString(RAW_TEXT_FIELD_NAMES[i], string);
            IChatBaseComponent component2 = this.filteredMessages[i];
            if (!component2.equals(component)) {
                nbt.setString(FILTERED_TEXT_FIELD_NAMES[i], IChatBaseComponent.ChatSerializer.toJson(component2));
            }
        }

        nbt.setString("Color", this.color.getName());
        nbt.setBoolean("GlowingText", this.hasGlowingText);
        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        this.isEditable = false;
        super.load(nbt);
        this.color = EnumColor.byName(nbt.getString("Color"), EnumColor.BLACK);

        for(int i = 0; i < 4; ++i) {
            String string = nbt.getString(RAW_TEXT_FIELD_NAMES[i]);
            IChatBaseComponent component = this.loadLine(string);
            this.messages[i] = component;
            String string2 = FILTERED_TEXT_FIELD_NAMES[i];
            if (nbt.hasKeyOfType(string2, 8)) {
                this.filteredMessages[i] = this.loadLine(nbt.getString(string2));
            } else {
                this.filteredMessages[i] = component;
            }
        }

        this.renderMessages = null;
        this.hasGlowingText = nbt.getBoolean("GlowingText");
    }

    private IChatBaseComponent loadLine(String json) {
        IChatBaseComponent component = this.deserializeTextSafe(json);
        if (this.level instanceof WorldServer) {
            try {
                return ChatComponentUtils.filterForDisplay(this.createCommandSourceStack((EntityPlayer)null), component, (Entity)null, 0);
            } catch (CommandSyntaxException var4) {
            }
        }

        return component;
    }

    private IChatBaseComponent deserializeTextSafe(String json) {
        try {
            IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(json);
            if (component != null) {
                return component;
            }
        } catch (Exception var3) {
        }

        return ChatComponentText.EMPTY;
    }

    public IChatBaseComponent getMessage(int row, boolean filtered) {
        return this.getMessages(filtered)[row];
    }

    public void setMessage(int row, IChatBaseComponent text) {
        this.setMessage(row, text, text);
    }

    public void setMessage(int row, IChatBaseComponent text, IChatBaseComponent filteredText) {
        this.messages[row] = text;
        this.filteredMessages[row] = filteredText;
        this.renderMessages = null;
    }

    public FormattedString[] getRenderMessages(boolean filterText, Function<IChatBaseComponent, FormattedString> textOrderingFunction) {
        if (this.renderMessages == null || this.renderMessagedFiltered != filterText) {
            this.renderMessagedFiltered = filterText;
            this.renderMessages = new FormattedString[4];

            for(int i = 0; i < 4; ++i) {
                this.renderMessages[i] = textOrderingFunction.apply(this.getMessage(i, filterText));
            }
        }

        return this.renderMessages;
    }

    private IChatBaseComponent[] getMessages(boolean filtered) {
        return filtered ? this.filteredMessages : this.messages;
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 9, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    @Override
    public boolean isFilteredNBT() {
        return true;
    }

    public boolean isEditable() {
        return this.isEditable;
    }

    public void setEditable(boolean editable) {
        this.isEditable = editable;
        if (!editable) {
            this.playerWhoMayEdit = null;
        }

    }

    public void setAllowedPlayerEditor(UUID editor) {
        this.playerWhoMayEdit = editor;
    }

    @Nullable
    public UUID getPlayerWhoMayEdit() {
        return this.playerWhoMayEdit;
    }

    public boolean executeClickCommands(EntityPlayer player) {
        for(IChatBaseComponent component : this.getMessages(player.isTextFilteringEnabled())) {
            ChatModifier style = component.getChatModifier();
            ChatClickable clickEvent = style.getClickEvent();
            if (clickEvent != null && clickEvent.getAction() == ChatClickable.EnumClickAction.RUN_COMMAND) {
                player.getMinecraftServer().getCommandDispatcher().performCommand(this.createCommandSourceStack(player), clickEvent.getValue());
            }
        }

        return true;
    }

    public CommandListenerWrapper createCommandSourceStack(@Nullable EntityPlayer player) {
        String string = player == null ? "Sign" : player.getDisplayName().getString();
        IChatBaseComponent component = (IChatBaseComponent)(player == null ? new ChatComponentText("Sign") : player.getScoreboardDisplayName());
        return new CommandListenerWrapper(ICommandListener.NULL, Vec3D.atCenterOf(this.worldPosition), Vec2F.ZERO, (WorldServer)this.level, 2, string, component, this.level.getMinecraftServer(), player);
    }

    public EnumColor getColor() {
        return this.color;
    }

    public boolean setColor(EnumColor value) {
        if (value != this.getColor()) {
            this.color = value;
            this.markUpdated();
            return true;
        } else {
            return false;
        }
    }

    public boolean hasGlowingText() {
        return this.hasGlowingText;
    }

    public boolean setHasGlowingText(boolean glowingText) {
        if (this.hasGlowingText != glowingText) {
            this.hasGlowingText = glowingText;
            this.markUpdated();
            return true;
        } else {
            return false;
        }
    }

    private void markUpdated() {
        this.update();
        this.level.notify(this.getPosition(), this.getBlock(), this.getBlock(), 3);
    }
}
