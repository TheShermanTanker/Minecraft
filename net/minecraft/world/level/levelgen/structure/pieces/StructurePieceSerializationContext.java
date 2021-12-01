package net.minecraft.world.level.levelgen.structure.pieces;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public record StructurePieceSerializationContext(IResourceManager resourceManager, IRegistryCustom registryAccess, DefinedStructureManager structureManager) {
    public StructurePieceSerializationContext(IResourceManager resourceManager, IRegistryCustom registryAccess, DefinedStructureManager structureManager) {
        this.resourceManager = resourceManager;
        this.registryAccess = registryAccess;
        this.structureManager = structureManager;
    }

    public static StructurePieceSerializationContext fromLevel(WorldServer world) {
        MinecraftServer minecraftServer = world.getMinecraftServer();
        return new StructurePieceSerializationContext(minecraftServer.getResourceManager(), minecraftServer.getCustomRegistry(), minecraftServer.getDefinedStructureManager());
    }

    public IResourceManager resourceManager() {
        return this.resourceManager;
    }

    public IRegistryCustom registryAccess() {
        return this.registryAccess;
    }

    public DefinedStructureManager structureManager() {
        return this.structureManager;
    }
}
