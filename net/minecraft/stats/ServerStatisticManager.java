package net.minecraft.stats;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutStatistic;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerStatisticManager extends StatisticManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final File file;
    private final Set<Statistic<?>> dirty = Sets.newHashSet();

    public ServerStatisticManager(MinecraftServer server, File file) {
        this.server = server;
        this.file = file;
        if (file.isFile()) {
            try {
                this.parseLocal(server.getDataFixer(), FileUtils.readFileToString(file));
            } catch (IOException var4) {
                LOGGER.error("Couldn't read statistics file {}", file, var4);
            } catch (JsonParseException var5) {
                LOGGER.error("Couldn't parse statistics file {}", file, var5);
            }
        }

    }

    public void save() {
        try {
            FileUtils.writeStringToFile(this.file, this.toJson());
        } catch (IOException var2) {
            LOGGER.error("Couldn't save stats", (Throwable)var2);
        }

    }

    @Override
    public void setStatistic(EntityHuman player, Statistic<?> stat, int value) {
        super.setStatistic(player, stat, value);
        this.dirty.add(stat);
    }

    private Set<Statistic<?>> getDirty() {
        Set<Statistic<?>> set = Sets.newHashSet(this.dirty);
        this.dirty.clear();
        return set;
    }

    public void parseLocal(DataFixer dataFixer, String json) {
        try {
            JsonReader jsonReader = new JsonReader(new StringReader(json));

            label51: {
                try {
                    jsonReader.setLenient(false);
                    JsonElement jsonElement = Streams.parse(jsonReader);
                    if (!jsonElement.isJsonNull()) {
                        NBTTagCompound compoundTag = fromJson(jsonElement.getAsJsonObject());
                        if (!compoundTag.hasKeyOfType("DataVersion", 99)) {
                            compoundTag.setInt("DataVersion", 1343);
                        }

                        compoundTag = GameProfileSerializer.update(dataFixer, DataFixTypes.STATS, compoundTag, compoundTag.getInt("DataVersion"));
                        if (!compoundTag.hasKeyOfType("stats", 10)) {
                            break label51;
                        }

                        NBTTagCompound compoundTag2 = compoundTag.getCompound("stats");
                        Iterator var7 = compoundTag2.getKeys().iterator();

                        while(true) {
                            if (!var7.hasNext()) {
                                break label51;
                            }

                            String string = (String)var7.next();
                            if (compoundTag2.hasKeyOfType(string, 10)) {
                                SystemUtils.ifElse(IRegistry.STAT_TYPE.getOptional(new MinecraftKey(string)), (statType) -> {
                                    NBTTagCompound compoundTag2 = compoundTag2.getCompound(string);

                                    for(String string2 : compoundTag2.getKeys()) {
                                        if (compoundTag2.hasKeyOfType(string2, 99)) {
                                            SystemUtils.ifElse(this.getStat(statType, string2), (stat) -> {
                                                this.stats.put(stat, compoundTag2.getInt(string2));
                                            }, () -> {
                                                LOGGER.warn("Invalid statistic in {}: Don't know what {} is", this.file, string2);
                                            });
                                        } else {
                                            LOGGER.warn("Invalid statistic value in {}: Don't know what {} is for key {}", this.file, compoundTag2.get(string2), string2);
                                        }
                                    }

                                }, () -> {
                                    LOGGER.warn("Invalid statistic type in {}: Don't know what {} is", this.file, string);
                                });
                            }
                        }
                    }

                    LOGGER.error("Unable to parse Stat data from {}", (Object)this.file);
                } catch (Throwable var10) {
                    try {
                        jsonReader.close();
                    } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                    }

                    throw var10;
                }

                jsonReader.close();
                return;
            }

            jsonReader.close();
        } catch (IOException | JsonParseException var11) {
            LOGGER.error("Unable to parse Stat data from {}", this.file, var11);
        }

    }

    private <T> Optional<Statistic<T>> getStat(StatisticWrapper<T> type, String id) {
        return Optional.ofNullable(MinecraftKey.tryParse(id)).flatMap(type.getRegistry()::getOptional).map(type::get);
    }

    private static NBTTagCompound fromJson(JsonObject json) {
        NBTTagCompound compoundTag = new NBTTagCompound();

        for(Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement jsonElement = entry.getValue();
            if (jsonElement.isJsonObject()) {
                compoundTag.set(entry.getKey(), fromJson(jsonElement.getAsJsonObject()));
            } else if (jsonElement.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                if (jsonPrimitive.isNumber()) {
                    compoundTag.setInt(entry.getKey(), jsonPrimitive.getAsInt());
                }
            }
        }

        return compoundTag;
    }

    protected String toJson() {
        Map<StatisticWrapper<?>, JsonObject> map = Maps.newHashMap();

        for(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Statistic<?>> entry : this.stats.object2IntEntrySet()) {
            Statistic<?> stat = entry.getKey();
            map.computeIfAbsent(stat.getWrapper(), (statType) -> {
                return new JsonObject();
            }).addProperty(getKey(stat).toString(), entry.getIntValue());
        }

        JsonObject jsonObject = new JsonObject();

        for(Entry<StatisticWrapper<?>, JsonObject> entry2 : map.entrySet()) {
            jsonObject.add(IRegistry.STAT_TYPE.getKey(entry2.getKey()).toString(), entry2.getValue());
        }

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.add("stats", jsonObject);
        jsonObject2.addProperty("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        return jsonObject2.toString();
    }

    private static <T> MinecraftKey getKey(Statistic<T> stat) {
        return stat.getWrapper().getRegistry().getKey(stat.getValue());
    }

    public void markAllDirty() {
        this.dirty.addAll(this.stats.keySet());
    }

    public void sendStats(EntityPlayer player) {
        Object2IntMap<Statistic<?>> object2IntMap = new Object2IntOpenHashMap<>();

        for(Statistic<?> stat : this.getDirty()) {
            object2IntMap.put(stat, this.getStatisticValue(stat));
        }

        player.connection.sendPacket(new PacketPlayOutStatistic(object2IntMap));
    }
}
