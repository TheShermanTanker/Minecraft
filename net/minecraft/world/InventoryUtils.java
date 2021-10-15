package net.minecraft.world;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public class InventoryUtils {
    private static final Random RANDOM = new Random();

    public static void dropInventory(World world, BlockPosition pos, IInventory inventory) {
        dropInventory(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), inventory);
    }

    public static void dropEntity(World world, Entity entity, IInventory inventory) {
        dropInventory(world, entity.locX(), entity.locY(), entity.locZ(), inventory);
    }

    private static void dropInventory(World world, double x, double y, double z, IInventory inventory) {
        for(int i = 0; i < inventory.getSize(); ++i) {
            dropItem(world, x, y, z, inventory.getItem(i));
        }

    }

    public static void dropContents(World world, BlockPosition pos, NonNullList<ItemStack> stacks) {
        stacks.forEach((stack) -> {
            dropItem(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), stack);
        });
    }

    public static void dropItem(World world, double x, double y, double z, ItemStack stack) {
        double d = (double)EntityTypes.ITEM.getWidth();
        double e = 1.0D - d;
        double f = d / 2.0D;
        double g = Math.floor(x) + RANDOM.nextDouble() * e + f;
        double h = Math.floor(y) + RANDOM.nextDouble() * e;
        double i = Math.floor(z) + RANDOM.nextDouble() * e + f;

        while(!stack.isEmpty()) {
            EntityItem itemEntity = new EntityItem(world, g, h, i, stack.cloneAndSubtract(RANDOM.nextInt(21) + 10));
            float j = 0.05F;
            itemEntity.setMot(RANDOM.nextGaussian() * (double)0.05F, RANDOM.nextGaussian() * (double)0.05F + (double)0.2F, RANDOM.nextGaussian() * (double)0.05F);
            world.addEntity(itemEntity);
        }

    }
}
