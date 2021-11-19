package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.entity.vehicle.EntityMinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockBeehive extends BlockTileEntity {
    private static final EnumDirection[] SPAWN_DIRECTIONS = new EnumDirection[]{EnumDirection.WEST, EnumDirection.EAST, EnumDirection.SOUTH};
    public static final BlockStateDirection FACING = BlockFacingHorizontal.FACING;
    public static final BlockStateInteger HONEY_LEVEL = BlockProperties.LEVEL_HONEY;
    public static final int MAX_HONEY_LEVELS = 5;
    private static final int SHEARED_HONEYCOMB_COUNT = 3;

    public BlockBeehive(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(HONEY_LEVEL, Integer.valueOf(0)).set(FACING, EnumDirection.NORTH));
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        return state.get(HONEY_LEVEL);
    }

    @Override
    public void playerDestroy(World world, EntityHuman player, BlockPosition pos, IBlockData state, @Nullable TileEntity blockEntity, ItemStack stack) {
        super.playerDestroy(world, player, pos, state, blockEntity, stack);
        if (!world.isClientSide && blockEntity instanceof TileEntityBeehive) {
            TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
            if (EnchantmentManager.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) == 0) {
                beehiveBlockEntity.emptyAllLivingFromHive(player, state, TileEntityBeehive.ReleaseStatus.EMERGENCY);
                world.updateAdjacentComparators(pos, this);
                this.angerNearbyBees(world, pos);
            }

            CriterionTriggers.BEE_NEST_DESTROYED.trigger((EntityPlayer)player, state, stack, beehiveBlockEntity.getBeeCount());
        }

    }

    private void angerNearbyBees(World world, BlockPosition pos) {
        List<EntityBee> list = world.getEntitiesOfClass(EntityBee.class, (new AxisAlignedBB(pos)).grow(8.0D, 6.0D, 8.0D));
        if (!list.isEmpty()) {
            List<EntityHuman> list2 = world.getEntitiesOfClass(EntityHuman.class, (new AxisAlignedBB(pos)).grow(8.0D, 6.0D, 8.0D));
            int i = list2.size();

            for(EntityBee bee : list) {
                if (bee.getGoalTarget() == null) {
                    bee.setGoalTarget(list2.get(world.random.nextInt(i)));
                }
            }
        }

    }

    public static void dropHoneycomb(World world, BlockPosition pos) {
        popResource(world, pos, new ItemStack(Items.HONEYCOMB, 3));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        int i = state.get(HONEY_LEVEL);
        boolean bl = false;
        if (i >= 5) {
            Item item = itemStack.getItem();
            if (itemStack.is(Items.SHEARS)) {
                world.playSound(player, player.locX(), player.locY(), player.locZ(), SoundEffects.BEEHIVE_SHEAR, EnumSoundCategory.NEUTRAL, 1.0F, 1.0F);
                dropHoneycomb(world, pos);
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
                bl = true;
                world.gameEvent(player, GameEvent.SHEAR, pos);
            } else if (itemStack.is(Items.GLASS_BOTTLE)) {
                itemStack.subtract(1);
                world.playSound(player, player.locX(), player.locY(), player.locZ(), SoundEffects.BOTTLE_FILL, EnumSoundCategory.NEUTRAL, 1.0F, 1.0F);
                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, new ItemStack(Items.HONEY_BOTTLE));
                } else if (!player.getInventory().pickup(new ItemStack(Items.HONEY_BOTTLE))) {
                    player.drop(new ItemStack(Items.HONEY_BOTTLE), false);
                }

                bl = true;
                world.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }

            if (!world.isClientSide() && bl) {
                player.awardStat(StatisticList.ITEM_USED.get(item));
            }
        }

        if (bl) {
            if (!BlockCampfire.isSmokeyPos(world, pos)) {
                if (this.hiveContainsBees(world, pos)) {
                    this.angerNearbyBees(world, pos);
                }

                this.releaseBeesAndResetHoneyLevel(world, state, pos, player, TileEntityBeehive.ReleaseStatus.EMERGENCY);
            } else {
                this.resetHoneyLevel(world, state, pos);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return super.interact(state, world, pos, player, hand, hit);
        }
    }

    private boolean hiveContainsBees(World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityBeehive) {
            TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
            return !beehiveBlockEntity.isEmpty();
        } else {
            return false;
        }
    }

    public void releaseBeesAndResetHoneyLevel(World world, IBlockData state, BlockPosition pos, @Nullable EntityHuman player, TileEntityBeehive.ReleaseStatus beeState) {
        this.resetHoneyLevel(world, state, pos);
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityBeehive) {
            TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
            beehiveBlockEntity.emptyAllLivingFromHive(player, state, beeState);
        }

    }

    public void resetHoneyLevel(World world, IBlockData state, BlockPosition pos) {
        world.setTypeAndData(pos, state.set(HONEY_LEVEL, Integer.valueOf(0)), 3);
    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (state.get(HONEY_LEVEL) >= 5) {
            for(int i = 0; i < random.nextInt(1) + 1; ++i) {
                this.trySpawnDripParticles(world, pos, state);
            }
        }

    }

    private void trySpawnDripParticles(World world, BlockPosition pos, IBlockData state) {
        if (state.getFluid().isEmpty() && !(world.random.nextFloat() < 0.3F)) {
            VoxelShape voxelShape = state.getCollisionShape(world, pos);
            double d = voxelShape.max(EnumDirection.EnumAxis.Y);
            if (d >= 1.0D && !state.is(TagsBlock.IMPERMEABLE)) {
                double e = voxelShape.min(EnumDirection.EnumAxis.Y);
                if (e > 0.0D) {
                    this.spawnParticle(world, pos, voxelShape, (double)pos.getY() + e - 0.05D);
                } else {
                    BlockPosition blockPos = pos.below();
                    IBlockData blockState = world.getType(blockPos);
                    VoxelShape voxelShape2 = blockState.getCollisionShape(world, blockPos);
                    double f = voxelShape2.max(EnumDirection.EnumAxis.Y);
                    if ((f < 1.0D || !blockState.isCollisionShapeFullBlock(world, blockPos)) && blockState.getFluid().isEmpty()) {
                        this.spawnParticle(world, pos, voxelShape, (double)pos.getY() - 0.05D);
                    }
                }
            }

        }
    }

    private void spawnParticle(World world, BlockPosition pos, VoxelShape shape, double height) {
        this.spawnFluidParticle(world, (double)pos.getX() + shape.min(EnumDirection.EnumAxis.X), (double)pos.getX() + shape.max(EnumDirection.EnumAxis.X), (double)pos.getZ() + shape.min(EnumDirection.EnumAxis.Z), (double)pos.getZ() + shape.max(EnumDirection.EnumAxis.Z), height);
    }

    private void spawnFluidParticle(World world, double minX, double maxX, double minZ, double maxZ, double height) {
        world.addParticle(Particles.DRIPPING_HONEY, MathHelper.lerp(world.random.nextDouble(), minX, maxX), height, MathHelper.lerp(world.random.nextDouble(), minZ, maxZ), 0.0D, 0.0D, 0.0D);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getHorizontalDirection().opposite());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(HONEY_LEVEL, FACING);
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityBeehive(pos, state);
    }

    @Nullable
    @Override
    public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return world.isClientSide ? null : createTickerHelper(type, TileEntityTypes.BEEHIVE, TileEntityBeehive::serverTick);
    }

    @Override
    public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        if (!world.isClientSide && player.isCreative() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBeehive) {
                TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
                ItemStack itemStack = new ItemStack(this);
                int i = state.get(HONEY_LEVEL);
                boolean bl = !beehiveBlockEntity.isEmpty();
                if (bl || i > 0) {
                    if (bl) {
                        NBTTagCompound compoundTag = new NBTTagCompound();
                        compoundTag.set("Bees", beehiveBlockEntity.writeBees());
                        itemStack.addTagElement("BlockEntityTag", compoundTag);
                    }

                    NBTTagCompound compoundTag2 = new NBTTagCompound();
                    compoundTag2.setInt("honey_level", i);
                    itemStack.addTagElement("BlockStateTag", compoundTag2);
                    EntityItem itemEntity = new EntityItem(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack);
                    itemEntity.defaultPickupDelay();
                    world.addEntity(itemEntity);
                }
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(IBlockData state, LootTableInfo.Builder builder) {
        Entity entity = builder.getOptionalParameter(LootContextParameters.THIS_ENTITY);
        if (entity instanceof EntityTNTPrimed || entity instanceof EntityCreeper || entity instanceof EntityWitherSkull || entity instanceof EntityWither || entity instanceof EntityMinecartTNT) {
            TileEntity blockEntity = builder.getOptionalParameter(LootContextParameters.BLOCK_ENTITY);
            if (blockEntity instanceof TileEntityBeehive) {
                TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
                beehiveBlockEntity.emptyAllLivingFromHive((EntityHuman)null, state, TileEntityBeehive.ReleaseStatus.EMERGENCY);
            }
        }

        return super.getDrops(state, builder);
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (world.getType(neighborPos).getBlock() instanceof BlockFire) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityBeehive) {
                TileEntityBeehive beehiveBlockEntity = (TileEntityBeehive)blockEntity;
                beehiveBlockEntity.emptyAllLivingFromHive((EntityHuman)null, state, TileEntityBeehive.ReleaseStatus.EMERGENCY);
            }
        }

        return super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    public static EnumDirection getRandomOffset(Random random) {
        return SystemUtils.getRandom(SPAWN_DIRECTIONS, random);
    }
}
