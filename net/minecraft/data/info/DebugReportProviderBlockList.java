package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.data.DebugReportProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class DebugReportProviderBlockList implements DebugReportProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DebugReportGenerator generator;

    public DebugReportProviderBlockList(DebugReportGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        JsonObject jsonObject = new JsonObject();

        for(Block block : IRegistry.BLOCK) {
            MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(block);
            JsonObject jsonObject2 = new JsonObject();
            BlockStateList<Block, IBlockData> stateDefinition = block.getStates();
            if (!stateDefinition.getProperties().isEmpty()) {
                JsonObject jsonObject3 = new JsonObject();

                for(IBlockState<?> property : stateDefinition.getProperties()) {
                    JsonArray jsonArray = new JsonArray();

                    for(Comparable<?> comparable : property.getValues()) {
                        jsonArray.add(SystemUtils.getPropertyName(property, comparable));
                    }

                    jsonObject3.add(property.getName(), jsonArray);
                }

                jsonObject2.add("properties", jsonObject3);
            }

            JsonArray jsonArray2 = new JsonArray();

            for(IBlockData blockState : stateDefinition.getPossibleStates()) {
                JsonObject jsonObject4 = new JsonObject();
                JsonObject jsonObject5 = new JsonObject();

                for(IBlockState<?> property2 : stateDefinition.getProperties()) {
                    jsonObject5.addProperty(property2.getName(), SystemUtils.getPropertyName(property2, blockState.get(property2)));
                }

                if (jsonObject5.size() > 0) {
                    jsonObject4.add("properties", jsonObject5);
                }

                jsonObject4.addProperty("id", Block.getCombinedId(blockState));
                if (blockState == block.getBlockData()) {
                    jsonObject4.addProperty("default", true);
                }

                jsonArray2.add(jsonObject4);
            }

            jsonObject2.add("states", jsonArray2);
            jsonObject.add(resourceLocation.toString(), jsonObject2);
        }

        Path path = this.generator.getOutputFolder().resolve("reports/blocks.json");
        DebugReportProvider.save(GSON, cache, jsonObject, path);
    }

    @Override
    public String getName() {
        return "Block List";
    }
}
