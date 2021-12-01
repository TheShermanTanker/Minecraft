package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record PiecesContainer(List<StructurePiece> pieces) {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final MinecraftKey JIGSAW_RENAME = new MinecraftKey("jigsaw");
    private static final Map<MinecraftKey, MinecraftKey> RENAMES = ImmutableMap.<MinecraftKey, MinecraftKey>builder().put(new MinecraftKey("nvi"), JIGSAW_RENAME).put(new MinecraftKey("pcp"), JIGSAW_RENAME).put(new MinecraftKey("bastionremnant"), JIGSAW_RENAME).put(new MinecraftKey("runtime"), JIGSAW_RENAME).build();

    public PiecesContainer(List<StructurePiece> pieces) {
        this.pieces = List.copyOf(pieces);
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public boolean isInsidePiece(BlockPosition pos) {
        for(StructurePiece structurePiece : this.pieces) {
            if (structurePiece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }

        return false;
    }

    public NBTBase save(StructurePieceSerializationContext context) {
        NBTTagList listTag = new NBTTagList();

        for(StructurePiece structurePiece : this.pieces) {
            listTag.add(structurePiece.createTag(context));
        }

        return listTag;
    }

    public static PiecesContainer load(NBTTagList list, StructurePieceSerializationContext context) {
        List<StructurePiece> list2 = Lists.newArrayList();

        for(int i = 0; i < list.size(); ++i) {
            NBTTagCompound compoundTag = list.getCompound(i);
            String string = compoundTag.getString("id").toLowerCase(Locale.ROOT);
            MinecraftKey resourceLocation = new MinecraftKey(string);
            MinecraftKey resourceLocation2 = RENAMES.getOrDefault(resourceLocation, resourceLocation);
            WorldGenFeatureStructurePieceType structurePieceType = IRegistry.STRUCTURE_PIECE.get(resourceLocation2);
            if (structurePieceType == null) {
                LOGGER.error("Unknown structure piece id: {}", (Object)resourceLocation2);
            } else {
                try {
                    StructurePiece structurePiece = structurePieceType.load(context, compoundTag);
                    list2.add(structurePiece);
                } catch (Exception var10) {
                    LOGGER.error("Exception loading structure piece with id {}", resourceLocation2, var10);
                }
            }
        }

        return new PiecesContainer(list2);
    }

    public StructureBoundingBox calculateBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }

    public List<StructurePiece> pieces() {
        return this.pieces;
    }
}
