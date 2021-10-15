package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV100 extends Schema {
    public DataConverterSchemaV100(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static TypeTemplate equipment(Schema schema) {
        return DSL.optionalFields("ArmorItems", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "HandItems", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)));
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String entityId) {
        schema.register(map, entityId, () -> {
            return equipment(schema);
        });
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerMob(schema, map, "ArmorStand");
        registerMob(schema, map, "Creeper");
        registerMob(schema, map, "Skeleton");
        registerMob(schema, map, "Spider");
        registerMob(schema, map, "Giant");
        registerMob(schema, map, "Zombie");
        registerMob(schema, map, "Slime");
        registerMob(schema, map, "Ghast");
        registerMob(schema, map, "PigZombie");
        schema.register(map, "Enderman", (string) -> {
            return DSL.optionalFields("carried", DataConverterTypes.BLOCK_NAME.in(schema), equipment(schema));
        });
        registerMob(schema, map, "CaveSpider");
        registerMob(schema, map, "Silverfish");
        registerMob(schema, map, "Blaze");
        registerMob(schema, map, "LavaSlime");
        registerMob(schema, map, "EnderDragon");
        registerMob(schema, map, "WitherBoss");
        registerMob(schema, map, "Bat");
        registerMob(schema, map, "Witch");
        registerMob(schema, map, "Endermite");
        registerMob(schema, map, "Guardian");
        registerMob(schema, map, "Pig");
        registerMob(schema, map, "Sheep");
        registerMob(schema, map, "Cow");
        registerMob(schema, map, "Chicken");
        registerMob(schema, map, "Squid");
        registerMob(schema, map, "Wolf");
        registerMob(schema, map, "MushroomCow");
        registerMob(schema, map, "SnowMan");
        registerMob(schema, map, "Ozelot");
        registerMob(schema, map, "VillagerGolem");
        schema.register(map, "EntityHorse", (string) -> {
            return DSL.optionalFields("Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "ArmorItem", DataConverterTypes.ITEM_STACK.in(schema), "SaddleItem", DataConverterTypes.ITEM_STACK.in(schema), equipment(schema));
        });
        registerMob(schema, map, "Rabbit");
        schema.register(map, "Villager", (string) -> {
            return DSL.optionalFields("Inventory", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "Offers", DSL.optionalFields("Recipes", DSL.list(DSL.optionalFields("buy", DataConverterTypes.ITEM_STACK.in(schema), "buyB", DataConverterTypes.ITEM_STACK.in(schema), "sell", DataConverterTypes.ITEM_STACK.in(schema)))), equipment(schema));
        });
        registerMob(schema, map, "Shulker");
        schema.registerSimple(map, "AreaEffectCloud");
        schema.registerSimple(map, "ShulkerBullet");
        return map;
    }

    @Override
    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(false, DataConverterTypes.STRUCTURE, () -> {
            return DSL.optionalFields("entities", DSL.list(DSL.optionalFields("nbt", DataConverterTypes.ENTITY_TREE.in(schema))), "blocks", DSL.list(DSL.optionalFields("nbt", DataConverterTypes.BLOCK_ENTITY.in(schema))), "palette", DSL.list(DataConverterTypes.BLOCK_STATE.in(schema)));
        });
        schema.registerType(false, DataConverterTypes.BLOCK_STATE, DSL::remainder);
    }
}
