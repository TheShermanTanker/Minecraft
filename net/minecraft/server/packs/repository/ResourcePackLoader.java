package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.IResourcePack;
import net.minecraft.server.packs.metadata.pack.ResourcePackInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourcePackLoader implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String id;
    private final Supplier<IResourcePack> supplier;
    private final IChatBaseComponent title;
    private final IChatBaseComponent description;
    private final EnumResourcePackVersion compatibility;
    private final ResourcePackLoader.Position defaultPosition;
    private final boolean required;
    private final boolean fixedPosition;
    private final PackSource packSource;

    @Nullable
    public static ResourcePackLoader create(String name, boolean alwaysEnabled, Supplier<IResourcePack> packFactory, ResourcePackLoader.PackConstructor profileFactory, ResourcePackLoader.Position insertionPosition, PackSource packSource) {
        try {
            IResourcePack packResources = packFactory.get();

            ResourcePackLoader var8;
            label54: {
                try {
                    ResourcePackInfo packMetadataSection = packResources.getMetadataSection(ResourcePackInfo.SERIALIZER);
                    if (packMetadataSection != null) {
                        var8 = profileFactory.create(name, new ChatComponentText(packResources.getName()), alwaysEnabled, packFactory, packMetadataSection, insertionPosition, packSource);
                        break label54;
                    }

                    LOGGER.warn("Couldn't find pack meta for pack {}", (Object)name);
                } catch (Throwable var10) {
                    if (packResources != null) {
                        try {
                            packResources.close();
                        } catch (Throwable var9) {
                            var10.addSuppressed(var9);
                        }
                    }

                    throw var10;
                }

                if (packResources != null) {
                    packResources.close();
                }

                return null;
            }

            if (packResources != null) {
                packResources.close();
            }

            return var8;
        } catch (IOException var11) {
            LOGGER.warn("Couldn't get pack info for: {}", (Object)var11.toString());
            return null;
        }
    }

    public ResourcePackLoader(String name, boolean alwaysEnabled, Supplier<IResourcePack> packFactory, IChatBaseComponent displayName, IChatBaseComponent description, EnumResourcePackVersion compatibility, ResourcePackLoader.Position direction, boolean pinned, PackSource source) {
        this.id = name;
        this.supplier = packFactory;
        this.title = displayName;
        this.description = description;
        this.compatibility = compatibility;
        this.required = alwaysEnabled;
        this.defaultPosition = direction;
        this.fixedPosition = pinned;
        this.packSource = source;
    }

    public ResourcePackLoader(String name, IChatBaseComponent displayName, boolean alwaysEnabled, Supplier<IResourcePack> packFactory, ResourcePackInfo metadata, EnumResourcePackType type, ResourcePackLoader.Position direction, PackSource source) {
        this(name, alwaysEnabled, packFactory, displayName, metadata.getDescription(), EnumResourcePackVersion.forMetadata(metadata, type), direction, false, source);
    }

    public IChatBaseComponent getTitle() {
        return this.title;
    }

    public IChatBaseComponent getDescription() {
        return this.description;
    }

    public IChatBaseComponent getChatLink(boolean enabled) {
        return ChatComponentUtils.wrapInSquareBrackets(this.packSource.decorate(new ChatComponentText(this.id))).format((style) -> {
            return style.setColor(enabled ? EnumChatFormat.GREEN : EnumChatFormat.RED).setInsertion(StringArgumentType.escapeIfRequired(this.id)).setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, (new ChatComponentText("")).addSibling(this.title).append("\n").addSibling(this.description)));
        });
    }

    public EnumResourcePackVersion getCompatibility() {
        return this.compatibility;
    }

    public IResourcePack open() {
        return this.supplier.get();
    }

    public String getId() {
        return this.id;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isFixedPosition() {
        return this.fixedPosition;
    }

    public ResourcePackLoader.Position getDefaultPosition() {
        return this.defaultPosition;
    }

    public PackSource getPackSource() {
        return this.packSource;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ResourcePackLoader)) {
            return false;
        } else {
            ResourcePackLoader pack = (ResourcePackLoader)object;
            return this.id.equals(pack.id);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public void close() {
    }

    @FunctionalInterface
    public interface PackConstructor {
        @Nullable
        ResourcePackLoader create(String name, IChatBaseComponent displayName, boolean alwaysEnabled, Supplier<IResourcePack> packFactory, ResourcePackInfo metadata, ResourcePackLoader.Position initialPosition, PackSource source);
    }

    public static enum Position {
        TOP,
        BOTTOM;

        public <T> int insert(List<T> items, T item, Function<T, ResourcePackLoader> profileGetter, boolean listInverted) {
            ResourcePackLoader.Position position = listInverted ? this.opposite() : this;
            if (position == BOTTOM) {
                int i;
                for(i = 0; i < items.size(); ++i) {
                    ResourcePackLoader pack = profileGetter.apply(items.get(i));
                    if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(i, item);
                return i;
            } else {
                int j;
                for(j = items.size() - 1; j >= 0; --j) {
                    ResourcePackLoader pack2 = profileGetter.apply(items.get(j));
                    if (!pack2.isFixedPosition() || pack2.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(j + 1, item);
                return j + 1;
            }
        }

        public ResourcePackLoader.Position opposite() {
            return this == TOP ? BOTTOM : TOP;
        }
    }
}
