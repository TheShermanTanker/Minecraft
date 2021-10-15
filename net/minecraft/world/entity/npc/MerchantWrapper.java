package net.minecraft.world.entity.npc;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.IMerchant;
import net.minecraft.world.item.trading.MerchantRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.World;

public class MerchantWrapper implements IMerchant {
    private final EntityHuman source;
    private MerchantRecipeList offers = new MerchantRecipeList();
    private int xp;

    public MerchantWrapper(EntityHuman player) {
        this.source = player;
    }

    @Override
    public EntityHuman getTrader() {
        return this.source;
    }

    @Override
    public void setTradingPlayer(@Nullable EntityHuman customer) {
    }

    @Override
    public MerchantRecipeList getOffers() {
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantRecipeList offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantRecipe offer) {
        offer.increaseUses();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public World getWorld() {
        return this.source.level;
    }

    @Override
    public int getExperience() {
        return this.xp;
    }

    @Override
    public void setForcedExperience(int experience) {
        this.xp = experience;
    }

    @Override
    public boolean isRegularVillager() {
        return true;
    }

    @Override
    public SoundEffect getTradeSound() {
        return SoundEffects.VILLAGER_YES;
    }
}
