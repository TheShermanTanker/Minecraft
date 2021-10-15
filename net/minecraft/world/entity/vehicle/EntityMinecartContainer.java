package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAI;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public abstract class EntityMinecartContainer extends EntityMinecartAbstract implements IInventory, ITileInventory {
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
    @Nullable
    public MinecraftKey lootTable;
    public long lootTableSeed;

    protected EntityMinecartContainer(EntityTypes<?> type, World world) {
        super(type, world);
    }

    protected EntityMinecartContainer(EntityTypes<?> type, double x, double y, double z, World world) {
        super(type, world, x, y, z);
    }

    @Override
    public void destroy(DamageSource damageSource) {
        super.destroy(damageSource);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            InventoryUtils.dropEntity(this.level, this, this);
            if (!this.level.isClientSide) {
                Entity entity = damageSource.getDirectEntity();
                if (entity != null && entity.getEntityType() == EntityTypes.PLAYER) {
                    PiglinAI.angerNearbyPiglins((EntityHuman)entity, true);
                }
            }
        }

    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.itemStacks) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        this.unpackLootTable((EntityHuman)null);
        return this.itemStacks.get(slot);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        this.unpackLootTable((EntityHuman)null);
        return ContainerUtil.removeItem(this.itemStacks, slot, amount);
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        this.unpackLootTable((EntityHuman)null);
        ItemStack itemStack = this.itemStacks.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemStacks.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((EntityHuman)null);
        this.itemStacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        return mappedIndex >= 0 && mappedIndex < this.getSize() ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return EntityMinecartContainer.this.getItem(mappedIndex);
            }

            @Override
            public boolean set(ItemStack stack) {
                EntityMinecartContainer.this.setItem(mappedIndex, stack);
                return true;
            }
        } : super.getSlot(mappedIndex);
    }

    @Override
    public void update() {
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(player.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level.isClientSide && reason.shouldDestroy()) {
            InventoryUtils.dropEntity(this.level, this, this);
        }

        super.remove(reason);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.lootTable != null) {
            nbt.setString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.setLong("LootTableSeed", this.lootTableSeed);
            }
        } else {
            ContainerUtil.saveAllItems(nbt, this.itemStacks);
        }

    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.itemStacks = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        if (nbt.hasKeyOfType("LootTable", 8)) {
            this.lootTable = new MinecraftKey(nbt.getString("LootTable"));
            this.lootTableSeed = nbt.getLong("LootTableSeed");
        } else {
            ContainerUtil.loadAllItems(nbt, this.itemStacks);
        }

    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        player.openContainer(this);
        if (!player.level.isClientSide) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAI.angerNearbyPiglins(player, true);
            return EnumInteractionResult.CONSUME;
        } else {
            return EnumInteractionResult.SUCCESS;
        }
    }

    @Override
    protected void decelerate() {
        float f = 0.98F;
        if (this.lootTable == null) {
            int i = 15 - Container.getRedstoneSignalFromContainer(this);
            f += (float)i * 0.001F;
        }

        if (this.isInWater()) {
            f *= 0.95F;
        }

        this.setMot(this.getMot().multiply((double)f, 0.0D, (double)f));
    }

    public void unpackLootTable(@Nullable EntityHuman player) {
        if (this.lootTable != null && this.level.getMinecraftServer() != null) {
            LootTable lootTable = this.level.getMinecraftServer().getLootTableRegistry().getLootTable(this.lootTable);
            if (player instanceof EntityPlayer) {
                CriterionTriggers.GENERATE_LOOT.trigger((EntityPlayer)player, this.lootTable);
            }

            this.lootTable = null;
            LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.level)).set(LootContextParameters.ORIGIN, this.getPositionVector()).withOptionalRandomSeed(this.lootTableSeed);
            if (player != null) {
                builder.withLuck(player.getLuck()).set(LootContextParameters.THIS_ENTITY, player);
            }

            lootTable.fillInventory(this, builder.build(LootContextParameterSets.CHEST));
        }

    }

    @Override
    public void clear() {
        this.unpackLootTable((EntityHuman)null);
        this.itemStacks.clear();
    }

    public void setLootTable(MinecraftKey id, long lootSeed) {
        this.lootTable = id;
        this.lootTableSeed = lootSeed;
    }

    @Nullable
    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        if (this.lootTable != null && player.isSpectator()) {
            return null;
        } else {
            this.unpackLootTable(inv.player);
            return this.createMenu(syncId, inv);
        }
    }

    protected abstract Container createMenu(int syncId, PlayerInventory playerInventory);
}
