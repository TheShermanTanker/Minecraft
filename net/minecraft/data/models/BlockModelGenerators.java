package net.minecraft.data.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPropertyJigsawOrientation;
import net.minecraft.core.EnumDirection;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemMonsterEgg;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCauldronLayered;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyAttachPosition;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.level.block.state.properties.BlockPropertyBellAttach;
import net.minecraft.world.level.block.state.properties.BlockPropertyComparatorMode;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoorHinge;
import net.minecraft.world.level.block.state.properties.BlockPropertyDoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockPropertyHalf;
import net.minecraft.world.level.block.state.properties.BlockPropertyPistonType;
import net.minecraft.world.level.block.state.properties.BlockPropertyRedstoneSide;
import net.minecraft.world.level.block.state.properties.BlockPropertySlabType;
import net.minecraft.world.level.block.state.properties.BlockPropertyStairsShape;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.block.state.properties.BlockPropertyWallHeight;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.Tilt;

public class BlockModelGenerators {
    final Consumer<BlockStateGenerator> blockStateOutput;
    final BiConsumer<MinecraftKey, Supplier<JsonElement>> modelOutput;
    private final Consumer<Item> skippedAutoModelsOutput;
    final List<Block> nonOrientableTrapdoor = ImmutableList.of(Blocks.OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
    final Map<Block, BlockModelGenerators.BlockStateGeneratorSupplier> fullBlockModelCustomGenerators = ImmutableMap.<Block, BlockModelGenerators.BlockStateGeneratorSupplier>builder().put(Blocks.STONE, BlockModelGenerators::createMirroredCubeGenerator).put(Blocks.DEEPSLATE, BlockModelGenerators::createMirroredColumnGenerator).build();
    final Map<Block, TexturedModel> texturedModels = ImmutableMap.<Block, TexturedModel>builder().put(Blocks.SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.SANDSTONE)).put(Blocks.RED_SANDSTONE, TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.RED_SANDSTONE)).put(Blocks.SMOOTH_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"))).put(Blocks.SMOOTH_RED_SANDSTONE, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"))).put(Blocks.CUT_SANDSTONE, TexturedModel.COLUMN.get(Blocks.SANDSTONE).updateTextures((textureMapping) -> {
        textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_SANDSTONE));
    })).put(Blocks.CUT_RED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.RED_SANDSTONE).updateTextures((textureMapping) -> {
        textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_RED_SANDSTONE));
    })).put(Blocks.QUARTZ_BLOCK, TexturedModel.COLUMN.get(Blocks.QUARTZ_BLOCK)).put(Blocks.SMOOTH_QUARTZ, TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom"))).put(Blocks.BLACKSTONE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.BLACKSTONE)).put(Blocks.DEEPSLATE, TexturedModel.COLUMN_WITH_WALL.get(Blocks.DEEPSLATE)).put(Blocks.CHISELED_QUARTZ_BLOCK, TexturedModel.COLUMN.get(Blocks.CHISELED_QUARTZ_BLOCK).updateTextures((textureMapping) -> {
        textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_QUARTZ_BLOCK));
    })).put(Blocks.CHISELED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_SANDSTONE).updateTextures((textureMapping) -> {
        textureMapping.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"));
        textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_SANDSTONE));
    })).put(Blocks.CHISELED_RED_SANDSTONE, TexturedModel.COLUMN.get(Blocks.CHISELED_RED_SANDSTONE).updateTextures((textureMapping) -> {
        textureMapping.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"));
        textureMapping.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_RED_SANDSTONE));
    })).build();
    static final Map<BlockFamily.Variant, BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block>> SHAPE_CONSUMERS = ImmutableMap.<BlockFamily.Variant, BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block>>builder().put(BlockFamily.Variant.BUTTON, BlockModelGenerators.BlockFamilyProvider::button).put(BlockFamily.Variant.DOOR, BlockModelGenerators.BlockFamilyProvider::door).put(BlockFamily.Variant.CHISELED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant).put(BlockFamily.Variant.CRACKED, BlockModelGenerators.BlockFamilyProvider::fullBlockVariant).put(BlockFamily.Variant.FENCE, BlockModelGenerators.BlockFamilyProvider::fence).put(BlockFamily.Variant.FENCE_GATE, BlockModelGenerators.BlockFamilyProvider::fenceGate).put(BlockFamily.Variant.SIGN, BlockModelGenerators.BlockFamilyProvider::sign).put(BlockFamily.Variant.SLAB, BlockModelGenerators.BlockFamilyProvider::slab).put(BlockFamily.Variant.STAIRS, BlockModelGenerators.BlockFamilyProvider::stairs).put(BlockFamily.Variant.PRESSURE_PLATE, BlockModelGenerators.BlockFamilyProvider::pressurePlate).put(BlockFamily.Variant.TRAPDOOR, BlockModelGenerators.BlockFamilyProvider::trapdoor).put(BlockFamily.Variant.WALL, BlockModelGenerators.BlockFamilyProvider::wall).build();
    public static final Map<BlockStateBoolean, Function<MinecraftKey, Variant>> MULTIFACE_GENERATOR = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put(BlockProperties.NORTH, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation);
        });
        hashMap.put(BlockProperties.EAST, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true);
        });
        hashMap.put(BlockProperties.SOUTH, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true);
        });
        hashMap.put(BlockProperties.WEST, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true);
        });
        hashMap.put(BlockProperties.UP, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true);
        });
        hashMap.put(BlockProperties.DOWN, (resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true);
        });
    });

    private static BlockStateGenerator createMirroredCubeGenerator(Block block, MinecraftKey modelId, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        MinecraftKey resourceLocation = ModelTemplates.CUBE_MIRRORED_ALL.create(block, texture, modelCollector);
        return createRotatedVariant(block, modelId, resourceLocation);
    }

    private static BlockStateGenerator createMirroredColumnGenerator(Block block, MinecraftKey modelId, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        MinecraftKey resourceLocation = ModelTemplates.CUBE_COLUMN_MIRRORED.create(block, texture, modelCollector);
        return createRotatedVariant(block, modelId, resourceLocation).with(createRotatedPillar());
    }

    public BlockModelGenerators(Consumer<BlockStateGenerator> blockStateCollector, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector, Consumer<Item> simpleItemModelExemptionCollector) {
        this.blockStateOutput = blockStateCollector;
        this.modelOutput = modelCollector;
        this.skippedAutoModelsOutput = simpleItemModelExemptionCollector;
    }

    void skipAutoItemBlock(Block block) {
        this.skippedAutoModelsOutput.accept(block.getItem());
    }

    void delegateItemModel(Block block, MinecraftKey parentModelId) {
        this.modelOutput.accept(ModelLocationUtils.getModelLocation(block.getItem()), new DelegatedModel(parentModelId));
    }

    private void delegateItemModel(Item item, MinecraftKey parentModelId) {
        this.modelOutput.accept(ModelLocationUtils.getModelLocation(item), new DelegatedModel(parentModelId));
    }

    void createSimpleFlatItemModel(Item item) {
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), this.modelOutput);
    }

    private void createSimpleFlatItemModel(Block block) {
        Item item = block.getItem();
        if (item != Items.AIR) {
            ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(block), this.modelOutput);
        }

    }

    private void createSimpleFlatItemModel(Block block, String textureSuffix) {
        Item item = block.getItem();
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(TextureMapping.getBlockTexture(block, textureSuffix)), this.modelOutput);
    }

    private static PropertyDispatch createHorizontalFacingDispatch() {
        return PropertyDispatch.property(BlockProperties.HORIZONTAL_FACING).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, Variant.variant());
    }

    private static PropertyDispatch createHorizontalFacingDispatchAlt() {
        return PropertyDispatch.property(BlockProperties.HORIZONTAL_FACING).select(EnumDirection.SOUTH, Variant.variant()).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
    }

    private static PropertyDispatch createTorchHorizontalDispatch() {
        return PropertyDispatch.property(BlockProperties.HORIZONTAL_FACING).select(EnumDirection.EAST, Variant.variant()).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270));
    }

    private static PropertyDispatch createFacingDispatch() {
        return PropertyDispatch.property(BlockProperties.FACING).select(EnumDirection.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.UP, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, Variant.variant()).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90));
    }

    private static MultiVariantGenerator createRotatedVariant(Block block, MinecraftKey modelId) {
        return MultiVariantGenerator.multiVariant(block, createRotatedVariants(modelId));
    }

    private static Variant[] createRotatedVariants(MinecraftKey modelId) {
        return new Variant[]{Variant.variant().with(VariantProperties.MODEL, modelId), Variant.variant().with(VariantProperties.MODEL, modelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, modelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, modelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)};
    }

    private static MultiVariantGenerator createRotatedVariant(Block block, MinecraftKey firstModelId, MinecraftKey secondModelId) {
        return MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, firstModelId), Variant.variant().with(VariantProperties.MODEL, secondModelId), Variant.variant().with(VariantProperties.MODEL, firstModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, secondModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180));
    }

    private static PropertyDispatch createBooleanModelDispatch(BlockStateBoolean property, MinecraftKey trueModel, MinecraftKey falseModel) {
        return PropertyDispatch.property(property).select(true, Variant.variant().with(VariantProperties.MODEL, trueModel)).select(false, Variant.variant().with(VariantProperties.MODEL, falseModel));
    }

    private void createRotatedMirroredVariantBlock(Block block) {
        MinecraftKey resourceLocation = TexturedModel.CUBE.create(block, this.modelOutput);
        MinecraftKey resourceLocation2 = TexturedModel.CUBE_MIRRORED.create(block, this.modelOutput);
        this.blockStateOutput.accept(createRotatedVariant(block, resourceLocation, resourceLocation2));
    }

    private void createRotatedVariantBlock(Block block) {
        MinecraftKey resourceLocation = TexturedModel.CUBE.create(block, this.modelOutput);
        this.blockStateOutput.accept(createRotatedVariant(block, resourceLocation));
    }

    static BlockStateGenerator createButton(Block buttonBlock, MinecraftKey regularModelId, MinecraftKey pressedModelId) {
        return MultiVariantGenerator.multiVariant(buttonBlock).with(PropertyDispatch.property(BlockProperties.POWERED).select(false, Variant.variant().with(VariantProperties.MODEL, regularModelId)).select(true, Variant.variant().with(VariantProperties.MODEL, pressedModelId))).with(PropertyDispatch.properties(BlockProperties.ATTACH_FACE, BlockProperties.HORIZONTAL_FACING).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.NORTH, Variant.variant()).select(BlockPropertyAttachPosition.WALL, EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(BlockPropertyAttachPosition.WALL, EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(BlockPropertyAttachPosition.WALL, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(BlockPropertyAttachPosition.WALL, EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)));
    }

    private static PropertyDispatch.C4<EnumDirection, BlockPropertyDoubleBlockHalf, BlockPropertyDoorHinge, Boolean> configureDoorHalf(PropertyDispatch.C4<EnumDirection, BlockPropertyDoubleBlockHalf, BlockPropertyDoorHinge, Boolean> variantMap, BlockPropertyDoubleBlockHalf targetHalf, MinecraftKey regularModel, MinecraftKey hingeModel) {
        return variantMap.select(EnumDirection.EAST, targetHalf, BlockPropertyDoorHinge.LEFT, false, Variant.variant().with(VariantProperties.MODEL, regularModel)).select(EnumDirection.SOUTH, targetHalf, BlockPropertyDoorHinge.LEFT, false, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, targetHalf, BlockPropertyDoorHinge.LEFT, false, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.NORTH, targetHalf, BlockPropertyDoorHinge.LEFT, false, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.EAST, targetHalf, BlockPropertyDoorHinge.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, hingeModel)).select(EnumDirection.SOUTH, targetHalf, BlockPropertyDoorHinge.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, targetHalf, BlockPropertyDoorHinge.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.NORTH, targetHalf, BlockPropertyDoorHinge.RIGHT, false, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.EAST, targetHalf, BlockPropertyDoorHinge.LEFT, true, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.SOUTH, targetHalf, BlockPropertyDoorHinge.LEFT, true, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, targetHalf, BlockPropertyDoorHinge.LEFT, true, Variant.variant().with(VariantProperties.MODEL, hingeModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, targetHalf, BlockPropertyDoorHinge.LEFT, true, Variant.variant().with(VariantProperties.MODEL, hingeModel)).select(EnumDirection.EAST, targetHalf, BlockPropertyDoorHinge.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.SOUTH, targetHalf, BlockPropertyDoorHinge.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, regularModel)).select(EnumDirection.WEST, targetHalf, BlockPropertyDoorHinge.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.NORTH, targetHalf, BlockPropertyDoorHinge.RIGHT, true, Variant.variant().with(VariantProperties.MODEL, regularModel).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180));
    }

    private static BlockStateGenerator createDoor(Block doorBlock, MinecraftKey bottomModelId, MinecraftKey bottomHingeModelId, MinecraftKey topModelId, MinecraftKey topHingeModelId) {
        return MultiVariantGenerator.multiVariant(doorBlock).with(configureDoorHalf(configureDoorHalf(PropertyDispatch.properties(BlockProperties.HORIZONTAL_FACING, BlockProperties.DOUBLE_BLOCK_HALF, BlockProperties.DOOR_HINGE, BlockProperties.OPEN), BlockPropertyDoubleBlockHalf.LOWER, bottomModelId, bottomHingeModelId), BlockPropertyDoubleBlockHalf.UPPER, topModelId, topHingeModelId));
    }

    static BlockStateGenerator createFence(Block fenceBlock, MinecraftKey postModelId, MinecraftKey sideModelId) {
        return MultiPartGenerator.multiPart(fenceBlock).with(Variant.variant().with(VariantProperties.MODEL, postModelId)).with(Condition.condition().term(BlockProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, sideModelId).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.EAST, true), Variant.variant().with(VariantProperties.MODEL, sideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, sideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, sideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true));
    }

    static BlockStateGenerator createWall(Block wallBlock, MinecraftKey postModelId, MinecraftKey lowSideModelId, MinecraftKey tallSideModelId) {
        return MultiPartGenerator.multiPart(wallBlock).with(Condition.condition().term(BlockProperties.UP, true), Variant.variant().with(VariantProperties.MODEL, postModelId)).with(Condition.condition().term(BlockProperties.NORTH_WALL, BlockPropertyWallHeight.LOW), Variant.variant().with(VariantProperties.MODEL, lowSideModelId).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.EAST_WALL, BlockPropertyWallHeight.LOW), Variant.variant().with(VariantProperties.MODEL, lowSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH_WALL, BlockPropertyWallHeight.LOW), Variant.variant().with(VariantProperties.MODEL, lowSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST_WALL, BlockPropertyWallHeight.LOW), Variant.variant().with(VariantProperties.MODEL, lowSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.NORTH_WALL, BlockPropertyWallHeight.TALL), Variant.variant().with(VariantProperties.MODEL, tallSideModelId).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.EAST_WALL, BlockPropertyWallHeight.TALL), Variant.variant().with(VariantProperties.MODEL, tallSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH_WALL, BlockPropertyWallHeight.TALL), Variant.variant().with(VariantProperties.MODEL, tallSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST_WALL, BlockPropertyWallHeight.TALL), Variant.variant().with(VariantProperties.MODEL, tallSideModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true));
    }

    static BlockStateGenerator createFenceGate(Block fenceGateBlock, MinecraftKey openModelId, MinecraftKey closedModelId, MinecraftKey openWallModelId, MinecraftKey closedWallModelId) {
        return MultiVariantGenerator.multiVariant(fenceGateBlock, Variant.variant().with(VariantProperties.UV_LOCK, true)).with(createHorizontalFacingDispatchAlt()).with(PropertyDispatch.properties(BlockProperties.IN_WALL, BlockProperties.OPEN).select(false, false, Variant.variant().with(VariantProperties.MODEL, closedModelId)).select(true, false, Variant.variant().with(VariantProperties.MODEL, closedWallModelId)).select(false, true, Variant.variant().with(VariantProperties.MODEL, openModelId)).select(true, true, Variant.variant().with(VariantProperties.MODEL, openWallModelId)));
    }

    static BlockStateGenerator createStairs(Block stairsBlock, MinecraftKey innerModelId, MinecraftKey regularModelId, MinecraftKey outerModelId) {
        return MultiVariantGenerator.multiVariant(stairsBlock).with(PropertyDispatch.properties(BlockProperties.HORIZONTAL_FACING, BlockProperties.HALF, BlockProperties.STAIRS_SHAPE).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.STRAIGHT, Variant.variant().with(VariantProperties.MODEL, regularModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_RIGHT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.OUTER_LEFT, Variant.variant().with(VariantProperties.MODEL, outerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_RIGHT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, BlockPropertyStairsShape.INNER_LEFT, Variant.variant().with(VariantProperties.MODEL, innerModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)));
    }

    private static BlockStateGenerator createOrientableTrapdoor(Block trapdoorBlock, MinecraftKey topModelId, MinecraftKey bottomModelId, MinecraftKey openModelId) {
        return MultiVariantGenerator.multiVariant(trapdoorBlock).with(PropertyDispatch.properties(BlockProperties.HORIZONTAL_FACING, BlockProperties.HALF, BlockProperties.OPEN).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R0)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)));
    }

    private static BlockStateGenerator createTrapdoor(Block trapdoorBlock, MinecraftKey topModelId, MinecraftKey bottomModelId, MinecraftKey openModelId) {
        return MultiVariantGenerator.multiVariant(trapdoorBlock).with(PropertyDispatch.properties(BlockProperties.HORIZONTAL_FACING, BlockProperties.HALF, BlockProperties.OPEN).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, false, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, false, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(EnumDirection.NORTH, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyHalf.BOTTOM, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId)).select(EnumDirection.SOUTH, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyHalf.TOP, true, Variant.variant().with(VariantProperties.MODEL, openModelId).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)));
    }

    static MultiVariantGenerator createSimpleBlock(Block block, MinecraftKey modelId) {
        return MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, modelId));
    }

    private static PropertyDispatch createRotatedPillar() {
        return PropertyDispatch.property(BlockProperties.AXIS).select(EnumDirection.EnumAxis.Y, Variant.variant()).select(EnumDirection.EnumAxis.Z, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.EnumAxis.X, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90));
    }

    static BlockStateGenerator createAxisAlignedPillarBlock(Block block, MinecraftKey modelId) {
        return MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, modelId)).with(createRotatedPillar());
    }

    private void createAxisAlignedPillarBlockCustomModel(Block block, MinecraftKey modelId) {
        this.blockStateOutput.accept(createAxisAlignedPillarBlock(block, modelId));
    }

    private void createAxisAlignedPillarBlock(Block block, TexturedModel.Provider modelFactory) {
        MinecraftKey resourceLocation = modelFactory.create(block, this.modelOutput);
        this.blockStateOutput.accept(createAxisAlignedPillarBlock(block, resourceLocation));
    }

    private void createHorizontallyRotatedBlock(Block block, TexturedModel.Provider modelFactory) {
        MinecraftKey resourceLocation = modelFactory.create(block, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(createHorizontalFacingDispatch()));
    }

    static BlockStateGenerator createRotatedPillarWithHorizontalVariant(Block block, MinecraftKey verticalModelId, MinecraftKey horizontalModelId) {
        return MultiVariantGenerator.multiVariant(block).with(PropertyDispatch.property(BlockProperties.AXIS).select(EnumDirection.EnumAxis.Y, Variant.variant().with(VariantProperties.MODEL, verticalModelId)).select(EnumDirection.EnumAxis.Z, Variant.variant().with(VariantProperties.MODEL, horizontalModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.EnumAxis.X, Variant.variant().with(VariantProperties.MODEL, horizontalModelId).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)));
    }

    private void createRotatedPillarWithHorizontalVariant(Block block, TexturedModel.Provider verticalModelFactory, TexturedModel.Provider horizontalModelFactory) {
        MinecraftKey resourceLocation = verticalModelFactory.create(block, this.modelOutput);
        MinecraftKey resourceLocation2 = horizontalModelFactory.create(block, this.modelOutput);
        this.blockStateOutput.accept(createRotatedPillarWithHorizontalVariant(block, resourceLocation, resourceLocation2));
    }

    private MinecraftKey createSuffixedVariant(Block block, String suffix, ModelTemplate model, Function<MinecraftKey, TextureMapping> textureFactory) {
        return model.createWithSuffix(block, suffix, textureFactory.apply(TextureMapping.getBlockTexture(block, suffix)), this.modelOutput);
    }

    static BlockStateGenerator createPressurePlate(Block pressurePlateBlock, MinecraftKey upModelId, MinecraftKey downModelId) {
        return MultiVariantGenerator.multiVariant(pressurePlateBlock).with(createBooleanModelDispatch(BlockProperties.POWERED, downModelId, upModelId));
    }

    static BlockStateGenerator createSlab(Block slabBlock, MinecraftKey bottomModelId, MinecraftKey topModelId, MinecraftKey fullModelId) {
        return MultiVariantGenerator.multiVariant(slabBlock).with(PropertyDispatch.property(BlockProperties.SLAB_TYPE).select(BlockPropertySlabType.BOTTOM, Variant.variant().with(VariantProperties.MODEL, bottomModelId)).select(BlockPropertySlabType.TOP, Variant.variant().with(VariantProperties.MODEL, topModelId)).select(BlockPropertySlabType.DOUBLE, Variant.variant().with(VariantProperties.MODEL, fullModelId)));
    }

    private void createTrivialCube(Block block) {
        this.createTrivialBlock(block, TexturedModel.CUBE);
    }

    private void createTrivialBlock(Block block, TexturedModel.Provider modelFactory) {
        this.blockStateOutput.accept(createSimpleBlock(block, modelFactory.create(block, this.modelOutput)));
    }

    private void createTrivialBlock(Block block, TextureMapping texture, ModelTemplate model) {
        MinecraftKey resourceLocation = model.create(block, texture, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private BlockModelGenerators.BlockFamilyProvider family(Block block) {
        TexturedModel texturedModel = this.texturedModels.getOrDefault(block, TexturedModel.CUBE.get(block));
        return (new BlockModelGenerators.BlockFamilyProvider(texturedModel.getMapping())).fullBlock(block, texturedModel.getTemplate());
    }

    void createDoor(Block doorBlock) {
        TextureMapping textureMapping = TextureMapping.door(doorBlock);
        MinecraftKey resourceLocation = ModelTemplates.DOOR_BOTTOM.create(doorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.DOOR_BOTTOM_HINGE.create(doorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.DOOR_TOP.create(doorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.DOOR_TOP_HINGE.create(doorBlock, textureMapping, this.modelOutput);
        this.createSimpleFlatItemModel(doorBlock.getItem());
        this.blockStateOutput.accept(createDoor(doorBlock, resourceLocation, resourceLocation2, resourceLocation3, resourceLocation4));
    }

    void createOrientableTrapdoor(Block trapdoorBlock) {
        TextureMapping textureMapping = TextureMapping.defaultTexture(trapdoorBlock);
        MinecraftKey resourceLocation = ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(trapdoorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(trapdoorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(trapdoorBlock, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createOrientableTrapdoor(trapdoorBlock, resourceLocation, resourceLocation2, resourceLocation3));
        this.delegateItemModel(trapdoorBlock, resourceLocation2);
    }

    void createTrapdoor(Block trapdoorBlock) {
        TextureMapping textureMapping = TextureMapping.defaultTexture(trapdoorBlock);
        MinecraftKey resourceLocation = ModelTemplates.TRAPDOOR_TOP.create(trapdoorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.TRAPDOOR_BOTTOM.create(trapdoorBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.TRAPDOOR_OPEN.create(trapdoorBlock, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createTrapdoor(trapdoorBlock, resourceLocation, resourceLocation2, resourceLocation3));
        this.delegateItemModel(trapdoorBlock, resourceLocation2);
    }

    private void createBigDripLeafBlock() {
        this.skipAutoItemBlock(Blocks.BIG_DRIPLEAF);
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_partial_tilt");
        MinecraftKey resourceLocation3 = ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_full_tilt");
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.BIG_DRIPLEAF).with(createHorizontalFacingDispatch()).with(PropertyDispatch.property(BlockProperties.TILT).select(Tilt.NONE, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(Tilt.UNSTABLE, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(Tilt.PARTIAL, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(Tilt.FULL, Variant.variant().with(VariantProperties.MODEL, resourceLocation3))));
    }

    private BlockModelGenerators.WoodProvider woodProvider(Block logBlock) {
        return new BlockModelGenerators.WoodProvider(TextureMapping.logColumn(logBlock));
    }

    private void createNonTemplateModelBlock(Block block) {
        this.createNonTemplateModelBlock(block, block);
    }

    private void createNonTemplateModelBlock(Block block, Block modelReference) {
        this.blockStateOutput.accept(createSimpleBlock(block, ModelLocationUtils.getModelLocation(modelReference)));
    }

    private void createCrossBlockWithDefaultItem(Block block, BlockModelGenerators.TintState tintType) {
        this.createSimpleFlatItemModel(block);
        this.createCrossBlock(block, tintType);
    }

    private void createCrossBlockWithDefaultItem(Block block, BlockModelGenerators.TintState tintType, TextureMapping texture) {
        this.createSimpleFlatItemModel(block);
        this.createCrossBlock(block, tintType, texture);
    }

    private void createCrossBlock(Block block, BlockModelGenerators.TintState tintType) {
        TextureMapping textureMapping = TextureMapping.cross(block);
        this.createCrossBlock(block, tintType, textureMapping);
    }

    private void createCrossBlock(Block block, BlockModelGenerators.TintState tintType, TextureMapping crossTexture) {
        MinecraftKey resourceLocation = tintType.getCross().create(block, crossTexture, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private void createPlant(Block plantBlock, Block flowerPotBlock, BlockModelGenerators.TintState tintType) {
        this.createCrossBlockWithDefaultItem(plantBlock, tintType);
        TextureMapping textureMapping = TextureMapping.plant(plantBlock);
        MinecraftKey resourceLocation = tintType.getCrossPot().create(flowerPotBlock, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(flowerPotBlock, resourceLocation));
    }

    private void createCoralFans(Block coralFanBlock, Block coralWallFanBlock) {
        TexturedModel texturedModel = TexturedModel.CORAL_FAN.get(coralFanBlock);
        MinecraftKey resourceLocation = texturedModel.create(coralFanBlock, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(coralFanBlock, resourceLocation));
        MinecraftKey resourceLocation2 = ModelTemplates.CORAL_WALL_FAN.create(coralWallFanBlock, texturedModel.getMapping(), this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(coralWallFanBlock, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).with(createHorizontalFacingDispatch()));
        this.createSimpleFlatItemModel(coralFanBlock);
    }

    private void createStems(Block stemBlock, Block attachedStemBlock) {
        this.createSimpleFlatItemModel(stemBlock.getItem());
        TextureMapping textureMapping = TextureMapping.stem(stemBlock);
        TextureMapping textureMapping2 = TextureMapping.attachedStem(stemBlock, attachedStemBlock);
        MinecraftKey resourceLocation = ModelTemplates.ATTACHED_STEM.create(attachedStemBlock, textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(attachedStemBlock, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(PropertyDispatch.property(BlockProperties.HORIZONTAL_FACING).select(EnumDirection.WEST, Variant.variant()).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(stemBlock).with(PropertyDispatch.property(BlockProperties.AGE_7).generate((integer) -> {
            return Variant.variant().with(VariantProperties.MODEL, ModelTemplates.STEMS[integer].create(stemBlock, textureMapping, this.modelOutput));
        })));
    }

    private void createCoral(Block coral, Block deadCoral, Block coralBlock, Block deadCoralBlock, Block coralFan, Block deadCoralFan, Block coralWallFan, Block deadCoralWallFan) {
        this.createCrossBlockWithDefaultItem(coral, BlockModelGenerators.TintState.NOT_TINTED);
        this.createCrossBlockWithDefaultItem(deadCoral, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialCube(coralBlock);
        this.createTrivialCube(deadCoralBlock);
        this.createCoralFans(coralFan, coralWallFan);
        this.createCoralFans(deadCoralFan, deadCoralWallFan);
    }

    private void createDoublePlant(Block doubleBlock, BlockModelGenerators.TintState tintType) {
        this.createSimpleFlatItemModel(doubleBlock, "_top");
        MinecraftKey resourceLocation = this.createSuffixedVariant(doubleBlock, "_top", tintType.getCross(), TextureMapping::cross);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(doubleBlock, "_bottom", tintType.getCross(), TextureMapping::cross);
        this.createDoubleBlock(doubleBlock, resourceLocation, resourceLocation2);
    }

    private void createSunflower() {
        this.createSimpleFlatItemModel(Blocks.SUNFLOWER, "_front");
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.SUNFLOWER, "_top");
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(Blocks.SUNFLOWER, "_bottom", BlockModelGenerators.TintState.NOT_TINTED.getCross(), TextureMapping::cross);
        this.createDoubleBlock(Blocks.SUNFLOWER, resourceLocation, resourceLocation2);
    }

    private void createTallSeagrass() {
        MinecraftKey resourceLocation = this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_top", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_bottom", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture);
        this.createDoubleBlock(Blocks.TALL_SEAGRASS, resourceLocation, resourceLocation2);
    }

    private void createSmallDripleaf() {
        this.skipAutoItemBlock(Blocks.SMALL_DRIPLEAF);
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_top");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_bottom");
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SMALL_DRIPLEAF).with(createHorizontalFacingDispatch()).with(PropertyDispatch.property(BlockProperties.DOUBLE_BLOCK_HALF).select(BlockPropertyDoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(BlockPropertyDoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, resourceLocation))));
    }

    private void createDoubleBlock(Block block, MinecraftKey upperHalfModelId, MinecraftKey lowerHalfModelId) {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(PropertyDispatch.property(BlockProperties.DOUBLE_BLOCK_HALF).select(BlockPropertyDoubleBlockHalf.LOWER, Variant.variant().with(VariantProperties.MODEL, lowerHalfModelId)).select(BlockPropertyDoubleBlockHalf.UPPER, Variant.variant().with(VariantProperties.MODEL, upperHalfModelId))));
    }

    private void createPassiveRail(Block rail) {
        TextureMapping textureMapping = TextureMapping.rail(rail);
        TextureMapping textureMapping2 = TextureMapping.rail(TextureMapping.getBlockTexture(rail, "_corner"));
        MinecraftKey resourceLocation = ModelTemplates.RAIL_FLAT.create(rail, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.RAIL_CURVED.create(rail, textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.RAIL_RAISED_NE.create(rail, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.RAIL_RAISED_SW.create(rail, textureMapping, this.modelOutput);
        this.createSimpleFlatItemModel(rail);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(rail).with(PropertyDispatch.property(BlockProperties.RAIL_SHAPE).select(BlockPropertyTrackPosition.NORTH_SOUTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(BlockPropertyTrackPosition.EAST_WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyTrackPosition.ASCENDING_EAST, Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyTrackPosition.ASCENDING_WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyTrackPosition.ASCENDING_NORTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation3)).select(BlockPropertyTrackPosition.ASCENDING_SOUTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation4)).select(BlockPropertyTrackPosition.SOUTH_EAST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(BlockPropertyTrackPosition.SOUTH_WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyTrackPosition.NORTH_WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyTrackPosition.NORTH_EAST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }

    private void createActiveRail(Block rail) {
        MinecraftKey resourceLocation = this.createSuffixedVariant(rail, "", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(rail, "", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
        MinecraftKey resourceLocation3 = this.createSuffixedVariant(rail, "", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
        MinecraftKey resourceLocation4 = this.createSuffixedVariant(rail, "_on", ModelTemplates.RAIL_FLAT, TextureMapping::rail);
        MinecraftKey resourceLocation5 = this.createSuffixedVariant(rail, "_on", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail);
        MinecraftKey resourceLocation6 = this.createSuffixedVariant(rail, "_on", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail);
        PropertyDispatch propertyDispatch = PropertyDispatch.properties(BlockProperties.POWERED, BlockProperties.RAIL_SHAPE_STRAIGHT).generate((boolean_, railShape) -> {
            switch(railShape) {
            case NORTH_SOUTH:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation4 : resourceLocation);
            case EAST_WEST:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation4 : resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
            case ASCENDING_EAST:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation5 : resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
            case ASCENDING_WEST:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation6 : resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
            case ASCENDING_NORTH:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation5 : resourceLocation2);
            case ASCENDING_SOUTH:
                return Variant.variant().with(VariantProperties.MODEL, boolean_ ? resourceLocation6 : resourceLocation3);
            default:
                throw new UnsupportedOperationException("Fix you generator!");
            }
        });
        this.createSimpleFlatItemModel(rail);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(rail).with(propertyDispatch));
    }

    private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(MinecraftKey modelId, Block particleBlock) {
        return new BlockModelGenerators.BlockEntityModelGenerator(modelId, particleBlock);
    }

    private BlockModelGenerators.BlockEntityModelGenerator blockEntityModels(Block block, Block particleBlock) {
        return new BlockModelGenerators.BlockEntityModelGenerator(ModelLocationUtils.getModelLocation(block), particleBlock);
    }

    private void createAirLikeBlock(Block block, Item particleSource) {
        MinecraftKey resourceLocation = ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particleFromItem(particleSource), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private void createAirLikeBlock(Block block, MinecraftKey particleSource) {
        MinecraftKey resourceLocation = ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particleSource), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private void createFullAndCarpetBlocks(Block wool, Block carpet) {
        this.createTrivialCube(wool);
        MinecraftKey resourceLocation = TexturedModel.CARPET.get(wool).create(carpet, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(carpet, resourceLocation));
    }

    private void createColoredBlockWithRandomRotations(TexturedModel.Provider modelFactory, Block... blocks) {
        for(Block block : blocks) {
            MinecraftKey resourceLocation = modelFactory.create(block, this.modelOutput);
            this.blockStateOutput.accept(createRotatedVariant(block, resourceLocation));
        }

    }

    private void createColoredBlockWithStateRotations(TexturedModel.Provider modelFactory, Block... blocks) {
        for(Block block : blocks) {
            MinecraftKey resourceLocation = modelFactory.create(block, this.modelOutput);
            this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(createHorizontalFacingDispatchAlt()));
        }

    }

    private void createGlassBlocks(Block glass, Block glassPane) {
        this.createTrivialCube(glass);
        TextureMapping textureMapping = TextureMapping.pane(glass, glassPane);
        MinecraftKey resourceLocation = ModelTemplates.STAINED_GLASS_PANE_POST.create(glassPane, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.STAINED_GLASS_PANE_SIDE.create(glassPane, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(glassPane, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(glassPane, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation5 = ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(glassPane, textureMapping, this.modelOutput);
        Item item = glassPane.getItem();
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(glass), this.modelOutput);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(glassPane).with(Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(Condition.condition().term(BlockProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).with(Condition.condition().term(BlockProperties.EAST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3)).with(Condition.condition().term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation4)).with(Condition.condition().term(BlockProperties.EAST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation5)).with(Condition.condition().term(BlockProperties.SOUTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)));
    }

    private void createCommandBlock(Block commandBlock) {
        TextureMapping textureMapping = TextureMapping.commandBlock(commandBlock);
        MinecraftKey resourceLocation = ModelTemplates.COMMAND_BLOCK.create(commandBlock, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(commandBlock, "_conditional", ModelTemplates.COMMAND_BLOCK, (resourceLocationx) -> {
            return textureMapping.copyAndUpdate(TextureSlot.SIDE, resourceLocationx);
        });
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(commandBlock).with(createBooleanModelDispatch(BlockProperties.CONDITIONAL, resourceLocation2, resourceLocation)).with(createFacingDispatch()));
    }

    private void createAnvil(Block anvil) {
        MinecraftKey resourceLocation = TexturedModel.ANVIL.create(anvil, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(anvil, resourceLocation).with(createHorizontalFacingDispatchAlt()));
    }

    private List<Variant> createBambooModels(int age) {
        String string = "_age" + age;
        return IntStream.range(1, 5).mapToObj((i) -> {
            return Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, i + string));
        }).collect(Collectors.toList());
    }

    private void createBamboo() {
        this.skipAutoItemBlock(Blocks.BAMBOO);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.BAMBOO).with(Condition.condition().term(BlockProperties.AGE_1, 0), this.createBambooModels(0)).with(Condition.condition().term(BlockProperties.AGE_1, 1), this.createBambooModels(1)).with(Condition.condition().term(BlockProperties.BAMBOO_LEAVES, BlockPropertyBambooSize.SMALL), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_small_leaves"))).with(Condition.condition().term(BlockProperties.BAMBOO_LEAVES, BlockPropertyBambooSize.LARGE), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_large_leaves"))));
    }

    private PropertyDispatch createColumnWithFacing() {
        return PropertyDispatch.property(BlockProperties.FACING).select(EnumDirection.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.UP, Variant.variant()).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90));
    }

    private void createBarrel() {
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.BARREL, "_top_open");
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.BARREL).with(this.createColumnWithFacing()).with(PropertyDispatch.property(BlockProperties.OPEN).select(false, Variant.variant().with(VariantProperties.MODEL, TexturedModel.CUBE_TOP_BOTTOM.create(Blocks.BARREL, this.modelOutput))).select(true, Variant.variant().with(VariantProperties.MODEL, TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.BARREL).updateTextures((textureMapping) -> {
            textureMapping.put(TextureSlot.TOP, resourceLocation);
        }).createWithSuffix(Blocks.BARREL, "_open", this.modelOutput)))));
    }

    private static <T extends Comparable<T>> PropertyDispatch createEmptyOrFullDispatch(IBlockState<T> property, T fence, MinecraftKey higherOrEqualModelId, MinecraftKey lowerModelId) {
        Variant variant = Variant.variant().with(VariantProperties.MODEL, higherOrEqualModelId);
        Variant variant2 = Variant.variant().with(VariantProperties.MODEL, lowerModelId);
        return PropertyDispatch.property(property).generate((comparable2) -> {
            boolean bl = comparable2.compareTo(fence) >= 0;
            return bl ? variant : variant2;
        });
    }

    private void createBeeNest(Block beehive, Function<Block, TextureMapping> textureGetter) {
        TextureMapping textureMapping = textureGetter.apply(beehive).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
        TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(beehive, "_front_honey"));
        MinecraftKey resourceLocation = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(beehive, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(beehive, "_honey", textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(beehive).with(createHorizontalFacingDispatch()).with(createEmptyOrFullDispatch(BlockProperties.LEVEL_HONEY, 5, resourceLocation2, resourceLocation)));
    }

    private void createCropBlock(Block crop, IBlockState<Integer> ageProperty, int... ageTextureIndices) {
        if (ageProperty.getValues().size() != ageTextureIndices.length) {
            throw new IllegalArgumentException();
        } else {
            Int2ObjectMap<MinecraftKey> int2ObjectMap = new Int2ObjectOpenHashMap<>();
            PropertyDispatch propertyDispatch = PropertyDispatch.property(ageProperty).generate((integer) -> {
                int i = ageTextureIndices[integer];
                MinecraftKey resourceLocation = int2ObjectMap.computeIfAbsent(i, (j) -> {
                    return this.createSuffixedVariant(crop, "_stage" + i, ModelTemplates.CROP, TextureMapping::crop);
                });
                return Variant.variant().with(VariantProperties.MODEL, resourceLocation);
            });
            this.createSimpleFlatItemModel(crop.getItem());
            this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(crop).with(propertyDispatch));
        }
    }

    private void createBell() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.BELL, "_floor");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_ceiling");
        MinecraftKey resourceLocation3 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_wall");
        MinecraftKey resourceLocation4 = ModelLocationUtils.getModelLocation(Blocks.BELL, "_between_walls");
        this.createSimpleFlatItemModel(Items.BELL);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.BELL).with(PropertyDispatch.properties(BlockProperties.HORIZONTAL_FACING, BlockProperties.BELL_ATTACHMENT).select(EnumDirection.NORTH, BlockPropertyBellAttach.FLOOR, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(EnumDirection.SOUTH, BlockPropertyBellAttach.FLOOR, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyBellAttach.FLOOR, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyBellAttach.FLOOR, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyBellAttach.CEILING, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(EnumDirection.SOUTH, BlockPropertyBellAttach.CEILING, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.EAST, BlockPropertyBellAttach.CEILING, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.WEST, BlockPropertyBellAttach.CEILING, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.NORTH, BlockPropertyBellAttach.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.SOUTH, BlockPropertyBellAttach.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.EAST, BlockPropertyBellAttach.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation3)).select(EnumDirection.WEST, BlockPropertyBellAttach.SINGLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.SOUTH, BlockPropertyBellAttach.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.NORTH, BlockPropertyBellAttach.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(EnumDirection.EAST, BlockPropertyBellAttach.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation4)).select(EnumDirection.WEST, BlockPropertyBellAttach.DOUBLE_WALL, Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180))));
    }

    private void createGrindstone() {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.GRINDSTONE, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.GRINDSTONE))).with(PropertyDispatch.properties(BlockProperties.ATTACH_FACE, BlockProperties.HORIZONTAL_FACING).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.NORTH, Variant.variant()).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(BlockPropertyAttachPosition.WALL, EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.WALL, EnumDirection.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.WALL, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.WALL, EnumDirection.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }

    private void createFurnace(Block cooker, TexturedModel.Provider modelFactory) {
        MinecraftKey resourceLocation = modelFactory.create(cooker, this.modelOutput);
        MinecraftKey resourceLocation2 = TextureMapping.getBlockTexture(cooker, "_front_on");
        MinecraftKey resourceLocation3 = modelFactory.get(cooker).updateTextures((textureMapping) -> {
            textureMapping.put(TextureSlot.FRONT, resourceLocation2);
        }).createWithSuffix(cooker, "_on", this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(cooker).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation3, resourceLocation)).with(createHorizontalFacingDispatch()));
    }

    private void createCampfires(Block... blocks) {
        MinecraftKey resourceLocation = ModelLocationUtils.decorateBlockModelLocation("campfire_off");

        for(Block block : blocks) {
            MinecraftKey resourceLocation2 = ModelTemplates.CAMPFIRE.create(block, TextureMapping.campfire(block), this.modelOutput);
            this.createSimpleFlatItemModel(block.getItem());
            this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation2, resourceLocation)).with(createHorizontalFacingDispatchAlt()));
        }

    }

    private void createAzalea(Block block) {
        MinecraftKey resourceLocation = ModelTemplates.AZALEA.create(block, TextureMapping.cubeTop(block), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private void createPottedAzalea(Block block) {
        MinecraftKey resourceLocation = ModelTemplates.POTTED_AZALEA.create(block, TextureMapping.cubeTop(block), this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(block, resourceLocation));
    }

    private void createBookshelf() {
        TextureMapping textureMapping = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.BOOKSHELF), TextureMapping.getBlockTexture(Blocks.OAK_PLANKS));
        MinecraftKey resourceLocation = ModelTemplates.CUBE_COLUMN.create(Blocks.BOOKSHELF, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.BOOKSHELF, resourceLocation));
    }

    private void createRedstoneWire() {
        this.createSimpleFlatItemModel(Items.REDSTONE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.REDSTONE_WIRE).with(Condition.or(Condition.condition().term(BlockProperties.NORTH_REDSTONE, BlockPropertyRedstoneSide.NONE).term(BlockProperties.EAST_REDSTONE, BlockPropertyRedstoneSide.NONE).term(BlockProperties.SOUTH_REDSTONE, BlockPropertyRedstoneSide.NONE).term(BlockProperties.WEST_REDSTONE, BlockPropertyRedstoneSide.NONE), Condition.condition().term(BlockProperties.NORTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP).term(BlockProperties.EAST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Condition.condition().term(BlockProperties.EAST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP).term(BlockProperties.SOUTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Condition.condition().term(BlockProperties.SOUTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP).term(BlockProperties.WEST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Condition.condition().term(BlockProperties.WEST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP).term(BlockProperties.NORTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP)), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot"))).with(Condition.condition().term(BlockProperties.NORTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0"))).with(Condition.condition().term(BlockProperties.SOUTH_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0"))).with(Condition.condition().term(BlockProperties.EAST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).with(Condition.condition().term(BlockProperties.WEST_REDSTONE, BlockPropertyRedstoneSide.SIDE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).with(Condition.condition().term(BlockProperties.NORTH_REDSTONE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))).with(Condition.condition().term(BlockProperties.EAST_REDSTONE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.SOUTH_REDSTONE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).with(Condition.condition().term(BlockProperties.WEST_REDSTONE, BlockPropertyRedstoneSide.UP), Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)));
    }

    private void createComparator() {
        this.createSimpleFlatItemModel(Items.COMPARATOR);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.COMPARATOR).with(createHorizontalFacingDispatchAlt()).with(PropertyDispatch.properties(BlockProperties.MODE_COMPARATOR, BlockProperties.POWERED).select(BlockPropertyComparatorMode.COMPARE, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR))).select(BlockPropertyComparatorMode.COMPARE, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on"))).select(BlockPropertyComparatorMode.SUBTRACT, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_subtract"))).select(BlockPropertyComparatorMode.SUBTRACT, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on_subtract")))));
    }

    private void createSmoothStoneSlab() {
        TextureMapping textureMapping = TextureMapping.cube(Blocks.SMOOTH_STONE);
        TextureMapping textureMapping2 = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.SMOOTH_STONE_SLAB, "_side"), textureMapping.get(TextureSlot.TOP));
        MinecraftKey resourceLocation = ModelTemplates.SLAB_BOTTOM.create(Blocks.SMOOTH_STONE_SLAB, textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.SLAB_TOP.create(Blocks.SMOOTH_STONE_SLAB, textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.CUBE_COLUMN.createWithOverride(Blocks.SMOOTH_STONE_SLAB, "_double", textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(createSlab(Blocks.SMOOTH_STONE_SLAB, resourceLocation, resourceLocation2, resourceLocation3));
        this.blockStateOutput.accept(createSimpleBlock(Blocks.SMOOTH_STONE, ModelTemplates.CUBE_ALL.create(Blocks.SMOOTH_STONE, textureMapping, this.modelOutput)));
    }

    private void createBrewingStand() {
        this.createSimpleFlatItemModel(Items.BREWING_STAND);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.BREWING_STAND).with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_0, true), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle0"))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_1, true), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle1"))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_2, true), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_bottle2"))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_0, false), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty0"))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_1, false), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty1"))).with(Condition.condition().term(BlockProperties.HAS_BOTTLE_2, false), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.BREWING_STAND, "_empty2"))));
    }

    private void createMushroomBlock(Block mushroomBlock) {
        MinecraftKey resourceLocation = ModelTemplates.SINGLE_FACE.create(mushroomBlock, TextureMapping.defaultTexture(mushroomBlock), this.modelOutput);
        MinecraftKey resourceLocation2 = ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside");
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(mushroomBlock).with(Condition.condition().term(BlockProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(Condition.condition().term(BlockProperties.EAST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.UP, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.DOWN, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).with(Condition.condition().term(BlockProperties.EAST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, false)).with(Condition.condition().term(BlockProperties.SOUTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, false)).with(Condition.condition().term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, false)).with(Condition.condition().term(BlockProperties.UP, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, false)).with(Condition.condition().term(BlockProperties.DOWN, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, false)));
        this.delegateItemModel(mushroomBlock, TexturedModel.CUBE.createWithSuffix(mushroomBlock, "_inventory", this.modelOutput));
    }

    private void createCakeBlock() {
        this.createSimpleFlatItemModel(Items.CAKE);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.CAKE).with(PropertyDispatch.property(BlockProperties.BITES).select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE))).select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice1"))).select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice2"))).select(3, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice3"))).select(4, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice4"))).select(5, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice5"))).select(6, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice6")))));
    }

    private void createCartographyTable() {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.DARK_OAK_PLANKS)).put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side1")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side2"));
        this.blockStateOutput.accept(createSimpleBlock(Blocks.CARTOGRAPHY_TABLE, ModelTemplates.CUBE.create(Blocks.CARTOGRAPHY_TABLE, textureMapping, this.modelOutput)));
    }

    private void createSmithingTable() {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_bottom")).put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"));
        this.blockStateOutput.accept(createSimpleBlock(Blocks.SMITHING_TABLE, ModelTemplates.CUBE.create(Blocks.SMITHING_TABLE, textureMapping, this.modelOutput)));
    }

    private void createCraftingTableLike(Block block, Block otherTextureSource, BiFunction<Block, Block, TextureMapping> textureFactory) {
        TextureMapping textureMapping = textureFactory.apply(block, otherTextureSource);
        this.blockStateOutput.accept(createSimpleBlock(block, ModelTemplates.CUBE.create(block, textureMapping, this.modelOutput)));
    }

    private void createPumpkins() {
        TextureMapping textureMapping = TextureMapping.column(Blocks.PUMPKIN);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.PUMPKIN, ModelLocationUtils.getModelLocation(Blocks.PUMPKIN)));
        this.createPumpkinVariant(Blocks.CARVED_PUMPKIN, textureMapping);
        this.createPumpkinVariant(Blocks.JACK_O_LANTERN, textureMapping);
    }

    private void createPumpkinVariant(Block block, TextureMapping texture) {
        MinecraftKey resourceLocation = ModelTemplates.CUBE_ORIENTABLE.create(block, texture.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block)), this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(createHorizontalFacingDispatch()));
    }

    private void createCauldrons() {
        this.createSimpleFlatItemModel(Items.CAULDRON);
        this.createNonTemplateModelBlock(Blocks.CAULDRON);
        this.blockStateOutput.accept(createSimpleBlock(Blocks.LAVA_CAULDRON, ModelTemplates.CAULDRON_FULL.create(Blocks.LAVA_CAULDRON, TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.LAVA, "_still")), this.modelOutput)));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.WATER_CAULDRON).with(PropertyDispatch.property(BlockCauldronLayered.LEVEL).select(1, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_LEVEL1.createWithSuffix(Blocks.WATER_CAULDRON, "_level1", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput))).select(2, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_LEVEL2.createWithSuffix(Blocks.WATER_CAULDRON, "_level2", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput))).select(3, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_FULL.createWithSuffix(Blocks.WATER_CAULDRON, "_full", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput)))));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.POWDER_SNOW_CAULDRON).with(PropertyDispatch.property(BlockCauldronLayered.LEVEL).select(1, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_LEVEL1.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_level1", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput))).select(2, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_LEVEL2.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_level2", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput))).select(3, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CAULDRON_FULL.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_full", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput)))));
    }

    private void createChorusFlower() {
        TextureMapping textureMapping = TextureMapping.defaultTexture(Blocks.CHORUS_FLOWER);
        MinecraftKey resourceLocation = ModelTemplates.CHORUS_FLOWER.create(Blocks.CHORUS_FLOWER, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(Blocks.CHORUS_FLOWER, "_dead", ModelTemplates.CHORUS_FLOWER, (resourceLocationx) -> {
            return textureMapping.copyAndUpdate(TextureSlot.TEXTURE, resourceLocationx);
        });
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.CHORUS_FLOWER).with(createEmptyOrFullDispatch(BlockProperties.AGE_5, 5, resourceLocation2, resourceLocation)));
    }

    private void createDispenserBlock(Block block) {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front"));
        TextureMapping textureMapping2 = (new TextureMapping()).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_vertical"));
        MinecraftKey resourceLocation = ModelTemplates.CUBE_ORIENTABLE.create(block, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(block, textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(PropertyDispatch.property(BlockProperties.FACING).select(EnumDirection.DOWN, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.UP, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }

    private void createEndPortalFrame() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME, "_filled");
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.END_PORTAL_FRAME).with(PropertyDispatch.property(BlockProperties.EYE).select(false, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(true, Variant.variant().with(VariantProperties.MODEL, resourceLocation2))).with(createHorizontalFacingDispatchAlt()));
    }

    private void createChorusPlant() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_side");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside");
        MinecraftKey resourceLocation3 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside1");
        MinecraftKey resourceLocation4 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside2");
        MinecraftKey resourceLocation5 = ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside3");
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.CHORUS_PLANT).with(Condition.condition().term(BlockProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(Condition.condition().term(BlockProperties.EAST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.UP, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.DOWN, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.NORTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2), Variant.variant().with(VariantProperties.MODEL, resourceLocation3), Variant.variant().with(VariantProperties.MODEL, resourceLocation4), Variant.variant().with(VariantProperties.MODEL, resourceLocation5)).with(Condition.condition().term(BlockProperties.EAST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.SOUTH, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.UP, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.UV_LOCK, true)).with(Condition.condition().term(BlockProperties.DOWN, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.WEIGHT, 2).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.UV_LOCK, true)));
    }

    private void createComposter() {
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.COMPOSTER).with(Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 1), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents1"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 2), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents2"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 3), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents3"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 4), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents4"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 5), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents5"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 6), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents6"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 7), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents7"))).with(Condition.condition().term(BlockProperties.LEVEL_COMPOSTER, 8), Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.COMPOSTER, "_contents_ready"))));
    }

    private void createAmethystCluster(Block block) {
        this.skipAutoItemBlock(block);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CROSS.create(block, TextureMapping.cross(block), this.modelOutput))).with(this.createColumnWithFacing()));
    }

    private void createAmethystClusters() {
        this.createAmethystCluster(Blocks.SMALL_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.MEDIUM_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.LARGE_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.AMETHYST_CLUSTER);
    }

    private void createPointedDripstone() {
        this.createSimpleFlatItemModel(Blocks.POINTED_DRIPSTONE.getItem());
        PropertyDispatch.C2<EnumDirection, DripstoneThickness> c2 = PropertyDispatch.properties(BlockProperties.VERTICAL_DIRECTION, BlockProperties.DRIPSTONE_THICKNESS);

        for(DripstoneThickness dripstoneThickness : DripstoneThickness.values()) {
            c2.select(EnumDirection.UP, dripstoneThickness, this.createPointedDripstoneVariant(EnumDirection.UP, dripstoneThickness));
        }

        for(DripstoneThickness dripstoneThickness2 : DripstoneThickness.values()) {
            c2.select(EnumDirection.DOWN, dripstoneThickness2, this.createPointedDripstoneVariant(EnumDirection.DOWN, dripstoneThickness2));
        }

        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.POINTED_DRIPSTONE).with(c2));
    }

    private Variant createPointedDripstoneVariant(EnumDirection direction, DripstoneThickness thickness) {
        String string = "_" + direction.getSerializedName() + "_" + thickness.getSerializedName();
        TextureMapping textureMapping = TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.POINTED_DRIPSTONE, string));
        return Variant.variant().with(VariantProperties.MODEL, ModelTemplates.POINTED_DRIPSTONE.createWithSuffix(Blocks.POINTED_DRIPSTONE, string, textureMapping, this.modelOutput));
    }

    private void createNyliumBlock(Block block) {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.NETHERRACK)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block)).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"));
        this.blockStateOutput.accept(createSimpleBlock(block, ModelTemplates.CUBE_BOTTOM_TOP.create(block, textureMapping, this.modelOutput)));
    }

    private void createDaylightDetector() {
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_side");
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_top")).put(TextureSlot.SIDE, resourceLocation);
        TextureMapping textureMapping2 = (new TextureMapping()).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_inverted_top")).put(TextureSlot.SIDE, resourceLocation);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.DAYLIGHT_DETECTOR).with(PropertyDispatch.property(BlockProperties.INVERTED).select(false, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.DAYLIGHT_DETECTOR.create(Blocks.DAYLIGHT_DETECTOR, textureMapping, this.modelOutput))).select(true, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.DAYLIGHT_DETECTOR.create(ModelLocationUtils.getModelLocation(Blocks.DAYLIGHT_DETECTOR, "_inverted"), textureMapping2, this.modelOutput)))));
    }

    private void createRotatableColumn(Block block) {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block))).with(this.createColumnWithFacing()));
    }

    private void createLightningRod() {
        Block block = Blocks.LIGHTNING_ROD;
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(block, "_on");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(block);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block))).with(this.createColumnWithFacing()).with(createBooleanModelDispatch(BlockProperties.POWERED, resourceLocation, resourceLocation2)));
    }

    private void createFarmland() {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND));
        TextureMapping textureMapping2 = (new TextureMapping()).put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"));
        MinecraftKey resourceLocation = ModelTemplates.FARMLAND.create(Blocks.FARMLAND, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.FARMLAND.create(TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"), textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.FARMLAND).with(createEmptyOrFullDispatch(BlockProperties.MOISTURE, 7, resourceLocation2, resourceLocation)));
    }

    private List<MinecraftKey> createFloorFireModels(Block texture) {
        MinecraftKey resourceLocation = ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(texture, "_floor0"), TextureMapping.fire0(texture), this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(texture, "_floor1"), TextureMapping.fire1(texture), this.modelOutput);
        return ImmutableList.of(resourceLocation, resourceLocation2);
    }

    private List<MinecraftKey> createSideFireModels(Block texture) {
        MinecraftKey resourceLocation = ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(texture, "_side0"), TextureMapping.fire0(texture), this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(texture, "_side1"), TextureMapping.fire1(texture), this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(texture, "_side_alt0"), TextureMapping.fire0(texture), this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(texture, "_side_alt1"), TextureMapping.fire1(texture), this.modelOutput);
        return ImmutableList.of(resourceLocation, resourceLocation2, resourceLocation3, resourceLocation4);
    }

    private List<MinecraftKey> createTopFireModels(Block texture) {
        MinecraftKey resourceLocation = ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(texture, "_up0"), TextureMapping.fire0(texture), this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(texture, "_up1"), TextureMapping.fire1(texture), this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(texture, "_up_alt0"), TextureMapping.fire0(texture), this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(texture, "_up_alt1"), TextureMapping.fire1(texture), this.modelOutput);
        return ImmutableList.of(resourceLocation, resourceLocation2, resourceLocation3, resourceLocation4);
    }

    private static List<Variant> wrapModels(List<MinecraftKey> modelIds, UnaryOperator<Variant> processor) {
        return modelIds.stream().map((resourceLocation) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocation);
        }).map(processor).collect(Collectors.toList());
    }

    private void createFire() {
        Condition condition = Condition.condition().term(BlockProperties.NORTH, false).term(BlockProperties.EAST, false).term(BlockProperties.SOUTH, false).term(BlockProperties.WEST, false).term(BlockProperties.UP, false);
        List<MinecraftKey> list = this.createFloorFireModels(Blocks.FIRE);
        List<MinecraftKey> list2 = this.createSideFireModels(Blocks.FIRE);
        List<MinecraftKey> list3 = this.createTopFireModels(Blocks.FIRE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.FIRE).with(condition, wrapModels(list, (variant) -> {
            return variant;
        })).with(Condition.or(Condition.condition().term(BlockProperties.NORTH, true), condition), wrapModels(list2, (variant) -> {
            return variant;
        })).with(Condition.or(Condition.condition().term(BlockProperties.EAST, true), condition), wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
        })).with(Condition.or(Condition.condition().term(BlockProperties.SOUTH, true), condition), wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
        })).with(Condition.or(Condition.condition().term(BlockProperties.WEST, true), condition), wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
        })).with(Condition.condition().term(BlockProperties.UP, true), wrapModels(list3, (variant) -> {
            return variant;
        })));
    }

    private void createSoulFire() {
        List<MinecraftKey> list = this.createFloorFireModels(Blocks.SOUL_FIRE);
        List<MinecraftKey> list2 = this.createSideFireModels(Blocks.SOUL_FIRE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.SOUL_FIRE).with(wrapModels(list, (variant) -> {
            return variant;
        })).with(wrapModels(list2, (variant) -> {
            return variant;
        })).with(wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
        })).with(wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
        })).with(wrapModels(list2, (variant) -> {
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
        })));
    }

    private void createLantern(Block lantern) {
        MinecraftKey resourceLocation = TexturedModel.LANTERN.create(lantern, this.modelOutput);
        MinecraftKey resourceLocation2 = TexturedModel.HANGING_LANTERN.create(lantern, this.modelOutput);
        this.createSimpleFlatItemModel(lantern.getItem());
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(lantern).with(createBooleanModelDispatch(BlockProperties.HANGING, resourceLocation2, resourceLocation)));
    }

    private void createFrostedIce() {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.FROSTED_ICE).with(PropertyDispatch.property(BlockProperties.AGE_3).select(0, Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_0", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(1, Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_1", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(2, Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_2", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(3, Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.FROSTED_ICE, "_3", ModelTemplates.CUBE_ALL, TextureMapping::cube)))));
    }

    private void createGrassBlocks() {
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.DIRT);
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.BOTTOM, resourceLocation).copyForced(TextureSlot.BOTTOM, TextureSlot.PARTICLE).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_snow"));
        Variant variant = Variant.variant().with(VariantProperties.MODEL, ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.GRASS_BLOCK, "_snow", textureMapping, this.modelOutput));
        this.createGrassLikeBlock(Blocks.GRASS_BLOCK, ModelLocationUtils.getModelLocation(Blocks.GRASS_BLOCK), variant);
        MinecraftKey resourceLocation2 = TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.MYCELIUM).updateTextures((textureMappingx) -> {
            textureMappingx.put(TextureSlot.BOTTOM, resourceLocation);
        }).create(Blocks.MYCELIUM, this.modelOutput);
        this.createGrassLikeBlock(Blocks.MYCELIUM, resourceLocation2, variant);
        MinecraftKey resourceLocation3 = TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.PODZOL).updateTextures((textureMappingx) -> {
            textureMappingx.put(TextureSlot.BOTTOM, resourceLocation);
        }).create(Blocks.PODZOL, this.modelOutput);
        this.createGrassLikeBlock(Blocks.PODZOL, resourceLocation3, variant);
    }

    private void createGrassLikeBlock(Block topSoil, MinecraftKey modelId, Variant snowyVariant) {
        List<Variant> list = Arrays.asList(createRotatedVariants(modelId));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(topSoil).with(PropertyDispatch.property(BlockProperties.SNOWY).select(true, snowyVariant).select(false, list)));
    }

    private void createCocoa() {
        this.createSimpleFlatItemModel(Items.COCOA_BEANS);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.COCOA).with(PropertyDispatch.property(BlockProperties.AGE_2).select(0, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage0"))).select(1, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage1"))).select(2, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage2")))).with(createHorizontalFacingDispatchAlt()));
    }

    private void createDirtPath() {
        this.blockStateOutput.accept(createRotatedVariant(Blocks.DIRT_PATH, ModelLocationUtils.getModelLocation(Blocks.DIRT_PATH)));
    }

    private void createWeightedPressurePlate(Block pressurePlate, Block textureSource) {
        TextureMapping textureMapping = TextureMapping.defaultTexture(textureSource);
        MinecraftKey resourceLocation = ModelTemplates.PRESSURE_PLATE_UP.create(pressurePlate, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.PRESSURE_PLATE_DOWN.create(pressurePlate, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(pressurePlate).with(createEmptyOrFullDispatch(BlockProperties.POWER, 1, resourceLocation2, resourceLocation)));
    }

    private void createHopper() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.HOPPER);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.HOPPER, "_side");
        this.createSimpleFlatItemModel(Items.HOPPER);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.HOPPER).with(PropertyDispatch.property(BlockProperties.FACING_HOPPER).select(EnumDirection.DOWN, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(EnumDirection.NORTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(EnumDirection.EAST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(EnumDirection.SOUTH, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(EnumDirection.WEST, Variant.variant().with(VariantProperties.MODEL, resourceLocation2).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }

    private void copyModel(Block modelSource, Block infested) {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(modelSource);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(infested, Variant.variant().with(VariantProperties.MODEL, resourceLocation)));
        this.delegateItemModel(infested, resourceLocation);
    }

    private void createIronBars() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post_ends");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_post");
        MinecraftKey resourceLocation3 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap");
        MinecraftKey resourceLocation4 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_cap_alt");
        MinecraftKey resourceLocation5 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side");
        MinecraftKey resourceLocation6 = ModelLocationUtils.getModelLocation(Blocks.IRON_BARS, "_side_alt");
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.IRON_BARS).with(Variant.variant().with(VariantProperties.MODEL, resourceLocation)).with(Condition.condition().term(BlockProperties.NORTH, false).term(BlockProperties.EAST, false).term(BlockProperties.SOUTH, false).term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).with(Condition.condition().term(BlockProperties.NORTH, true).term(BlockProperties.EAST, false).term(BlockProperties.SOUTH, false).term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation3)).with(Condition.condition().term(BlockProperties.NORTH, false).term(BlockProperties.EAST, true).term(BlockProperties.SOUTH, false).term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation3).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.NORTH, false).term(BlockProperties.EAST, false).term(BlockProperties.SOUTH, true).term(BlockProperties.WEST, false), Variant.variant().with(VariantProperties.MODEL, resourceLocation4)).with(Condition.condition().term(BlockProperties.NORTH, false).term(BlockProperties.EAST, false).term(BlockProperties.SOUTH, false).term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation4).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.NORTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation5)).with(Condition.condition().term(BlockProperties.EAST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation5).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).with(Condition.condition().term(BlockProperties.SOUTH, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation6)).with(Condition.condition().term(BlockProperties.WEST, true), Variant.variant().with(VariantProperties.MODEL, resourceLocation6).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)));
        this.createSimpleFlatItemModel(Blocks.IRON_BARS);
    }

    private void createNonTemplateHorizontalBlock(Block block) {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block))).with(createHorizontalFacingDispatch()));
    }

    private void createLever() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.LEVER);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.LEVER, "_on");
        this.createSimpleFlatItemModel(Blocks.LEVER);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.LEVER).with(createBooleanModelDispatch(BlockProperties.POWERED, resourceLocation, resourceLocation2)).with(PropertyDispatch.properties(BlockProperties.ATTACH_FACE, BlockProperties.HORIZONTAL_FACING).select(BlockPropertyAttachPosition.CEILING, EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.CEILING, EnumDirection.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.NORTH, Variant.variant()).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.EAST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.FLOOR, EnumDirection.WEST, Variant.variant().with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(BlockPropertyAttachPosition.WALL, EnumDirection.NORTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.WALL, EnumDirection.EAST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(BlockPropertyAttachPosition.WALL, EnumDirection.SOUTH, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(BlockPropertyAttachPosition.WALL, EnumDirection.WEST, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270))));
    }

    private void createLilyPad() {
        this.createSimpleFlatItemModel(Blocks.LILY_PAD);
        this.blockStateOutput.accept(createRotatedVariant(Blocks.LILY_PAD, ModelLocationUtils.getModelLocation(Blocks.LILY_PAD)));
    }

    private void createNetherPortalBlock() {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.NETHER_PORTAL).with(PropertyDispatch.property(BlockProperties.HORIZONTAL_AXIS).select(EnumDirection.EnumAxis.X, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ns"))).select(EnumDirection.EnumAxis.Z, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ew")))));
    }

    private void createNetherrack() {
        MinecraftKey resourceLocation = TexturedModel.CUBE.create(Blocks.NETHERRACK, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.NETHERRACK, Variant.variant().with(VariantProperties.MODEL, resourceLocation), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R90), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R180), Variant.variant().with(VariantProperties.MODEL, resourceLocation).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270).with(VariantProperties.X_ROT, VariantProperties.Rotation.R270)));
    }

    private void createObserver() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.OBSERVER);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.OBSERVER, "_on");
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.OBSERVER).with(createBooleanModelDispatch(BlockProperties.POWERED, resourceLocation2, resourceLocation)).with(createFacingDispatch()));
    }

    private void createPistons() {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.PISTON, "_bottom")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky");
        MinecraftKey resourceLocation2 = TextureMapping.getBlockTexture(Blocks.PISTON, "_top");
        TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, resourceLocation);
        TextureMapping textureMapping3 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, resourceLocation2);
        MinecraftKey resourceLocation3 = ModelLocationUtils.getModelLocation(Blocks.PISTON, "_base");
        this.createPistonVariant(Blocks.PISTON, resourceLocation3, textureMapping3);
        this.createPistonVariant(Blocks.STICKY_PISTON, resourceLocation3, textureMapping2);
        MinecraftKey resourceLocation4 = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.PISTON, "_inventory", textureMapping.copyAndUpdate(TextureSlot.TOP, resourceLocation2), this.modelOutput);
        MinecraftKey resourceLocation5 = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.STICKY_PISTON, "_inventory", textureMapping.copyAndUpdate(TextureSlot.TOP, resourceLocation), this.modelOutput);
        this.delegateItemModel(Blocks.PISTON, resourceLocation4);
        this.delegateItemModel(Blocks.STICKY_PISTON, resourceLocation5);
    }

    private void createPistonVariant(Block piston, MinecraftKey extendedModelId, TextureMapping texture) {
        MinecraftKey resourceLocation = ModelTemplates.PISTON.create(piston, texture, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(piston).with(createBooleanModelDispatch(BlockProperties.EXTENDED, extendedModelId, resourceLocation)).with(createFacingDispatch()));
    }

    private void createPistonHeads() {
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(Blocks.PISTON, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        TextureMapping textureMapping2 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky"));
        TextureMapping textureMapping3 = textureMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.PISTON_HEAD).with(PropertyDispatch.properties(BlockProperties.SHORT, BlockProperties.PISTON_TYPE).select(false, BlockPropertyPistonType.DEFAULT, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head", textureMapping3, this.modelOutput))).select(false, BlockPropertyPistonType.STICKY, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head_sticky", textureMapping2, this.modelOutput))).select(true, BlockPropertyPistonType.DEFAULT, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short", textureMapping3, this.modelOutput))).select(true, BlockPropertyPistonType.STICKY, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short_sticky", textureMapping2, this.modelOutput)))).with(createFacingDispatch()));
    }

    private void createSculkSensor() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_inactive");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_active");
        this.delegateItemModel(Blocks.SCULK_SENSOR, resourceLocation);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SCULK_SENSOR).with(PropertyDispatch.property(BlockProperties.SCULK_SENSOR_PHASE).generate((sculkSensorPhase) -> {
            return Variant.variant().with(VariantProperties.MODEL, sculkSensorPhase == SculkSensorPhase.ACTIVE ? resourceLocation2 : resourceLocation);
        })));
    }

    private void createScaffolding() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_stable");
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_unstable");
        this.delegateItemModel(Blocks.SCAFFOLDING, resourceLocation);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SCAFFOLDING).with(createBooleanModelDispatch(BlockProperties.BOTTOM, resourceLocation2, resourceLocation)));
    }

    private void createCaveVines() {
        MinecraftKey resourceLocation = this.createSuffixedVariant(Blocks.CAVE_VINES, "", ModelTemplates.CROSS, TextureMapping::cross);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(Blocks.CAVE_VINES, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES).with(createBooleanModelDispatch(BlockProperties.BERRIES, resourceLocation2, resourceLocation)));
        MinecraftKey resourceLocation3 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "", ModelTemplates.CROSS, TextureMapping::cross);
        MinecraftKey resourceLocation4 = this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "_lit", ModelTemplates.CROSS, TextureMapping::cross);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.CAVE_VINES_PLANT).with(createBooleanModelDispatch(BlockProperties.BERRIES, resourceLocation4, resourceLocation3)));
    }

    private void createRedstoneLamp() {
        MinecraftKey resourceLocation = TexturedModel.CUBE.create(Blocks.REDSTONE_LAMP, this.modelOutput);
        MinecraftKey resourceLocation2 = this.createSuffixedVariant(Blocks.REDSTONE_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.REDSTONE_LAMP).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation2, resourceLocation)));
    }

    private void createNormalTorch(Block torch, Block wallTorch) {
        TextureMapping textureMapping = TextureMapping.torch(torch);
        this.blockStateOutput.accept(createSimpleBlock(torch, ModelTemplates.TORCH.create(torch, textureMapping, this.modelOutput)));
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(wallTorch, Variant.variant().with(VariantProperties.MODEL, ModelTemplates.WALL_TORCH.create(wallTorch, textureMapping, this.modelOutput))).with(createTorchHorizontalDispatch()));
        this.createSimpleFlatItemModel(torch);
        this.skipAutoItemBlock(wallTorch);
    }

    private void createRedstoneTorch() {
        TextureMapping textureMapping = TextureMapping.torch(Blocks.REDSTONE_TORCH);
        TextureMapping textureMapping2 = TextureMapping.torch(TextureMapping.getBlockTexture(Blocks.REDSTONE_TORCH, "_off"));
        MinecraftKey resourceLocation = ModelTemplates.TORCH.create(Blocks.REDSTONE_TORCH, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.TORCH.createWithSuffix(Blocks.REDSTONE_TORCH, "_off", textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.REDSTONE_TORCH).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation, resourceLocation2)));
        MinecraftKey resourceLocation3 = ModelTemplates.WALL_TORCH.create(Blocks.REDSTONE_WALL_TORCH, textureMapping, this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.WALL_TORCH.createWithSuffix(Blocks.REDSTONE_WALL_TORCH, "_off", textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.REDSTONE_WALL_TORCH).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation3, resourceLocation4)).with(createTorchHorizontalDispatch()));
        this.createSimpleFlatItemModel(Blocks.REDSTONE_TORCH);
        this.skipAutoItemBlock(Blocks.REDSTONE_WALL_TORCH);
    }

    private void createRepeater() {
        this.createSimpleFlatItemModel(Items.REPEATER);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.REPEATER).with(PropertyDispatch.properties(BlockProperties.DELAY, BlockProperties.LOCKED, BlockProperties.POWERED).generate((integer, boolean_, boolean2) -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('_').append((Object)integer).append("tick");
            if (boolean2) {
                stringBuilder.append("_on");
            }

            if (boolean_) {
                stringBuilder.append("_locked");
            }

            return Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.REPEATER, stringBuilder.toString()));
        })).with(createHorizontalFacingDispatchAlt()));
    }

    private void createSeaPickle() {
        this.createSimpleFlatItemModel(Items.SEA_PICKLE);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SEA_PICKLE).with(PropertyDispatch.properties(BlockProperties.PICKLES, BlockProperties.WATERLOGGED).select(1, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("dead_sea_pickle")))).select(2, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_dead_sea_pickles")))).select(3, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_dead_sea_pickles")))).select(4, false, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_dead_sea_pickles")))).select(1, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("sea_pickle")))).select(2, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("two_sea_pickles")))).select(3, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("three_sea_pickles")))).select(4, true, Arrays.asList(createRotatedVariants(ModelLocationUtils.decorateBlockModelLocation("four_sea_pickles"))))));
    }

    private void createSnowBlocks() {
        TextureMapping textureMapping = TextureMapping.cube(Blocks.SNOW);
        MinecraftKey resourceLocation = ModelTemplates.CUBE_ALL.create(Blocks.SNOW_BLOCK, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SNOW).with(PropertyDispatch.property(BlockProperties.LAYERS).generate((integer) -> {
            return Variant.variant().with(VariantProperties.MODEL, integer < 8 ? ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height" + integer * 2) : resourceLocation);
        })));
        this.delegateItemModel(Blocks.SNOW, ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height2"));
        this.blockStateOutput.accept(createSimpleBlock(Blocks.SNOW_BLOCK, resourceLocation));
    }

    private void createStonecutter() {
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.STONECUTTER, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.STONECUTTER))).with(createHorizontalFacingDispatch()));
    }

    private void createStructureBlock() {
        MinecraftKey resourceLocation = TexturedModel.CUBE.create(Blocks.STRUCTURE_BLOCK, this.modelOutput);
        this.delegateItemModel(Blocks.STRUCTURE_BLOCK, resourceLocation);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.STRUCTURE_BLOCK).with(PropertyDispatch.property(BlockProperties.STRUCTUREBLOCK_MODE).generate((structureMode) -> {
            return Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.STRUCTURE_BLOCK, "_" + structureMode.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube));
        })));
    }

    private void createSweetBerryBush() {
        this.createSimpleFlatItemModel(Items.SWEET_BERRIES);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.SWEET_BERRY_BUSH).with(PropertyDispatch.property(BlockProperties.AGE_3).generate((integer) -> {
            return Variant.variant().with(VariantProperties.MODEL, this.createSuffixedVariant(Blocks.SWEET_BERRY_BUSH, "_stage" + integer, ModelTemplates.CROSS, TextureMapping::cross));
        })));
    }

    private void createTripwire() {
        this.createSimpleFlatItemModel(Items.STRING);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE).with(PropertyDispatch.properties(BlockProperties.ATTACHED, BlockProperties.EAST, BlockProperties.NORTH, BlockProperties.SOUTH, BlockProperties.WEST).select(false, false, false, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))).select(false, true, false, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(false, false, true, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))).select(false, false, false, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(false, false, false, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(false, true, true, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))).select(false, true, false, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(false, false, false, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(false, false, true, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(false, false, true, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))).select(false, true, false, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(false, true, true, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))).select(false, true, false, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(false, false, true, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(false, true, true, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(false, true, true, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nsew"))).select(true, false, false, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))).select(true, false, true, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))).select(true, false, false, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(true, true, false, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(true, false, false, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(true, true, true, false, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))).select(true, true, false, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(true, false, false, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(true, false, true, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(true, false, true, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))).select(true, true, false, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(true, true, true, true, false, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))).select(true, true, false, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)).select(true, false, true, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180)).select(true, true, true, false, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270)).select(true, true, true, true, true, Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nsew")))));
    }

    private void createTripwireHook() {
        this.createSimpleFlatItemModel(Blocks.TRIPWIRE_HOOK);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.TRIPWIRE_HOOK).with(PropertyDispatch.properties(BlockProperties.ATTACHED, BlockProperties.POWERED).generate((boolean_, boolean2) -> {
            return Variant.variant().with(VariantProperties.MODEL, TextureMapping.getBlockTexture(Blocks.TRIPWIRE_HOOK, (boolean_ ? "_attached" : "") + (boolean2 ? "_on" : "")));
        })).with(createHorizontalFacingDispatch()));
    }

    private MinecraftKey createTurtleEggModel(int eggs, String prefix, TextureMapping texture) {
        switch(eggs) {
        case 1:
            return ModelTemplates.TURTLE_EGG.create(ModelLocationUtils.decorateBlockModelLocation(prefix + "turtle_egg"), texture, this.modelOutput);
        case 2:
            return ModelTemplates.TWO_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("two_" + prefix + "turtle_eggs"), texture, this.modelOutput);
        case 3:
            return ModelTemplates.THREE_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("three_" + prefix + "turtle_eggs"), texture, this.modelOutput);
        case 4:
            return ModelTemplates.FOUR_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("four_" + prefix + "turtle_eggs"), texture, this.modelOutput);
        default:
            throw new UnsupportedOperationException();
        }
    }

    private MinecraftKey createTurtleEggModel(Integer eggs, Integer hatch) {
        switch(hatch) {
        case 0:
            return this.createTurtleEggModel(eggs, "", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG)));
        case 1:
            return this.createTurtleEggModel(eggs, "slightly_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_slightly_cracked")));
        case 2:
            return this.createTurtleEggModel(eggs, "very_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_very_cracked")));
        default:
            throw new UnsupportedOperationException();
        }
    }

    private void createTurtleEgg() {
        this.createSimpleFlatItemModel(Items.TURTLE_EGG);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.TURTLE_EGG).with(PropertyDispatch.properties(BlockProperties.EGGS, BlockProperties.HATCH).generateList((integer, integer2) -> {
            return Arrays.asList(createRotatedVariants(this.createTurtleEggModel(integer, integer2)));
        })));
    }

    private void createMultiface(Block block) {
        this.createSimpleFlatItemModel(block);
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(block);
        MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(block);
        Condition.TerminalCondition terminalCondition = SystemUtils.make(Condition.condition(), (terminalConditionx) -> {
            MULTIFACE_GENERATOR.forEach((booleanProperty, function) -> {
                if (block.getBlockData().hasProperty(booleanProperty)) {
                    terminalConditionx.term(booleanProperty, false);
                }

            });
        });
        MULTIFACE_GENERATOR.forEach((booleanProperty, function) -> {
            if (block.getBlockData().hasProperty(booleanProperty)) {
                multiPartGenerator.with(Condition.condition().term(booleanProperty, true), function.apply(resourceLocation));
                multiPartGenerator.with(terminalCondition, function.apply(resourceLocation));
            }

        });
        this.blockStateOutput.accept(multiPartGenerator);
    }

    private void createMagmaBlock() {
        this.blockStateOutput.accept(createSimpleBlock(Blocks.MAGMA_BLOCK, ModelTemplates.CUBE_ALL.create(Blocks.MAGMA_BLOCK, TextureMapping.cube(ModelLocationUtils.decorateBlockModelLocation("magma")), this.modelOutput)));
    }

    private void createShulkerBox(Block shulkerBox) {
        this.createTrivialBlock(shulkerBox, TexturedModel.PARTICLE_ONLY);
        ModelTemplates.SHULKER_BOX_INVENTORY.create(ModelLocationUtils.getModelLocation(shulkerBox.getItem()), TextureMapping.particle(shulkerBox), this.modelOutput);
    }

    private void createGrowingPlant(Block plant, Block plantStem, BlockModelGenerators.TintState tintType) {
        this.createCrossBlock(plant, tintType);
        this.createCrossBlock(plantStem, tintType);
    }

    private void createBedItem(Block bed, Block particleSource) {
        ModelTemplates.BED_INVENTORY.create(ModelLocationUtils.getModelLocation(bed.getItem()), TextureMapping.particle(particleSource), this.modelOutput);
    }

    private void createInfestedStone() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.STONE);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.STONE, "_mirrored");
        this.blockStateOutput.accept(createRotatedVariant(Blocks.INFESTED_STONE, resourceLocation, resourceLocation2));
        this.delegateItemModel(Blocks.INFESTED_STONE, resourceLocation);
    }

    private void createInfestedDeepslate() {
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE);
        MinecraftKey resourceLocation2 = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE, "_mirrored");
        this.blockStateOutput.accept(createRotatedVariant(Blocks.INFESTED_DEEPSLATE, resourceLocation, resourceLocation2).with(createRotatedPillar()));
        this.delegateItemModel(Blocks.INFESTED_DEEPSLATE, resourceLocation);
    }

    private void createNetherRoots(Block root, Block pottedRoot) {
        this.createCrossBlockWithDefaultItem(root, BlockModelGenerators.TintState.NOT_TINTED);
        TextureMapping textureMapping = TextureMapping.plant(TextureMapping.getBlockTexture(root, "_pot"));
        MinecraftKey resourceLocation = BlockModelGenerators.TintState.NOT_TINTED.getCrossPot().create(pottedRoot, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(pottedRoot, resourceLocation));
    }

    private void createRespawnAnchor() {
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_bottom");
        MinecraftKey resourceLocation2 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top_off");
        MinecraftKey resourceLocation3 = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top");
        MinecraftKey[] resourceLocations = new MinecraftKey[5];

        for(int i = 0; i < 5; ++i) {
            TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.BOTTOM, resourceLocation).put(TextureSlot.TOP, i == 0 ? resourceLocation2 : resourceLocation3).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_side" + i));
            resourceLocations[i] = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.RESPAWN_ANCHOR, "_" + i, textureMapping, this.modelOutput);
        }

        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.RESPAWN_ANCHOR).with(PropertyDispatch.property(BlockProperties.RESPAWN_ANCHOR_CHARGES).generate((integer) -> {
            return Variant.variant().with(VariantProperties.MODEL, resourceLocations[integer]);
        })));
        this.delegateItemModel(Items.RESPAWN_ANCHOR, resourceLocations[0]);
    }

    private Variant applyRotation(BlockPropertyJigsawOrientation orientation, Variant variant) {
        switch(orientation) {
        case DOWN_NORTH:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90);
        case DOWN_SOUTH:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
        case DOWN_WEST:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
        case DOWN_EAST:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R90).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
        case UP_NORTH:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
        case UP_SOUTH:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270);
        case UP_WEST:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
        case UP_EAST:
            return variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270).with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
        case NORTH_UP:
            return variant;
        case SOUTH_UP:
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R180);
        case WEST_UP:
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R270);
        case EAST_UP:
            return variant.with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90);
        default:
            throw new UnsupportedOperationException("Rotation " + orientation + " can't be expressed with existing x and y values");
        }
    }

    private void createJigsaw() {
        MinecraftKey resourceLocation = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_top");
        MinecraftKey resourceLocation2 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_bottom");
        MinecraftKey resourceLocation3 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_side");
        MinecraftKey resourceLocation4 = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_lock");
        TextureMapping textureMapping = (new TextureMapping()).put(TextureSlot.DOWN, resourceLocation3).put(TextureSlot.WEST, resourceLocation3).put(TextureSlot.EAST, resourceLocation3).put(TextureSlot.PARTICLE, resourceLocation).put(TextureSlot.NORTH, resourceLocation).put(TextureSlot.SOUTH, resourceLocation2).put(TextureSlot.UP, resourceLocation4);
        MinecraftKey resourceLocation5 = ModelTemplates.CUBE_DIRECTIONAL.create(Blocks.JIGSAW, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(Blocks.JIGSAW, Variant.variant().with(VariantProperties.MODEL, resourceLocation5)).with(PropertyDispatch.property(BlockProperties.ORIENTATION).generate((frontAndTop) -> {
            return this.applyRotation(frontAndTop, Variant.variant());
        })));
    }

    private void createPetrifiedOakSlab() {
        Block block = Blocks.OAK_PLANKS;
        MinecraftKey resourceLocation = ModelLocationUtils.getModelLocation(block);
        TexturedModel texturedModel = TexturedModel.CUBE.get(block);
        Block block2 = Blocks.PETRIFIED_OAK_SLAB;
        MinecraftKey resourceLocation2 = ModelTemplates.SLAB_BOTTOM.create(block2, texturedModel.getMapping(), this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.SLAB_TOP.create(block2, texturedModel.getMapping(), this.modelOutput);
        this.blockStateOutput.accept(createSlab(block2, resourceLocation2, resourceLocation3, resourceLocation));
    }

    public void run() {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateModel).forEach((blockFamily) -> {
            this.family(blockFamily.getBaseBlock()).generateFor(blockFamily);
        });
        this.family(Blocks.CUT_COPPER).generateFor(BlockFamilies.CUT_COPPER).fullBlockCopies(Blocks.WAXED_CUT_COPPER).generateFor(BlockFamilies.WAXED_CUT_COPPER);
        this.family(Blocks.EXPOSED_CUT_COPPER).generateFor(BlockFamilies.EXPOSED_CUT_COPPER).fullBlockCopies(Blocks.WAXED_EXPOSED_CUT_COPPER).generateFor(BlockFamilies.WAXED_EXPOSED_CUT_COPPER);
        this.family(Blocks.WEATHERED_CUT_COPPER).generateFor(BlockFamilies.WEATHERED_CUT_COPPER).fullBlockCopies(Blocks.WAXED_WEATHERED_CUT_COPPER).generateFor(BlockFamilies.WAXED_WEATHERED_CUT_COPPER);
        this.family(Blocks.OXIDIZED_CUT_COPPER).generateFor(BlockFamilies.OXIDIZED_CUT_COPPER).fullBlockCopies(Blocks.WAXED_OXIDIZED_CUT_COPPER).generateFor(BlockFamilies.WAXED_OXIDIZED_CUT_COPPER);
        this.createNonTemplateModelBlock(Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.CAVE_AIR, Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.VOID_AIR, Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.BEACON);
        this.createNonTemplateModelBlock(Blocks.CACTUS);
        this.createNonTemplateModelBlock(Blocks.BUBBLE_COLUMN, Blocks.WATER);
        this.createNonTemplateModelBlock(Blocks.DRAGON_EGG);
        this.createNonTemplateModelBlock(Blocks.DRIED_KELP_BLOCK);
        this.createNonTemplateModelBlock(Blocks.ENCHANTING_TABLE);
        this.createNonTemplateModelBlock(Blocks.FLOWER_POT);
        this.createSimpleFlatItemModel(Items.FLOWER_POT);
        this.createNonTemplateModelBlock(Blocks.HONEY_BLOCK);
        this.createNonTemplateModelBlock(Blocks.WATER);
        this.createNonTemplateModelBlock(Blocks.LAVA);
        this.createNonTemplateModelBlock(Blocks.SLIME_BLOCK);
        this.createSimpleFlatItemModel(Items.CHAIN);
        this.createCandleAndCandleCake(Blocks.WHITE_CANDLE, Blocks.WHITE_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.ORANGE_CANDLE, Blocks.ORANGE_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.MAGENTA_CANDLE, Blocks.MAGENTA_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.LIGHT_BLUE_CANDLE, Blocks.LIGHT_BLUE_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.YELLOW_CANDLE, Blocks.YELLOW_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.LIME_CANDLE, Blocks.LIME_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.PINK_CANDLE, Blocks.PINK_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.GRAY_CANDLE, Blocks.GRAY_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.LIGHT_GRAY_CANDLE, Blocks.LIGHT_GRAY_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.CYAN_CANDLE, Blocks.CYAN_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.PURPLE_CANDLE, Blocks.PURPLE_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.BLUE_CANDLE, Blocks.BLUE_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.BROWN_CANDLE, Blocks.BROWN_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.GREEN_CANDLE, Blocks.GREEN_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.RED_CANDLE, Blocks.RED_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.BLACK_CANDLE, Blocks.BLACK_CANDLE_CAKE);
        this.createCandleAndCandleCake(Blocks.CANDLE, Blocks.CANDLE_CAKE);
        this.createNonTemplateModelBlock(Blocks.POTTED_BAMBOO);
        this.createNonTemplateModelBlock(Blocks.POTTED_CACTUS);
        this.createNonTemplateModelBlock(Blocks.POWDER_SNOW);
        this.createNonTemplateModelBlock(Blocks.SPORE_BLOSSOM);
        this.createAzalea(Blocks.AZALEA);
        this.createAzalea(Blocks.FLOWERING_AZALEA);
        this.createPottedAzalea(Blocks.POTTED_AZALEA);
        this.createPottedAzalea(Blocks.POTTED_FLOWERING_AZALEA);
        this.createCaveVines();
        this.createFullAndCarpetBlocks(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET);
        this.createAirLikeBlock(Blocks.BARRIER, Items.BARRIER);
        this.createSimpleFlatItemModel(Items.BARRIER);
        this.createAirLikeBlock(Blocks.LIGHT, Items.LIGHT);
        this.createLightBlockItems();
        this.createAirLikeBlock(Blocks.STRUCTURE_VOID, Items.STRUCTURE_VOID);
        this.createSimpleFlatItemModel(Items.STRUCTURE_VOID);
        this.createAirLikeBlock(Blocks.MOVING_PISTON, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        this.createTrivialCube(Blocks.COAL_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_COAL_ORE);
        this.createTrivialCube(Blocks.COAL_BLOCK);
        this.createTrivialCube(Blocks.DIAMOND_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_DIAMOND_ORE);
        this.createTrivialCube(Blocks.DIAMOND_BLOCK);
        this.createTrivialCube(Blocks.EMERALD_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_EMERALD_ORE);
        this.createTrivialCube(Blocks.EMERALD_BLOCK);
        this.createTrivialCube(Blocks.GOLD_ORE);
        this.createTrivialCube(Blocks.NETHER_GOLD_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_GOLD_ORE);
        this.createTrivialCube(Blocks.GOLD_BLOCK);
        this.createTrivialCube(Blocks.IRON_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_IRON_ORE);
        this.createTrivialCube(Blocks.IRON_BLOCK);
        this.createTrivialBlock(Blocks.ANCIENT_DEBRIS, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.NETHERITE_BLOCK);
        this.createTrivialCube(Blocks.LAPIS_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_LAPIS_ORE);
        this.createTrivialCube(Blocks.LAPIS_BLOCK);
        this.createTrivialCube(Blocks.NETHER_QUARTZ_ORE);
        this.createTrivialCube(Blocks.REDSTONE_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_REDSTONE_ORE);
        this.createTrivialCube(Blocks.REDSTONE_BLOCK);
        this.createTrivialCube(Blocks.GILDED_BLACKSTONE);
        this.createTrivialCube(Blocks.BLUE_ICE);
        this.createTrivialCube(Blocks.CLAY);
        this.createTrivialCube(Blocks.COARSE_DIRT);
        this.createTrivialCube(Blocks.CRYING_OBSIDIAN);
        this.createTrivialCube(Blocks.END_STONE);
        this.createTrivialCube(Blocks.GLOWSTONE);
        this.createTrivialCube(Blocks.GRAVEL);
        this.createTrivialCube(Blocks.HONEYCOMB_BLOCK);
        this.createTrivialCube(Blocks.ICE);
        this.createTrivialBlock(Blocks.JUKEBOX, TexturedModel.CUBE_TOP);
        this.createTrivialBlock(Blocks.LODESTONE, TexturedModel.COLUMN);
        this.createTrivialBlock(Blocks.MELON, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.NETHER_WART_BLOCK);
        this.createTrivialCube(Blocks.NOTE_BLOCK);
        this.createTrivialCube(Blocks.PACKED_ICE);
        this.createTrivialCube(Blocks.OBSIDIAN);
        this.createTrivialCube(Blocks.QUARTZ_BRICKS);
        this.createTrivialCube(Blocks.SEA_LANTERN);
        this.createTrivialCube(Blocks.SHROOMLIGHT);
        this.createTrivialCube(Blocks.SOUL_SAND);
        this.createTrivialCube(Blocks.SOUL_SOIL);
        this.createTrivialCube(Blocks.SPAWNER);
        this.createTrivialCube(Blocks.SPONGE);
        this.createTrivialBlock(Blocks.SEAGRASS, TexturedModel.SEAGRASS);
        this.createSimpleFlatItemModel(Items.SEAGRASS);
        this.createTrivialBlock(Blocks.TNT, TexturedModel.CUBE_TOP_BOTTOM);
        this.createTrivialBlock(Blocks.TARGET, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.WARPED_WART_BLOCK);
        this.createTrivialCube(Blocks.WET_SPONGE);
        this.createTrivialCube(Blocks.AMETHYST_BLOCK);
        this.createTrivialCube(Blocks.BUDDING_AMETHYST);
        this.createTrivialCube(Blocks.CALCITE);
        this.createTrivialCube(Blocks.TUFF);
        this.createTrivialCube(Blocks.DRIPSTONE_BLOCK);
        this.createTrivialCube(Blocks.RAW_IRON_BLOCK);
        this.createTrivialCube(Blocks.RAW_COPPER_BLOCK);
        this.createTrivialCube(Blocks.RAW_GOLD_BLOCK);
        this.createPetrifiedOakSlab();
        this.createTrivialCube(Blocks.COPPER_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_COPPER_ORE);
        this.createTrivialCube(Blocks.COPPER_BLOCK);
        this.createTrivialCube(Blocks.EXPOSED_COPPER);
        this.createTrivialCube(Blocks.WEATHERED_COPPER);
        this.createTrivialCube(Blocks.OXIDIZED_COPPER);
        this.copyModel(Blocks.COPPER_BLOCK, Blocks.WAXED_COPPER_BLOCK);
        this.copyModel(Blocks.EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER);
        this.copyModel(Blocks.WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER);
        this.copyModel(Blocks.OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER);
        this.createWeightedPressurePlate(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.GOLD_BLOCK);
        this.createWeightedPressurePlate(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.IRON_BLOCK);
        this.createAmethystClusters();
        this.createBookshelf();
        this.createBrewingStand();
        this.createCakeBlock();
        this.createCampfires(Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
        this.createCartographyTable();
        this.createCauldrons();
        this.createChorusFlower();
        this.createChorusPlant();
        this.createComposter();
        this.createDaylightDetector();
        this.createEndPortalFrame();
        this.createRotatableColumn(Blocks.END_ROD);
        this.createLightningRod();
        this.createFarmland();
        this.createFire();
        this.createSoulFire();
        this.createFrostedIce();
        this.createGrassBlocks();
        this.createCocoa();
        this.createDirtPath();
        this.createGrindstone();
        this.createHopper();
        this.createIronBars();
        this.createLever();
        this.createLilyPad();
        this.createNetherPortalBlock();
        this.createNetherrack();
        this.createObserver();
        this.createPistons();
        this.createPistonHeads();
        this.createScaffolding();
        this.createRedstoneTorch();
        this.createRedstoneLamp();
        this.createRepeater();
        this.createSeaPickle();
        this.createSmithingTable();
        this.createSnowBlocks();
        this.createStonecutter();
        this.createStructureBlock();
        this.createSweetBerryBush();
        this.createTripwire();
        this.createTripwireHook();
        this.createTurtleEgg();
        this.createMultiface(Blocks.VINE);
        this.createMultiface(Blocks.GLOW_LICHEN);
        this.createMagmaBlock();
        this.createJigsaw();
        this.createSculkSensor();
        this.createNonTemplateHorizontalBlock(Blocks.LADDER);
        this.createSimpleFlatItemModel(Blocks.LADDER);
        this.createNonTemplateHorizontalBlock(Blocks.LECTERN);
        this.createBigDripLeafBlock();
        this.createNonTemplateHorizontalBlock(Blocks.BIG_DRIPLEAF_STEM);
        this.createNormalTorch(Blocks.TORCH, Blocks.WALL_TORCH);
        this.createNormalTorch(Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH);
        this.createCraftingTableLike(Blocks.CRAFTING_TABLE, Blocks.OAK_PLANKS, TextureMapping::craftingTable);
        this.createCraftingTableLike(Blocks.FLETCHING_TABLE, Blocks.BIRCH_PLANKS, TextureMapping::fletchingTable);
        this.createNyliumBlock(Blocks.CRIMSON_NYLIUM);
        this.createNyliumBlock(Blocks.WARPED_NYLIUM);
        this.createDispenserBlock(Blocks.DISPENSER);
        this.createDispenserBlock(Blocks.DROPPER);
        this.createLantern(Blocks.LANTERN);
        this.createLantern(Blocks.SOUL_LANTERN);
        this.createAxisAlignedPillarBlockCustomModel(Blocks.CHAIN, ModelLocationUtils.getModelLocation(Blocks.CHAIN));
        this.createAxisAlignedPillarBlock(Blocks.BASALT, TexturedModel.COLUMN);
        this.createAxisAlignedPillarBlock(Blocks.POLISHED_BASALT, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.SMOOTH_BASALT);
        this.createAxisAlignedPillarBlock(Blocks.BONE_BLOCK, TexturedModel.COLUMN);
        this.createRotatedVariantBlock(Blocks.DIRT);
        this.createRotatedVariantBlock(Blocks.ROOTED_DIRT);
        this.createRotatedVariantBlock(Blocks.SAND);
        this.createRotatedVariantBlock(Blocks.RED_SAND);
        this.createRotatedMirroredVariantBlock(Blocks.BEDROCK);
        this.createRotatedPillarWithHorizontalVariant(Blocks.HAY_BLOCK, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        this.createRotatedPillarWithHorizontalVariant(Blocks.PURPUR_PILLAR, TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        this.createRotatedPillarWithHorizontalVariant(Blocks.QUARTZ_PILLAR, TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        this.createHorizontallyRotatedBlock(Blocks.LOOM, TexturedModel.ORIENTABLE);
        this.createPumpkins();
        this.createBeeNest(Blocks.BEE_NEST, TextureMapping::orientableCube);
        this.createBeeNest(Blocks.BEEHIVE, TextureMapping::orientableCubeSameEnds);
        this.createCropBlock(Blocks.BEETROOTS, BlockProperties.AGE_3, 0, 1, 2, 3);
        this.createCropBlock(Blocks.CARROTS, BlockProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
        this.createCropBlock(Blocks.NETHER_WART, BlockProperties.AGE_3, 0, 1, 1, 2);
        this.createCropBlock(Blocks.POTATOES, BlockProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
        this.createCropBlock(Blocks.WHEAT, BlockProperties.AGE_7, 0, 1, 2, 3, 4, 5, 6, 7);
        this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("banner"), Blocks.OAK_PLANKS).createWithCustomBlockItemModel(ModelTemplates.BANNER_INVENTORY, Blocks.WHITE_BANNER, Blocks.ORANGE_BANNER, Blocks.MAGENTA_BANNER, Blocks.LIGHT_BLUE_BANNER, Blocks.YELLOW_BANNER, Blocks.LIME_BANNER, Blocks.PINK_BANNER, Blocks.GRAY_BANNER, Blocks.LIGHT_GRAY_BANNER, Blocks.CYAN_BANNER, Blocks.PURPLE_BANNER, Blocks.BLUE_BANNER, Blocks.BROWN_BANNER, Blocks.GREEN_BANNER, Blocks.RED_BANNER, Blocks.BLACK_BANNER).createWithoutBlockItem(Blocks.WHITE_WALL_BANNER, Blocks.ORANGE_WALL_BANNER, Blocks.MAGENTA_WALL_BANNER, Blocks.LIGHT_BLUE_WALL_BANNER, Blocks.YELLOW_WALL_BANNER, Blocks.LIME_WALL_BANNER, Blocks.PINK_WALL_BANNER, Blocks.GRAY_WALL_BANNER, Blocks.LIGHT_GRAY_WALL_BANNER, Blocks.CYAN_WALL_BANNER, Blocks.PURPLE_WALL_BANNER, Blocks.BLUE_WALL_BANNER, Blocks.BROWN_WALL_BANNER, Blocks.GREEN_WALL_BANNER, Blocks.RED_WALL_BANNER, Blocks.BLACK_WALL_BANNER);
        this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("bed"), Blocks.OAK_PLANKS).createWithoutBlockItem(Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED, Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED);
        this.createBedItem(Blocks.WHITE_BED, Blocks.WHITE_WOOL);
        this.createBedItem(Blocks.ORANGE_BED, Blocks.ORANGE_WOOL);
        this.createBedItem(Blocks.MAGENTA_BED, Blocks.MAGENTA_WOOL);
        this.createBedItem(Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL);
        this.createBedItem(Blocks.YELLOW_BED, Blocks.YELLOW_WOOL);
        this.createBedItem(Blocks.LIME_BED, Blocks.LIME_WOOL);
        this.createBedItem(Blocks.PINK_BED, Blocks.PINK_WOOL);
        this.createBedItem(Blocks.GRAY_BED, Blocks.GRAY_WOOL);
        this.createBedItem(Blocks.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL);
        this.createBedItem(Blocks.CYAN_BED, Blocks.CYAN_WOOL);
        this.createBedItem(Blocks.PURPLE_BED, Blocks.PURPLE_WOOL);
        this.createBedItem(Blocks.BLUE_BED, Blocks.BLUE_WOOL);
        this.createBedItem(Blocks.BROWN_BED, Blocks.BROWN_WOOL);
        this.createBedItem(Blocks.GREEN_BED, Blocks.GREEN_WOOL);
        this.createBedItem(Blocks.RED_BED, Blocks.RED_WOOL);
        this.createBedItem(Blocks.BLACK_BED, Blocks.BLACK_WOOL);
        this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("skull"), Blocks.SOUL_SAND).createWithCustomBlockItemModel(ModelTemplates.SKULL_INVENTORY, Blocks.CREEPER_HEAD, Blocks.PLAYER_HEAD, Blocks.ZOMBIE_HEAD, Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL).create(Blocks.DRAGON_HEAD).createWithoutBlockItem(Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL);
        this.createShulkerBox(Blocks.SHULKER_BOX);
        this.createShulkerBox(Blocks.WHITE_SHULKER_BOX);
        this.createShulkerBox(Blocks.ORANGE_SHULKER_BOX);
        this.createShulkerBox(Blocks.MAGENTA_SHULKER_BOX);
        this.createShulkerBox(Blocks.LIGHT_BLUE_SHULKER_BOX);
        this.createShulkerBox(Blocks.YELLOW_SHULKER_BOX);
        this.createShulkerBox(Blocks.LIME_SHULKER_BOX);
        this.createShulkerBox(Blocks.PINK_SHULKER_BOX);
        this.createShulkerBox(Blocks.GRAY_SHULKER_BOX);
        this.createShulkerBox(Blocks.LIGHT_GRAY_SHULKER_BOX);
        this.createShulkerBox(Blocks.CYAN_SHULKER_BOX);
        this.createShulkerBox(Blocks.PURPLE_SHULKER_BOX);
        this.createShulkerBox(Blocks.BLUE_SHULKER_BOX);
        this.createShulkerBox(Blocks.BROWN_SHULKER_BOX);
        this.createShulkerBox(Blocks.GREEN_SHULKER_BOX);
        this.createShulkerBox(Blocks.RED_SHULKER_BOX);
        this.createShulkerBox(Blocks.BLACK_SHULKER_BOX);
        this.createTrivialBlock(Blocks.CONDUIT, TexturedModel.PARTICLE_ONLY);
        this.skipAutoItemBlock(Blocks.CONDUIT);
        this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("chest"), Blocks.OAK_PLANKS).createWithoutBlockItem(Blocks.CHEST, Blocks.TRAPPED_CHEST);
        this.blockEntityModels(ModelLocationUtils.decorateBlockModelLocation("ender_chest"), Blocks.OBSIDIAN).createWithoutBlockItem(Blocks.ENDER_CHEST);
        this.blockEntityModels(Blocks.END_PORTAL, Blocks.OBSIDIAN).create(Blocks.END_PORTAL, Blocks.END_GATEWAY);
        this.createTrivialCube(Blocks.AZALEA_LEAVES);
        this.createTrivialCube(Blocks.FLOWERING_AZALEA_LEAVES);
        this.createTrivialCube(Blocks.WHITE_CONCRETE);
        this.createTrivialCube(Blocks.ORANGE_CONCRETE);
        this.createTrivialCube(Blocks.MAGENTA_CONCRETE);
        this.createTrivialCube(Blocks.LIGHT_BLUE_CONCRETE);
        this.createTrivialCube(Blocks.YELLOW_CONCRETE);
        this.createTrivialCube(Blocks.LIME_CONCRETE);
        this.createTrivialCube(Blocks.PINK_CONCRETE);
        this.createTrivialCube(Blocks.GRAY_CONCRETE);
        this.createTrivialCube(Blocks.LIGHT_GRAY_CONCRETE);
        this.createTrivialCube(Blocks.CYAN_CONCRETE);
        this.createTrivialCube(Blocks.PURPLE_CONCRETE);
        this.createTrivialCube(Blocks.BLUE_CONCRETE);
        this.createTrivialCube(Blocks.BROWN_CONCRETE);
        this.createTrivialCube(Blocks.GREEN_CONCRETE);
        this.createTrivialCube(Blocks.RED_CONCRETE);
        this.createTrivialCube(Blocks.BLACK_CONCRETE);
        this.createColoredBlockWithRandomRotations(TexturedModel.CUBE, Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER, Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER, Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER);
        this.createTrivialCube(Blocks.TERRACOTTA);
        this.createTrivialCube(Blocks.WHITE_TERRACOTTA);
        this.createTrivialCube(Blocks.ORANGE_TERRACOTTA);
        this.createTrivialCube(Blocks.MAGENTA_TERRACOTTA);
        this.createTrivialCube(Blocks.LIGHT_BLUE_TERRACOTTA);
        this.createTrivialCube(Blocks.YELLOW_TERRACOTTA);
        this.createTrivialCube(Blocks.LIME_TERRACOTTA);
        this.createTrivialCube(Blocks.PINK_TERRACOTTA);
        this.createTrivialCube(Blocks.GRAY_TERRACOTTA);
        this.createTrivialCube(Blocks.LIGHT_GRAY_TERRACOTTA);
        this.createTrivialCube(Blocks.CYAN_TERRACOTTA);
        this.createTrivialCube(Blocks.PURPLE_TERRACOTTA);
        this.createTrivialCube(Blocks.BLUE_TERRACOTTA);
        this.createTrivialCube(Blocks.BROWN_TERRACOTTA);
        this.createTrivialCube(Blocks.GREEN_TERRACOTTA);
        this.createTrivialCube(Blocks.RED_TERRACOTTA);
        this.createTrivialCube(Blocks.BLACK_TERRACOTTA);
        this.createTrivialCube(Blocks.TINTED_GLASS);
        this.createGlassBlocks(Blocks.GLASS, Blocks.GLASS_PANE);
        this.createGlassBlocks(Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.PINK_STAINED_GLASS, Blocks.PINK_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.GRAY_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.BLUE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE);
        this.createGlassBlocks(Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE);
        this.createColoredBlockWithStateRotations(TexturedModel.GLAZED_TERRACOTTA, Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA, Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA, Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA, Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA, Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA, Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA, Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA, Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA);
        this.createFullAndCarpetBlocks(Blocks.WHITE_WOOL, Blocks.WHITE_CARPET);
        this.createFullAndCarpetBlocks(Blocks.ORANGE_WOOL, Blocks.ORANGE_CARPET);
        this.createFullAndCarpetBlocks(Blocks.MAGENTA_WOOL, Blocks.MAGENTA_CARPET);
        this.createFullAndCarpetBlocks(Blocks.LIGHT_BLUE_WOOL, Blocks.LIGHT_BLUE_CARPET);
        this.createFullAndCarpetBlocks(Blocks.YELLOW_WOOL, Blocks.YELLOW_CARPET);
        this.createFullAndCarpetBlocks(Blocks.LIME_WOOL, Blocks.LIME_CARPET);
        this.createFullAndCarpetBlocks(Blocks.PINK_WOOL, Blocks.PINK_CARPET);
        this.createFullAndCarpetBlocks(Blocks.GRAY_WOOL, Blocks.GRAY_CARPET);
        this.createFullAndCarpetBlocks(Blocks.LIGHT_GRAY_WOOL, Blocks.LIGHT_GRAY_CARPET);
        this.createFullAndCarpetBlocks(Blocks.CYAN_WOOL, Blocks.CYAN_CARPET);
        this.createFullAndCarpetBlocks(Blocks.PURPLE_WOOL, Blocks.PURPLE_CARPET);
        this.createFullAndCarpetBlocks(Blocks.BLUE_WOOL, Blocks.BLUE_CARPET);
        this.createFullAndCarpetBlocks(Blocks.BROWN_WOOL, Blocks.BROWN_CARPET);
        this.createFullAndCarpetBlocks(Blocks.GREEN_WOOL, Blocks.GREEN_CARPET);
        this.createFullAndCarpetBlocks(Blocks.RED_WOOL, Blocks.RED_CARPET);
        this.createFullAndCarpetBlocks(Blocks.BLACK_WOOL, Blocks.BLACK_CARPET);
        this.createPlant(Blocks.FERN, Blocks.POTTED_FERN, BlockModelGenerators.TintState.TINTED);
        this.createPlant(Blocks.DANDELION, Blocks.POTTED_DANDELION, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.POPPY, Blocks.POTTED_POPPY, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.BLUE_ORCHID, Blocks.POTTED_BLUE_ORCHID, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.ALLIUM, Blocks.POTTED_ALLIUM, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.AZURE_BLUET, Blocks.POTTED_AZURE_BLUET, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.RED_TULIP, Blocks.POTTED_RED_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.ORANGE_TULIP, Blocks.POTTED_ORANGE_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.WHITE_TULIP, Blocks.POTTED_WHITE_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.PINK_TULIP, Blocks.POTTED_PINK_TULIP, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.OXEYE_DAISY, Blocks.POTTED_OXEYE_DAISY, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.CORNFLOWER, Blocks.POTTED_CORNFLOWER, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.RED_MUSHROOM, Blocks.POTTED_RED_MUSHROOM, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.BROWN_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPlant(Blocks.DEAD_BUSH, Blocks.POTTED_DEAD_BUSH, BlockModelGenerators.TintState.NOT_TINTED);
        this.createPointedDripstone();
        this.createMushroomBlock(Blocks.BROWN_MUSHROOM_BLOCK);
        this.createMushroomBlock(Blocks.RED_MUSHROOM_BLOCK);
        this.createMushroomBlock(Blocks.MUSHROOM_STEM);
        this.createCrossBlockWithDefaultItem(Blocks.GRASS, BlockModelGenerators.TintState.TINTED);
        this.createCrossBlock(Blocks.SUGAR_CANE, BlockModelGenerators.TintState.TINTED);
        this.createSimpleFlatItemModel(Items.SUGAR_CANE);
        this.createGrowingPlant(Blocks.KELP, Blocks.KELP_PLANT, BlockModelGenerators.TintState.TINTED);
        this.createSimpleFlatItemModel(Items.KELP);
        this.skipAutoItemBlock(Blocks.KELP_PLANT);
        this.createCrossBlock(Blocks.HANGING_ROOTS, BlockModelGenerators.TintState.NOT_TINTED);
        this.skipAutoItemBlock(Blocks.HANGING_ROOTS);
        this.skipAutoItemBlock(Blocks.CAVE_VINES_PLANT);
        this.createGrowingPlant(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, BlockModelGenerators.TintState.NOT_TINTED);
        this.createGrowingPlant(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, BlockModelGenerators.TintState.NOT_TINTED);
        this.createSimpleFlatItemModel(Blocks.WEEPING_VINES, "_plant");
        this.skipAutoItemBlock(Blocks.WEEPING_VINES_PLANT);
        this.createSimpleFlatItemModel(Blocks.TWISTING_VINES, "_plant");
        this.skipAutoItemBlock(Blocks.TWISTING_VINES_PLANT);
        this.createCrossBlockWithDefaultItem(Blocks.BAMBOO_SAPLING, BlockModelGenerators.TintState.TINTED, TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.BAMBOO, "_stage0")));
        this.createBamboo();
        this.createCrossBlockWithDefaultItem(Blocks.COBWEB, BlockModelGenerators.TintState.NOT_TINTED);
        this.createDoublePlant(Blocks.LILAC, BlockModelGenerators.TintState.NOT_TINTED);
        this.createDoublePlant(Blocks.ROSE_BUSH, BlockModelGenerators.TintState.NOT_TINTED);
        this.createDoublePlant(Blocks.PEONY, BlockModelGenerators.TintState.NOT_TINTED);
        this.createDoublePlant(Blocks.TALL_GRASS, BlockModelGenerators.TintState.TINTED);
        this.createDoublePlant(Blocks.LARGE_FERN, BlockModelGenerators.TintState.TINTED);
        this.createSunflower();
        this.createTallSeagrass();
        this.createSmallDripleaf();
        this.createCoral(Blocks.TUBE_CORAL, Blocks.DEAD_TUBE_CORAL, Blocks.TUBE_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.TUBE_CORAL_FAN, Blocks.DEAD_TUBE_CORAL_FAN, Blocks.TUBE_CORAL_WALL_FAN, Blocks.DEAD_TUBE_CORAL_WALL_FAN);
        this.createCoral(Blocks.BRAIN_CORAL, Blocks.DEAD_BRAIN_CORAL, Blocks.BRAIN_CORAL_BLOCK, Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.BRAIN_CORAL_FAN, Blocks.DEAD_BRAIN_CORAL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.DEAD_BRAIN_CORAL_WALL_FAN);
        this.createCoral(Blocks.BUBBLE_CORAL, Blocks.DEAD_BUBBLE_CORAL, Blocks.BUBBLE_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.BUBBLE_CORAL_FAN, Blocks.DEAD_BUBBLE_CORAL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.DEAD_BUBBLE_CORAL_WALL_FAN);
        this.createCoral(Blocks.FIRE_CORAL, Blocks.DEAD_FIRE_CORAL, Blocks.FIRE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.FIRE_CORAL_FAN, Blocks.DEAD_FIRE_CORAL_FAN, Blocks.FIRE_CORAL_WALL_FAN, Blocks.DEAD_FIRE_CORAL_WALL_FAN);
        this.createCoral(Blocks.HORN_CORAL, Blocks.DEAD_HORN_CORAL, Blocks.HORN_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.HORN_CORAL_FAN, Blocks.DEAD_HORN_CORAL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.DEAD_HORN_CORAL_WALL_FAN);
        this.createStems(Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM);
        this.createStems(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
        this.woodProvider(Blocks.ACACIA_LOG).logWithHorizontal(Blocks.ACACIA_LOG).wood(Blocks.ACACIA_WOOD);
        this.woodProvider(Blocks.STRIPPED_ACACIA_LOG).logWithHorizontal(Blocks.STRIPPED_ACACIA_LOG).wood(Blocks.STRIPPED_ACACIA_WOOD);
        this.createPlant(Blocks.ACACIA_SAPLING, Blocks.POTTED_ACACIA_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.ACACIA_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.BIRCH_LOG).logWithHorizontal(Blocks.BIRCH_LOG).wood(Blocks.BIRCH_WOOD);
        this.woodProvider(Blocks.STRIPPED_BIRCH_LOG).logWithHorizontal(Blocks.STRIPPED_BIRCH_LOG).wood(Blocks.STRIPPED_BIRCH_WOOD);
        this.createPlant(Blocks.BIRCH_SAPLING, Blocks.POTTED_BIRCH_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.BIRCH_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.OAK_LOG).logWithHorizontal(Blocks.OAK_LOG).wood(Blocks.OAK_WOOD);
        this.woodProvider(Blocks.STRIPPED_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_OAK_LOG).wood(Blocks.STRIPPED_OAK_WOOD);
        this.createPlant(Blocks.OAK_SAPLING, Blocks.POTTED_OAK_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.OAK_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.SPRUCE_LOG).logWithHorizontal(Blocks.SPRUCE_LOG).wood(Blocks.SPRUCE_WOOD);
        this.woodProvider(Blocks.STRIPPED_SPRUCE_LOG).logWithHorizontal(Blocks.STRIPPED_SPRUCE_LOG).wood(Blocks.STRIPPED_SPRUCE_WOOD);
        this.createPlant(Blocks.SPRUCE_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.SPRUCE_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.DARK_OAK_LOG).logWithHorizontal(Blocks.DARK_OAK_LOG).wood(Blocks.DARK_OAK_WOOD);
        this.woodProvider(Blocks.STRIPPED_DARK_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_DARK_OAK_LOG).wood(Blocks.STRIPPED_DARK_OAK_WOOD);
        this.createPlant(Blocks.DARK_OAK_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.DARK_OAK_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.JUNGLE_LOG).logWithHorizontal(Blocks.JUNGLE_LOG).wood(Blocks.JUNGLE_WOOD);
        this.woodProvider(Blocks.STRIPPED_JUNGLE_LOG).logWithHorizontal(Blocks.STRIPPED_JUNGLE_LOG).wood(Blocks.STRIPPED_JUNGLE_WOOD);
        this.createPlant(Blocks.JUNGLE_SAPLING, Blocks.POTTED_JUNGLE_SAPLING, BlockModelGenerators.TintState.NOT_TINTED);
        this.createTrivialBlock(Blocks.JUNGLE_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.CRIMSON_STEM).log(Blocks.CRIMSON_STEM).wood(Blocks.CRIMSON_HYPHAE);
        this.woodProvider(Blocks.STRIPPED_CRIMSON_STEM).log(Blocks.STRIPPED_CRIMSON_STEM).wood(Blocks.STRIPPED_CRIMSON_HYPHAE);
        this.createPlant(Blocks.CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_FUNGUS, BlockModelGenerators.TintState.NOT_TINTED);
        this.createNetherRoots(Blocks.CRIMSON_ROOTS, Blocks.POTTED_CRIMSON_ROOTS);
        this.woodProvider(Blocks.WARPED_STEM).log(Blocks.WARPED_STEM).wood(Blocks.WARPED_HYPHAE);
        this.woodProvider(Blocks.STRIPPED_WARPED_STEM).log(Blocks.STRIPPED_WARPED_STEM).wood(Blocks.STRIPPED_WARPED_HYPHAE);
        this.createPlant(Blocks.WARPED_FUNGUS, Blocks.POTTED_WARPED_FUNGUS, BlockModelGenerators.TintState.NOT_TINTED);
        this.createNetherRoots(Blocks.WARPED_ROOTS, Blocks.POTTED_WARPED_ROOTS);
        this.createCrossBlock(Blocks.NETHER_SPROUTS, BlockModelGenerators.TintState.NOT_TINTED);
        this.createSimpleFlatItemModel(Items.NETHER_SPROUTS);
        this.createDoor(Blocks.IRON_DOOR);
        this.createTrapdoor(Blocks.IRON_TRAPDOOR);
        this.createSmoothStoneSlab();
        this.createPassiveRail(Blocks.RAIL);
        this.createActiveRail(Blocks.POWERED_RAIL);
        this.createActiveRail(Blocks.DETECTOR_RAIL);
        this.createActiveRail(Blocks.ACTIVATOR_RAIL);
        this.createComparator();
        this.createCommandBlock(Blocks.COMMAND_BLOCK);
        this.createCommandBlock(Blocks.REPEATING_COMMAND_BLOCK);
        this.createCommandBlock(Blocks.CHAIN_COMMAND_BLOCK);
        this.createAnvil(Blocks.ANVIL);
        this.createAnvil(Blocks.CHIPPED_ANVIL);
        this.createAnvil(Blocks.DAMAGED_ANVIL);
        this.createBarrel();
        this.createBell();
        this.createFurnace(Blocks.FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        this.createFurnace(Blocks.BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        this.createFurnace(Blocks.SMOKER, TexturedModel.ORIENTABLE);
        this.createRedstoneWire();
        this.createRespawnAnchor();
        this.copyModel(Blocks.CHISELED_STONE_BRICKS, Blocks.INFESTED_CHISELED_STONE_BRICKS);
        this.copyModel(Blocks.COBBLESTONE, Blocks.INFESTED_COBBLESTONE);
        this.copyModel(Blocks.CRACKED_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS);
        this.copyModel(Blocks.MOSSY_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS);
        this.createInfestedStone();
        this.copyModel(Blocks.STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS);
        this.createInfestedDeepslate();
        ItemMonsterEgg.eggs().forEach((spawnEggItem) -> {
            this.delegateItemModel(spawnEggItem, ModelLocationUtils.decorateItemModelLocation("template_spawn_egg"));
        });
    }

    private void createLightBlockItems() {
        this.skipAutoItemBlock(Blocks.LIGHT);

        for(int i = 0; i < 16; ++i) {
            String string = String.format("_%02d", i);
            ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(Items.LIGHT, string), TextureMapping.layer0(TextureMapping.getItemTexture(Items.LIGHT, string)), this.modelOutput);
        }

    }

    private void createCandleAndCandleCake(Block candle, Block block) {
        this.createSimpleFlatItemModel(candle.getItem());
        TextureMapping textureMapping = TextureMapping.cube(TextureMapping.getBlockTexture(candle));
        TextureMapping textureMapping2 = TextureMapping.cube(TextureMapping.getBlockTexture(candle, "_lit"));
        MinecraftKey resourceLocation = ModelTemplates.CANDLE.createWithSuffix(candle, "_one_candle", textureMapping, this.modelOutput);
        MinecraftKey resourceLocation2 = ModelTemplates.TWO_CANDLES.createWithSuffix(candle, "_two_candles", textureMapping, this.modelOutput);
        MinecraftKey resourceLocation3 = ModelTemplates.THREE_CANDLES.createWithSuffix(candle, "_three_candles", textureMapping, this.modelOutput);
        MinecraftKey resourceLocation4 = ModelTemplates.FOUR_CANDLES.createWithSuffix(candle, "_four_candles", textureMapping, this.modelOutput);
        MinecraftKey resourceLocation5 = ModelTemplates.CANDLE.createWithSuffix(candle, "_one_candle_lit", textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation6 = ModelTemplates.TWO_CANDLES.createWithSuffix(candle, "_two_candles_lit", textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation7 = ModelTemplates.THREE_CANDLES.createWithSuffix(candle, "_three_candles_lit", textureMapping2, this.modelOutput);
        MinecraftKey resourceLocation8 = ModelTemplates.FOUR_CANDLES.createWithSuffix(candle, "_four_candles_lit", textureMapping2, this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(candle).with(PropertyDispatch.properties(BlockProperties.CANDLES, BlockProperties.LIT).select(1, false, Variant.variant().with(VariantProperties.MODEL, resourceLocation)).select(2, false, Variant.variant().with(VariantProperties.MODEL, resourceLocation2)).select(3, false, Variant.variant().with(VariantProperties.MODEL, resourceLocation3)).select(4, false, Variant.variant().with(VariantProperties.MODEL, resourceLocation4)).select(1, true, Variant.variant().with(VariantProperties.MODEL, resourceLocation5)).select(2, true, Variant.variant().with(VariantProperties.MODEL, resourceLocation6)).select(3, true, Variant.variant().with(VariantProperties.MODEL, resourceLocation7)).select(4, true, Variant.variant().with(VariantProperties.MODEL, resourceLocation8))));
        MinecraftKey resourceLocation9 = ModelTemplates.CANDLE_CAKE.create(block, TextureMapping.candleCake(candle, false), this.modelOutput);
        MinecraftKey resourceLocation10 = ModelTemplates.CANDLE_CAKE.createWithSuffix(block, "_lit", TextureMapping.candleCake(candle, true), this.modelOutput);
        this.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(createBooleanModelDispatch(BlockProperties.LIT, resourceLocation10, resourceLocation9)));
    }

    class BlockEntityModelGenerator {
        private final MinecraftKey baseModel;

        public BlockEntityModelGenerator(MinecraftKey modelId, Block block) {
            this.baseModel = ModelTemplates.PARTICLE_ONLY.create(modelId, TextureMapping.particle(block), BlockModelGenerators.this.modelOutput);
        }

        public BlockModelGenerators.BlockEntityModelGenerator create(Block... blocks) {
            for(Block block : blocks) {
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, this.baseModel));
            }

            return this;
        }

        public BlockModelGenerators.BlockEntityModelGenerator createWithoutBlockItem(Block... blocks) {
            for(Block block : blocks) {
                BlockModelGenerators.this.skipAutoItemBlock(block);
            }

            return this.create(blocks);
        }

        public BlockModelGenerators.BlockEntityModelGenerator createWithCustomBlockItemModel(ModelTemplate model, Block... blocks) {
            for(Block block : blocks) {
                model.create(ModelLocationUtils.getModelLocation(block.getItem()), TextureMapping.particle(block), BlockModelGenerators.this.modelOutput);
            }

            return this.create(blocks);
        }
    }

    class BlockFamilyProvider {
        private final TextureMapping mapping;
        private final Map<ModelTemplate, MinecraftKey> models = Maps.newHashMap();
        @Nullable
        private BlockFamily family;
        @Nullable
        private MinecraftKey fullBlock;

        public BlockFamilyProvider(TextureMapping texture) {
            this.mapping = texture;
        }

        public BlockModelGenerators.BlockFamilyProvider fullBlock(Block block, ModelTemplate model) {
            this.fullBlock = model.create(block, this.mapping, BlockModelGenerators.this.modelOutput);
            if (BlockModelGenerators.this.fullBlockModelCustomGenerators.containsKey(block)) {
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.this.fullBlockModelCustomGenerators.get(block).create(block, this.fullBlock, this.mapping, BlockModelGenerators.this.modelOutput));
            } else {
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, this.fullBlock));
            }

            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider fullBlockCopies(Block... blocks) {
            if (this.fullBlock == null) {
                throw new IllegalStateException("Full block not generated yet");
            } else {
                for(Block block : blocks) {
                    BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, this.fullBlock));
                    BlockModelGenerators.this.delegateItemModel(block, this.fullBlock);
                }

                return this;
            }
        }

        public BlockModelGenerators.BlockFamilyProvider button(Block buttonBlock) {
            MinecraftKey resourceLocation = ModelTemplates.BUTTON.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.BUTTON_PRESSED.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createButton(buttonBlock, resourceLocation, resourceLocation2));
            MinecraftKey resourceLocation3 = ModelTemplates.BUTTON_INVENTORY.create(buttonBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.delegateItemModel(buttonBlock, resourceLocation3);
            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider wall(Block wallBlock) {
            MinecraftKey resourceLocation = ModelTemplates.WALL_POST.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.WALL_LOW_SIDE.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation3 = ModelTemplates.WALL_TALL_SIDE.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createWall(wallBlock, resourceLocation, resourceLocation2, resourceLocation3));
            MinecraftKey resourceLocation4 = ModelTemplates.WALL_INVENTORY.create(wallBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.delegateItemModel(wallBlock, resourceLocation4);
            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider fence(Block fenceBlock) {
            MinecraftKey resourceLocation = ModelTemplates.FENCE_POST.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.FENCE_SIDE.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFence(fenceBlock, resourceLocation, resourceLocation2));
            MinecraftKey resourceLocation3 = ModelTemplates.FENCE_INVENTORY.create(fenceBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.delegateItemModel(fenceBlock, resourceLocation3);
            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider fenceGate(Block fenceGateBlock) {
            MinecraftKey resourceLocation = ModelTemplates.FENCE_GATE_OPEN.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.FENCE_GATE_CLOSED.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation3 = ModelTemplates.FENCE_GATE_WALL_OPEN.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation4 = ModelTemplates.FENCE_GATE_WALL_CLOSED.create(fenceGateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createFenceGate(fenceGateBlock, resourceLocation, resourceLocation2, resourceLocation3, resourceLocation4));
            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider pressurePlate(Block pressurePlateBlock) {
            MinecraftKey resourceLocation = ModelTemplates.PRESSURE_PLATE_UP.create(pressurePlateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.PRESSURE_PLATE_DOWN.create(pressurePlateBlock, this.mapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(pressurePlateBlock, resourceLocation, resourceLocation2));
            return this;
        }

        public BlockModelGenerators.BlockFamilyProvider sign(Block signBlock) {
            if (this.family == null) {
                throw new IllegalStateException("Family not defined");
            } else {
                Block block = this.family.getVariants().get(BlockFamily.Variant.WALL_SIGN);
                MinecraftKey resourceLocation = ModelTemplates.PARTICLE_ONLY.create(signBlock, this.mapping, BlockModelGenerators.this.modelOutput);
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(signBlock, resourceLocation));
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, resourceLocation));
                BlockModelGenerators.this.createSimpleFlatItemModel(signBlock.getItem());
                BlockModelGenerators.this.skipAutoItemBlock(block);
                return this;
            }
        }

        public BlockModelGenerators.BlockFamilyProvider slab(Block block) {
            if (this.fullBlock == null) {
                throw new IllegalStateException("Full block not generated yet");
            } else {
                MinecraftKey resourceLocation = this.getOrCreateModel(ModelTemplates.SLAB_BOTTOM, block);
                MinecraftKey resourceLocation2 = this.getOrCreateModel(ModelTemplates.SLAB_TOP, block);
                BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSlab(block, resourceLocation, resourceLocation2, this.fullBlock));
                BlockModelGenerators.this.delegateItemModel(block, resourceLocation);
                return this;
            }
        }

        public BlockModelGenerators.BlockFamilyProvider stairs(Block block) {
            MinecraftKey resourceLocation = this.getOrCreateModel(ModelTemplates.STAIRS_INNER, block);
            MinecraftKey resourceLocation2 = this.getOrCreateModel(ModelTemplates.STAIRS_STRAIGHT, block);
            MinecraftKey resourceLocation3 = this.getOrCreateModel(ModelTemplates.STAIRS_OUTER, block);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createStairs(block, resourceLocation, resourceLocation2, resourceLocation3));
            BlockModelGenerators.this.delegateItemModel(block, resourceLocation2);
            return this;
        }

        private BlockModelGenerators.BlockFamilyProvider fullBlockVariant(Block block) {
            TexturedModel texturedModel = BlockModelGenerators.this.texturedModels.getOrDefault(block, TexturedModel.CUBE.get(block));
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, texturedModel.create(block, BlockModelGenerators.this.modelOutput)));
            return this;
        }

        private BlockModelGenerators.BlockFamilyProvider door(Block block) {
            BlockModelGenerators.this.createDoor(block);
            return this;
        }

        private void trapdoor(Block block) {
            if (BlockModelGenerators.this.nonOrientableTrapdoor.contains(block)) {
                BlockModelGenerators.this.createTrapdoor(block);
            } else {
                BlockModelGenerators.this.createOrientableTrapdoor(block);
            }

        }

        private MinecraftKey getOrCreateModel(ModelTemplate model, Block block) {
            return this.models.computeIfAbsent(model, (newModel) -> {
                return newModel.create(block, this.mapping, BlockModelGenerators.this.modelOutput);
            });
        }

        public BlockModelGenerators.BlockFamilyProvider generateFor(BlockFamily family) {
            this.family = family;
            family.getVariants().forEach((variant, block) -> {
                BiConsumer<BlockModelGenerators.BlockFamilyProvider, Block> biConsumer = BlockModelGenerators.SHAPE_CONSUMERS.get(variant);
                if (biConsumer != null) {
                    biConsumer.accept(this, block);
                }

            });
            return this;
        }
    }

    @FunctionalInterface
    interface BlockStateGeneratorSupplier {
        BlockStateGenerator create(Block block, MinecraftKey modelId, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector);
    }

    static enum TintState {
        TINTED,
        NOT_TINTED;

        public ModelTemplate getCross() {
            return this == TINTED ? ModelTemplates.TINTED_CROSS : ModelTemplates.CROSS;
        }

        public ModelTemplate getCrossPot() {
            return this == TINTED ? ModelTemplates.TINTED_FLOWER_POT_CROSS : ModelTemplates.FLOWER_POT_CROSS;
        }
    }

    class WoodProvider {
        private final TextureMapping logMapping;

        public WoodProvider(TextureMapping texture) {
            this.logMapping = texture;
        }

        public BlockModelGenerators.WoodProvider wood(Block woodBlock) {
            TextureMapping textureMapping = this.logMapping.copyAndUpdate(TextureSlot.END, this.logMapping.get(TextureSlot.SIDE));
            MinecraftKey resourceLocation = ModelTemplates.CUBE_COLUMN.create(woodBlock, textureMapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(woodBlock, resourceLocation));
            return this;
        }

        public BlockModelGenerators.WoodProvider log(Block stemBlock) {
            MinecraftKey resourceLocation = ModelTemplates.CUBE_COLUMN.create(stemBlock, this.logMapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(stemBlock, resourceLocation));
            return this;
        }

        public BlockModelGenerators.WoodProvider logWithHorizontal(Block logBlock) {
            MinecraftKey resourceLocation = ModelTemplates.CUBE_COLUMN.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput);
            MinecraftKey resourceLocation2 = ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(logBlock, this.logMapping, BlockModelGenerators.this.modelOutput);
            BlockModelGenerators.this.blockStateOutput.accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(logBlock, resourceLocation, resourceLocation2));
            return this;
        }
    }
}
