package net.minecraft.data.structures;

import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureUpdater implements DebugReportProviderStructureToNBT.Filter {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public NBTTagCompound apply(String name, NBTTagCompound nbt) {
        return name.startsWith("data/minecraft/structures/") ? update(name, nbt) : nbt;
    }

    public static NBTTagCompound update(String name, NBTTagCompound nbt) {
        return updateStructure(name, patchVersion(nbt));
    }

    private static NBTTagCompound patchVersion(NBTTagCompound nbt) {
        if (!nbt.hasKeyOfType("DataVersion", 99)) {
            nbt.setInt("DataVersion", 500);
        }

        return nbt;
    }

    private static NBTTagCompound updateStructure(String name, NBTTagCompound nbt) {
        DefinedStructure structureTemplate = new DefinedStructure();
        int i = nbt.getInt("DataVersion");
        int j = 2678;
        if (i < 2678) {
            LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", i, 2678, name);
        }

        NBTTagCompound compoundTag = GameProfileSerializer.update(DataConverterRegistry.getDataFixer(), DataFixTypes.STRUCTURE, nbt, i);
        structureTemplate.load(compoundTag);
        return structureTemplate.save(new NBTTagCompound());
    }
}
