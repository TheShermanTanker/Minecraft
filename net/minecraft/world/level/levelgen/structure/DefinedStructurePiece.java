package net.minecraft.world.level.levelgen.structure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class DefinedStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final String templateName;
    protected DefinedStructure template;
    protected DefinedStructureInfo placeSettings;
    protected BlockPosition templatePosition;

    public DefinedStructurePiece(WorldGenFeatureStructurePieceType type, int length, DefinedStructureManager structureManager, MinecraftKey id, String template, DefinedStructureInfo placementData, BlockPosition pos) {
        super(type, length, structureManager.getOrCreate(id).getBoundingBox(placementData, pos));
        this.setOrientation(EnumDirection.NORTH);
        this.templateName = template;
        this.templatePosition = pos;
        this.template = structureManager.getOrCreate(id);
        this.placeSettings = placementData;
    }

    public DefinedStructurePiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt, DefinedStructureManager structureManager, Function<MinecraftKey, DefinedStructureInfo> placementDataGetter) {
        super(type, nbt);
        this.setOrientation(EnumDirection.NORTH);
        this.templateName = nbt.getString("Template");
        this.templatePosition = new BlockPosition(nbt.getInt("TPX"), nbt.getInt("TPY"), nbt.getInt("TPZ"));
        MinecraftKey resourceLocation = this.makeTemplateLocation();
        this.template = structureManager.getOrCreate(resourceLocation);
        this.placeSettings = placementDataGetter.apply(resourceLocation);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
    }

    protected MinecraftKey makeTemplateLocation() {
        return new MinecraftKey(this.templateName);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        nbt.setInt("TPX", this.templatePosition.getX());
        nbt.setInt("TPY", this.templatePosition.getY());
        nbt.setInt("TPZ", this.templatePosition.getZ());
        nbt.setString("Template", this.templateName);
    }

    @Override
    public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        this.placeSettings.setBoundingBox(chunkBox);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (this.template.placeInWorld(world, this.templatePosition, pos, this.placeSettings, random, 2)) {
            for(DefinedStructure.BlockInfo structureBlockInfo : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.STRUCTURE_BLOCK)) {
                if (structureBlockInfo.nbt != null) {
                    BlockPropertyStructureMode structureMode = BlockPropertyStructureMode.valueOf(structureBlockInfo.nbt.getString("mode"));
                    if (structureMode == BlockPropertyStructureMode.DATA) {
                        this.handleDataMarker(structureBlockInfo.nbt.getString("metadata"), structureBlockInfo.pos, world, random, chunkBox);
                    }
                }
            }

            for(DefinedStructure.BlockInfo structureBlockInfo2 : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW)) {
                if (structureBlockInfo2.nbt != null) {
                    String string = structureBlockInfo2.nbt.getString("final_state");
                    ArgumentBlock blockStateParser = new ArgumentBlock(new StringReader(string), false);
                    IBlockData blockState = Blocks.AIR.getBlockData();

                    try {
                        blockStateParser.parse(true);
                        IBlockData blockState2 = blockStateParser.getBlockData();
                        if (blockState2 != null) {
                            blockState = blockState2;
                        } else {
                            LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", string, structureBlockInfo2.pos);
                        }
                    } catch (CommandSyntaxException var16) {
                        LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", string, structureBlockInfo2.pos);
                    }

                    world.setTypeAndData(structureBlockInfo2.pos, blockState, 3);
                }
            }
        }

    }

    protected abstract void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox);

    /** @deprecated */
    @Deprecated
    @Override
    public void move(int x, int y, int z) {
        super.move(x, y, z);
        this.templatePosition = this.templatePosition.offset(x, y, z);
    }

    @Override
    public EnumBlockRotation getRotation() {
        return this.placeSettings.getRotation();
    }
}
