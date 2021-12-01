package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityJukeBox extends TileEntity implements Clearable {
    private ItemStack record = ItemStack.EMPTY;

    public TileEntityJukeBox(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.JUKEBOX, pos, state);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("RecordItem", 10)) {
            this.setRecord(ItemStack.of(nbt.getCompound("RecordItem")));
        }

    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        if (!this.getRecord().isEmpty()) {
            nbt.set("RecordItem", this.getRecord().save(new NBTTagCompound()));
        }

    }

    public ItemStack getRecord() {
        return this.record;
    }

    public void setRecord(ItemStack stack) {
        this.record = stack;
        this.update();
    }

    @Override
    public void clear() {
        this.setRecord(ItemStack.EMPTY);
    }
}
