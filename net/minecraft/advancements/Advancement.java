package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.advancements.critereon.LootDeserializationContext;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;
import org.apache.commons.lang3.ArrayUtils;

public class Advancement {
    private final Advancement parent;
    private final AdvancementDisplay display;
    private final AdvancementRewards rewards;
    private final MinecraftKey id;
    private final Map<String, Criterion> criteria;
    private final String[][] requirements;
    private final Set<Advancement> children = Sets.newLinkedHashSet();
    private final IChatBaseComponent chatComponent;

    public Advancement(MinecraftKey id, @Nullable Advancement parent, @Nullable AdvancementDisplay display, AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements) {
        this.id = id;
        this.display = display;
        this.criteria = ImmutableMap.copyOf(criteria);
        this.parent = parent;
        this.rewards = rewards;
        this.requirements = requirements;
        if (parent != null) {
            parent.addChild(this);
        }

        if (display == null) {
            this.chatComponent = new ChatComponentText(id.toString());
        } else {
            IChatBaseComponent component = display.getTitle();
            EnumChatFormat chatFormatting = display.getFrame().getChatColor();
            IChatBaseComponent component2 = ChatComponentUtils.mergeStyles(component.mutableCopy(), ChatModifier.EMPTY.setColor(chatFormatting)).append("\n").addSibling(display.getDescription());
            IChatBaseComponent component3 = component.mutableCopy().format((style) -> {
                return style.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, component2));
            });
            this.chatComponent = ChatComponentUtils.wrapInSquareBrackets(component3).withStyle(chatFormatting);
        }

    }

    public Advancement.SerializedAdvancement deconstruct() {
        return new Advancement.SerializedAdvancement(this.parent == null ? null : this.parent.getName(), this.display, this.rewards, this.criteria, this.requirements);
    }

    @Nullable
    public Advancement getParent() {
        return this.parent;
    }

    @Nullable
    public AdvancementDisplay getDisplay() {
        return this.display;
    }

    public AdvancementRewards getRewards() {
        return this.rewards;
    }

    @Override
    public String toString() {
        return "SimpleAdvancement{id=" + this.getName() + ", parent=" + (this.parent == null ? "null" : this.parent.getName()) + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
    }

    public Iterable<Advancement> getChildren() {
        return this.children;
    }

    public Map<String, Criterion> getCriteria() {
        return this.criteria;
    }

    public int getMaxCriteraRequired() {
        return this.requirements.length;
    }

    public void addChild(Advancement child) {
        this.children.add(child);
    }

    public MinecraftKey getName() {
        return this.id;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Advancement)) {
            return false;
        } else {
            Advancement advancement = (Advancement)object;
            return this.id.equals(advancement.id);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public String[][] getRequirements() {
        return this.requirements;
    }

    public IChatBaseComponent getChatComponent() {
        return this.chatComponent;
    }

    public static class SerializedAdvancement {
        private MinecraftKey parentId;
        private Advancement parent;
        private AdvancementDisplay display;
        private AdvancementRewards rewards = AdvancementRewards.EMPTY;
        private Map<String, Criterion> criteria = Maps.newLinkedHashMap();
        private String[][] requirements;
        private AdvancementRequirements requirementsStrategy = AdvancementRequirements.AND;

        SerializedAdvancement(@Nullable MinecraftKey parentId, @Nullable AdvancementDisplay display, AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements) {
            this.parentId = parentId;
            this.display = display;
            this.rewards = rewards;
            this.criteria = criteria;
            this.requirements = requirements;
        }

        private SerializedAdvancement() {
        }

        public static Advancement.SerializedAdvancement advancement() {
            return new Advancement.SerializedAdvancement();
        }

        public Advancement.SerializedAdvancement parent(Advancement parent) {
            this.parent = parent;
            return this;
        }

        public Advancement.SerializedAdvancement parent(MinecraftKey parentId) {
            this.parentId = parentId;
            return this;
        }

        public Advancement.SerializedAdvancement display(ItemStack icon, IChatBaseComponent title, IChatBaseComponent description, @Nullable MinecraftKey background, AdvancementFrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
            return this.display(new AdvancementDisplay(icon, title, description, background, frame, showToast, announceToChat, hidden));
        }

        public Advancement.SerializedAdvancement display(IMaterial icon, IChatBaseComponent title, IChatBaseComponent description, @Nullable MinecraftKey background, AdvancementFrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
            return this.display(new AdvancementDisplay(new ItemStack(icon.getItem()), title, description, background, frame, showToast, announceToChat, hidden));
        }

        public Advancement.SerializedAdvancement display(AdvancementDisplay display) {
            this.display = display;
            return this;
        }

        public Advancement.SerializedAdvancement rewards(AdvancementRewards.Builder builder) {
            return this.rewards(builder.build());
        }

        public Advancement.SerializedAdvancement rewards(AdvancementRewards rewards) {
            this.rewards = rewards;
            return this;
        }

        public Advancement.SerializedAdvancement addCriterion(String name, CriterionInstance conditions) {
            return this.addCriterion(name, new Criterion(conditions));
        }

        public Advancement.SerializedAdvancement addCriterion(String name, Criterion criterion) {
            if (this.criteria.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate criterion " + name);
            } else {
                this.criteria.put(name, criterion);
                return this;
            }
        }

        public Advancement.SerializedAdvancement requirements(AdvancementRequirements merger) {
            this.requirementsStrategy = merger;
            return this;
        }

        public Advancement.SerializedAdvancement requirements(String[][] strings) {
            this.requirements = strings;
            return this;
        }

        public boolean canBuild(Function<MinecraftKey, Advancement> parentProvider) {
            if (this.parentId == null) {
                return true;
            } else {
                if (this.parent == null) {
                    this.parent = parentProvider.apply(this.parentId);
                }

                return this.parent != null;
            }
        }

        public Advancement build(MinecraftKey id) {
            if (!this.canBuild((idx) -> {
                return null;
            })) {
                throw new IllegalStateException("Tried to build incomplete advancement!");
            } else {
                if (this.requirements == null) {
                    this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
                }

                return new Advancement(id, this.parent, this.display, this.rewards, this.criteria, this.requirements);
            }
        }

        public Advancement save(Consumer<Advancement> consumer, String id) {
            Advancement advancement = this.build(new MinecraftKey(id));
            consumer.accept(advancement);
            return advancement;
        }

        public JsonObject serializeToJson() {
            if (this.requirements == null) {
                this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
            }

            JsonObject jsonObject = new JsonObject();
            if (this.parent != null) {
                jsonObject.addProperty("parent", this.parent.getName().toString());
            } else if (this.parentId != null) {
                jsonObject.addProperty("parent", this.parentId.toString());
            }

            if (this.display != null) {
                jsonObject.add("display", this.display.serializeToJson());
            }

            jsonObject.add("rewards", this.rewards.serializeToJson());
            JsonObject jsonObject2 = new JsonObject();

            for(Entry<String, Criterion> entry : this.criteria.entrySet()) {
                jsonObject2.add(entry.getKey(), entry.getValue().serializeToJson());
            }

            jsonObject.add("criteria", jsonObject2);
            JsonArray jsonArray = new JsonArray();

            for(String[] strings : this.requirements) {
                JsonArray jsonArray2 = new JsonArray();

                for(String string : strings) {
                    jsonArray2.add(string);
                }

                jsonArray.add(jsonArray2);
            }

            jsonObject.add("requirements", jsonArray);
            return jsonObject;
        }

        public void serializeToNetwork(PacketDataSerializer buf) {
            if (this.parentId == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeResourceLocation(this.parentId);
            }

            if (this.display == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                this.display.serializeToNetwork(buf);
            }

            Criterion.serializeToNetwork(this.criteria, buf);
            buf.writeVarInt(this.requirements.length);

            for(String[] strings : this.requirements) {
                buf.writeVarInt(strings.length);

                for(String string : strings) {
                    buf.writeUtf(string);
                }
            }

        }

        @Override
        public String toString() {
            return "Task Advancement{parentId=" + this.parentId + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
        }

        public static Advancement.SerializedAdvancement fromJson(JsonObject obj, LootDeserializationContext predicateDeserializer) {
            MinecraftKey resourceLocation = obj.has("parent") ? new MinecraftKey(ChatDeserializer.getAsString(obj, "parent")) : null;
            AdvancementDisplay displayInfo = obj.has("display") ? AdvancementDisplay.fromJson(ChatDeserializer.getAsJsonObject(obj, "display")) : null;
            AdvancementRewards advancementRewards = obj.has("rewards") ? AdvancementRewards.deserialize(ChatDeserializer.getAsJsonObject(obj, "rewards")) : AdvancementRewards.EMPTY;
            Map<String, Criterion> map = Criterion.criteriaFromJson(ChatDeserializer.getAsJsonObject(obj, "criteria"), predicateDeserializer);
            if (map.isEmpty()) {
                throw new JsonSyntaxException("Advancement criteria cannot be empty");
            } else {
                JsonArray jsonArray = ChatDeserializer.getAsJsonArray(obj, "requirements", new JsonArray());
                String[][] strings = new String[jsonArray.size()][];

                for(int i = 0; i < jsonArray.size(); ++i) {
                    JsonArray jsonArray2 = ChatDeserializer.convertToJsonArray(jsonArray.get(i), "requirements[" + i + "]");
                    strings[i] = new String[jsonArray2.size()];

                    for(int j = 0; j < jsonArray2.size(); ++j) {
                        strings[i][j] = ChatDeserializer.convertToString(jsonArray2.get(j), "requirements[" + i + "][" + j + "]");
                    }
                }

                if (strings.length == 0) {
                    strings = new String[map.size()][];
                    int k = 0;

                    for(String string : map.keySet()) {
                        strings[k++] = new String[]{string};
                    }
                }

                for(String[] strings2 : strings) {
                    if (strings2.length == 0 && map.isEmpty()) {
                        throw new JsonSyntaxException("Requirement entry cannot be empty");
                    }

                    for(String string2 : strings2) {
                        if (!map.containsKey(string2)) {
                            throw new JsonSyntaxException("Unknown required criterion '" + string2 + "'");
                        }
                    }
                }

                for(String string3 : map.keySet()) {
                    boolean bl = false;

                    for(String[] strings3 : strings) {
                        if (ArrayUtils.contains(strings3, string3)) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        throw new JsonSyntaxException("Criterion '" + string3 + "' isn't a requirement for completion. This isn't supported behaviour, all criteria must be required.");
                    }
                }

                return new Advancement.SerializedAdvancement(resourceLocation, displayInfo, advancementRewards, map, strings);
            }
        }

        public static Advancement.SerializedAdvancement fromNetwork(PacketDataSerializer buf) {
            MinecraftKey resourceLocation = buf.readBoolean() ? buf.readResourceLocation() : null;
            AdvancementDisplay displayInfo = buf.readBoolean() ? AdvancementDisplay.fromNetwork(buf) : null;
            Map<String, Criterion> map = Criterion.criteriaFromNetwork(buf);
            String[][] strings = new String[buf.readVarInt()][];

            for(int i = 0; i < strings.length; ++i) {
                strings[i] = new String[buf.readVarInt()];

                for(int j = 0; j < strings[i].length; ++j) {
                    strings[i][j] = buf.readUtf();
                }
            }

            return new Advancement.SerializedAdvancement(resourceLocation, displayInfo, AdvancementRewards.EMPTY, map, strings);
        }

        public Map<String, Criterion> getCriteria() {
            return this.criteria;
        }
    }
}
