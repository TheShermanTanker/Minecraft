package net.minecraft.server.packs.repository;

import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.ResourcePackVanilla;
import net.minecraft.server.packs.metadata.pack.ResourcePackInfo;

public class ResourcePackSourceVanilla implements ResourcePackSource {
    public static final ResourcePackInfo BUILT_IN_METADATA = new ResourcePackInfo(new ChatMessage("dataPack.vanilla.description"), EnumResourcePackType.SERVER_DATA.getVersion(SharedConstants.getGameVersion()));
    public static final String VANILLA_ID = "vanilla";
    private final ResourcePackVanilla vanillaPack = new ResourcePackVanilla(BUILT_IN_METADATA, "minecraft");

    @Override
    public void loadPacks(Consumer<ResourcePackLoader> profileAdder, ResourcePackLoader.PackConstructor factory) {
        ResourcePackLoader pack = ResourcePackLoader.create("vanilla", false, () -> {
            return this.vanillaPack;
        }, factory, ResourcePackLoader.Position.BOTTOM, PackSource.BUILT_IN);
        if (pack != null) {
            profileAdder.accept(pack);
        }

    }
}
