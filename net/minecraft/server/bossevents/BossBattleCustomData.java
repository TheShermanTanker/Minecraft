package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;

public class BossBattleCustomData {
    private final Map<MinecraftKey, BossBattleCustom> events = Maps.newHashMap();

    @Nullable
    public BossBattleCustom get(MinecraftKey id) {
        return this.events.get(id);
    }

    public BossBattleCustom register(MinecraftKey id, IChatBaseComponent displayName) {
        BossBattleCustom customBossEvent = new BossBattleCustom(id, displayName);
        this.events.put(id, customBossEvent);
        return customBossEvent;
    }

    public void remove(BossBattleCustom bossBar) {
        this.events.remove(bossBar.getKey());
    }

    public Collection<MinecraftKey> getIds() {
        return this.events.keySet();
    }

    public Collection<BossBattleCustom> getBattles() {
        return this.events.values();
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();

        for(BossBattleCustom customBossEvent : this.events.values()) {
            compoundTag.set(customBossEvent.getKey().toString(), customBossEvent.save());
        }

        return compoundTag;
    }

    public void load(NBTTagCompound nbt) {
        for(String string : nbt.getKeys()) {
            MinecraftKey resourceLocation = new MinecraftKey(string);
            this.events.put(resourceLocation, BossBattleCustom.load(nbt.getCompound(string), resourceLocation));
        }

    }

    public void onPlayerConnect(EntityPlayer player) {
        for(BossBattleCustom customBossEvent : this.events.values()) {
            customBossEvent.onPlayerConnect(player);
        }

    }

    public void onPlayerDisconnect(EntityPlayer player) {
        for(BossBattleCustom customBossEvent : this.events.values()) {
            customBossEvent.onPlayerDisconnect(player);
        }

    }
}
