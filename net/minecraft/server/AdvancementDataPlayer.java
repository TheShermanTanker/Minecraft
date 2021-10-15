package net.minecraft.server;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.protocol.game.PacketPlayOutAdvancements;
import net.minecraft.network.protocol.game.PacketPlayOutSelectAdvancementTab;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementDataPlayer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int VISIBILITY_DEPTH = 2;
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.Serializer()).registerTypeAdapter(MinecraftKey.class, new MinecraftKey.Serializer()).setPrettyPrinting().create();
    private static final TypeToken<Map<MinecraftKey, AdvancementProgress>> TYPE_TOKEN = new TypeToken<Map<MinecraftKey, AdvancementProgress>>() {
    };
    private final DataFixer dataFixer;
    private final PlayerList playerList;
    private final File file;
    public final Map<Advancement, AdvancementProgress> advancements = Maps.newLinkedHashMap();
    private final Set<Advancement> visible = Sets.newLinkedHashSet();
    private final Set<Advancement> visibilityChanged = Sets.newLinkedHashSet();
    private final Set<Advancement> progressChanged = Sets.newLinkedHashSet();
    private EntityPlayer player;
    @Nullable
    private Advancement lastSelectedTab;
    private boolean isFirstPacket = true;

    public AdvancementDataPlayer(DataFixer dataFixer, PlayerList playerManager, AdvancementDataWorld advancementLoader, File advancementFile, EntityPlayer owner) {
        this.dataFixer = dataFixer;
        this.playerList = playerManager;
        this.file = advancementFile;
        this.player = owner;
        this.load(advancementLoader);
    }

    public void setPlayer(EntityPlayer owner) {
        this.player = owner;
    }

    public void stopListening() {
        for(CriterionTrigger<?> criterionTrigger : CriterionTriggers.all()) {
            criterionTrigger.removePlayerListeners(this);
        }

    }

    public void reload(AdvancementDataWorld advancementLoader) {
        this.stopListening();
        this.advancements.clear();
        this.visible.clear();
        this.visibilityChanged.clear();
        this.progressChanged.clear();
        this.isFirstPacket = true;
        this.lastSelectedTab = null;
        this.load(advancementLoader);
    }

    private void registerListeners(AdvancementDataWorld advancementLoader) {
        for(Advancement advancement : advancementLoader.getAdvancements()) {
            this.registerListeners(advancement);
        }

    }

    private void ensureAllVisible() {
        List<Advancement> list = Lists.newArrayList();

        for(Entry<Advancement, AdvancementProgress> entry : this.advancements.entrySet()) {
            if (entry.getValue().isDone()) {
                list.add(entry.getKey());
                this.progressChanged.add(entry.getKey());
            }
        }

        for(Advancement advancement : list) {
            this.ensureVisibility(advancement);
        }

    }

    private void checkForAutomaticTriggers(AdvancementDataWorld advancementLoader) {
        for(Advancement advancement : advancementLoader.getAdvancements()) {
            if (advancement.getCriteria().isEmpty()) {
                this.grantCriteria(advancement, "");
                advancement.getRewards().grant(this.player);
            }
        }

    }

    private void load(AdvancementDataWorld advancementLoader) {
        if (this.file.isFile()) {
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(Files.toString(this.file, StandardCharsets.UTF_8)));

                try {
                    jsonReader.setLenient(false);
                    Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, Streams.parse(jsonReader));
                    if (!dynamic.get("DataVersion").asNumber().result().isPresent()) {
                        dynamic = dynamic.set("DataVersion", dynamic.createInt(1343));
                    }

                    dynamic = this.dataFixer.update(DataFixTypes.ADVANCEMENTS.getType(), dynamic, dynamic.get("DataVersion").asInt(0), SharedConstants.getGameVersion().getWorldVersion());
                    dynamic = dynamic.remove("DataVersion");
                    Map<MinecraftKey, AdvancementProgress> map = GSON.getAdapter(TYPE_TOKEN).fromJsonTree(dynamic.getValue());
                    if (map == null) {
                        throw new JsonParseException("Found null for advancements");
                    }

                    Stream<Entry<MinecraftKey, AdvancementProgress>> stream = map.entrySet().stream().sorted(Comparator.comparing(Entry::getValue));

                    for(Entry<MinecraftKey, AdvancementProgress> entry : stream.collect(Collectors.toList())) {
                        Advancement advancement = advancementLoader.getAdvancement(entry.getKey());
                        if (advancement == null) {
                            LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", entry.getKey(), this.file);
                        } else {
                            this.startProgress(advancement, entry.getValue());
                        }
                    }
                } catch (Throwable var10) {
                    try {
                        jsonReader.close();
                    } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                    }

                    throw var10;
                }

                jsonReader.close();
            } catch (JsonParseException var11) {
                LOGGER.error("Couldn't parse player advancements in {}", this.file, var11);
            } catch (IOException var12) {
                LOGGER.error("Couldn't access player advancements in {}", this.file, var12);
            }
        }

        this.checkForAutomaticTriggers(advancementLoader);
        this.ensureAllVisible();
        this.registerListeners(advancementLoader);
    }

    public void save() {
        Map<MinecraftKey, AdvancementProgress> map = Maps.newHashMap();

        for(Entry<Advancement, AdvancementProgress> entry : this.advancements.entrySet()) {
            AdvancementProgress advancementProgress = entry.getValue();
            if (advancementProgress.hasProgress()) {
                map.put(entry.getKey().getName(), advancementProgress);
            }
        }

        if (this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
        }

        JsonElement jsonElement = GSON.toJsonTree(map);
        jsonElement.getAsJsonObject().addProperty("DataVersion", SharedConstants.getGameVersion().getWorldVersion());

        try {
            OutputStream outputStream = new FileOutputStream(this.file);

            try {
                Writer writer = new OutputStreamWriter(outputStream, Charsets.UTF_8.newEncoder());

                try {
                    GSON.toJson(jsonElement, writer);
                } catch (Throwable var9) {
                    try {
                        writer.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                writer.close();
            } catch (Throwable var10) {
                try {
                    outputStream.close();
                } catch (Throwable var7) {
                    var10.addSuppressed(var7);
                }

                throw var10;
            }

            outputStream.close();
        } catch (IOException var11) {
            LOGGER.error("Couldn't save player advancements to {}", this.file, var11);
        }

    }

    public boolean grantCriteria(Advancement advancement, String criterionName) {
        boolean bl = false;
        AdvancementProgress advancementProgress = this.getProgress(advancement);
        boolean bl2 = advancementProgress.isDone();
        if (advancementProgress.grantProgress(criterionName)) {
            this.unregisterListeners(advancement);
            this.progressChanged.add(advancement);
            bl = true;
            if (!bl2 && advancementProgress.isDone()) {
                advancement.getRewards().grant(this.player);
                if (advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceChat() && this.player.level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
                    this.playerList.sendMessage(new ChatMessage("chat.type.advancement." + advancement.getDisplay().getFrame().getName(), this.player.getScoreboardDisplayName(), advancement.getChatComponent()), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
                }
            }
        }

        if (advancementProgress.isDone()) {
            this.ensureVisibility(advancement);
        }

        return bl;
    }

    public boolean revokeCritera(Advancement advancement, String criterionName) {
        boolean bl = false;
        AdvancementProgress advancementProgress = this.getProgress(advancement);
        if (advancementProgress.revokeProgress(criterionName)) {
            this.registerListeners(advancement);
            this.progressChanged.add(advancement);
            bl = true;
        }

        if (!advancementProgress.hasProgress()) {
            this.ensureVisibility(advancement);
        }

        return bl;
    }

    private void registerListeners(Advancement advancement) {
        AdvancementProgress advancementProgress = this.getProgress(advancement);
        if (!advancementProgress.isDone()) {
            for(Entry<String, Criterion> entry : advancement.getCriteria().entrySet()) {
                CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                if (criterionProgress != null && !criterionProgress.isDone()) {
                    CriterionInstance criterionTriggerInstance = entry.getValue().getTrigger();
                    if (criterionTriggerInstance != null) {
                        CriterionTrigger<CriterionInstance> criterionTrigger = CriterionTriggers.getCriterion(criterionTriggerInstance.getCriterion());
                        if (criterionTrigger != null) {
                            criterionTrigger.addPlayerListener(this, new CriterionTrigger.Listener<>(criterionTriggerInstance, advancement, entry.getKey()));
                        }
                    }
                }
            }

        }
    }

    private void unregisterListeners(Advancement advancement) {
        AdvancementProgress advancementProgress = this.getProgress(advancement);

        for(Entry<String, Criterion> entry : advancement.getCriteria().entrySet()) {
            CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
            if (criterionProgress != null && (criterionProgress.isDone() || advancementProgress.isDone())) {
                CriterionInstance criterionTriggerInstance = entry.getValue().getTrigger();
                if (criterionTriggerInstance != null) {
                    CriterionTrigger<CriterionInstance> criterionTrigger = CriterionTriggers.getCriterion(criterionTriggerInstance.getCriterion());
                    if (criterionTrigger != null) {
                        criterionTrigger.removePlayerListener(this, new CriterionTrigger.Listener<>(criterionTriggerInstance, advancement, entry.getKey()));
                    }
                }
            }
        }

    }

    public void flushDirty(EntityPlayer player) {
        if (this.isFirstPacket || !this.visibilityChanged.isEmpty() || !this.progressChanged.isEmpty()) {
            Map<MinecraftKey, AdvancementProgress> map = Maps.newHashMap();
            Set<Advancement> set = Sets.newLinkedHashSet();
            Set<MinecraftKey> set2 = Sets.newLinkedHashSet();

            for(Advancement advancement : this.progressChanged) {
                if (this.visible.contains(advancement)) {
                    map.put(advancement.getName(), this.advancements.get(advancement));
                }
            }

            for(Advancement advancement2 : this.visibilityChanged) {
                if (this.visible.contains(advancement2)) {
                    set.add(advancement2);
                } else {
                    set2.add(advancement2.getName());
                }
            }

            if (this.isFirstPacket || !map.isEmpty() || !set.isEmpty() || !set2.isEmpty()) {
                player.connection.sendPacket(new PacketPlayOutAdvancements(this.isFirstPacket, set, set2, map));
                this.visibilityChanged.clear();
                this.progressChanged.clear();
            }
        }

        this.isFirstPacket = false;
    }

    public void setSelectedTab(@Nullable Advancement advancement) {
        Advancement advancement2 = this.lastSelectedTab;
        if (advancement != null && advancement.getParent() == null && advancement.getDisplay() != null) {
            this.lastSelectedTab = advancement;
        } else {
            this.lastSelectedTab = null;
        }

        if (advancement2 != this.lastSelectedTab) {
            this.player.connection.sendPacket(new PacketPlayOutSelectAdvancementTab(this.lastSelectedTab == null ? null : this.lastSelectedTab.getName()));
        }

    }

    public AdvancementProgress getProgress(Advancement advancement) {
        AdvancementProgress advancementProgress = this.advancements.get(advancement);
        if (advancementProgress == null) {
            advancementProgress = new AdvancementProgress();
            this.startProgress(advancement, advancementProgress);
        }

        return advancementProgress;
    }

    private void startProgress(Advancement advancement, AdvancementProgress progress) {
        progress.update(advancement.getCriteria(), advancement.getRequirements());
        this.advancements.put(advancement, progress);
    }

    private void ensureVisibility(Advancement advancement) {
        boolean bl = this.shouldBeVisible(advancement);
        boolean bl2 = this.visible.contains(advancement);
        if (bl && !bl2) {
            this.visible.add(advancement);
            this.visibilityChanged.add(advancement);
            if (this.advancements.containsKey(advancement)) {
                this.progressChanged.add(advancement);
            }
        } else if (!bl && bl2) {
            this.visible.remove(advancement);
            this.visibilityChanged.add(advancement);
        }

        if (bl != bl2 && advancement.getParent() != null) {
            this.ensureVisibility(advancement.getParent());
        }

        for(Advancement advancement2 : advancement.getChildren()) {
            this.ensureVisibility(advancement2);
        }

    }

    private boolean shouldBeVisible(Advancement advancement) {
        for(int i = 0; advancement != null && i <= 2; ++i) {
            if (i == 0 && this.hasCompletedChildrenOrSelf(advancement)) {
                return true;
            }

            if (advancement.getDisplay() == null) {
                return false;
            }

            AdvancementProgress advancementProgress = this.getProgress(advancement);
            if (advancementProgress.isDone()) {
                return true;
            }

            if (advancement.getDisplay().isHidden()) {
                return false;
            }

            advancement = advancement.getParent();
        }

        return false;
    }

    private boolean hasCompletedChildrenOrSelf(Advancement advancement) {
        AdvancementProgress advancementProgress = this.getProgress(advancement);
        if (advancementProgress.isDone()) {
            return true;
        } else {
            for(Advancement advancement2 : advancement.getChildren()) {
                if (this.hasCompletedChildrenOrSelf(advancement2)) {
                    return true;
                }
            }

            return false;
        }
    }
}
