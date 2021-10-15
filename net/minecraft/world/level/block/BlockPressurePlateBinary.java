package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AxisAlignedBB;

public class BlockPressurePlateBinary extends BlockPressurePlateAbstract {
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    private final BlockPressurePlateBinary.EnumMobType sensitivity;

    protected BlockPressurePlateBinary(BlockPressurePlateBinary.EnumMobType type, BlockBase.Info settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.getBlockData().set(POWERED, Boolean.valueOf(false)));
        this.sensitivity = type;
    }

    @Override
    protected int getPower(IBlockData state) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    protected IBlockData setSignalForState(IBlockData state, int rsOut) {
        return state.set(POWERED, Boolean.valueOf(rsOut > 0));
    }

    @Override
    protected void playOnSound(GeneratorAccess world, BlockPosition pos) {
        if (this.material != Material.WOOD && this.material != Material.NETHER_WOOD) {
            world.playSound((EntityHuman)null, pos, SoundEffects.STONE_PRESSURE_PLATE_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.6F);
        } else {
            world.playSound((EntityHuman)null, pos, SoundEffects.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.8F);
        }

    }

    @Override
    protected void playOffSound(GeneratorAccess world, BlockPosition pos) {
        if (this.material != Material.WOOD && this.material != Material.NETHER_WOOD) {
            world.playSound((EntityHuman)null, pos, SoundEffects.STONE_PRESSURE_PLATE_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 0.5F);
        } else {
            world.playSound((EntityHuman)null, pos, SoundEffects.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 0.7F);
        }

    }

    @Override
    protected int getSignalStrength(World world, BlockPosition pos) {
        AxisAlignedBB aABB = TOUCH_AABB.move(pos);
        List<? extends Entity> list;
        switch(this.sensitivity) {
        case EVERYTHING:
            list = world.getEntities((Entity)null, aABB);
            break;
        case MOBS:
            list = world.getEntitiesOfClass(EntityLiving.class, aABB);
            break;
        default:
            return 0;
        }

        if (!list.isEmpty()) {
            for(Entity entity : list) {
                if (!entity.isIgnoreBlockTrigger()) {
                    return 15;
                }
            }
        }

        return 0;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.Builder<Block, IBlockData> builder) {
        builder.add(POWERED);
    }

    public static enum EnumMobType {
        EVERYTHING,
        MOBS;
    }
}
