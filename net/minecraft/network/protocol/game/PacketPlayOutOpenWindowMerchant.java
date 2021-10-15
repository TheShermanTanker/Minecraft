package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.trading.MerchantRecipeList;

public class PacketPlayOutOpenWindowMerchant implements Packet<PacketListenerPlayOut> {
    private final int containerId;
    private final MerchantRecipeList offers;
    private final int villagerLevel;
    private final int villagerXp;
    private final boolean showProgress;
    private final boolean canRestock;

    public PacketPlayOutOpenWindowMerchant(int syncId, MerchantRecipeList recipes, int levelProgress, int experience, boolean leveled, boolean refreshable) {
        this.containerId = syncId;
        this.offers = recipes;
        this.villagerLevel = levelProgress;
        this.villagerXp = experience;
        this.showProgress = leveled;
        this.canRestock = refreshable;
    }

    public PacketPlayOutOpenWindowMerchant(PacketDataSerializer buf) {
        this.containerId = buf.readVarInt();
        this.offers = MerchantRecipeList.createFromStream(buf);
        this.villagerLevel = buf.readVarInt();
        this.villagerXp = buf.readVarInt();
        this.showProgress = buf.readBoolean();
        this.canRestock = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.containerId);
        this.offers.writeToStream(buf);
        buf.writeVarInt(this.villagerLevel);
        buf.writeVarInt(this.villagerXp);
        buf.writeBoolean(this.showProgress);
        buf.writeBoolean(this.canRestock);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleMerchantOffers(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public MerchantRecipeList getOffers() {
        return this.offers;
    }

    public int getVillagerLevel() {
        return this.villagerLevel;
    }

    public int getVillagerXp() {
        return this.villagerXp;
    }

    public boolean showProgress() {
        return this.showProgress;
    }

    public boolean canRestock() {
        return this.canRestock;
    }
}
