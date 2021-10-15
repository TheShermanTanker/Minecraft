package net.minecraft.world.entity.vehicle;

import net.minecraft.core.EnumDirection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityMinecartChest extends EntityMinecartContainer {
    public EntityMinecartChest(EntityTypes<? extends EntityMinecartChest> type, World world) {
        super(type, world);
    }

    public EntityMinecartChest(World world, double x, double y, double z) {
        super(EntityTypes.CHEST_MINECART, x, y, z, world);
    }

    @Override
    public void destroy(DamageSource damageSource) {
        super.destroy(damageSource);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(Blocks.CHEST);
        }

    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.CHEST;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.CHEST.getBlockData().set(BlockChest.FACING, EnumDirection.NORTH);
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    @Override
    public Container createMenu(int syncId, PlayerInventory playerInventory) {
        return ContainerChest.threeRows(syncId, playerInventory, this);
    }
}
