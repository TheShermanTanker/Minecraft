package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockButtonAbstract;
import net.minecraft.world.level.block.BlockLever;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityContainer;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public class GameTestHarnessHelper {
    private final GameTestHarnessInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHarnessHelper(GameTestHarnessInfo test) {
        this.testInfo = test;
    }

    public WorldServer getLevel() {
        return this.testInfo.getLevel();
    }

    public IBlockData getBlockState(BlockPosition pos) {
        return this.getLevel().getType(this.absolutePos(pos));
    }

    @Nullable
    public TileEntity getBlockEntity(BlockPosition pos) {
        return this.getLevel().getTileEntity(this.absolutePos(pos));
    }

    public void killAllEntities() {
        AxisAlignedBB aABB = this.getBounds();
        List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aABB.inflate(1.0D), (entity) -> {
            return !(entity instanceof EntityHuman);
        });
        list.forEach(Entity::killEntity);
    }

    public EntityItem spawnItem(Item item, float x, float y, float z) {
        WorldServer serverLevel = this.getLevel();
        Vec3D vec3 = this.absoluteVec(new Vec3D((double)x, (double)y, (double)z));
        EntityItem itemEntity = new EntityItem(serverLevel, vec3.x, vec3.y, vec3.z, new ItemStack(item, 1));
        itemEntity.setMot(0.0D, 0.0D, 0.0D);
        serverLevel.addEntity(itemEntity);
        return itemEntity;
    }

    public <E extends Entity> E spawn(EntityTypes<E> type, BlockPosition pos) {
        return this.spawn(type, Vec3D.atBottomCenterOf(pos));
    }

    public <E extends Entity> E spawn(EntityTypes<E> type, Vec3D pos) {
        WorldServer serverLevel = this.getLevel();
        E entity = type.create(serverLevel);
        if (entity instanceof EntityInsentient) {
            ((EntityInsentient)entity).setPersistent();
        }

        Vec3D vec3 = this.absoluteVec(pos);
        entity.setPositionRotation(vec3.x, vec3.y, vec3.z, entity.getYRot(), entity.getXRot());
        serverLevel.addEntity(entity);
        return entity;
    }

    public <E extends Entity> E spawn(EntityTypes<E> type, int x, int y, int z) {
        return this.spawn(type, new BlockPosition(x, y, z));
    }

    public <E extends Entity> E spawn(EntityTypes<E> type, float x, float y, float z) {
        return this.spawn(type, new Vec3D((double)x, (double)y, (double)z));
    }

    public <E extends EntityInsentient> E spawnWithNoFreeWill(EntityTypes<E> type, BlockPosition pos) {
        E mob = this.spawn(type, pos);
        mob.removeFreeWill();
        return mob;
    }

    public <E extends EntityInsentient> E spawnWithNoFreeWill(EntityTypes<E> type, int x, int y, int z) {
        return this.spawnWithNoFreeWill(type, new BlockPosition(x, y, z));
    }

    public <E extends EntityInsentient> E spawnWithNoFreeWill(EntityTypes<E> type, Vec3D pos) {
        E mob = this.spawn(type, pos);
        mob.removeFreeWill();
        return mob;
    }

    public <E extends EntityInsentient> E spawnWithNoFreeWill(EntityTypes<E> type, float x, float y, float z) {
        return this.spawnWithNoFreeWill(type, new Vec3D((double)x, (double)y, (double)z));
    }

    public GameTestHarnessSequence walkTo(EntityInsentient entity, BlockPosition pos, float f) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            PathEntity path = entity.getNavigation().createPath(this.absolutePos(pos), 0);
            entity.getNavigation().moveTo(path, (double)f);
        });
    }

    public void pressButton(int x, int y, int z) {
        this.pressButton(new BlockPosition(x, y, z));
    }

    public void pressButton(BlockPosition pos) {
        this.assertBlockState(pos, (blockStatex) -> {
            return blockStatex.is(TagsBlock.BUTTONS);
        }, () -> {
            return "Expected button";
        });
        BlockPosition blockPos = this.absolutePos(pos);
        IBlockData blockState = this.getLevel().getType(blockPos);
        BlockButtonAbstract buttonBlock = (BlockButtonAbstract)blockState.getBlock();
        buttonBlock.press(blockState, this.getLevel(), blockPos);
    }

    public void useBlock(BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        IBlockData blockState = this.getLevel().getType(blockPos);
        blockState.interact(this.getLevel(), this.makeMockPlayer(), EnumHand.MAIN_HAND, new MovingObjectPositionBlock(Vec3D.atCenterOf(blockPos), EnumDirection.NORTH, blockPos, true));
    }

    public EntityLiving makeAboutToDrown(EntityLiving entity) {
        entity.setAirTicks(0);
        entity.setHealth(0.25F);
        return entity;
    }

    public EntityHuman makeMockPlayer() {
        return new EntityHuman(this.getLevel(), BlockPosition.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
    }

    public void pullLever(int x, int y, int z) {
        this.pullLever(new BlockPosition(x, y, z));
    }

    public void pullLever(BlockPosition pos) {
        this.assertBlockPresent(Blocks.LEVER, pos);
        BlockPosition blockPos = this.absolutePos(pos);
        IBlockData blockState = this.getLevel().getType(blockPos);
        BlockLever leverBlock = (BlockLever)blockState.getBlock();
        leverBlock.pull(blockState, this.getLevel(), blockPos);
    }

    public void pulseRedstone(BlockPosition pos, long delay) {
        this.setBlock(pos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(delay, () -> {
            this.setBlock(pos, Blocks.AIR);
        });
    }

    public void destroyBlock(BlockPosition pos) {
        this.getLevel().destroyBlock(this.absolutePos(pos), false, (Entity)null);
    }

    public void setBlock(int x, int y, int z, Block block) {
        this.setBlock(new BlockPosition(x, y, z), block);
    }

    public void setBlock(int x, int y, int z, IBlockData state) {
        this.setBlock(new BlockPosition(x, y, z), state);
    }

    public void setBlock(BlockPosition pos, Block block) {
        this.setBlock(pos, block.getBlockData());
    }

    public void setBlock(BlockPosition pos, IBlockData state) {
        this.getLevel().setTypeAndData(this.absolutePos(pos), state, 3);
    }

    public void setNight() {
        this.setDayTime(13000);
    }

    public void setDayTime(int timeOfDay) {
        this.getLevel().setDayTime((long)timeOfDay);
    }

    public void assertBlockPresent(Block block, int x, int y, int z) {
        this.assertBlockPresent(block, new BlockPosition(x, y, z));
    }

    public void assertBlockPresent(Block block, BlockPosition pos) {
        IBlockData blockState = this.getBlockState(pos);
        this.assertBlock(pos, (block1) -> {
            return blockState.is(block);
        }, "Expected " + block.getName().getString() + ", got " + blockState.getBlock().getName().getString());
    }

    public void assertBlockNotPresent(Block block, int x, int y, int z) {
        this.assertBlockNotPresent(block, new BlockPosition(x, y, z));
    }

    public void assertBlockNotPresent(Block block, BlockPosition pos) {
        this.assertBlock(pos, (block1) -> {
            return !this.getBlockState(pos).is(block);
        }, "Did not expect " + block.getName().getString());
    }

    public void succeedWhenBlockPresent(Block block, int x, int y, int z) {
        this.succeedWhenBlockPresent(block, new BlockPosition(x, y, z));
    }

    public void succeedWhenBlockPresent(Block block, BlockPosition pos) {
        this.succeedWhen(() -> {
            this.assertBlockPresent(block, pos);
        });
    }

    public void assertBlock(BlockPosition pos, Predicate<Block> predicate, String errorMessage) {
        this.assertBlock(pos, predicate, () -> {
            return errorMessage;
        });
    }

    public void assertBlock(BlockPosition pos, Predicate<Block> predicate, Supplier<String> errorMessageSupplier) {
        this.assertBlockState(pos, (state) -> {
            return predicate.test(state.getBlock());
        }, errorMessageSupplier);
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPosition pos, IBlockState<T> property, T value) {
        this.assertBlockState(pos, (state) -> {
            return state.hasProperty(property) && state.<T>get(property).equals(value);
        }, () -> {
            return "Expected property " + property.getName() + " to be " + value;
        });
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPosition pos, IBlockState<T> property, Predicate<T> predicate, String errorMessage) {
        this.assertBlockState(pos, (blockState) -> {
            return predicate.test(blockState.get(property));
        }, () -> {
            return errorMessage;
        });
    }

    public void assertBlockState(BlockPosition pos, Predicate<IBlockData> predicate, Supplier<String> errorMessageSupplier) {
        IBlockData blockState = this.getBlockState(pos);
        if (!predicate.test(blockState)) {
            throw new GameTestHarnessAssertionPosition(errorMessageSupplier.get(), this.absolutePos(pos), pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityTypes<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertion("Expected " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityPresent(EntityTypes<?> type, int x, int y, int z) {
        this.assertEntityPresent(type, new BlockPosition(x, y, z));
    }

    public void assertEntityPresent(EntityTypes<?> type, BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AxisAlignedBB(blockPos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertionPosition("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityTypes<?> type, BlockPosition pos, double radius) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, (new AxisAlignedBB(blockPos)).inflate(radius), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertionPosition("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityInstancePresent(Entity entity, int x, int y, int z) {
        this.assertEntityInstancePresent(entity, new BlockPosition(x, y, z));
    }

    public void assertEntityInstancePresent(Entity entity, BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(entity.getEntityType(), new AxisAlignedBB(blockPos), Entity::isAlive);
        list.stream().filter((entity2) -> {
            return entity2 == entity;
        }).findFirst().orElseThrow(() -> {
            return new GameTestHarnessAssertionPosition("Expected " + entity.getEntityType().toShortString(), blockPos, pos, this.testInfo.getTick());
        });
    }

    public void assertItemEntityCountIs(Item item, BlockPosition pos, double radius, int amount) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<EntityItem> list = this.getLevel().getEntities(EntityTypes.ITEM, (new AxisAlignedBB(blockPos)).inflate(radius), Entity::isAlive);
        int i = 0;

        for(Entity entity : list) {
            EntityItem itemEntity = (EntityItem)entity;
            if (itemEntity.getItemStack().getItem().equals(item)) {
                i += itemEntity.getItemStack().getCount();
            }
        }

        if (i != amount) {
            throw new GameTestHarnessAssertionPosition("Expected " + amount + " " + item.getDescription().getString() + " items to exist (found " + i + ")", blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertItemEntityPresent(Item item, BlockPosition pos, double radius) {
        BlockPosition blockPos = this.absolutePos(pos);

        for(Entity entity : this.getLevel().getEntities(EntityTypes.ITEM, (new AxisAlignedBB(blockPos)).inflate(radius), Entity::isAlive)) {
            EntityItem itemEntity = (EntityItem)entity;
            if (itemEntity.getItemStack().getItem().equals(item)) {
                return;
            }
        }

        throw new GameTestHarnessAssertionPosition("Expected " + item.getDescription().getString() + " item", blockPos, pos, this.testInfo.getTick());
    }

    public void assertEntityNotPresent(EntityTypes<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestHarnessAssertion("Did not expect " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityNotPresent(EntityTypes<?> type, int x, int y, int z) {
        this.assertEntityNotPresent(type, new BlockPosition(x, y, z));
    }

    public void assertEntityNotPresent(EntityTypes<?> type, BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AxisAlignedBB(blockPos), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestHarnessAssertionPosition("Did not expect " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityTouching(EntityTypes<?> type, double x, double y, double z) {
        Vec3D vec3 = new Vec3D(x, y, z);
        Vec3D vec32 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = (entity) -> {
            return entity.getBoundingBox().intersects(vec32, vec32);
        };
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertion("Expected " + type.toShortString() + " to touch " + vec32 + " (relative " + vec3 + ")");
        }
    }

    public void assertEntityNotTouching(EntityTypes<?> type, double x, double y, double z) {
        Vec3D vec3 = new Vec3D(x, y, z);
        Vec3D vec32 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = (entity) -> {
            return !entity.getBoundingBox().intersects(vec32, vec32);
        };
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertion("Did not expect " + type.toShortString() + " to touch " + vec32 + " (relative " + vec3 + ")");
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPosition pos, EntityTypes<E> type, Function<? super E, T> entityDataGetter, @Nullable T data) {
        BlockPosition blockPos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(type, new AxisAlignedBB(blockPos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestHarnessAssertionPosition("Expected " + type.toShortString(), blockPos, pos, this.testInfo.getTick());
        } else {
            for(E entity : list) {
                T object = entityDataGetter.apply(entity);
                if (object == null) {
                    if (data != null) {
                        throw new GameTestHarnessAssertion("Expected entity data to be: " + data + ", but was: " + object);
                    }
                } else if (!object.equals(data)) {
                    throw new GameTestHarnessAssertion("Expected entity data to be: " + data + ", but was: " + object);
                }
            }

        }
    }

    public void assertContainerEmpty(BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        TileEntity blockEntity = this.getLevel().getTileEntity(blockPos);
        if (blockEntity instanceof TileEntityContainer && !((TileEntityContainer)blockEntity).isEmpty()) {
            throw new GameTestHarnessAssertion("Container should be empty");
        }
    }

    public void assertContainerContains(BlockPosition pos, Item item) {
        BlockPosition blockPos = this.absolutePos(pos);
        TileEntity blockEntity = this.getLevel().getTileEntity(blockPos);
        if (blockEntity instanceof TileEntityContainer && ((TileEntityContainer)blockEntity).countItem(item) != 1) {
            throw new GameTestHarnessAssertion("Container should contain: " + item);
        }
    }

    public void assertSameBlockStates(StructureBoundingBox checkedBlockBox, BlockPosition correctStatePos) {
        BlockPosition.betweenClosedStream(checkedBlockBox).forEach((checkedPos) -> {
            BlockPosition blockPos2 = correctStatePos.offset(checkedPos.getX() - checkedBlockBox.minX(), checkedPos.getY() - checkedBlockBox.minY(), checkedPos.getZ() - checkedBlockBox.minZ());
            this.assertSameBlockState(checkedPos, blockPos2);
        });
    }

    public void assertSameBlockState(BlockPosition checkedPos, BlockPosition correctStatePos) {
        IBlockData blockState = this.getBlockState(checkedPos);
        IBlockData blockState2 = this.getBlockState(correctStatePos);
        if (blockState != blockState2) {
            this.fail("Incorrect state. Expected " + blockState2 + ", got " + blockState, checkedPos);
        }

    }

    public void assertAtTickTimeContainerContains(long l, BlockPosition blockPos, Item item) {
        this.runAtTickTime(l, () -> {
            this.assertContainerContains(blockPos, item);
        });
    }

    public void assertAtTickTimeContainerEmpty(long l, BlockPosition blockPos) {
        this.runAtTickTime(l, () -> {
            this.assertContainerEmpty(blockPos);
        });
    }

    public <E extends Entity, T> void succeedWhenEntityData(BlockPosition blockPos, EntityTypes<E> entityType, Function<E, T> function, T object) {
        this.succeedWhen(() -> {
            this.assertEntityData(blockPos, entityType, function, object);
        });
    }

    public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, String string) {
        if (!predicate.test(entity)) {
            throw new GameTestHarnessAssertion("Entity " + entity + " failed " + string + " test");
        }
    }

    public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> function, String string, T object) {
        T object2 = function.apply(entity);
        if (!object2.equals(object)) {
            throw new GameTestHarnessAssertion("Entity " + entity + " value " + string + "=" + object2 + " is not equal to expected " + object);
        }
    }

    public void succeedWhenEntityPresent(EntityTypes<?> type, int x, int y, int z) {
        this.succeedWhenEntityPresent(type, new BlockPosition(x, y, z));
    }

    public void succeedWhenEntityPresent(EntityTypes<?> type, BlockPosition pos) {
        this.succeedWhen(() -> {
            this.assertEntityPresent(type, pos);
        });
    }

    public void succeedWhenEntityNotPresent(EntityTypes<?> type, int x, int y, int z) {
        this.succeedWhenEntityNotPresent(type, new BlockPosition(x, y, z));
    }

    public void succeedWhenEntityNotPresent(EntityTypes<?> type, BlockPosition pos) {
        this.succeedWhen(() -> {
            this.assertEntityNotPresent(type, pos);
        });
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        } else {
            this.finalCheckAdded = true;
        }
    }

    public void succeedIf(Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, runnable).thenSucceed();
    }

    public void succeedWhen(Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(runnable).thenSucceed();
    }

    public void succeedOnTickWhen(int duration, Runnable runnable) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil((long)duration, runnable).thenSucceed();
    }

    public void runAtTickTime(long tick, Runnable runnable) {
        this.testInfo.setRunAtTickTime(tick, runnable);
    }

    public void runAfterDelay(long ticks, Runnable runnable) {
        this.runAtTickTime(this.testInfo.getTick() + ticks, runnable);
    }

    public void randomTick(BlockPosition pos) {
        BlockPosition blockPos = this.absolutePos(pos);
        WorldServer serverLevel = this.getLevel();
        serverLevel.getType(blockPos).randomTick(serverLevel, blockPos, serverLevel.random);
    }

    public void fail(String message, BlockPosition pos) {
        throw new GameTestHarnessAssertionPosition(message, this.absolutePos(pos), pos, this.getTick());
    }

    public void fail(String message, Entity entity) {
        throw new GameTestHarnessAssertionPosition(message, entity.getChunkCoordinates(), this.relativePos(entity.getChunkCoordinates()), this.getTick());
    }

    public void fail(String message) {
        throw new GameTestHarnessAssertion(message);
    }

    public void failIf(Runnable runnable) {
        this.testInfo.createSequence().thenWaitUntil(runnable).thenFail(() -> {
            return new GameTestHarnessAssertion("Fail conditions met");
        });
    }

    public void failIfEver(Runnable runnable) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((l) -> {
            this.testInfo.setRunAtTickTime(l, runnable::run);
        });
    }

    public GameTestHarnessSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPosition absolutePos(BlockPosition pos) {
        BlockPosition blockPos = this.testInfo.getStructureBlockPos();
        BlockPosition blockPos2 = blockPos.offset(pos);
        return DefinedStructure.transform(blockPos2, EnumBlockMirror.NONE, this.testInfo.getRotation(), blockPos);
    }

    public BlockPosition relativePos(BlockPosition pos) {
        BlockPosition blockPos = this.testInfo.getStructureBlockPos();
        EnumBlockRotation rotation = this.testInfo.getRotation().getRotated(EnumBlockRotation.CLOCKWISE_180);
        BlockPosition blockPos2 = DefinedStructure.transform(pos, EnumBlockMirror.NONE, rotation, blockPos);
        return blockPos2.subtract(blockPos);
    }

    public Vec3D absoluteVec(Vec3D pos) {
        Vec3D vec3 = Vec3D.atLowerCornerOf(this.testInfo.getStructureBlockPos());
        return DefinedStructure.transform(vec3.add(pos), EnumBlockMirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    private AxisAlignedBB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    private AxisAlignedBB getRelativeBounds() {
        AxisAlignedBB aABB = this.testInfo.getStructureBounds();
        return aABB.move(BlockPosition.ZERO.subtract(this.absolutePos(BlockPosition.ZERO)));
    }

    public void forEveryBlockInStructure(Consumer<BlockPosition> consumer) {
        AxisAlignedBB aABB = this.getRelativeBounds();
        BlockPosition.MutableBlockPosition.betweenClosedStream(aABB.move(0.0D, 1.0D, 0.0D)).forEach(consumer);
    }

    public void onEachTick(Runnable runnable) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((l) -> {
            this.testInfo.setRunAtTickTime(l, runnable::run);
        });
    }
}
