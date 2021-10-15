package net.minecraft.world.item.trading;

import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerMerchant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public interface IMerchant {
    void setTradingPlayer(@Nullable EntityHuman customer);

    @Nullable
    EntityHuman getTrader();

    MerchantRecipeList getOffers();

    void overrideOffers(MerchantRecipeList offers);

    void notifyTrade(MerchantRecipe offer);

    void notifyTradeUpdated(ItemStack stack);

    World getWorld();

    int getExperience();

    void setForcedExperience(int experience);

    boolean isRegularVillager();

    SoundEffect getTradeSound();

    default boolean canRestock() {
        return false;
    }

    default void openTrade(EntityHuman player, IChatBaseComponent test, int levelProgress) {
        OptionalInt optionalInt = player.openContainer(new TileInventory((syncId, playerInventory, playerx) -> {
            return new ContainerMerchant(syncId, playerInventory, this);
        }, test));
        if (optionalInt.isPresent()) {
            MerchantRecipeList merchantOffers = this.getOffers();
            if (!merchantOffers.isEmpty()) {
                player.openTrade(optionalInt.getAsInt(), merchantOffers, levelProgress, this.getExperience(), this.isRegularVillager(), this.canRestock());
            }
        }

    }
}
