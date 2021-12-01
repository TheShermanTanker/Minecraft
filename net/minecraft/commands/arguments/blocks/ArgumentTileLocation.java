package net.minecraft.commands.arguments.blocks;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class ArgumentTileLocation implements Predicate<ShapeDetectorBlock> {
    private final IBlockData state;
    private final Set<IBlockState<?>> properties;
    @Nullable
    private final NBTTagCompound tag;

    public ArgumentTileLocation(IBlockData state, Set<IBlockState<?>> properties, @Nullable NBTTagCompound data) {
        this.state = state;
        this.properties = properties;
        this.tag = data;
    }

    public IBlockData getState() {
        return this.state;
    }

    public Set<IBlockState<?>> getDefinedProperties() {
        return this.properties;
    }

    @Override
    public boolean test(ShapeDetectorBlock blockInWorld) {
        IBlockData blockState = blockInWorld.getState();
        if (!blockState.is(this.state.getBlock())) {
            return false;
        } else {
            for(IBlockState<?> property : this.properties) {
                if (blockState.get(property) != this.state.get(property)) {
                    return false;
                }
            }

            if (this.tag == null) {
                return true;
            } else {
                TileEntity blockEntity = blockInWorld.getEntity();
                return blockEntity != null && GameProfileSerializer.compareNbt(this.tag, blockEntity.saveWithFullMetadata(), true);
            }
        }
    }

    public boolean test(WorldServer world, BlockPosition pos) {
        return this.test(new ShapeDetectorBlock(world, pos, false));
    }

    public boolean place(WorldServer world, BlockPosition pos, int flags) {
        IBlockData blockState = Block.updateFromNeighbourShapes(this.state, world, pos);
        if (blockState.isAir()) {
            blockState = this.state;
        }

        if (!world.setTypeAndData(pos, blockState, flags)) {
            return false;
        } else {
            if (this.tag != null) {
                TileEntity blockEntity = world.getTileEntity(pos);
                if (blockEntity != null) {
                    blockEntity.load(this.tag);
                }
            }

            return true;
        }
    }
}
