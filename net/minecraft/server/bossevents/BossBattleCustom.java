package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.BossBattleServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.BossBattle;

public class BossBattleCustom extends BossBattleServer {
    private final MinecraftKey id;
    private final Set<UUID> players = Sets.newHashSet();
    private int value;
    private int max = 100;

    public BossBattleCustom(MinecraftKey id, IChatBaseComponent displayName) {
        super(displayName, BossBattle.BarColor.WHITE, BossBattle.BarStyle.PROGRESS);
        this.id = id;
        this.setProgress(0.0F);
    }

    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public void addPlayer(EntityPlayer player) {
        super.addPlayer(player);
        this.players.add(player.getUniqueID());
    }

    public void addOfflinePlayer(UUID uuid) {
        this.players.add(uuid);
    }

    @Override
    public void removePlayer(EntityPlayer player) {
        super.removePlayer(player);
        this.players.remove(player.getUniqueID());
    }

    @Override
    public void removeAllPlayers() {
        super.removeAllPlayers();
        this.players.clear();
    }

    public int getValue() {
        return this.value;
    }

    public int getMax() {
        return this.max;
    }

    public void setValue(int value) {
        this.value = value;
        this.setProgress(MathHelper.clamp((float)value / (float)this.max, 0.0F, 1.0F));
    }

    public void setMax(int maxValue) {
        this.max = maxValue;
        this.setProgress(MathHelper.clamp((float)this.value / (float)maxValue, 0.0F, 1.0F));
    }

    public final IChatBaseComponent getDisplayName() {
        return ChatComponentUtils.wrapInSquareBrackets(this.getName()).format((style) -> {
            return style.setColor(this.getColor().getFormatting()).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, new ChatComponentText(this.getKey().toString()))).setInsertion(this.getKey().toString());
        });
    }

    public boolean setPlayers(Collection<EntityPlayer> players) {
        Set<UUID> set = Sets.newHashSet();
        Set<EntityPlayer> set2 = Sets.newHashSet();

        for(UUID uUID : this.players) {
            boolean bl = false;

            for(EntityPlayer serverPlayer : players) {
                if (serverPlayer.getUniqueID().equals(uUID)) {
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                set.add(uUID);
            }
        }

        for(EntityPlayer serverPlayer2 : players) {
            boolean bl2 = false;

            for(UUID uUID2 : this.players) {
                if (serverPlayer2.getUniqueID().equals(uUID2)) {
                    bl2 = true;
                    break;
                }
            }

            if (!bl2) {
                set2.add(serverPlayer2);
            }
        }

        for(UUID uUID3 : set) {
            for(EntityPlayer serverPlayer3 : this.getPlayers()) {
                if (serverPlayer3.getUniqueID().equals(uUID3)) {
                    this.removePlayer(serverPlayer3);
                    break;
                }
            }

            this.players.remove(uUID3);
        }

        for(EntityPlayer serverPlayer4 : set2) {
            this.addPlayer(serverPlayer4);
        }

        return !set.isEmpty() || !set2.isEmpty();
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", IChatBaseComponent.ChatSerializer.toJson(this.name));
        compoundTag.setBoolean("Visible", this.isVisible());
        compoundTag.setInt("Value", this.value);
        compoundTag.setInt("Max", this.max);
        compoundTag.setString("Color", this.getColor().getName());
        compoundTag.setString("Overlay", this.getOverlay().getName());
        compoundTag.setBoolean("DarkenScreen", this.isDarkenSky());
        compoundTag.setBoolean("PlayBossMusic", this.isPlayMusic());
        compoundTag.setBoolean("CreateWorldFog", this.isCreateFog());
        NBTTagList listTag = new NBTTagList();

        for(UUID uUID : this.players) {
            listTag.add(GameProfileSerializer.createUUID(uUID));
        }

        compoundTag.set("Players", listTag);
        return compoundTag;
    }

    public static BossBattleCustom load(NBTTagCompound nbt, MinecraftKey id) {
        BossBattleCustom customBossEvent = new BossBattleCustom(id, IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("Name")));
        customBossEvent.setVisible(nbt.getBoolean("Visible"));
        customBossEvent.setValue(nbt.getInt("Value"));
        customBossEvent.setMax(nbt.getInt("Max"));
        customBossEvent.setColor(BossBattle.BarColor.byName(nbt.getString("Color")));
        customBossEvent.setOverlay(BossBattle.BarStyle.byName(nbt.getString("Overlay")));
        customBossEvent.setDarkenSky(nbt.getBoolean("DarkenScreen"));
        customBossEvent.setPlayMusic(nbt.getBoolean("PlayBossMusic"));
        customBossEvent.setCreateFog(nbt.getBoolean("CreateWorldFog"));
        NBTTagList listTag = nbt.getList("Players", 11);

        for(int i = 0; i < listTag.size(); ++i) {
            customBossEvent.addOfflinePlayer(GameProfileSerializer.loadUUID(listTag.get(i)));
        }

        return customBossEvent;
    }

    public void onPlayerConnect(EntityPlayer player) {
        if (this.players.contains(player.getUniqueID())) {
            this.addPlayer(player);
        }

    }

    public void onPlayerDisconnect(EntityPlayer player) {
        super.removePlayer(player);
    }
}
