package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockPropertyInstrument;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateEnum;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockNote extends Block {
    public static final BlockStateEnum<BlockPropertyInstrument> INSTRUMENT = BlockProperties.NOTEBLOCK_INSTRUMENT;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateInteger NOTE = BlockProperties.NOTE;

    public BlockNote(BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(INSTRUMENT, BlockPropertyInstrument.HARP).set(NOTE, Integer.valueOf(0)).set(POWERED, Boolean.valueOf(false)));
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext ctx) {
        return this.getBlockData().set(INSTRUMENT, BlockPropertyInstrument.byState(ctx.getWorld().getType(ctx.getClickPosition().below())));
    }

    @Override
    public IBlockData updateState(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        return direction == EnumDirection.DOWN ? state.set(INSTRUMENT, BlockPropertyInstrument.byState(neighborState)) : super.updateState(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void doPhysics(IBlockData state, World world, BlockPosition pos, Block block, BlockPosition fromPos, boolean notify) {
        boolean bl = world.isBlockIndirectlyPowered(pos);
        if (bl != state.get(POWERED)) {
            if (bl) {
                this.play(world, pos);
            }

            world.setTypeAndData(pos, state.set(POWERED, Boolean.valueOf(bl)), 3);
        }

    }

    private void play(World world, BlockPosition pos) {
        if (world.getType(pos.above()).isAir()) {
            world.playBlockAction(pos, this, 0, 0);
        }

    }

    @Override
    public EnumInteractionResult interact(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            state = state.cycle(NOTE);
            world.setTypeAndData(pos, state, 3);
            this.play(world, pos);
            player.awardStat(StatisticList.TUNE_NOTEBLOCK);
            return EnumInteractionResult.CONSUME;
        }
    }

    @Override
    public void attack(IBlockData state, World world, BlockPosition pos, EntityHuman player) {
        if (!world.isClientSide) {
            this.play(world, pos);
            player.awardStat(StatisticList.PLAY_NOTEBLOCK);
        }
    }

    @Override
    public boolean triggerEvent(IBlockData state, World world, BlockPosition pos, int type, int data) {
        int i = state.get(NOTE);
        float f = (float)Math.pow(2.0D, (double)(i - 12) / 12.0D);
        world.playSound((EntityHuman)null, pos, state.get(INSTRUMENT).getSoundEvent(), EnumSoundCategory.RECORDS, 3.0F, f);
        world.addParticle(Particles.NOTE, (double)pos.getX() + 0.5D, (double)pos.getY() + 1.2D, (double)pos.getZ() + 0.5D, (double)i / 24.0D, 0.0D, 0.0D);
        return true;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(INSTRUMENT, POWERED, NOTE);
    }
}
