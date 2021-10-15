package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.BlockAccessAir;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.EnumBlockSupport;
import net.minecraft.world.level.block.EnumRenderType;
import net.minecraft.world.level.block.ITileEntity;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialMapColor;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public abstract class BlockBase {
    protected static final EnumDirection[] UPDATE_SHAPE_ORDER = new EnumDirection[]{EnumDirection.WEST, EnumDirection.EAST, EnumDirection.NORTH, EnumDirection.SOUTH, EnumDirection.DOWN, EnumDirection.UP};
    protected final Material material;
    protected final boolean hasCollision;
    protected final float explosionResistance;
    protected final boolean isRandomlyTicking;
    protected final SoundEffectType soundType;
    protected final float friction;
    protected final float speedFactor;
    protected final float jumpFactor;
    protected final boolean dynamicShape;
    protected final BlockBase.Info properties;
    @Nullable
    protected MinecraftKey drops;

    public BlockBase(BlockBase.Info settings) {
        this.material = settings.material;
        this.hasCollision = settings.hasCollision;
        this.drops = settings.drops;
        this.explosionResistance = settings.explosionResistance;
        this.isRandomlyTicking = settings.isRandomlyTicking;
        this.soundType = settings.soundType;
        this.friction = settings.friction;
        this.speedFactor = settings.speedFactor;
        this.jumpFactor = settings.jumpFactor;
        this.dynamicShape = settings.dynamicShape;
        this.properties = settings;
    }

    @Deprecated
    public void updateIndirectNeighbourShapes(IBlockData state, GeneratorAccess world, BlockPosition pos, int flags, int maxUpdateDepth) {
    }

    @Deprecated
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        switch(type) {
        case LAND:
            return !state.isCollisionShapeFullBlock(world, pos);
        case WATER:
            return world.getFluid(pos).is(TagsFluid.WATER);
        case AIR:
            return !state.isCollisionShapeFullBlock(world, pos);
        default:
            return false;
        }
    }

    @Deprecated
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return state;
    }

    @Deprecated
    public boolean skipRendering(IBlockData state, IBlockData stateFrom, EnumDirection direction) {
        return false;
    }

    @Deprecated
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        PacketDebug.sendNeighborsUpdatePacket(world, pos);
    }

    @Deprecated
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
    }

    @Deprecated
    public void remove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (state.isTileEntity() && !state.is(newState.getBlock())) {
            world.removeTileEntity(pos);
        }

    }

    @Deprecated
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        return EnumInteractionResult.PASS;
    }

    @Deprecated
    public boolean triggerEvent(IBlockData state, World world, BlockPosition pos, int type, int data) {
        return false;
    }

    @Deprecated
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Deprecated
    public boolean useShapeForLightOcclusion(IBlockData state) {
        return false;
    }

    @Deprecated
    public boolean isPowerSource(IBlockData state) {
        return false;
    }

    @Deprecated
    public EnumPistonReaction getPushReaction(IBlockData state) {
        return this.material.getPushReaction();
    }

    @Deprecated
    public Fluid getFluidState(IBlockData state) {
        return FluidTypes.EMPTY.defaultFluidState();
    }

    @Deprecated
    public boolean isComplexRedstone(IBlockData state) {
        return false;
    }

    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.NONE;
    }

    public float getMaxHorizontalOffset() {
        return 0.25F;
    }

    public float getMaxVerticalOffset() {
        return 0.2F;
    }

    @Deprecated
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state;
    }

    @Deprecated
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state;
    }

    @Deprecated
    public boolean canBeReplaced(IBlockData state, BlockActionContext context) {
        return this.material.isReplaceable() && (context.getItemStack().isEmpty() || !context.getItemStack().is(this.getItem()));
    }

    @Deprecated
    public boolean canBeReplaced(IBlockData state, FluidType fluid) {
        return this.material.isReplaceable() || !this.material.isBuildable();
    }

    @Deprecated
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        MinecraftKey resourceLocation = this.getLootTable();
        if (resourceLocation == LootTables.EMPTY) {
            return Collections.emptyList();
        } else {
            LootTableInfo lootContext = builder.set(LootContextParameters.BLOCK_STATE, state).build(LootContextParameterSets.BLOCK);
            WorldServer serverLevel = lootContext.getWorld();
            LootTable lootTable = serverLevel.getMinecraftServer().getLootTableRegistry().getLootTable(resourceLocation);
            return lootTable.populateLoot(lootContext);
        }
    }

    @Deprecated
    public long getSeed(IBlockData state, BlockPosition pos) {
        return MathHelper.getSeed(pos);
    }

    @Deprecated
    public VoxelShape getOcclusionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.getShape(world, pos);
    }

    @Deprecated
    public VoxelShape getBlockSupportShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return this.getCollisionShape(state, world, pos, VoxelShapeCollision.empty());
    }

    @Deprecated
    public VoxelShape getInteractionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return VoxelShapes.empty();
    }

    @Deprecated
    public int getLightBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        if (state.isSolidRender(world, pos)) {
            return world.getMaxLightLevel();
        } else {
            return state.propagatesSkylightDown(world, pos) ? 0 : 1;
        }
    }

    @Nullable
    @Deprecated
    public ITileInventory getInventory(IBlockData state, World world, BlockPosition pos) {
        return null;
    }

    @Deprecated
    public boolean canPlace(IBlockData state, IWorldReader world, BlockPosition pos) {
        return true;
    }

    @Deprecated
    public float getShadeBrightness(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return state.isCollisionShapeFullBlock(world, pos) ? 0.2F : 1.0F;
    }

    @Deprecated
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return 0;
    }

    @Deprecated
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return VoxelShapes.block();
    }

    @Deprecated
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.hasCollision ? state.getShape(world, pos) : VoxelShapes.empty();
    }

    @Deprecated
    public boolean isCollisionShapeFullBlock(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return Block.isShapeFullBlock(state.getCollisionShape(world, pos));
    }

    @Deprecated
    public VoxelShape getVisualShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return this.getCollisionShape(state, world, pos, context);
    }

    @Deprecated
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        this.tickAlways(state, world, pos, random);
    }

    @Deprecated
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
    }

    @Deprecated
    public float getDamage(IBlockData state, EntityHuman player, IBlockAccess world, BlockPosition pos) {
        float f = state.getDestroySpeed(world, pos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = player.hasBlock(state) ? 30 : 100;
            return player.getDestroySpeed(state) / f / (float)i;
        }
    }

    @Deprecated
    public void dropNaturally(IBlockData state, WorldServer world, BlockPosition pos, ItemStack stack) {
    }

    @Deprecated
    public void attack(IBlockData state, World world, BlockPosition pos, EntityHuman player) {
    }

    @Deprecated
    public int getSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return 0;
    }

    @Deprecated
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
    }

    @Deprecated
    public int getDirectSignal(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
        return 0;
    }

    public final MinecraftKey getLootTable() {
        if (this.drops == null) {
            MinecraftKey resourceLocation = IRegistry.BLOCK.getKey(this.asBlock());
            this.drops = new MinecraftKey(resourceLocation.getNamespace(), "blocks/" + resourceLocation.getKey());
        }

        return this.drops;
    }

    @Deprecated
    public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
    }

    public abstract Item getItem();

    protected abstract Block asBlock();

    public MaterialMapColor defaultMaterialColor() {
        return this.properties.materialColor.apply(this.asBlock().getBlockData());
    }

    public float defaultDestroyTime() {
        return this.properties.destroyTime;
    }

    public abstract static class BlockData extends IBlockDataHolder<Block, IBlockData> {
        private final int lightEmission;
        private final boolean useShapeForLightOcclusion;
        private final boolean isAir;
        private final Material material;
        private final MaterialMapColor materialColor;
        public final float destroySpeed;
        private final boolean requiresCorrectToolForDrops;
        private final boolean canOcclude;
        private final BlockBase.StatePredicate isRedstoneConductor;
        private final BlockBase.StatePredicate isSuffocating;
        private final BlockBase.StatePredicate isViewBlocking;
        private final BlockBase.StatePredicate hasPostProcess;
        private final BlockBase.StatePredicate emissiveRendering;
        @Nullable
        protected BlockBase.BlockData.Cache cache;

        protected BlockData(Block block, ImmutableMap<IBlockState<?>, Comparable<?>> propertyMap, MapCodec<IBlockData> codec) {
            super(block, propertyMap, codec);
            BlockBase.Info properties = block.properties;
            this.lightEmission = properties.lightEmission.applyAsInt(this.asState());
            this.useShapeForLightOcclusion = block.useShapeForLightOcclusion(this.asState());
            this.isAir = properties.isAir;
            this.material = properties.material;
            this.materialColor = properties.materialColor.apply(this.asState());
            this.destroySpeed = properties.destroyTime;
            this.requiresCorrectToolForDrops = properties.requiresCorrectToolForDrops;
            this.canOcclude = properties.canOcclude;
            this.isRedstoneConductor = properties.isRedstoneConductor;
            this.isSuffocating = properties.isSuffocating;
            this.isViewBlocking = properties.isViewBlocking;
            this.hasPostProcess = properties.hasPostProcess;
            this.emissiveRendering = properties.emissiveRendering;
        }

        public void initCache() {
            if (!this.getBlock().hasDynamicShape()) {
                this.cache = new BlockBase.BlockData.Cache(this.asState());
            }

        }

        public Block getBlock() {
            return this.owner;
        }

        public Material getMaterial() {
            return this.material;
        }

        public boolean isValidSpawn(IBlockAccess world, BlockPosition pos, EntityTypes<?> type) {
            return this.getBlock().properties.isValidSpawn.test(this.asState(), world, pos, type);
        }

        public boolean propagatesSkylightDown(IBlockAccess world, BlockPosition pos) {
            return this.cache != null ? this.cache.propagatesSkylightDown : this.getBlock().propagatesSkylightDown(this.asState(), world, pos);
        }

        public int getLightBlock(IBlockAccess world, BlockPosition pos) {
            return this.cache != null ? this.cache.lightBlock : this.getBlock().getLightBlock(this.asState(), world, pos);
        }

        public VoxelShape getFaceOcclusionShape(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return this.cache != null && this.cache.occlusionShapes != null ? this.cache.occlusionShapes[direction.ordinal()] : VoxelShapes.getFaceShape(this.getOcclusionShape(world, pos), direction);
        }

        public VoxelShape getOcclusionShape(IBlockAccess world, BlockPosition pos) {
            return this.getBlock().getOcclusionShape(this.asState(), world, pos);
        }

        public boolean hasLargeCollisionShape() {
            return this.cache == null || this.cache.largeCollisionShape;
        }

        public boolean useShapeForLightOcclusion() {
            return this.useShapeForLightOcclusion;
        }

        public int getLightEmission() {
            return this.lightEmission;
        }

        public boolean isAir() {
            return this.isAir;
        }

        public MaterialMapColor getMapColor(IBlockAccess world, BlockPosition pos) {
            return this.materialColor;
        }

        public IBlockData rotate(EnumBlockRotation rotation) {
            return this.getBlock().rotate(this.asState(), rotation);
        }

        public IBlockData mirror(EnumBlockMirror mirror) {
            return this.getBlock().mirror(this.asState(), mirror);
        }

        public EnumRenderType getRenderShape() {
            return this.getBlock().getRenderShape(this.asState());
        }

        public boolean emissiveRendering(IBlockAccess world, BlockPosition pos) {
            return this.emissiveRendering.test(this.asState(), world, pos);
        }

        public float getShadeBrightness(IBlockAccess world, BlockPosition pos) {
            return this.getBlock().getShadeBrightness(this.asState(), world, pos);
        }

        public boolean isOccluding(IBlockAccess world, BlockPosition pos) {
            return this.isRedstoneConductor.test(this.asState(), world, pos);
        }

        public boolean isPowerSource() {
            return this.getBlock().isPowerSource(this.asState());
        }

        public int getSignal(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return this.getBlock().getSignal(this.asState(), world, pos, direction);
        }

        public boolean isComplexRedstone() {
            return this.getBlock().isComplexRedstone(this.asState());
        }

        public int getAnalogOutputSignal(World world, BlockPosition pos) {
            return this.getBlock().getAnalogOutputSignal(this.asState(), world, pos);
        }

        public float getDestroySpeed(IBlockAccess world, BlockPosition pos) {
            return this.destroySpeed;
        }

        public float getDamage(EntityHuman player, IBlockAccess world, BlockPosition pos) {
            return this.getBlock().getDamage(this.asState(), player, world, pos);
        }

        public int getDirectSignal(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return this.getBlock().getDirectSignal(this.asState(), world, pos, direction);
        }

        public EnumPistonReaction getPushReaction() {
            return this.getBlock().getPushReaction(this.asState());
        }

        public boolean isSolidRender(IBlockAccess world, BlockPosition pos) {
            if (this.cache != null) {
                return this.cache.solidRender;
            } else {
                IBlockData blockState = this.asState();
                return blockState.canOcclude() ? Block.isShapeFullBlock(blockState.getOcclusionShape(world, pos)) : false;
            }
        }

        public boolean canOcclude() {
            return this.canOcclude;
        }

        public boolean skipRendering(IBlockData state, EnumDirection direction) {
            return this.getBlock().skipRendering(this.asState(), state, direction);
        }

        public VoxelShape getShape(IBlockAccess world, BlockPosition pos) {
            return this.getShape(world, pos, VoxelShapeCollision.empty());
        }

        public VoxelShape getShape(IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
            return this.getBlock().getShape(this.asState(), world, pos, context);
        }

        public VoxelShape getCollisionShape(IBlockAccess world, BlockPosition pos) {
            return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(world, pos, VoxelShapeCollision.empty());
        }

        public VoxelShape getCollisionShape(IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
            return this.getBlock().getCollisionShape(this.asState(), world, pos, context);
        }

        public VoxelShape getBlockSupportShape(IBlockAccess world, BlockPosition pos) {
            return this.getBlock().getBlockSupportShape(this.asState(), world, pos);
        }

        public VoxelShape getVisualShape(IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
            return this.getBlock().getVisualShape(this.asState(), world, pos, context);
        }

        public VoxelShape getInteractionShape(IBlockAccess world, BlockPosition pos) {
            return this.getBlock().getInteractionShape(this.asState(), world, pos);
        }

        public final boolean entityCanStandOn(IBlockAccess world, BlockPosition pos, Entity entity) {
            return this.entityCanStandOnFace(world, pos, entity, EnumDirection.UP);
        }

        public final boolean entityCanStandOnFace(IBlockAccess world, BlockPosition pos, Entity entity, EnumDirection direction) {
            return Block.isFaceFull(this.getCollisionShape(world, pos, VoxelShapeCollision.of(entity)), direction);
        }

        public Vec3D getOffset(IBlockAccess world, BlockPosition pos) {
            Block block = this.getBlock();
            BlockBase.EnumRandomOffset offsetType = block.getOffsetType();
            if (offsetType == BlockBase.EnumRandomOffset.NONE) {
                return Vec3D.ZERO;
            } else {
                long l = MathHelper.getSeed(pos.getX(), 0, pos.getZ());
                float f = block.getMaxHorizontalOffset();
                double d = MathHelper.clamp(((double)((float)(l & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
                double e = offsetType == BlockBase.EnumRandomOffset.XYZ ? ((double)((float)(l >> 4 & 15L) / 15.0F) - 1.0D) * (double)block.getMaxVerticalOffset() : 0.0D;
                double g = MathHelper.clamp(((double)((float)(l >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
                return new Vec3D(d, e, g);
            }
        }

        public boolean triggerEvent(World world, BlockPosition pos, int type, int data) {
            return this.getBlock().triggerEvent(this.asState(), world, pos, type, data);
        }

        public void doPhysics(World world, BlockPosition pos, Block block, BlockPosition posFrom, boolean notify) {
            this.getBlock().doPhysics(this.asState(), world, pos, block, posFrom, notify);
        }

        public final void updateNeighbourShapes(GeneratorAccess world, BlockPosition pos, int flags) {
            this.updateNeighbourShapes(world, pos, flags, 512);
        }

        public final void updateNeighbourShapes(GeneratorAccess world, BlockPosition pos, int flags, int maxUpdateDepth) {
            this.getBlock();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(EnumDirection direction : BlockBase.UPDATE_SHAPE_ORDER) {
                mutableBlockPos.setWithOffset(pos, direction);
                IBlockData blockState = world.getType(mutableBlockPos);
                IBlockData blockState2 = blockState.updateState(direction.opposite(), this.asState(), world, mutableBlockPos, pos);
                Block.updateOrDestroy(blockState, blockState2, world, mutableBlockPos, flags, maxUpdateDepth);
            }

        }

        public final void updateIndirectNeighbourShapes(GeneratorAccess world, BlockPosition pos, int flags) {
            this.updateIndirectNeighbourShapes(world, pos, flags, 512);
        }

        public void updateIndirectNeighbourShapes(GeneratorAccess world, BlockPosition pos, int flags, int maxUpdateDepth) {
            this.getBlock().updateIndirectNeighbourShapes(this.asState(), world, pos, flags, maxUpdateDepth);
        }

        public void onPlace(World world, BlockPosition pos, IBlockData state, boolean notify) {
            this.getBlock().onPlace(this.asState(), world, pos, state, notify);
        }

        public void remove(World world, BlockPosition pos, IBlockData state, boolean moved) {
            this.getBlock().remove(this.asState(), world, pos, state, moved);
        }

        public void tick(WorldServer world, BlockPosition pos, Random random) {
            this.getBlock().tickAlways(this.asState(), world, pos, random);
        }

        public void randomTick(WorldServer world, BlockPosition pos, Random random) {
            this.getBlock().tick(this.asState(), world, pos, random);
        }

        public void entityInside(World world, BlockPosition pos, Entity entity) {
            this.getBlock().entityInside(this.asState(), world, pos, entity);
        }

        public void dropNaturally(WorldServer world, BlockPosition pos, ItemStack stack) {
            this.getBlock().dropNaturally(this.asState(), world, pos, stack);
        }

        public List<ItemStack> getDrops(LootTableInfo.Builder builder) {
            return this.getBlock().getDrops(this.asState(), builder);
        }

        public EnumInteractionResult interact(World world, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
            return this.getBlock().interact(this.asState(), world, hit.getBlockPosition(), player, hand, hit);
        }

        public void attack(World world, BlockPosition pos, EntityHuman player) {
            this.getBlock().attack(this.asState(), world, pos, player);
        }

        public boolean isSuffocating(IBlockAccess world, BlockPosition pos) {
            return this.isSuffocating.test(this.asState(), world, pos);
        }

        public boolean isViewBlocking(IBlockAccess world, BlockPosition pos) {
            return this.isViewBlocking.test(this.asState(), world, pos);
        }

        public IBlockData updateState(EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
            return this.getBlock().updateState(this.asState(), direction, neighborState, world, pos, neighborPos);
        }

        public boolean isPathfindable(IBlockAccess world, BlockPosition pos, PathMode type) {
            return this.getBlock().isPathfindable(this.asState(), world, pos, type);
        }

        public boolean canBeReplaced(BlockActionContext context) {
            return this.getBlock().canBeReplaced(this.asState(), context);
        }

        public boolean canBeReplaced(FluidType fluid) {
            return this.getBlock().canBeReplaced(this.asState(), fluid);
        }

        public boolean canPlace(IWorldReader world, BlockPosition pos) {
            return this.getBlock().canPlace(this.asState(), world, pos);
        }

        public boolean hasPostProcess(IBlockAccess world, BlockPosition pos) {
            return this.hasPostProcess.test(this.asState(), world, pos);
        }

        @Nullable
        public ITileInventory getMenuProvider(World world, BlockPosition pos) {
            return this.getBlock().getInventory(this.asState(), world, pos);
        }

        public boolean is(Tag<Block> tag) {
            return tag.isTagged(this.getBlock());
        }

        public boolean is(Tag<Block> tag, Predicate<BlockBase.BlockData> predicate) {
            return this.is(tag) && predicate.test(this);
        }

        public boolean isTileEntity() {
            return this.getBlock() instanceof ITileEntity;
        }

        @Nullable
        public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, TileEntityTypes<T> blockEntityType) {
            return this.getBlock() instanceof ITileEntity ? ((ITileEntity)this.getBlock()).getTicker(world, this.asState(), blockEntityType) : null;
        }

        public boolean is(Block block) {
            return this.getBlock() == block;
        }

        public Fluid getFluid() {
            return this.getBlock().getFluidState(this.asState());
        }

        public boolean isTicking() {
            return this.getBlock().isTicking(this.asState());
        }

        public long getSeed(BlockPosition pos) {
            return this.getBlock().getSeed(this.asState(), pos);
        }

        public SoundEffectType getStepSound() {
            return this.getBlock().getStepSound(this.asState());
        }

        public void onProjectileHit(World world, IBlockData state, MovingObjectPositionBlock hit, IProjectile projectile) {
            this.getBlock().onProjectileHit(world, state, hit, projectile);
        }

        public boolean isFaceSturdy(IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return this.isFaceSturdy(world, pos, direction, EnumBlockSupport.FULL);
        }

        public boolean isFaceSturdy(IBlockAccess world, BlockPosition pos, EnumDirection direction, EnumBlockSupport shapeType) {
            return this.cache != null ? this.cache.isFaceSturdy(direction, shapeType) : shapeType.isSupporting(this.asState(), world, pos, direction);
        }

        public boolean isCollisionShapeFullBlock(IBlockAccess world, BlockPosition pos) {
            return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), world, pos);
        }

        protected abstract IBlockData asState();

        public boolean isRequiresSpecialTool() {
            return this.requiresCorrectToolForDrops;
        }

        static final class Cache {
            private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
            private static final int SUPPORT_TYPE_COUNT = EnumBlockSupport.values().length;
            protected final boolean solidRender;
            final boolean propagatesSkylightDown;
            final int lightBlock;
            @Nullable
            final VoxelShape[] occlusionShapes;
            protected final VoxelShape collisionShape;
            protected final boolean largeCollisionShape;
            private final boolean[] faceSturdy;
            protected final boolean isCollisionShapeFullBlock;

            Cache(IBlockData state) {
                Block block = state.getBlock();
                this.solidRender = state.isSolidRender(BlockAccessAir.INSTANCE, BlockPosition.ZERO);
                this.propagatesSkylightDown = block.propagatesSkylightDown(state, BlockAccessAir.INSTANCE, BlockPosition.ZERO);
                this.lightBlock = block.getLightBlock(state, BlockAccessAir.INSTANCE, BlockPosition.ZERO);
                if (!state.canOcclude()) {
                    this.occlusionShapes = null;
                } else {
                    this.occlusionShapes = new VoxelShape[DIRECTIONS.length];
                    VoxelShape voxelShape = block.getOcclusionShape(state, BlockAccessAir.INSTANCE, BlockPosition.ZERO);

                    for(EnumDirection direction : DIRECTIONS) {
                        this.occlusionShapes[direction.ordinal()] = VoxelShapes.getFaceShape(voxelShape, direction);
                    }
                }

                this.collisionShape = block.getCollisionShape(state, BlockAccessAir.INSTANCE, BlockPosition.ZERO, VoxelShapeCollision.empty());
                if (!this.collisionShape.isEmpty() && block.getOffsetType() != BlockBase.EnumRandomOffset.NONE) {
                    throw new IllegalStateException(String.format("%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.", IRegistry.BLOCK.getKey(block)));
                } else {
                    this.largeCollisionShape = Arrays.stream(EnumDirection.EnumAxis.values()).anyMatch((axis) -> {
                        return this.collisionShape.min(axis) < 0.0D || this.collisionShape.max(axis) > 1.0D;
                    });
                    this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

                    for(EnumDirection direction2 : DIRECTIONS) {
                        for(EnumBlockSupport supportType : EnumBlockSupport.values()) {
                            this.faceSturdy[getFaceSupportIndex(direction2, supportType)] = supportType.isSupporting(state, BlockAccessAir.INSTANCE, BlockPosition.ZERO, direction2);
                        }
                    }

                    this.isCollisionShapeFullBlock = Block.isShapeFullBlock(state.getCollisionShape(BlockAccessAir.INSTANCE, BlockPosition.ZERO));
                }
            }

            public boolean isFaceSturdy(EnumDirection direction, EnumBlockSupport shapeType) {
                return this.faceSturdy[getFaceSupportIndex(direction, shapeType)];
            }

            private static int getFaceSupportIndex(EnumDirection direction, EnumBlockSupport shapeType) {
                return direction.ordinal() * SUPPORT_TYPE_COUNT + shapeType.ordinal();
            }
        }
    }

    public static enum EnumRandomOffset {
        NONE,
        XZ,
        XYZ;
    }

    public static class Info {
        Material material;
        Function<IBlockData, MaterialMapColor> materialColor;
        boolean hasCollision = true;
        SoundEffectType soundType = SoundEffectType.STONE;
        ToIntFunction<IBlockData> lightEmission = (state) -> {
            return 0;
        };
        float explosionResistance;
        float destroyTime;
        boolean requiresCorrectToolForDrops;
        boolean isRandomlyTicking;
        float friction = 0.6F;
        float speedFactor = 1.0F;
        float jumpFactor = 1.0F;
        MinecraftKey drops;
        boolean canOcclude = true;
        boolean isAir;
        BlockBase.StateArgumentPredicate<EntityTypes<?>> isValidSpawn = (state, world, pos, type) -> {
            return state.isFaceSturdy(world, pos, EnumDirection.UP) && state.getLightEmission() < 14;
        };
        BlockBase.StatePredicate isRedstoneConductor = (state, world, pos) -> {
            return state.getMaterial().isSolidBlocking() && state.isCollisionShapeFullBlock(world, pos);
        };
        BlockBase.StatePredicate isSuffocating = (state, world, pos) -> {
            return this.material.isSolid() && state.isCollisionShapeFullBlock(world, pos);
        };
        BlockBase.StatePredicate isViewBlocking = this.isSuffocating;
        BlockBase.StatePredicate hasPostProcess = (state, world, pos) -> {
            return false;
        };
        BlockBase.StatePredicate emissiveRendering = (state, world, pos) -> {
            return false;
        };
        boolean dynamicShape;

        private Info(Material material, MaterialMapColor mapColorProvider) {
            this(material, (state) -> {
                return mapColorProvider;
            });
        }

        private Info(Material material, Function<IBlockData, MaterialMapColor> mapColorProvider) {
            this.material = material;
            this.materialColor = mapColorProvider;
        }

        public static BlockBase.Info of(Material material) {
            return of(material, material.getColor());
        }

        public static BlockBase.Info of(Material material, EnumColor color) {
            return of(material, color.getMaterialColor());
        }

        public static BlockBase.Info of(Material material, MaterialMapColor color) {
            return new BlockBase.Info(material, color);
        }

        public static BlockBase.Info of(Material material, Function<IBlockData, MaterialMapColor> mapColor) {
            return new BlockBase.Info(material, mapColor);
        }

        public static BlockBase.Info copy(BlockBase block) {
            BlockBase.Info properties = new BlockBase.Info(block.material, block.properties.materialColor);
            properties.material = block.properties.material;
            properties.destroyTime = block.properties.destroyTime;
            properties.explosionResistance = block.properties.explosionResistance;
            properties.hasCollision = block.properties.hasCollision;
            properties.isRandomlyTicking = block.properties.isRandomlyTicking;
            properties.lightEmission = block.properties.lightEmission;
            properties.materialColor = block.properties.materialColor;
            properties.soundType = block.properties.soundType;
            properties.friction = block.properties.friction;
            properties.speedFactor = block.properties.speedFactor;
            properties.dynamicShape = block.properties.dynamicShape;
            properties.canOcclude = block.properties.canOcclude;
            properties.isAir = block.properties.isAir;
            properties.requiresCorrectToolForDrops = block.properties.requiresCorrectToolForDrops;
            return properties;
        }

        public BlockBase.Info noCollission() {
            this.hasCollision = false;
            this.canOcclude = false;
            return this;
        }

        public BlockBase.Info noOcclusion() {
            this.canOcclude = false;
            return this;
        }

        public BlockBase.Info friction(float slipperiness) {
            this.friction = slipperiness;
            return this;
        }

        public BlockBase.Info speedFactor(float velocityMultiplier) {
            this.speedFactor = velocityMultiplier;
            return this;
        }

        public BlockBase.Info jumpFactor(float jumpVelocityMultiplier) {
            this.jumpFactor = jumpVelocityMultiplier;
            return this;
        }

        public BlockBase.Info sound(SoundEffectType soundGroup) {
            this.soundType = soundGroup;
            return this;
        }

        public BlockBase.Info lightLevel(ToIntFunction<IBlockData> luminance) {
            this.lightEmission = luminance;
            return this;
        }

        public BlockBase.Info strength(float hardness, float resistance) {
            return this.destroyTime(hardness).explosionResistance(resistance);
        }

        public BlockBase.Info instabreak() {
            return this.strength(0.0F);
        }

        public BlockBase.Info strength(float strength) {
            this.strength(strength, strength);
            return this;
        }

        public BlockBase.Info randomTicks() {
            this.isRandomlyTicking = true;
            return this;
        }

        public BlockBase.Info dynamicShape() {
            this.dynamicShape = true;
            return this;
        }

        public BlockBase.Info noDrops() {
            this.drops = LootTables.EMPTY;
            return this;
        }

        public BlockBase.Info dropsLike(Block source) {
            this.drops = source.getLootTable();
            return this;
        }

        public BlockBase.Info air() {
            this.isAir = true;
            return this;
        }

        public BlockBase.Info isValidSpawn(BlockBase.StateArgumentPredicate<EntityTypes<?>> predicate) {
            this.isValidSpawn = predicate;
            return this;
        }

        public BlockBase.Info isRedstoneConductor(BlockBase.StatePredicate predicate) {
            this.isRedstoneConductor = predicate;
            return this;
        }

        public BlockBase.Info isSuffocating(BlockBase.StatePredicate predicate) {
            this.isSuffocating = predicate;
            return this;
        }

        public BlockBase.Info isViewBlocking(BlockBase.StatePredicate predicate) {
            this.isViewBlocking = predicate;
            return this;
        }

        public BlockBase.Info hasPostProcess(BlockBase.StatePredicate predicate) {
            this.hasPostProcess = predicate;
            return this;
        }

        public BlockBase.Info emissiveRendering(BlockBase.StatePredicate predicate) {
            this.emissiveRendering = predicate;
            return this;
        }

        public BlockBase.Info requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public BlockBase.Info color(MaterialMapColor color) {
            this.materialColor = (state) -> {
                return color;
            };
            return this;
        }

        public BlockBase.Info destroyTime(float hardness) {
            this.destroyTime = hardness;
            return this;
        }

        public BlockBase.Info explosionResistance(float resistance) {
            this.explosionResistance = Math.max(0.0F, resistance);
            return this;
        }
    }

    public interface StateArgumentPredicate<A> {
        boolean test(IBlockData state, IBlockAccess world, BlockPosition pos, A type);
    }

    public interface StatePredicate {
        boolean test(IBlockData state, IBlockAccess world, BlockPosition pos);
    }
}
