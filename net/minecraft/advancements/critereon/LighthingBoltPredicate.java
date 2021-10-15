package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.phys.Vec3D;

public class LighthingBoltPredicate {
    public static final LighthingBoltPredicate ANY = new LighthingBoltPredicate(CriterionConditionValue.IntegerRange.ANY, CriterionConditionEntity.ANY);
    private static final String BLOCKS_SET_ON_FIRE_KEY = "blocks_set_on_fire";
    private static final String ENTITY_STRUCK_KEY = "entity_struck";
    private final CriterionConditionValue.IntegerRange blocksSetOnFire;
    private final CriterionConditionEntity entityStruck;

    private LighthingBoltPredicate(CriterionConditionValue.IntegerRange blocksSetOnFire, CriterionConditionEntity entityStruck) {
        this.blocksSetOnFire = blocksSetOnFire;
        this.entityStruck = entityStruck;
    }

    public static LighthingBoltPredicate blockSetOnFire(CriterionConditionValue.IntegerRange blocksSetOnFire) {
        return new LighthingBoltPredicate(blocksSetOnFire, CriterionConditionEntity.ANY);
    }

    public static LighthingBoltPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "lightning");
            return new LighthingBoltPredicate(CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("blocks_set_on_fire")), CriterionConditionEntity.fromJson(jsonObject.get("entity_struck")));
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("blocks_set_on_fire", this.blocksSetOnFire.serializeToJson());
            jsonObject.add("entity_struck", this.entityStruck.serializeToJson());
            return jsonObject;
        }
    }

    public boolean matches(Entity lightningBolt, WorldServer world, @Nullable Vec3D vec3) {
        if (this == ANY) {
            return true;
        } else if (!(lightningBolt instanceof EntityLightning)) {
            return false;
        } else {
            EntityLightning lightningBolt2 = (EntityLightning)lightningBolt;
            return this.blocksSetOnFire.matches(lightningBolt2.getBlocksSetOnFire()) && (this.entityStruck == CriterionConditionEntity.ANY || lightningBolt2.getHitEntities().anyMatch((entity) -> {
                return this.entityStruck.matches(world, vec3, entity);
            }));
        }
    }
}
