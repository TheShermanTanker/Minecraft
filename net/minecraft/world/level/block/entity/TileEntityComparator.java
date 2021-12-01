package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityComparator extends TileEntity {
    private int output;

    public TileEntityComparator(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.COMPARATOR, pos, state);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        nbt.setInt("OutputSignal", this.output);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.output = nbt.getInt("OutputSignal");
    }

    public int getOutputSignal() {
        return this.output;
    }

    public void setOutputSignal(int outputSignal) {
        this.output = outputSignal;
    }
}
