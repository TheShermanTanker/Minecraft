package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.BlockPropertyJigsawOrientation;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockJigsaw extends Block implements ITileEntity, GameMasterBlock {
    public static final BlockStateEnum<BlockPropertyJigsawOrientation> ORIENTATION = BlockProperties.ORIENTATION;

    protected BlockJigsaw(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(ORIENTATION, BlockPropertyJigsawOrientation.NORTH_UP));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(ORIENTATION, rotation.rotation().rotate(state.get(ORIENTATION)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.set(ORIENTATION, mirror.rotation().rotate(state.get(ORIENTATION)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace();
        EnumDirection direction2;
        if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
            direction2 = ctx.getHorizontalDirection().opposite();
        } else {
            direction2 = EnumDirection.UP;
        }

        return this.getBlockData().set(ORIENTATION, BlockPropertyJigsawOrientation.fromFrontAndTop(direction, direction2));
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityJigsaw(pos, state);
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityJigsaw && player.isCreativeAndOp()) {
            player.openJigsawBlock((TileEntityJigsaw)blockEntity);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    public static boolean canAttach(DefinedStructure.BlockInfo info1, DefinedStructure.BlockInfo info2) {
        EnumDirection direction = getFrontFacing(info1.state);
        EnumDirection direction2 = getFrontFacing(info2.state);
        EnumDirection direction3 = getTopFacing(info1.state);
        EnumDirection direction4 = getTopFacing(info2.state);
        TileEntityJigsaw.JointType jointType = TileEntityJigsaw.JointType.byName(info1.nbt.getString("joint")).orElseGet(() -> {
            return direction.getAxis().isHorizontal() ? TileEntityJigsaw.JointType.ALIGNED : TileEntityJigsaw.JointType.ROLLABLE;
        });
        boolean bl = jointType == TileEntityJigsaw.JointType.ROLLABLE;
        return direction == direction2.opposite() && (bl || direction3 == direction4) && info1.nbt.getString("target").equals(info2.nbt.getString("name"));
    }

    public static EnumDirection getFrontFacing(IBlockData state) {
        return state.get(ORIENTATION).front();
    }

    public static EnumDirection getTopFacing(IBlockData state) {
        return state.get(ORIENTATION).top();
    }
}
