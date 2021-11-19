package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventoryHolder;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockComposter extends Block implements IInventoryHolder {
    public static final int READY = 8;
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 7;
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL_COMPOSTER;
    public static final Object2FloatMap<IMaterial> COMPOSTABLES = new Object2FloatOpenHashMap<>();
    private static final int AABB_SIDE_THICKNESS = 2;
    private static final VoxelShape OUTER_SHAPE = VoxelShapes.block();
    private static final VoxelShape[] SHAPES = SystemUtils.make(new VoxelShape[9], (shapes) -> {
        for(int i = 0; i < 8; ++i) {
            shapes[i] = VoxelShapes.join(OUTER_SHAPE, Block.box(2.0D, (double)Math.max(2, 1 + i * 2), 2.0D, 14.0D, 16.0D, 14.0D), OperatorBoolean.ONLY_FIRST);
        }

        shapes[8] = shapes[7];
    });

    public static void bootStrap() {
        COMPOSTABLES.defaultReturnValue(-1.0F);
        float f = 0.3F;
        float g = 0.5F;
        float h = 0.65F;
        float i = 0.85F;
        float j = 1.0F;
        add(0.3F, Items.JUNGLE_LEAVES);
        add(0.3F, Items.OAK_LEAVES);
        add(0.3F, Items.SPRUCE_LEAVES);
        add(0.3F, Items.DARK_OAK_LEAVES);
        add(0.3F, Items.ACACIA_LEAVES);
        add(0.3F, Items.BIRCH_LEAVES);
        add(0.3F, Items.AZALEA_LEAVES);
        add(0.3F, Items.OAK_SAPLING);
        add(0.3F, Items.SPRUCE_SAPLING);
        add(0.3F, Items.BIRCH_SAPLING);
        add(0.3F, Items.JUNGLE_SAPLING);
        add(0.3F, Items.ACACIA_SAPLING);
        add(0.3F, Items.DARK_OAK_SAPLING);
        add(0.3F, Items.BEETROOT_SEEDS);
        add(0.3F, Items.DRIED_KELP);
        add(0.3F, Items.GRASS);
        add(0.3F, Items.KELP);
        add(0.3F, Items.MELON_SEEDS);
        add(0.3F, Items.PUMPKIN_SEEDS);
        add(0.3F, Items.SEAGRASS);
        add(0.3F, Items.SWEET_BERRIES);
        add(0.3F, Items.GLOW_BERRIES);
        add(0.3F, Items.WHEAT_SEEDS);
        add(0.3F, Items.MOSS_CARPET);
        add(0.3F, Items.SMALL_DRIPLEAF);
        add(0.3F, Items.HANGING_ROOTS);
        add(0.5F, Items.DRIED_KELP_BLOCK);
        add(0.5F, Items.TALL_GRASS);
        add(0.5F, Items.AZALEA_LEAVES_FLOWERS);
        add(0.5F, Items.CACTUS);
        add(0.5F, Items.SUGAR_CANE);
        add(0.5F, Items.VINE);
        add(0.5F, Items.NETHER_SPROUTS);
        add(0.5F, Items.WEEPING_VINES);
        add(0.5F, Items.TWISTING_VINES);
        add(0.5F, Items.MELON_SLICE);
        add(0.5F, Items.GLOW_LICHEN);
        add(0.65F, Items.SEA_PICKLE);
        add(0.65F, Items.LILY_PAD);
        add(0.65F, Items.PUMPKIN);
        add(0.65F, Items.CARVED_PUMPKIN);
        add(0.65F, Items.MELON);
        add(0.65F, Items.APPLE);
        add(0.65F, Items.BEETROOT);
        add(0.65F, Items.CARROT);
        add(0.65F, Items.COCOA_BEANS);
        add(0.65F, Items.POTATO);
        add(0.65F, Items.WHEAT);
        add(0.65F, Items.BROWN_MUSHROOM);
        add(0.65F, Items.RED_MUSHROOM);
        add(0.65F, Items.MUSHROOM_STEM);
        add(0.65F, Items.CRIMSON_FUNGUS);
        add(0.65F, Items.WARPED_FUNGUS);
        add(0.65F, Items.NETHER_WART);
        add(0.65F, Items.CRIMSON_ROOTS);
        add(0.65F, Items.WARPED_ROOTS);
        add(0.65F, Items.SHROOMLIGHT);
        add(0.65F, Items.DANDELION);
        add(0.65F, Items.POPPY);
        add(0.65F, Items.BLUE_ORCHID);
        add(0.65F, Items.ALLIUM);
        add(0.65F, Items.AZURE_BLUET);
        add(0.65F, Items.RED_TULIP);
        add(0.65F, Items.ORANGE_TULIP);
        add(0.65F, Items.WHITE_TULIP);
        add(0.65F, Items.PINK_TULIP);
        add(0.65F, Items.OXEYE_DAISY);
        add(0.65F, Items.CORNFLOWER);
        add(0.65F, Items.LILY_OF_THE_VALLEY);
        add(0.65F, Items.WITHER_ROSE);
        add(0.65F, Items.FERN);
        add(0.65F, Items.SUNFLOWER);
        add(0.65F, Items.LILAC);
        add(0.65F, Items.ROSE_BUSH);
        add(0.65F, Items.PEONY);
        add(0.65F, Items.LARGE_FERN);
        add(0.65F, Items.SPORE_BLOSSOM);
        add(0.65F, Items.AZALEA);
        add(0.65F, Items.MOSS_BLOCK);
        add(0.65F, Items.BIG_DRIPLEAF);
        add(0.85F, Items.HAY_BLOCK);
        add(0.85F, Items.BROWN_MUSHROOM_BLOCK);
        add(0.85F, Items.RED_MUSHROOM_BLOCK);
        add(0.85F, Items.NETHER_WART_BLOCK);
        add(0.85F, Items.WARPED_WART_BLOCK);
        add(0.85F, Items.FLOWERING_AZALEA);
        add(0.85F, Items.BREAD);
        add(0.85F, Items.BAKED_POTATO);
        add(0.85F, Items.COOKIE);
        add(1.0F, Items.CAKE);
        add(1.0F, Items.PUMPKIN_PIE);
    }

    private static void add(float levelIncreaseChance, IMaterial item) {
        COMPOSTABLES.put(item.getItem(), levelIncreaseChance);
    }

    public BlockComposter(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(LEVEL, Integer.valueOf(0)));
    }

    public static void handleFill(World world, BlockPosition pos, boolean fill) {
        IBlockData blockState = world.getType(pos);
        world.playLocalSound((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), fill ? SoundEffects.COMPOSTER_FILL_SUCCESS : SoundEffects.COMPOSTER_FILL, EnumSoundCategory.BLOCKS, 1.0F, 1.0F, false);
        double d = blockState.getShape(world, pos).max(EnumDirection.EnumAxis.Y, 0.5D, 0.5D) + 0.03125D;
        double e = (double)0.13125F;
        double f = (double)0.7375F;
        Random random = world.getRandom();

        for(int i = 0; i < 10; ++i) {
            double g = random.nextGaussian() * 0.02D;
            double h = random.nextGaussian() * 0.02D;
            double j = random.nextGaussian() * 0.02D;
            world.addParticle(Particles.COMPOSTER, (double)pos.getX() + (double)0.13125F + (double)0.7375F * (double)random.nextFloat(), (double)pos.getY() + d + (double)random.nextFloat() * (1.0D - d), (double)pos.getZ() + (double)0.13125F + (double)0.7375F * (double)random.nextFloat(), g, h, j);
        }

    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPES[state.get(LEVEL)];
    }

    @Override
    public VoxelShape getInteractionShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return OUTER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        return SHAPES[0];
    }

    @Override
    public void onPlace(IBlockData state, World world, BlockPosition pos, IBlockData oldState, boolean notify) {
        if (state.get(LEVEL) == 7) {
            world.getBlockTickList().scheduleTick(pos, state.getBlock(), 20);
        }

    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        int i = state.get(LEVEL);
        ItemStack itemStack = player.getItemInHand(hand);
        if (i < 8 && COMPOSTABLES.containsKey(itemStack.getItem())) {
            if (i < 7 && !world.isClientSide) {
                IBlockData blockState = addItem(state, world, pos, itemStack);
                world.triggerEffect(1500, pos, state != blockState ? 1 : 0);
                player.awardStat(StatisticList.ITEM_USED.get(itemStack.getItem()));
                if (!player.getAbilities().instabuild) {
                    itemStack.subtract(1);
                }
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else if (i == 8) {
            extractProduce(state, world, pos);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    public static IBlockData insertItem(IBlockData state, WorldServer world, ItemStack stack, BlockPosition pos) {
        int i = state.get(LEVEL);
        if (i < 7 && COMPOSTABLES.containsKey(stack.getItem())) {
            IBlockData blockState = addItem(state, world, pos, stack);
            stack.subtract(1);
            return blockState;
        } else {
            return state;
        }
    }

    public static IBlockData extractProduce(IBlockData state, World world, BlockPosition pos) {
        if (!world.isClientSide) {
            float f = 0.7F;
            double d = (double)(world.random.nextFloat() * 0.7F) + (double)0.15F;
            double e = (double)(world.random.nextFloat() * 0.7F) + (double)0.060000002F + 0.6D;
            double g = (double)(world.random.nextFloat() * 0.7F) + (double)0.15F;
            EntityItem itemEntity = new EntityItem(world, (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + g, new ItemStack(Items.BONE_MEAL));
            itemEntity.defaultPickupDelay();
            world.addEntity(itemEntity);
        }

        IBlockData blockState = empty(state, world, pos);
        world.playSound((EntityHuman)null, pos, SoundEffects.COMPOSTER_EMPTY, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
        return blockState;
    }

    static IBlockData empty(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = state.set(LEVEL, Integer.valueOf(0));
        world.setTypeAndData(pos, blockState, 3);
        return blockState;
    }

    static IBlockData addItem(IBlockData state, GeneratorAccess world, BlockPosition pos, ItemStack item) {
        int i = state.get(LEVEL);
        float f = COMPOSTABLES.getFloat(item.getItem());
        if ((i != 0 || !(f > 0.0F)) && !(world.getRandom().nextDouble() < (double)f)) {
            return state;
        } else {
            int j = i + 1;
            IBlockData blockState = state.set(LEVEL, Integer.valueOf(j));
            world.setTypeAndData(pos, blockState, 3);
            if (j == 7) {
                world.getBlockTickList().scheduleTick(pos, state.getBlock(), 20);
            }

            return blockState;
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (state.get(LEVEL) == 7) {
            world.setTypeAndData(pos, state.cycle(LEVEL), 3);
            world.playSound((EntityHuman)null, pos, SoundEffects.COMPOSTER_READY, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
        }

    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return state.get(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(LEVEL);
    }

    @Override
    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }

    @Override
    public IWorldInventory getContainer(IBlockData state, GeneratorAccess world, BlockPosition pos) {
        int i = state.get(LEVEL);
        if (i == 8) {
            return new BlockComposter.ContainerOutput(state, world, pos, new ItemStack(Items.BONE_MEAL));
        } else {
            return (IWorldInventory)(i < 7 ? new BlockComposter.ContainerInput(state, world, pos) : new BlockComposter.ContainerEmpty());
        }
    }

    public static class ContainerEmpty extends InventorySubcontainer implements IWorldInventory {
        public ContainerEmpty() {
            super(0);
        }

        @Override
        public int[] getSlotsForFace(EnumDirection side) {
            return new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
            return false;
        }
    }

    public static class ContainerInput extends InventorySubcontainer implements IWorldInventory {
        private final IBlockData state;
        private final GeneratorAccess level;
        private final BlockPosition pos;
        private boolean changed;

        public ContainerInput(IBlockData state, GeneratorAccess world, BlockPosition pos) {
            super(1);
            this.state = state;
            this.level = world;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(EnumDirection side) {
            return side == EnumDirection.UP ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
            return !this.changed && dir == EnumDirection.UP && BlockComposter.COMPOSTABLES.containsKey(stack.getItem());
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
            return false;
        }

        @Override
        public void update() {
            ItemStack itemStack = this.getItem(0);
            if (!itemStack.isEmpty()) {
                this.changed = true;
                IBlockData blockState = BlockComposter.addItem(this.state, this.level, this.pos, itemStack);
                this.level.triggerEffect(1500, this.pos, blockState != this.state ? 1 : 0);
                this.splitWithoutUpdate(0);
            }

        }
    }

    public static class ContainerOutput extends InventorySubcontainer implements IWorldInventory {
        private final IBlockData state;
        private final GeneratorAccess level;
        private final BlockPosition pos;
        private boolean changed;

        public ContainerOutput(IBlockData state, GeneratorAccess world, BlockPosition pos, ItemStack outputItem) {
            super(outputItem);
            this.state = state;
            this.level = world;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(EnumDirection side) {
            return side == EnumDirection.DOWN ? new int[]{0} : new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
            return !this.changed && dir == EnumDirection.DOWN && stack.is(Items.BONE_MEAL);
        }

        @Override
        public void update() {
            BlockComposter.empty(this.state, this.level, this.pos);
            this.changed = true;
        }
    }
}
