package net.minecraft.world.level.block.entity;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public abstract class TileEntityLootable extends TileEntityContainer {
    public static final String LOOT_TABLE_TAG = "LootTable";
    public static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
    @Nullable
    public MinecraftKey lootTable;
    public long lootTableSeed;

    protected TileEntityLootable(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        super(type, pos, state);
    }

    public static void setLootTable(IBlockAccess world, Random random, BlockPosition pos, MinecraftKey id) {
        TileEntity blockEntity = world.getTileEntity(pos);
        if (blockEntity instanceof TileEntityLootable) {
            ((TileEntityLootable)blockEntity).setLootTable(id, random.nextLong());
        }

    }

    protected boolean tryLoadLootTable(NBTTagCompound nbt) {
        if (nbt.hasKeyOfType("LootTable", 8)) {
            this.lootTable = new MinecraftKey(nbt.getString("LootTable"));
            this.lootTableSeed = nbt.getLong("LootTableSeed");
            return true;
        } else {
            return false;
        }
    }

    protected boolean trySaveLootTable(NBTTagCompound nbt) {
        if (this.lootTable == null) {
            return false;
        } else {
            nbt.setString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.setLong("LootTableSeed", this.lootTableSeed);
            }

            return true;
        }
    }

    public void unpackLootTable(@Nullable EntityHuman player) {
        if (this.lootTable != null && this.level.getMinecraftServer() != null) {
            LootTable lootTable = this.level.getMinecraftServer().getLootTableRegistry().getLootTable(this.lootTable);
            if (player instanceof EntityPlayer) {
                CriterionTriggers.GENERATE_LOOT.trigger((EntityPlayer)player, this.lootTable);
            }

            this.lootTable = null;
            LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.level)).set(LootContextParameters.ORIGIN, Vec3D.atCenterOf(this.worldPosition)).withOptionalRandomSeed(this.lootTableSeed);
            if (player != null) {
                builder.withLuck(player.getLuck()).set(LootContextParameters.THIS_ENTITY, player);
            }

            lootTable.fillInventory(this, builder.build(LootContextParameterSets.CHEST));
        }

    }

    public void setLootTable(MinecraftKey id, long seed) {
        this.lootTable = id;
        this.lootTableSeed = seed;
    }

    @Override
    public boolean isEmpty() {
        this.unpackLootTable((EntityHuman)null);
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        this.unpackLootTable((EntityHuman)null);
        return this.getItems().get(slot);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        this.unpackLootTable((EntityHuman)null);
        ItemStack itemStack = ContainerUtil.removeItem(this.getItems(), slot, amount);
        if (!itemStack.isEmpty()) {
            this.update();
        }

        return itemStack;
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        this.unpackLootTable((EntityHuman)null);
        return ContainerUtil.takeItem(this.getItems(), slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((EntityHuman)null);
        this.getItems().set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.update();
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        if (this.level.getTileEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public void clear() {
        this.getItems().clear();
    }

    protected abstract NonNullList<ItemStack> getItems();

    protected abstract void setItems(NonNullList<ItemStack> list);

    @Override
    public boolean canOpen(EntityHuman player) {
        return super.canOpen(player) && (this.lootTable == null || !player.isSpectator());
    }

    @Nullable
    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        if (this.canOpen(player)) {
            this.unpackLootTable(inv.player);
            return this.createContainer(syncId, inv);
        } else {
            return null;
        }
    }
}
