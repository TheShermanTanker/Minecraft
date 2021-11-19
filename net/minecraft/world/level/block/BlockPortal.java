package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.portal.BlockPortalShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockPortal extends Block {
    public static final BlockStateEnum<EnumDirection.EnumAxis> AXIS = BlockProperties.HORIZONTAL_AXIS;
    protected static final int AABB_OFFSET = 2;
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public BlockPortal(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(AXIS, EnumDirection.EnumAxis.X));
    }

    @Override
    public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        switch((EnumDirection.EnumAxis)state.get(AXIS)) {
        case Z:
            return Z_AXIS_AABB;
        case X:
        default:
            return X_AXIS_AABB;
        }
    }

    @Override
    public void tick(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        if (world.getDimensionManager().isNatural() && world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && random.nextInt(2000) < world.getDifficulty().getId()) {
            while(world.getType(pos).is(this)) {
                pos = pos.below();
            }

            if (world.getType(pos).isValidSpawn(world, pos, EntityTypes.ZOMBIFIED_PIGLIN)) {
                Entity entity = EntityTypes.ZOMBIFIED_PIGLIN.spawnCreature(world, (NBTTagCompound)null, (IChatBaseComponent)null, (EntityHuman)null, pos.above(), EnumMobSpawn.STRUCTURE, false, false);
                if (entity != null) {
                    entity.resetPortalCooldown();
                }
            }
        }

    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        EnumDirection.EnumAxis axis = direction.getAxis();
        EnumDirection.EnumAxis axis2 = state.get(AXIS);
        boolean bl = axis2 != axis && axis.isHorizontal();
        return !bl && !neighborState.is(this) && !(new BlockPortalShape(world, pos, axis2)).isComplete() ? Blocks.AIR.getBlockData() : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void entityInside(IBlockData state, World world, BlockPosition pos, Entity entity) {
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canPortal()) {
            entity.handleInsidePortal(pos);
        }

    }

    @Override
    public void animateTick(IBlockData state, World world, BlockPosition pos, Random random) {
        if (random.nextInt(100) == 0) {
            world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEffects.PORTAL_AMBIENT, EnumSoundCategory.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }

        for(int i = 0; i < 4; ++i) {
            double d = (double)pos.getX() + random.nextDouble();
            double e = (double)pos.getY() + random.nextDouble();
            double f = (double)pos.getZ() + random.nextDouble();
            double g = ((double)random.nextFloat() - 0.5D) * 0.5D;
            double h = ((double)random.nextFloat() - 0.5D) * 0.5D;
            double j = ((double)random.nextFloat() - 0.5D) * 0.5D;
            int k = random.nextInt(2) * 2 - 1;
            if (!world.getType(pos.west()).is(this) && !world.getType(pos.east()).is(this)) {
                d = (double)pos.getX() + 0.5D + 0.25D * (double)k;
                g = (double)(random.nextFloat() * 2.0F * (float)k);
            } else {
                f = (double)pos.getZ() + 0.5D + 0.25D * (double)k;
                j = (double)(random.nextFloat() * 2.0F * (float)k);
            }

            world.addParticle(Particles.PORTAL, d, e, f, g, h, j);
        }

    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess world, BlockPosition pos, IBlockData state) {
        return ItemStack.EMPTY;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
        case CLOCKWISE_90:
            switch((EnumDirection.EnumAxis)state.get(AXIS)) {
            case Z:
                return state.set(AXIS, EnumDirection.EnumAxis.X);
            case X:
                return state.set(AXIS, EnumDirection.EnumAxis.Z);
            default:
                return state;
            }
        default:
            return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(AXIS);
    }
}
