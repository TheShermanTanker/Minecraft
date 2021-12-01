package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.CriterionConditionNBT;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentNBTKey;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.commands.arguments.coordinates.IVectorPosition;
import net.minecraft.commands.arguments.selector.ArgumentParserSelector;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ChatComponentNBT extends ChatBaseComponent implements ChatComponentContextual {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final boolean interpreting;
    protected final Optional<IChatBaseComponent> separator;
    protected final String nbtPathPattern;
    @Nullable
    protected final ArgumentNBTKey.NbtPath compiledNbtPath;

    @Nullable
    private static ArgumentNBTKey.NbtPath compileNbtPath(String rawPath) {
        try {
            return (new ArgumentNBTKey()).parse(new StringReader(rawPath));
        } catch (CommandSyntaxException var2) {
            return null;
        }
    }

    public ChatComponentNBT(String rawPath, boolean interpret, Optional<IChatBaseComponent> separator) {
        this(rawPath, compileNbtPath(rawPath), interpret, separator);
    }

    protected ChatComponentNBT(String rawPath, @Nullable ArgumentNBTKey.NbtPath path, boolean interpret, Optional<IChatBaseComponent> separator) {
        this.nbtPathPattern = rawPath;
        this.compiledNbtPath = path;
        this.interpreting = interpret;
        this.separator = separator;
    }

    protected abstract Stream<NBTTagCompound> getData(CommandListenerWrapper source) throws CommandSyntaxException;

    public String getNbtPath() {
        return this.nbtPathPattern;
    }

    public boolean isInterpreting() {
        return this.interpreting;
    }

    @Override
    public IChatMutableComponent resolve(@Nullable CommandListenerWrapper source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (source != null && this.compiledNbtPath != null) {
            Stream<String> stream = this.getData(source).flatMap((nbt) -> {
                try {
                    return this.compiledNbtPath.get(nbt).stream();
                } catch (CommandSyntaxException var3) {
                    return Stream.empty();
                }
            }).map(NBTBase::asString);
            if (this.interpreting) {
                IChatBaseComponent component = DataFixUtils.orElse(ChatComponentUtils.updateForEntity(source, this.separator, sender, depth), ChatComponentUtils.DEFAULT_NO_STYLE_SEPARATOR);
                return stream.flatMap((text) -> {
                    try {
                        IChatMutableComponent mutableComponent = IChatBaseComponent.ChatSerializer.fromJson(text);
                        return Stream.of(ChatComponentUtils.filterForDisplay(source, mutableComponent, sender, depth));
                    } catch (Exception var5) {
                        LOGGER.warn("Failed to parse component: {}", text, var5);
                        return Stream.of();
                    }
                }).reduce((accumulator, current) -> {
                    return accumulator.addSibling(component).addSibling(current);
                }).orElseGet(() -> {
                    return new ChatComponentText("");
                });
            } else {
                return ChatComponentUtils.updateForEntity(source, this.separator, sender, depth).map((text) -> {
                    return stream.map((string) -> {
                        return new ChatComponentText(string);
                    }).reduce((accumulator, current) -> {
                        return accumulator.addSibling(text).addSibling(current);
                    }).orElseGet(() -> {
                        return new ChatComponentText("");
                    });
                }).orElseGet(() -> {
                    return new ChatComponentText(stream.collect(Collectors.joining(", ")));
                });
            }
        } else {
            return new ChatComponentText("");
        }
    }

    public static class BlockNbtComponent extends ChatComponentNBT {
        private final String posPattern;
        @Nullable
        private final IVectorPosition compiledPos;

        public BlockNbtComponent(String rawPath, boolean rawJson, String rawPos, Optional<IChatBaseComponent> separator) {
            super(rawPath, rawJson, separator);
            this.posPattern = rawPos;
            this.compiledPos = this.compilePos(this.posPattern);
        }

        @Nullable
        private IVectorPosition compilePos(String rawPos) {
            try {
                return ArgumentPosition.blockPos().parse(new StringReader(rawPos));
            } catch (CommandSyntaxException var3) {
                return null;
            }
        }

        private BlockNbtComponent(String rawPath, @Nullable ArgumentNBTKey.NbtPath path, boolean interpret, String rawPos, @Nullable IVectorPosition pos, Optional<IChatBaseComponent> separator) {
            super(rawPath, path, interpret, separator);
            this.posPattern = rawPos;
            this.compiledPos = pos;
        }

        @Nullable
        public String getPos() {
            return this.posPattern;
        }

        @Override
        public ChatComponentNBT.BlockNbtComponent plainCopy() {
            return new ChatComponentNBT.BlockNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.posPattern, this.compiledPos, this.separator);
        }

        @Override
        protected Stream<NBTTagCompound> getData(CommandListenerWrapper source) {
            if (this.compiledPos != null) {
                WorldServer serverLevel = source.getWorld();
                BlockPosition blockPos = this.compiledPos.getBlockPos(source);
                if (serverLevel.isLoaded(blockPos)) {
                    TileEntity blockEntity = serverLevel.getTileEntity(blockPos);
                    if (blockEntity != null) {
                        return Stream.of(blockEntity.saveWithFullMetadata());
                    }
                }
            }

            return Stream.empty();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof ChatComponentNBT.BlockNbtComponent)) {
                return false;
            } else {
                ChatComponentNBT.BlockNbtComponent blockNbtComponent = (ChatComponentNBT.BlockNbtComponent)object;
                return Objects.equals(this.posPattern, blockNbtComponent.posPattern) && Objects.equals(this.nbtPathPattern, blockNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "BlockPosArgument{pos='" + this.posPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
        }
    }

    public static class EntityNbtComponent extends ChatComponentNBT {
        private final String selectorPattern;
        @Nullable
        private final EntitySelector compiledSelector;

        public EntityNbtComponent(String rawPath, boolean interpret, String rawSelector, Optional<IChatBaseComponent> separator) {
            super(rawPath, interpret, separator);
            this.selectorPattern = rawSelector;
            this.compiledSelector = compileSelector(rawSelector);
        }

        @Nullable
        private static EntitySelector compileSelector(String rawSelector) {
            try {
                ArgumentParserSelector entitySelectorParser = new ArgumentParserSelector(new StringReader(rawSelector));
                return entitySelectorParser.parse();
            } catch (CommandSyntaxException var2) {
                return null;
            }
        }

        private EntityNbtComponent(String rawPath, @Nullable ArgumentNBTKey.NbtPath path, boolean interpret, String rawSelector, @Nullable EntitySelector selector, Optional<IChatBaseComponent> separator) {
            super(rawPath, path, interpret, separator);
            this.selectorPattern = rawSelector;
            this.compiledSelector = selector;
        }

        public String getSelector() {
            return this.selectorPattern;
        }

        @Override
        public ChatComponentNBT.EntityNbtComponent plainCopy() {
            return new ChatComponentNBT.EntityNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.selectorPattern, this.compiledSelector, this.separator);
        }

        @Override
        protected Stream<NBTTagCompound> getData(CommandListenerWrapper source) throws CommandSyntaxException {
            if (this.compiledSelector != null) {
                List<? extends Entity> list = this.compiledSelector.getEntities(source);
                return list.stream().map(CriterionConditionNBT::getEntityTagToCompare);
            } else {
                return Stream.empty();
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof ChatComponentNBT.EntityNbtComponent)) {
                return false;
            } else {
                ChatComponentNBT.EntityNbtComponent entityNbtComponent = (ChatComponentNBT.EntityNbtComponent)object;
                return Objects.equals(this.selectorPattern, entityNbtComponent.selectorPattern) && Objects.equals(this.nbtPathPattern, entityNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "EntityNbtComponent{selector='" + this.selectorPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
        }
    }

    public static class StorageNbtComponent extends ChatComponentNBT {
        private final MinecraftKey id;

        public StorageNbtComponent(String rawPath, boolean interpret, MinecraftKey id, Optional<IChatBaseComponent> separator) {
            super(rawPath, interpret, separator);
            this.id = id;
        }

        public StorageNbtComponent(String rawPath, @Nullable ArgumentNBTKey.NbtPath path, boolean interpret, MinecraftKey id, Optional<IChatBaseComponent> separator) {
            super(rawPath, path, interpret, separator);
            this.id = id;
        }

        public MinecraftKey getId() {
            return this.id;
        }

        @Override
        public ChatComponentNBT.StorageNbtComponent plainCopy() {
            return new ChatComponentNBT.StorageNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.id, this.separator);
        }

        @Override
        protected Stream<NBTTagCompound> getData(CommandListenerWrapper source) {
            NBTTagCompound compoundTag = source.getServer().getCommandStorage().get(this.id);
            return Stream.of(compoundTag);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof ChatComponentNBT.StorageNbtComponent)) {
                return false;
            } else {
                ChatComponentNBT.StorageNbtComponent storageNbtComponent = (ChatComponentNBT.StorageNbtComponent)object;
                return Objects.equals(this.id, storageNbtComponent.id) && Objects.equals(this.nbtPathPattern, storageNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "StorageNbtComponent{id='" + this.id + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getChatModifier() + "}";
        }
    }
}
