package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface ICaveVine {
    VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    BlockStateBoolean BERRIES = BlockProperties.BERRIES;

    static EnumInteractionResult harvest(IBlockData state, World world, BlockPosition pos) {
        if (state.get(BERRIES)) {
            Block.popResource(world, pos, new ItemStack(Items.GLOW_BERRIES, 1));
            float f = MathHelper.randomBetween(world.random, 0.8F, 1.2F);
            world.playSound((EntityHuman)null, pos, SoundEffects.CAVE_VINES_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, f);
            world.setTypeAndData(pos, state.set(BERRIES, Boolean.valueOf(false)), 2);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    static boolean hasGlowBerries(IBlockData state) {
        return state.hasProperty(BERRIES) && state.get(BERRIES);
    }

    static ToIntFunction<IBlockData> emission(int luminance) {
        return (state) -> {
            return state.get(BlockProperties.BERRIES) ? luminance : 0;
        };
    }
}
