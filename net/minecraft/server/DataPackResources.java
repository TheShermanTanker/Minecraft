package net.minecraft.server;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.IResourcePack;
import net.minecraft.server.packs.resources.IReloadableResourceManager;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.tags.TagRegistry;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.LootTableRegistry;

public class DataPackResources implements AutoCloseable {
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private final IReloadableResourceManager resources = new ResourceManager(EnumResourcePackType.SERVER_DATA);
    public CommandDispatcher commands;
    private final CraftingManager recipes = new CraftingManager();
    private final TagRegistry tagManager;
    private final LootPredicateManager predicateManager = new LootPredicateManager();
    private final LootTableRegistry lootTables = new LootTableRegistry(this.predicateManager);
    private final ItemModifierManager itemModifierManager = new ItemModifierManager(this.predicateManager, this.lootTables);
    private final AdvancementDataWorld advancements = new AdvancementDataWorld(this.predicateManager);
    private final CustomFunctionManager functionLibrary;

    public DataPackResources(IRegistryCustom registryManager, CommandDispatcher.ServerType commandEnvironment, int functionPermissionLevel) {
        this.tagManager = new TagRegistry(registryManager);
        this.commands = new CommandDispatcher(commandEnvironment);
        this.functionLibrary = new CustomFunctionManager(functionPermissionLevel, this.commands.getDispatcher());
        this.resources.registerReloadListener(this.tagManager);
        this.resources.registerReloadListener(this.predicateManager);
        this.resources.registerReloadListener(this.recipes);
        this.resources.registerReloadListener(this.lootTables);
        this.resources.registerReloadListener(this.itemModifierManager);
        this.resources.registerReloadListener(this.functionLibrary);
        this.resources.registerReloadListener(this.advancements);
    }

    public CustomFunctionManager getFunctionLibrary() {
        return this.functionLibrary;
    }

    public LootPredicateManager getPredicateManager() {
        return this.predicateManager;
    }

    public LootTableRegistry getLootTables() {
        return this.lootTables;
    }

    public ItemModifierManager getItemModifierManager() {
        return this.itemModifierManager;
    }

    public ITagRegistry getTags() {
        return this.tagManager.getTags();
    }

    public CraftingManager getRecipeManager() {
        return this.recipes;
    }

    public CommandDispatcher getCommands() {
        return this.commands;
    }

    public AdvancementDataWorld getAdvancements() {
        return this.advancements;
    }

    public IResourceManager getResourceManager() {
        return this.resources;
    }

    public static CompletableFuture<DataPackResources> loadResources(List<IResourcePack> packs, IRegistryCustom registryManager, CommandDispatcher.ServerType commandEnvironment, int functionPermissionLevel, Executor prepareExecutor, Executor applyExecutor) {
        DataPackResources serverResources = new DataPackResources(registryManager, commandEnvironment, functionPermissionLevel);
        CompletableFuture<Unit> completableFuture = serverResources.resources.reload(prepareExecutor, applyExecutor, packs, DATA_RELOAD_INITIAL_TASK);
        return completableFuture.whenComplete((unit, throwable) -> {
            if (throwable != null) {
                serverResources.close();
            }

        }).thenApply((unit) -> {
            return serverResources;
        });
    }

    public void updateGlobals() {
        this.tagManager.getTags().bind();
    }

    @Override
    public void close() {
        this.resources.close();
    }
}
