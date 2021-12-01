package net.minecraft.server.dedicated;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.WorldNBTStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DedicatedPlayerList extends PlayerList {
    private static final Logger LOGGER = LogManager.getLogger();

    public DedicatedPlayerList(DedicatedServer server, IRegistryCustom.Dimension tracker, WorldNBTStorage saveHandler) {
        super(server, tracker, saveHandler, server.getDedicatedServerProperties().maxPlayers);
        DedicatedServerProperties dedicatedServerProperties = server.getDedicatedServerProperties();
        this.setViewDistance(dedicatedServerProperties.viewDistance);
        this.setSimulationDistance(dedicatedServerProperties.simulationDistance);
        super.setHasWhitelist(dedicatedServerProperties.whiteList.get());
        this.loadUserBanList();
        this.saveUserBanList();
        this.loadIpBanList();
        this.saveIpBanList();
        this.loadOps();
        this.loadWhiteList();
        this.saveOps();
        if (!this.getWhitelist().getFile().exists()) {
            this.saveWhiteList();
        }

    }

    @Override
    public void setHasWhitelist(boolean whitelistEnabled) {
        super.setHasWhitelist(whitelistEnabled);
        this.getServer().setHasWhitelist(whitelistEnabled);
    }

    @Override
    public void addOp(GameProfile profile) {
        super.addOp(profile);
        this.saveOps();
    }

    @Override
    public void removeOp(GameProfile profile) {
        super.removeOp(profile);
        this.saveOps();
    }

    @Override
    public void reloadWhitelist() {
        this.loadWhiteList();
    }

    private void saveIpBanList() {
        try {
            this.getIPBans().save();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save ip banlist: ", (Throwable)var2);
        }

    }

    private void saveUserBanList() {
        try {
            this.getProfileBans().save();
        } catch (IOException var2) {
            LOGGER.warn("Failed to save user banlist: ", (Throwable)var2);
        }

    }

    private void loadIpBanList() {
        try {
            this.getIPBans().load();
        } catch (IOException var2) {
            LOGGER.warn("Failed to load ip banlist: ", (Throwable)var2);
        }

    }

    private void loadUserBanList() {
        try {
            this.getProfileBans().load();
        } catch (IOException var2) {
            LOGGER.warn("Failed to load user banlist: ", (Throwable)var2);
        }

    }

    private void loadOps() {
        try {
            this.getOPs().load();
        } catch (Exception var2) {
            LOGGER.warn("Failed to load operators list: ", (Throwable)var2);
        }

    }

    private void saveOps() {
        try {
            this.getOPs().save();
        } catch (Exception var2) {
            LOGGER.warn("Failed to save operators list: ", (Throwable)var2);
        }

    }

    private void loadWhiteList() {
        try {
            this.getWhitelist().load();
        } catch (Exception var2) {
            LOGGER.warn("Failed to load white-list: ", (Throwable)var2);
        }

    }

    private void saveWhiteList() {
        try {
            this.getWhitelist().save();
        } catch (Exception var2) {
            LOGGER.warn("Failed to save white-list: ", (Throwable)var2);
        }

    }

    @Override
    public boolean isWhitelisted(GameProfile profile) {
        return !this.getHasWhitelist() || this.isOp(profile) || this.getWhitelist().isWhitelisted(profile);
    }

    @Override
    public DedicatedServer getServer() {
        return (DedicatedServer)super.getServer();
    }

    @Override
    public boolean canBypassPlayerLimit(GameProfile profile) {
        return this.getOPs().canBypassPlayerLimit(profile);
    }
}
