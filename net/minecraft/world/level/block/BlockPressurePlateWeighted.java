package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;

public class BlockPressurePlateWeighted extends BlockPressurePlateAbstract {
    public static final BlockStateInteger POWER = BlockProperties.POWER;
    private final int maxWeight;

    protected BlockPressurePlateWeighted(int weight, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(POWER, Integer.valueOf(0)));
        this.maxWeight = weight;
    }

    @Override
    protected int getSignalStrength(World world, BlockPosition pos) {
        int i = Math.min(world.getEntitiesOfClass(Entity.class, TOUCH_AABB.move(pos)).size(), this.maxWeight);
        if (i > 0) {
            float f = (float)Math.min(this.maxWeight, i) / (float)this.maxWeight;
            return MathHelper.ceil(f * 15.0F);
        } else {
            return 0;
        }
    }

    @Override
    protected void playOnSound(GeneratorAccess world, BlockPosition pos) {
        world.playSound((EntityHuman)null, pos, SoundEffects.METAL_PRESSURE_PLATE_CLICK_ON, EnumSoundCategory.BLOCKS, 0.3F, 0.90000004F);
    }

    @Override
    protected void playOffSound(GeneratorAccess world, BlockPosition pos) {
        world.playSound((EntityHuman)null, pos, SoundEffects.METAL_PRESSURE_PLATE_CLICK_OFF, EnumSoundCategory.BLOCKS, 0.3F, 0.75F);
    }

    @Override
    protected int getPower(IBlockData state) {
        return state.get(POWER);
    }

    @Override
    protected IBlockData setSignalForState(IBlockData state, int rsOut) {
        return state.set(POWER, Integer.valueOf(rsOut));
    }

    @Override
    protected int getPressedTime() {
        return 10;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(POWER);
    }
}
