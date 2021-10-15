package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;

@Immutable
public class ChestLock {
    public static final ChestLock NO_LOCK = new ChestLock("");
    public static final String TAG_LOCK = "Lock";
    public final String key;

    public ChestLock(String key) {
        this.key = key;
    }

    public boolean unlocksWith(ItemStack stack) {
        return this.key.isEmpty() || !stack.isEmpty() && stack.hasName() && this.key.equals(stack.getName().getString());
    }

    public void addToTag(NBTTagCompound nbt) {
        if (!this.key.isEmpty()) {
            nbt.setString("Lock", this.key);
        }

    }

    public static ChestLock fromTag(NBTTagCompound nbt) {
        return nbt.hasKeyOfType("Lock", 8) ? new ChestLock(nbt.getString("Lock")) : NO_LOCK;
    }
}
