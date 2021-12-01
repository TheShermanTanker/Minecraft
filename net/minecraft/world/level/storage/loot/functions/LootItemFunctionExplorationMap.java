package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.saveddata.maps.MapIcon;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemFunctionExplorationMap extends LootItemFunctionConditional {
    static final Logger LOGGER = LogManager.getLogger();
    public static final StructureGenerator<?> DEFAULT_FEATURE = StructureGenerator.BURIED_TREASURE;
    public static final String DEFAULT_DECORATION_NAME = "mansion";
    public static final MapIcon.Type DEFAULT_DECORATION = MapIcon.Type.MANSION;
    public static final byte DEFAULT_ZOOM = 2;
    public static final int DEFAULT_SEARCH_RADIUS = 50;
    public static final boolean DEFAULT_SKIP_EXISTING = true;
    final StructureGenerator<?> destination;
    final MapIcon.Type mapDecoration;
    final byte zoom;
    final int searchRadius;
    final boolean skipKnownStructures;

    LootItemFunctionExplorationMap(LootItemCondition[] conditions, StructureGenerator<?> destination, MapIcon.Type decoration, byte zoom, int searchRadius, boolean skipExistingChunks) {
        super(conditions);
        this.destination = destination;
        this.mapDecoration = decoration;
        this.zoom = zoom;
        this.searchRadius = searchRadius;
        this.skipKnownStructures = skipExistingChunks;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.EXPLORATION_MAP;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.ORIGIN);
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (!stack.is(Items.MAP)) {
            return stack;
        } else {
            Vec3D vec3 = context.getContextParameter(LootContextParameters.ORIGIN);
            if (vec3 != null) {
                WorldServer serverLevel = context.getWorld();
                BlockPosition blockPos = serverLevel.findNearestMapFeature(this.destination, new BlockPosition(vec3), this.searchRadius, this.skipKnownStructures);
                if (blockPos != null) {
                    ItemStack itemStack = ItemWorldMap.createFilledMapView(serverLevel, blockPos.getX(), blockPos.getZ(), this.zoom, true, true);
                    ItemWorldMap.applySepiaFilter(serverLevel, itemStack);
                    WorldMap.decorateMap(itemStack, blockPos, "+", this.mapDecoration);
                    itemStack.setHoverName(new ChatMessage("filled_map." + this.destination.getFeatureName().toLowerCase(Locale.ROOT)));
                    return itemStack;
                }
            }

            return stack;
        }
    }

    public static LootItemFunctionExplorationMap.Builder makeExplorationMap() {
        return new LootItemFunctionExplorationMap.Builder();
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionExplorationMap.Builder> {
        private StructureGenerator<?> destination = LootItemFunctionExplorationMap.DEFAULT_FEATURE;
        private MapIcon.Type mapDecoration = LootItemFunctionExplorationMap.DEFAULT_DECORATION;
        private byte zoom = 2;
        private int searchRadius = 50;
        private boolean skipKnownStructures = true;

        @Override
        protected LootItemFunctionExplorationMap.Builder getThis() {
            return this;
        }

        public LootItemFunctionExplorationMap.Builder setDestination(StructureGenerator<?> destination) {
            this.destination = destination;
            return this;
        }

        public LootItemFunctionExplorationMap.Builder setMapDecoration(MapIcon.Type decoration) {
            this.mapDecoration = decoration;
            return this;
        }

        public LootItemFunctionExplorationMap.Builder setZoom(byte zoom) {
            this.zoom = zoom;
            return this;
        }

        public LootItemFunctionExplorationMap.Builder setSearchRadius(int searchRadius) {
            this.searchRadius = searchRadius;
            return this;
        }

        public LootItemFunctionExplorationMap.Builder setSkipKnownStructures(boolean skipExistingChunks) {
            this.skipKnownStructures = skipExistingChunks;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionExplorationMap(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionExplorationMap> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionExplorationMap object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.destination.equals(LootItemFunctionExplorationMap.DEFAULT_FEATURE)) {
                json.add("destination", context.serialize(object.destination.getFeatureName()));
            }

            if (object.mapDecoration != LootItemFunctionExplorationMap.DEFAULT_DECORATION) {
                json.add("decoration", context.serialize(object.mapDecoration.toString().toLowerCase(Locale.ROOT)));
            }

            if (object.zoom != 2) {
                json.addProperty("zoom", object.zoom);
            }

            if (object.searchRadius != 50) {
                json.addProperty("search_radius", object.searchRadius);
            }

            if (!object.skipKnownStructures) {
                json.addProperty("skip_existing_chunks", object.skipKnownStructures);
            }

        }

        @Override
        public LootItemFunctionExplorationMap deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            StructureGenerator<?> structureFeature = readStructure(jsonObject);
            String string = jsonObject.has("decoration") ? ChatDeserializer.getAsString(jsonObject, "decoration") : "mansion";
            MapIcon.Type type = LootItemFunctionExplorationMap.DEFAULT_DECORATION;

            try {
                type = MapIcon.Type.valueOf(string.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var10) {
                LootItemFunctionExplorationMap.LOGGER.error("Error while parsing loot table decoration entry. Found {}. Defaulting to {}", string, LootItemFunctionExplorationMap.DEFAULT_DECORATION);
            }

            byte b = ChatDeserializer.getAsByte(jsonObject, "zoom", (byte)2);
            int i = ChatDeserializer.getAsInt(jsonObject, "search_radius", 50);
            boolean bl = ChatDeserializer.getAsBoolean(jsonObject, "skip_existing_chunks", true);
            return new LootItemFunctionExplorationMap(lootItemConditions, structureFeature, type, b, i, bl);
        }

        private static StructureGenerator<?> readStructure(JsonObject json) {
            if (json.has("destination")) {
                String string = ChatDeserializer.getAsString(json, "destination");
                StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string.toLowerCase(Locale.ROOT));
                if (structureFeature != null) {
                    return structureFeature;
                }
            }

            return LootItemFunctionExplorationMap.DEFAULT_FEATURE;
        }
    }
}
