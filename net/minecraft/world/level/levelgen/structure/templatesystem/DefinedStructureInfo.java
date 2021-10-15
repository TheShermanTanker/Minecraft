package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class DefinedStructureInfo {
    private EnumBlockMirror mirror = EnumBlockMirror.NONE;
    private EnumBlockRotation rotation = EnumBlockRotation.NONE;
    private BlockPosition rotationPivot = BlockPosition.ZERO;
    private boolean ignoreEntities;
    @Nullable
    private StructureBoundingBox boundingBox;
    private boolean keepLiquids = true;
    @Nullable
    private Random random;
    @Nullable
    private int palette;
    private final List<DefinedStructureProcessor> processors = Lists.newArrayList();
    private boolean knownShape;
    private boolean finalizeEntities;

    public DefinedStructureInfo copy() {
        DefinedStructureInfo structurePlaceSettings = new DefinedStructureInfo();
        structurePlaceSettings.mirror = this.mirror;
        structurePlaceSettings.rotation = this.rotation;
        structurePlaceSettings.rotationPivot = this.rotationPivot;
        structurePlaceSettings.ignoreEntities = this.ignoreEntities;
        structurePlaceSettings.boundingBox = this.boundingBox;
        structurePlaceSettings.keepLiquids = this.keepLiquids;
        structurePlaceSettings.random = this.random;
        structurePlaceSettings.palette = this.palette;
        structurePlaceSettings.processors.addAll(this.processors);
        structurePlaceSettings.knownShape = this.knownShape;
        structurePlaceSettings.finalizeEntities = this.finalizeEntities;
        return structurePlaceSettings;
    }

    public DefinedStructureInfo setMirror(EnumBlockMirror mirror) {
        this.mirror = mirror;
        return this;
    }

    public DefinedStructureInfo setRotation(EnumBlockRotation rotation) {
        this.rotation = rotation;
        return this;
    }

    public DefinedStructureInfo setRotationPivot(BlockPosition position) {
        this.rotationPivot = position;
        return this;
    }

    public DefinedStructureInfo setIgnoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
        return this;
    }

    public DefinedStructureInfo setBoundingBox(StructureBoundingBox boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }

    public DefinedStructureInfo setRandom(@Nullable Random random) {
        this.random = random;
        return this;
    }

    public DefinedStructureInfo setKeepLiquids(boolean placeFluids) {
        this.keepLiquids = placeFluids;
        return this;
    }

    public DefinedStructureInfo setKnownShape(boolean updateNeighbors) {
        this.knownShape = updateNeighbors;
        return this;
    }

    public DefinedStructureInfo clearProcessors() {
        this.processors.clear();
        return this;
    }

    public DefinedStructureInfo addProcessor(DefinedStructureProcessor processor) {
        this.processors.add(processor);
        return this;
    }

    public DefinedStructureInfo popProcessor(DefinedStructureProcessor processor) {
        this.processors.remove(processor);
        return this;
    }

    public EnumBlockMirror getMirror() {
        return this.mirror;
    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public BlockPosition getRotationPivot() {
        return this.rotationPivot;
    }

    public Random getRandom(@Nullable BlockPosition pos) {
        if (this.random != null) {
            return this.random;
        } else {
            return pos == null ? new Random(SystemUtils.getMonotonicMillis()) : new Random(MathHelper.getSeed(pos));
        }
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    @Nullable
    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public boolean getKnownShape() {
        return this.knownShape;
    }

    public List<DefinedStructureProcessor> getProcessors() {
        return this.processors;
    }

    public boolean shouldKeepLiquids() {
        return this.keepLiquids;
    }

    public DefinedStructure.Palette getRandomPalette(List<DefinedStructure.Palette> list, @Nullable BlockPosition pos) {
        int i = list.size();
        if (i == 0) {
            throw new IllegalStateException("No palettes");
        } else {
            return list.get(this.getRandom(pos).nextInt(i));
        }
    }

    public DefinedStructureInfo setFinalizeEntities(boolean bl) {
        this.finalizeEntities = bl;
        return this;
    }

    public boolean shouldFinalizeEntities() {
        return this.finalizeEntities;
    }
}
