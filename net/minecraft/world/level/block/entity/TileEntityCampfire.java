package net.minecraft.world.level.block.entity;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeCampfire;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityCampfire extends TileEntity implements Clearable {
    private static final int BURN_COOL_SPEED = 2;
    private static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    public final int[] cookingProgress = new int[4];
    public final int[] cookingTime = new int[4];

    public TileEntityCampfire(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.CAMPFIRE, pos, state);
    }

    public static void cookTick(World world, BlockPosition pos, IBlockData state, TileEntityCampfire campfire) {
        boolean bl = false;

        for(int i = 0; i < campfire.items.size(); ++i) {
            ItemStack itemStack = campfire.items.get(i);
            if (!itemStack.isEmpty()) {
                bl = true;
                int var10002 = campfire.cookingProgress[i]++;
                if (campfire.cookingProgress[i] >= campfire.cookingTime[i]) {
                    IInventory container = new InventorySubcontainer(itemStack);
                    ItemStack itemStack2 = world.getCraftingManager().craft(Recipes.CAMPFIRE_COOKING, container, world).map((campfireCookingRecipe) -> {
                        return campfireCookingRecipe.assemble(container);
                    }).orElse(itemStack);
                    InventoryUtils.dropItem(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack2);
                    campfire.items.set(i, ItemStack.EMPTY);
                    world.notify(pos, state, state, 3);
                }
            }
        }

        if (bl) {
            setChanged(world, pos, state);
        }

    }

    public static void cooldownTick(World world, BlockPosition pos, IBlockData state, TileEntityCampfire campfire) {
        boolean bl = false;

        for(int i = 0; i < campfire.items.size(); ++i) {
            if (campfire.cookingProgress[i] > 0) {
                bl = true;
                campfire.cookingProgress[i] = MathHelper.clamp(campfire.cookingProgress[i] - 2, 0, campfire.cookingTime[i]);
            }
        }

        if (bl) {
            setChanged(world, pos, state);
        }

    }

    public static void particleTick(World world, BlockPosition pos, IBlockData state, TileEntityCampfire campfire) {
        Random random = world.random;
        if (random.nextFloat() < 0.11F) {
            for(int i = 0; i < random.nextInt(2) + 2; ++i) {
                BlockCampfire.makeParticles(world, pos, state.get(BlockCampfire.SIGNAL_FIRE), false);
            }
        }

        int j = state.get(BlockCampfire.FACING).get2DRotationValue();

        for(int k = 0; k < campfire.items.size(); ++k) {
            if (!campfire.items.get(k).isEmpty() && random.nextFloat() < 0.2F) {
                EnumDirection direction = EnumDirection.fromType2(Math.floorMod(k + j, 4));
                float f = 0.3125F;
                double d = (double)pos.getX() + 0.5D - (double)((float)direction.getAdjacentX() * 0.3125F) + (double)((float)direction.getClockWise().getAdjacentX() * 0.3125F);
                double e = (double)pos.getY() + 0.5D;
                double g = (double)pos.getZ() + 0.5D - (double)((float)direction.getAdjacentZ() * 0.3125F) + (double)((float)direction.getClockWise().getAdjacentZ() * 0.3125F);

                for(int l = 0; l < 4; ++l) {
                    world.addParticle(Particles.SMOKE, d, e, g, 0.0D, 5.0E-4D, 0.0D);
                }
            }
        }

    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.items.clear();
        ContainerUtil.loadAllItems(nbt, this.items);
        if (nbt.hasKeyOfType("CookingTimes", 11)) {
            int[] is = nbt.getIntArray("CookingTimes");
            System.arraycopy(is, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, is.length));
        }

        if (nbt.hasKeyOfType("CookingTotalTimes", 11)) {
            int[] js = nbt.getIntArray("CookingTotalTimes");
            System.arraycopy(js, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, js.length));
        }

    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        ContainerUtil.saveAllItems(nbt, this.items, true);
        nbt.setIntArray("CookingTimes", this.cookingProgress);
        nbt.setIntArray("CookingTotalTimes", this.cookingTime);
    }

    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return PacketPlayOutTileEntityData.create(this);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        ContainerUtil.saveAllItems(compoundTag, this.items, true);
        return compoundTag;
    }

    public Optional<RecipeCampfire> getCookableRecipe(ItemStack item) {
        return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.level.getCraftingManager().craft(Recipes.CAMPFIRE_COOKING, new InventorySubcontainer(item), this.level);
    }

    public boolean placeFood(ItemStack item, int integer) {
        for(int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.items.get(i);
            if (itemStack.isEmpty()) {
                this.cookingTime[i] = integer;
                this.cookingProgress[i] = 0;
                this.items.set(i, item.cloneAndSubtract(1));
                this.markUpdated();
                return true;
            }
        }

        return false;
    }

    private void markUpdated() {
        this.update();
        this.getWorld().notify(this.getPosition(), this.getBlock(), this.getBlock(), 3);
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    public void dowse() {
        if (this.level != null) {
            this.markUpdated();
        }

    }
}
