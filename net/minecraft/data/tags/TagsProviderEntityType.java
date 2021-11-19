package net.minecraft.data.tags;

import java.nio.file.Path;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsEntity;
import net.minecraft.world.entity.EntityTypes;

public class TagsProviderEntityType extends TagsProvider<EntityTypes<?>> {
    public TagsProviderEntityType(DebugReportGenerator root) {
        super(root, IRegistry.ENTITY_TYPE);
    }

    @Override
    protected void addTags() {
        this.tag(TagsEntity.SKELETONS).add(EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.WITHER_SKELETON);
        this.tag(TagsEntity.RAIDERS).add(EntityTypes.EVOKER, EntityTypes.PILLAGER, EntityTypes.RAVAGER, EntityTypes.VINDICATOR, EntityTypes.ILLUSIONER, EntityTypes.WITCH);
        this.tag(TagsEntity.BEEHIVE_INHABITORS).add(EntityTypes.BEE);
        this.tag(TagsEntity.ARROWS).add(EntityTypes.ARROW, EntityTypes.SPECTRAL_ARROW);
        this.tag(TagsEntity.IMPACT_PROJECTILES).addTag(TagsEntity.ARROWS).add(EntityTypes.SNOWBALL, EntityTypes.FIREBALL, EntityTypes.SMALL_FIREBALL, EntityTypes.EGG, EntityTypes.TRIDENT, EntityTypes.DRAGON_FIREBALL, EntityTypes.WITHER_SKULL);
        this.tag(TagsEntity.POWDER_SNOW_WALKABLE_MOBS).add(EntityTypes.RABBIT, EntityTypes.ENDERMITE, EntityTypes.SILVERFISH, EntityTypes.FOX);
        this.tag(TagsEntity.AXOLOTL_HUNT_TARGETS).add(EntityTypes.TROPICAL_FISH, EntityTypes.PUFFERFISH, EntityTypes.SALMON, EntityTypes.COD, EntityTypes.SQUID, EntityTypes.GLOW_SQUID);
        this.tag(TagsEntity.AXOLOTL_ALWAYS_HOSTILES).add(EntityTypes.DROWNED, EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN);
        this.tag(TagsEntity.FREEZE_IMMUNE_ENTITY_TYPES).add(EntityTypes.STRAY, EntityTypes.POLAR_BEAR, EntityTypes.SNOW_GOLEM, EntityTypes.WITHER);
        this.tag(TagsEntity.FREEZE_HURTS_EXTRA_TYPES).add(EntityTypes.STRIDER, EntityTypes.BLAZE, EntityTypes.MAGMA_CUBE);
    }

    @Override
    protected Path getPath(MinecraftKey id) {
        return this.generator.getOutputFolder().resolve("data/" + id.getNamespace() + "/tags/entity_types/" + id.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Entity Type Tags";
    }
}
