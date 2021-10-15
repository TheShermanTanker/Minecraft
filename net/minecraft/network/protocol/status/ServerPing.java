package net.minecraft.network.protocol.status;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Type;
import java.util.UUID;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.ChatDeserializer;

public class ServerPing {
    public static final int FAVICON_WIDTH = 64;
    public static final int FAVICON_HEIGHT = 64;
    private IChatBaseComponent description;
    private ServerPing.ServerPingPlayerSample players;
    private ServerPing.ServerData version;
    private String favicon;

    public IChatBaseComponent getDescription() {
        return this.description;
    }

    public void setMOTD(IChatBaseComponent description) {
        this.description = description;
    }

    public ServerPing.ServerPingPlayerSample getPlayers() {
        return this.players;
    }

    public void setPlayerSample(ServerPing.ServerPingPlayerSample players) {
        this.players = players;
    }

    public ServerPing.ServerData getServerData() {
        return this.version;
    }

    public void setServerInfo(ServerPing.ServerData version) {
        this.version = version;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getFavicon() {
        return this.favicon;
    }

    public static class Serializer implements JsonDeserializer<ServerPing>, JsonSerializer<ServerPing> {
        @Override
        public ServerPing deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(jsonElement, "status");
            ServerPing serverStatus = new ServerPing();
            if (jsonObject.has("description")) {
                serverStatus.setMOTD(jsonDeserializationContext.deserialize(jsonObject.get("description"), IChatBaseComponent.class));
            }

            if (jsonObject.has("players")) {
                serverStatus.setPlayerSample(jsonDeserializationContext.deserialize(jsonObject.get("players"), ServerPing.ServerPingPlayerSample.class));
            }

            if (jsonObject.has("version")) {
                serverStatus.setServerInfo(jsonDeserializationContext.deserialize(jsonObject.get("version"), ServerPing.ServerData.class));
            }

            if (jsonObject.has("favicon")) {
                serverStatus.setFavicon(ChatDeserializer.getAsString(jsonObject, "favicon"));
            }

            return serverStatus;
        }

        @Override
        public JsonElement serialize(ServerPing serverStatus, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            if (serverStatus.getDescription() != null) {
                jsonObject.add("description", jsonSerializationContext.serialize(serverStatus.getDescription()));
            }

            if (serverStatus.getPlayers() != null) {
                jsonObject.add("players", jsonSerializationContext.serialize(serverStatus.getPlayers()));
            }

            if (serverStatus.getServerData() != null) {
                jsonObject.add("version", jsonSerializationContext.serialize(serverStatus.getServerData()));
            }

            if (serverStatus.getFavicon() != null) {
                jsonObject.addProperty("favicon", serverStatus.getFavicon());
            }

            return jsonObject;
        }
    }

    public static class ServerData {
        private final String name;
        private final int protocol;

        public ServerData(String gameVersion, int protocolVersion) {
            this.name = gameVersion;
            this.protocol = protocolVersion;
        }

        public String getName() {
            return this.name;
        }

        public int getProtocolVersion() {
            return this.protocol;
        }

        public static class Serializer implements JsonDeserializer<ServerPing.ServerData>, JsonSerializer<ServerPing.ServerData> {
            @Override
            public ServerPing.ServerData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(jsonElement, "version");
                return new ServerPing.ServerData(ChatDeserializer.getAsString(jsonObject, "name"), ChatDeserializer.getAsInt(jsonObject, "protocol"));
            }

            @Override
            public JsonElement serialize(ServerPing.ServerData version, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", version.getName());
                jsonObject.addProperty("protocol", version.getProtocolVersion());
                return jsonObject;
            }
        }
    }

    public static class ServerPingPlayerSample {
        private final int maxPlayers;
        private final int numPlayers;
        private GameProfile[] sample;

        public ServerPingPlayerSample(int max, int online) {
            this.maxPlayers = max;
            this.numPlayers = online;
        }

        public int getMaxPlayers() {
            return this.maxPlayers;
        }

        public int getNumPlayers() {
            return this.numPlayers;
        }

        public GameProfile[] getSample() {
            return this.sample;
        }

        public void setSample(GameProfile[] sample) {
            this.sample = sample;
        }

        public static class Serializer implements JsonDeserializer<ServerPing.ServerPingPlayerSample>, JsonSerializer<ServerPing.ServerPingPlayerSample> {
            @Override
            public ServerPing.ServerPingPlayerSample deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(jsonElement, "players");
                ServerPing.ServerPingPlayerSample players = new ServerPing.ServerPingPlayerSample(ChatDeserializer.getAsInt(jsonObject, "max"), ChatDeserializer.getAsInt(jsonObject, "online"));
                if (ChatDeserializer.isArrayNode(jsonObject, "sample")) {
                    JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "sample");
                    if (jsonArray.size() > 0) {
                        GameProfile[] gameProfiles = new GameProfile[jsonArray.size()];

                        for(int i = 0; i < gameProfiles.length; ++i) {
                            JsonObject jsonObject2 = ChatDeserializer.convertToJsonObject(jsonArray.get(i), "player[" + i + "]");
                            String string = ChatDeserializer.getAsString(jsonObject2, "id");
                            gameProfiles[i] = new GameProfile(UUID.fromString(string), ChatDeserializer.getAsString(jsonObject2, "name"));
                        }

                        players.setSample(gameProfiles);
                    }
                }

                return players;
            }

            @Override
            public JsonElement serialize(ServerPing.ServerPingPlayerSample players, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("max", players.getMaxPlayers());
                jsonObject.addProperty("online", players.getNumPlayers());
                if (players.getSample() != null && players.getSample().length > 0) {
                    JsonArray jsonArray = new JsonArray();

                    for(int i = 0; i < players.getSample().length; ++i) {
                        JsonObject jsonObject2 = new JsonObject();
                        UUID uUID = players.getSample()[i].getId();
                        jsonObject2.addProperty("id", uUID == null ? "" : uUID.toString());
                        jsonObject2.addProperty("name", players.getSample()[i].getName());
                        jsonArray.add(jsonObject2);
                    }

                    jsonObject.add("sample", jsonArray);
                }

                return jsonObject;
            }
        }
    }
}
