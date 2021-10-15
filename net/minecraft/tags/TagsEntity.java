package net.minecraft.tags;

import net.minecraft.core.IRegistry;
import net.minecraft.world.entity.EntityTypes;

public final class TagsEntity {
    protected static final TagUtil<EntityTypes<?>> HELPER = TagStatic.create(IRegistry.ENTITY_TYPE_REGISTRY, "tags/entity_types");
    public static final Tag.Named<EntityTypes<?>> SKELETONS = bind("skeletons");
    public static final Tag.Named<EntityTypes<?>> RAIDERS = bind("raiders");
    public static final Tag.Named<EntityTypes<?>> BEEHIVE_INHABITORS = bind("beehive_inhabitors");
    public static final Tag.Named<EntityTypes<?>> ARROWS = bind("arrows");
    public static final Tag.Named<EntityTypes<?>> IMPACT_PROJECTILES = bind("impact_projectiles");
    public static final Tag.Named<EntityTypes<?>> POWDER_SNOW_WALKABLE_MOBS = bind("powder_snow_walkable_mobs");
    public static final Tag.Named<EntityTypes<?>> AXOLOTL_ALWAYS_HOSTILES = bind("axolotl_always_hostiles");
    public static final Tag.Named<EntityTypes<?>> AXOLOTL_HUNT_TARGETS = bind("axolotl_hunt_targets");
    public static final Tag.Named<EntityTypes<?>> FREEZE_IMMUNE_ENTITY_TYPES = bind("freeze_immune_entity_types");
    public static final Tag.Named<EntityTypes<?>> FREEZE_HURTS_EXTRA_TYPES = bind("freeze_hurts_extra_types");

    private TagsEntity() {
    }

    private static Tag.Named<EntityTypes<?>> bind(String id) {
        return HELPER.bind(id);
    }

    public static Tags<EntityTypes<?>> getAllTags() {
        return HELPER.getAllTags();
    }
}
