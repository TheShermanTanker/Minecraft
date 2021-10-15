package net.minecraft.data.models.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;

public class DelegatedModel implements Supplier<JsonElement> {
    private final MinecraftKey parent;

    public DelegatedModel(MinecraftKey parent) {
        this.parent = parent;
    }

    @Override
    public JsonElement get() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("parent", this.parent.toString());
        return jsonObject;
    }
}
