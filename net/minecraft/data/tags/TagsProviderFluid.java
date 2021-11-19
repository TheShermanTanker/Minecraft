package net.minecraft.data.tags;

import java.nio.file.Path;
import net.minecraft.core.IRegistry;
import net.minecraft.data.DebugReportGenerator;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public class TagsProviderFluid extends TagsProvider<FluidType> {
    public TagsProviderFluid(DebugReportGenerator root) {
        super(root, IRegistry.FLUID);
    }

    @Override
    protected void addTags() {
        this.tag(TagsFluid.WATER).add(FluidTypes.WATER, FluidTypes.FLOWING_WATER);
        this.tag(TagsFluid.LAVA).add(FluidTypes.LAVA, FluidTypes.FLOWING_LAVA);
    }

    @Override
    protected Path getPath(MinecraftKey id) {
        return this.generator.getOutputFolder().resolve("data/" + id.getNamespace() + "/tags/fluids/" + id.getKey() + ".json");
    }

    @Override
    public String getName() {
        return "Fluid Tags";
    }
}
