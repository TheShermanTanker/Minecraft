package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;

public class IpBanEntry extends ExpirableListEntry<String> {
    public IpBanEntry(String ip) {
        this(ip, (Date)null, (String)null, (Date)null, (String)null);
    }

    public IpBanEntry(String ip, @Nullable Date created, @Nullable String source, @Nullable Date expiry, @Nullable String reason) {
        super(ip, created, source, expiry, reason);
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return new ChatComponentText(String.valueOf(this.getKey()));
    }

    public IpBanEntry(JsonObject json) {
        super(createIpInfo(json), json);
    }

    private static String createIpInfo(JsonObject json) {
        return json.has("ip") ? json.get("ip").getAsString() : null;
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getKey() != null) {
            json.addProperty("ip", this.getKey());
            super.serialize(json);
        }
    }
}
