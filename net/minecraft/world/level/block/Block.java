package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Block extends BlockBase implements IMaterial {
    protected static final Logger LOGGER = LogManager.getLogger();
    public static final RegistryBlockID<IBlockData> BLOCK_STATE_REGISTRY = new RegistryBlockID<>();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build(new CacheLoader<VoxelShape, Boolean>() {
        @Override
        public Boolean load(VoxelShape voxelShape) {
            return !VoxelShapes.joinIsNotEmpty(VoxelShapes.block(), voxelShape, OperatorBoolean.NOT_SAME);
        }
    });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SUPPRESS_LIGHT = 128;
    public static final int UPDATE_NONE = 4;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final BlockStateList<Block, IBlockData> stateDefinition;
    private IBlockData defaultBlockState;
    @Nullable
    private String descriptionId;
    @Nullable
    private Item item;
    private static final int CACHE_SIZE = 2048;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(2048, 0.25F) {
            protected void rehash(int i) {
            }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
        return object2ByteLinkedOpenHashMap;
    });

    public static int getCombinedId(@Nullable IBlockData state) {
        if (state == null) {
            return 0;
        } else {
            int i = BLOCK_STATE_REGISTRY.getId(state);
            return i == -1 ? 0 : i;
        }
    }

    public static IBlockData getByCombinedId(int stateId) {
        IBlockData blockState = BLOCK_STATE_REGISTRY.fromId(stateId);
        return blockState == null ? Blocks.AIR.getBlockData() : blockState;
    }

    public static Block asBlock(@Nullable Item item) {
        return item instanceof ItemBlock ? ((ItemBlock)item).getBlock() : Blocks.AIR;
    }

    public static IBlockData pushEntitiesUp(IBlockData from, IBlockData to, World world, BlockPosition pos) {
        VoxelShape voxelShape = VoxelShapes.joinUnoptimized(from.getCollisionShape(world, pos), to.getCollisionShape(world, pos), OperatorBoolean.ONLY_SECOND).move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (voxelShape.isEmpty()) {
            return to;
        } else {
            for(Entity entity : world.getEntities((Entity)null, voxelShape.getBoundingBox())) {
                double d = VoxelShapes.collide(EnumDirection.EnumAxis.Y, entity.getBoundingBox().move(0.0D, 1.0D, 0.0D), List.of(voxelShape), -1.0D);
                entity.enderTeleportTo(entity.locX(), entity.locY() + 1.0D + d, entity.locZ());
            }

            return to;
        }
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return VoxelShapes.box(minX / 16.0D, minY / 16.0D, minZ / 16.0D, maxX / 16.0D, maxY / 16.0D, maxZ / 16.0D);
    }

    public static IBlockData updateFromNeighbourShapes(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = state;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(EnumDirection direction : UPDATE_SHAPE_ORDER) {
            mutableBlockPos.setWithOffset(pos, direction);
            blockState = blockState.updateState(direction, world.getType(mutableBlockPos), world, pos, mutableBlockPos);
        }

        return blockState;
    }

    public static void updateOrDestroy(IBlockData state, IBlockData newState, GeneratorAccess world, BlockPosition pos, int flags) {
        updateOrDestroy(state, newState, world, pos, flags, 512);
    }

    public static void updateOrDestroy(IBlockData state, IBlockData newState, GeneratorAccess world, BlockPosition pos, int flags, int maxUpdateDepth) {
        if (newState != state) {
            if (newState.isAir()) {
                if (!world.isClientSide()) {
                    world.destroyBlock(pos, (flags & 32) == 0, (Entity)null, maxUpdateDepth);
                }
            } else {
                world.setBlock(pos, newState, flags & -33, maxUpdateDepth);
            }
        }

    }

    public Block(BlockBase.Info settings) {
        super(settings);
        BlockStateList.Builder<Block, IBlockData> builder = new BlockStateList.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::getBlockData, IBlockData::new);
        this.registerDefaultState(this.stateDefinition.getBlockData());
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String string = this.getClass().getSimpleName();
            if (!string.endsWith("Block")) {
                LOGGER.error("Block classes should end with Block and {} doesn't.", (Object)string);
            }
        }

    }

    public static boolean isExceptionForConnection(IBlockData state) {
        return state.getBlock() instanceof BlockLeaves || state.is(Blocks.BARRIER) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(TagsBlock.SHULKER_BOXES);
    }

    public boolean isTicking(IBlockData state) {
        return this.isRandomlyTicking;
    }

    public static boolean shouldRenderFace(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection side, BlockPosition blockPos) {
        IBlockData blockState = world.getType(blockPos);
        if (state.skipRendering(blockState, side)) {
            return false;
        } else if (blockState.canOcclude()) {
            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(state, blockState, side);
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = OCCLUSION_CACHE.get();
            byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            } else {
                VoxelShape voxelShape = state.getFaceOcclusionShape(world, pos, side);
                if (voxelShape.isEmpty()) {
                    return true;
                } else {
                    VoxelShape voxelShape2 = blockState.getFaceOcclusionShape(world, blockPos, side.opposite());
                    boolean bl = VoxelShapes.joinIsNotEmpty(voxelShape, voxelShape2, OperatorBoolean.ONLY_FIRST);
                    if (object2ByteLinkedOpenHashMap.size() == 2048) {
                        object2ByteLinkedOpenHashMap.removeLastByte();
                    }

                    object2ByteLinkedOpenHashMap.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
                    return bl;
                }
            }
        } else {
            return true;
        }
    }

    public static boolean canSupportRigidBlock(IBlockAccess world, BlockPosition pos) {
        return world.getType(pos).isFaceSturdy(world, pos, EnumDirection.UP, EnumBlockSupport.RIGID);
    }

    public static boolean canSupportCenter(IWorldReader world, BlockPosition pos, EnumDirection side) {
        IBlockData blockState = world.getType(pos);
        return side == EnumDirection.DOWN && blockState.is(TagsBlock.UNSTABLE_BOTTOM_CENTER) ? false : blockState.isFaceSturdy(world, pos, side, EnumBlockSupport.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, EnumDirection side) {
        VoxelShape voxelShape = shape.getFaceShape(side);
        return isShapeFullBlock(voxelShape);
    }

    public static boolean isShapeFullBlock(VoxelShape shape) {
        return SHAPE_FULL_BLOCK_CACHE.getUnchecked(shape);
    }

    public boolean propagatesSkylightDown(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return !isShapeFullBlock(state.getShape(world, pos)) && state.getFluid().isEmpty();
    }

    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
    }

    public void postBreak(GeneratorAccess world, BlockPosition pos, IBlockData state) {
    }

    public static List<ItemStack> getDrops(IBlockData state, WorldServer world, BlockPosition pos, @Nullable TileEntity blockEntity) {
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(world)).withRandom(world.random).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(pos)).set(LootContextParameters.TOOL, ItemStack.EMPTY).setOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
        return state.getDrops(builder);
    }

    public static List<ItemStack> getDrops(IBlockData state, WorldServer world, BlockPosition pos, @Nullable TileEntity blockEntity, @Nullable Entity entity, ItemStack stack) {
        LootTableInfo.Builder builder = (new LootTableInfo.Builder(world)).withRandom(world.random).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(pos)).set(LootContextParameters.TOOL, stack).setOptional(LootContextParameters.THIS_ENTITY, entity).setOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
        return state.getDrops(builder);
    }

    public static void dropResources(IBlockData state, LootTableInfo.Builder lootContext) {
        WorldServer serverLevel = lootContext.getLevel();
        BlockPosition blockPos = new BlockPosition(lootContext.getParameter(LootContextParameters.ORIGIN));
        state.getDrops(lootContext).forEach((stack) -> {
            popResource(serverLevel, blockPos, stack);
        });
        state.dropNaturally(serverLevel, blockPos, ItemStack.EMPTY);
    }

    public static void dropResources(IBlockData state, World world, BlockPosition pos) {
        if (world instanceof WorldServer) {
            getDrops(state, (WorldServer)world, pos, (TileEntity)null).forEach((stack) -> {
                popResource(world, pos, stack);
            });
            state.dropNaturally((WorldServer)world, pos, ItemStack.EMPTY);
        }

    }

    public static void dropResources(IBlockData state, GeneratorAccess world, BlockPosition pos, @Nullable TileEntity blockEntity) {
        if (world instanceof WorldServer) {
            getDrops(state, (WorldServer)world, pos, blockEntity).forEach((stack) -> {
                popResource((WorldServer)world, pos, stack);
            });
            state.dropNaturally((WorldServer)world, pos, ItemStack.EMPTY);
        }

    }

    public static void dropItems(IBlockData state, World world, BlockPosition pos, @Nullable TileEntity blockEntity, Entity entity, ItemStack stack) {
        if (world instanceof WorldServer) {
            getDrops(state, (WorldServer)world, pos, blockEntity, entity, stack).forEach((stackx) -> {
                popResource(world, pos, stackx);
            });
            state.dropNaturally((WorldServer)world, pos, stack);
        }

    }

    public static void popResource(World world, BlockPosition pos, ItemStack stack) {
        float f = EntityTypes.ITEM.getHeight() / 2.0F;
        double d = (double)((float)pos.getX() + 0.5F) + MathHelper.nextDouble(world.random, -0.25D, 0.25D);
        double e = (double)((float)pos.getY() + 0.5F) + MathHelper.nextDouble(world.random, -0.25D, 0.25D) - (double)f;
        double g = (double)((float)pos.getZ() + 0.5F) + MathHelper.nextDouble(world.random, -0.25D, 0.25D);
        popResource(world, () -> {
            return new EntityItem(world, d, e, g, stack);
        }, stack);
    }

    public static void popResourceFromFace(World world, BlockPosition pos, EnumDirection direction, ItemStack stack) {
        int i = direction.getAdjacentX();
        int j = direction.getAdjacentY();
        int k = direction.getAdjacentZ();
        float f = EntityTypes.ITEM.getWidth() / 2.0F;
        float g = EntityTypes.ITEM.getHeight() / 2.0F;
        double d = (double)((float)pos.getX() + 0.5F) + (i == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)i * (0.5F + f)));
        double e = (double)((float)pos.getY() + 0.5F) + (j == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)j * (0.5F + g))) - (double)g;
        double h = (double)((float)pos.getZ() + 0.5F) + (k == 0 ? MathHelper.nextDouble(world.random, -0.25D, 0.25D) : (double)((float)k * (0.5F + f)));
        double l = i == 0 ? MathHelper.nextDouble(world.random, -0.1D, 0.1D) : (double)i * 0.1D;
        double m = j == 0 ? MathHelper.nextDouble(world.random, 0.0D, 0.1D) : (double)j * 0.1D + 0.1D;
        double n = k == 0 ? MathHelper.nextDouble(world.random, -0.1D, 0.1D) : (double)k * 0.1D;
        popResource(world, () -> {
            return new EntityItem(world, d, e, h, stack, l, m, n);
        }, stack);
    }

    private static void popResource(World world, Supplier<EntityItem> itemEntitySupplier, ItemStack stack) {
        if (!world.isClientSide && !stack.isEmpty() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            EntityItem itemEntity = itemEntitySupplier.get();
            itemEntity.defaultPickupDelay();
            world.addEntity(itemEntity);
        }
    }

    public void dropExperience(WorldServer world, BlockPosition pos, int size) {
        if (world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            EntityExperienceOrb.award(world, Vec3D.atCenterOf(pos), size);
        }

    }

    public float getDurability() {
        return this.explosionResistance;
    }

    public void wasExploded(World world, BlockPosition pos, Explosion explosion) {
    }

    public void stepOn(World world, BlockPosition pos, IBlockData state, Entity entity) {
    }

    @Nullable
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData();
    }

    public void playerDestroy(World world, EntityHuman player, BlockPosition pos, IBlockData state, @Nullable TileEntity blockEntity, ItemStack stack) {
        player.awardStat(StatisticList.BLOCK_MINED.get(this));
        player.applyExhaustion(0.005F);
        dropItems(state, world, pos, blockEntity, player, stack);
    }

    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
    }

    public boolean isPossibleToRespawnInThis() {
        return !this.material.isBuildable() && !this.material.isLiquid();
    }

    public IChatMutableComponent getName() {
        return new ChatMessage(this.getDescriptionId());
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.makeDescriptionId("block", IRegistry.BLOCK.getKey(this));
        }

        return this.descriptionId;
    }

    public void fallOn(World world, IBlockData state, BlockPosition pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, DamageSource.FALL);
    }

    public void updateEntityAfterFallOn(IBlockAccess world, Entity entity) {
        entity.setMot(entity.getMot().multiply(1.0D, 0.0D, 1.0D));
    }

    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return new ItemStack(this);
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        stacks.add(new ItemStack(this));
    }

    public float getFrictionFactor() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(World world, EntityHuman player, BlockPosition pos, IBlockData state) {
        world.triggerEffect(player, 2001, pos, getCombinedId(state));
    }

    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        this.spawnDestroyParticles(world, player, pos, state);
        if (state.is(TagsBlock.GUARDED_BY_PIGLINS)) {
            PiglinAI.angerNearbyPiglins(player, false);
        }

        world.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
    }

    public void handlePrecipitation(IBlockData state, World world, BlockPosition pos, BiomeBase.Precipitation precipitation) {
    }

    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
    }

    public BlockStateList<Block, IBlockData> getStates() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(IBlockData state) {
        this.defaultBlockState = state;
    }

    public final IBlockData getBlockData() {
        return this.defaultBlockState;
    }

    public final IBlockData withPropertiesOf(IBlockData state) {
        IBlockData blockState = this.getBlockData();

        for(IBlockState<?> property : state.getBlock().getStates().getProperties()) {
            if (blockState.hasProperty(property)) {
                blockState = copyProperty(state, blockState, property);
            }
        }

        return blockState;
    }

    private static <T extends Comparable<T>> IBlockData copyProperty(IBlockData source, IBlockData target, IBlockState<T> property) {
        return target.set(property, source.get(property));
    }

    public SoundEffectType getStepSound(IBlockData state) {
        return this.soundType;
    }

    @Override
    public Item getItem() {
        if (this.item == null) {
            this.item = Item.getItemOf(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    @Override
    public String toString() {
        return "Block{" + IRegistry.BLOCK.getKey(this) + "}";
    }

    public void appendHoverText(ItemStack stack, @Nullable IBlockAccess world, List<IChatBaseComponent> tooltip, TooltipFlag options) {
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected ImmutableMap<IBlockData, VoxelShape> getShapeForEachState(Function<IBlockData, VoxelShape> stateToShape) {
        return this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), stateToShape));
    }

    public static final class BlockStatePairKey {
        private final IBlockData first;
        private final IBlockData second;
        private final EnumDirection direction;

        public BlockStatePairKey(IBlockData self, IBlockData other, EnumDirection facing) {
            this.first = self;
            this.second = other;
            this.direction = facing;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof Block.BlockStatePairKey)) {
                return false;
            } else {
                Block.BlockStatePairKey blockStatePairKey = (Block.BlockStatePairKey)object;
                return this.first == blockStatePairKey.first && this.second == blockStatePairKey.second && this.direction == blockStatePairKey.direction;
            }
        }

        @Override
        public int hashCode() {
            int i = this.first.hashCode();
            i = 31 * i + this.second.hashCode();
            return 31 * i + this.direction.hashCode();
        }
    }
}
