package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockStructure extends BlockTileEntity implements GameMasterBlock {
    public static final BlockStateEnum<BlockPropertyStructureMode> MODE = BlockProperties.STRUCTUREBLOCK_MODE;

    protected BlockStructure(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(MODE, BlockPropertyStructureMode.LOAD));
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityStructure(pos, state);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityStructure) {
            return ((TileEntityStructure)blockEntity).usedBy(player) ? EnumInteractionResult.sidedSuccess(world.isClientSide) : EnumInteractionResult.PASS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        if (!world.isClientSide) {
            if (placer != null) {
                TileEntity blockEntity = world.getTileEntity(pos);
                if (blockEntity instanceof TileEntityStructure) {
                    ((TileEntityStructure)blockEntity).setAuthor(placer);
                }
            }

        }
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(MODE);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (world instanceof WorldServer) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityStructure) {
                TileEntityStructure structureBlockEntity = (TileEntityStructure)blockEntity;
                boolean bl = world.isBlockIndirectlyPowered(pos);
                boolean bl2 = structureBlockEntity.isPowered();
                if (bl && !bl2) {
                    structureBlockEntity.setPowered(true);
                    this.trigger((WorldServer)world, structureBlockEntity);
                } else if (!bl && bl2) {
                    structureBlockEntity.setPowered(false);
                }

            }
        }
    }

    private void trigger(WorldServer world, TileEntityStructure blockEntity) {
        switch(blockEntity.getUsageMode()) {
        case SAVE:
            blockEntity.saveStructure(false);
            break;
        case LOAD:
            blockEntity.loadStructure(world, false);
            break;
        case CORNER:
            blockEntity.unloadStructure();
        case DATA:
        }

    }
}
