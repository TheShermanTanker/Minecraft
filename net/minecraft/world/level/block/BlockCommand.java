package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.UtilColor;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlockCommand extends BlockTileEntity implements GameMasterBlock {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final BlockStateDirection FACING = BlockDirectional.FACING;
    public static final BlockStateBoolean CONDITIONAL = BlockProperties.CONDITIONAL;
    private final boolean automatic;

    public BlockCommand(BlockBase.Info settings, boolean auto) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(FACING, EnumDirection.NORTH).set(CONDITIONAL, Boolean.valueOf(false)));
        this.automatic = auto;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        TileEntityCommand commandBlockEntity = new TileEntityCommand(pos, state);
        commandBlockEntity.setAutomatic(this.automatic);
        return commandBlockEntity;
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        if (!world.isClientSide) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityCommand) {
                TileEntityCommand commandBlockEntity = (TileEntityCommand)blockEntity;
                boolean bl = world.isBlockIndirectlyPowered(pos);
                boolean bl2 = commandBlockEntity.isPowered();
                commandBlockEntity.setPowered(bl);
                if (!bl2 && !commandBlockEntity.isAutomatic() && commandBlockEntity.getMode() != TileEntityCommand.Type.SEQUENCE) {
                    if (bl) {
                        commandBlockEntity.markConditionMet();
                        world.getBlockTickList().scheduleTick(pos, this, 1);
                    }

                }
            }
        }
    }

    @Override
    public void tickAlways(IBlockData state, WorldServer world, BlockPosition pos, Random random) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityCommand) {
            TileEntityCommand commandBlockEntity = (TileEntityCommand)blockEntity;
            CommandBlockListenerAbstract baseCommandBlock = commandBlockEntity.getCommandBlock();
            boolean bl = !UtilColor.isNullOrEmpty(baseCommandBlock.getCommand());
            TileEntityCommand.Type mode = commandBlockEntity.getMode();
            boolean bl2 = commandBlockEntity.wasConditionMet();
            if (mode == TileEntityCommand.Type.AUTO) {
                commandBlockEntity.markConditionMet();
                if (bl2) {
                    this.execute(state, world, pos, baseCommandBlock, bl);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }

                if (commandBlockEntity.isPowered() || commandBlockEntity.isAutomatic()) {
                    world.getBlockTicks().scheduleTick(pos, this, 1);
                }
            } else if (mode == TileEntityCommand.Type.REDSTONE) {
                if (bl2) {
                    this.execute(state, world, pos, baseCommandBlock, bl);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }

            world.updateAdjacentComparators(pos, this);
        }

    }

    private void execute(IBlockData state, World world, BlockPosition pos, CommandBlockListenerAbstract executor, boolean hasCommand) {
        if (hasCommand) {
            executor.performCommand(world);
        } else {
            executor.setSuccessCount(0);
        }

        executeChain(world, pos, state.get(FACING));
    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityCommand && player.isCreativeAndOp()) {
            player.openCommandBlock((TileEntityCommand)blockEntity);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public boolean isComplexRedstone(IBlockData state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(IBlockData state, World world, BlockPosition pos) {
        TileEntity blockEntity = world.getTileEntity(pos);
        return blockEntity instanceof TileEntityCommand ? ((TileEntityCommand)blockEntity).getCommandBlock().getSuccessCount() : 0;
    }

    @Override
    public void postPlace(World world, BlockPosition pos, IBlockData state, EntityLiving placer, ItemStack itemStack) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityCommand) {
            TileEntityCommand commandBlockEntity = (TileEntityCommand)blockEntity;
            CommandBlockListenerAbstract baseCommandBlock = commandBlockEntity.getCommandBlock();
            if (itemStack.hasName()) {
                baseCommandBlock.setName(itemStack.getName());
            }

            if (!world.isClientSide) {
                if (itemStack.getTagElement("BlockEntityTag") == null) {
                    baseCommandBlock.setTrackOutput(world.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
                    commandBlockEntity.setAutomatic(this.automatic);
                }

                if (commandBlockEntity.getMode() == TileEntityCommand.Type.SEQUENCE) {
                    boolean bl = world.isBlockIndirectlyPowered(pos);
                    commandBlockEntity.setPowered(bl);
                }
            }

        }
    }

    @Override
    public EnumRenderType getRenderShape(IBlockData state) {
        return EnumRenderType.MODEL;
    }

    @Override
    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return state.set(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(FACING, CONDITIONAL);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(FACING, ctx.getNearestLookingDirection().opposite());
    }

    private static void executeChain(World world, BlockPosition pos, EnumDirection facing) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        GameRules gameRules = world.getGameRules();

        int i;
        IBlockData blockState;
        for(i = gameRules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH); i-- > 0; facing = blockState.get(FACING)) {
            mutableBlockPos.move(facing);
            blockState = world.getType(mutableBlockPos);
            Block block = blockState.getBlock();
            if (!blockState.is(Blocks.CHAIN_COMMAND_BLOCK)) {
                break;
            }

            TileEntity blockEntity = world.getTileEntity(mutableBlockPos);
            if (!(blockEntity instanceof TileEntityCommand)) {
                break;
            }

            TileEntityCommand commandBlockEntity = (TileEntityCommand)blockEntity;
            if (commandBlockEntity.getMode() != TileEntityCommand.Type.SEQUENCE) {
                break;
            }

            if (commandBlockEntity.isPowered() || commandBlockEntity.isAutomatic()) {
                CommandBlockListenerAbstract baseCommandBlock = commandBlockEntity.getCommandBlock();
                if (commandBlockEntity.markConditionMet()) {
                    if (!baseCommandBlock.performCommand(world)) {
                        break;
                    }

                    world.updateAdjacentComparators(mutableBlockPos, block);
                } else if (commandBlockEntity.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }
        }

        if (i <= 0) {
            int j = Math.max(gameRules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
            LOGGER.warn("Command Block chain tried to execute more than {} steps!", (int)j);
        }

    }
}
