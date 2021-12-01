package net.minecraft.server.network;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.thread.ThreadedMailbox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextFilter implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ThreadFactory THREAD_FACTORY = (runnable) -> {
        Thread thread = new Thread(runnable);
        thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
        return thread;
    };
    private final URL chatEndpoint;
    final URL joinEndpoint;
    final URL leaveEndpoint;
    private final String authKey;
    private final int ruleId;
    private final String serverId;
    final TextFilter.IgnoreStrategy chatIgnoreStrategy;
    final ExecutorService workerPool;

    private TextFilter(URI apiUrl, String apiKey, int ruleId, String serverId, TextFilter.IgnoreStrategy ignorer, int threadsNumber) throws MalformedURLException {
        this.authKey = apiKey;
        this.ruleId = ruleId;
        this.serverId = serverId;
        this.chatIgnoreStrategy = ignorer;
        this.chatEndpoint = apiUrl.resolve("/v1/chat").toURL();
        this.joinEndpoint = apiUrl.resolve("/v1/join").toURL();
        this.leaveEndpoint = apiUrl.resolve("/v1/leave").toURL();
        this.workerPool = Executors.newFixedThreadPool(threadsNumber, THREAD_FACTORY);
    }

    @Nullable
    public static TextFilter createFromConfig(String config) {
        if (Strings.isNullOrEmpty(config)) {
            return null;
        } else {
            try {
                JsonObject jsonObject = ChatDeserializer.parse(config);
                URI uRI = new URI(ChatDeserializer.getAsString(jsonObject, "apiServer"));
                String string = ChatDeserializer.getAsString(jsonObject, "apiKey");
                if (string.isEmpty()) {
                    throw new IllegalArgumentException("Missing API key");
                } else {
                    int i = ChatDeserializer.getAsInt(jsonObject, "ruleId", 1);
                    String string2 = ChatDeserializer.getAsString(jsonObject, "serverId", "");
                    int j = ChatDeserializer.getAsInt(jsonObject, "hashesToDrop", -1);
                    int k = ChatDeserializer.getAsInt(jsonObject, "maxConcurrentRequests", 7);
                    TextFilter.IgnoreStrategy ignoreStrategy = TextFilter.IgnoreStrategy.select(j);
                    return new TextFilter(uRI, Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.US_ASCII)), i, string2, ignoreStrategy, k);
                }
            } catch (Exception var9) {
                LOGGER.warn("Failed to parse chat filter config {}", config, var9);
                return null;
            }
        }
    }

    void processJoinOrLeave(GameProfile gameProfile, URL endpoint, Executor executor) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("server", this.serverId);
        jsonObject.addProperty("room", "Chat");
        jsonObject.addProperty("user_id", gameProfile.getId().toString());
        jsonObject.addProperty("user_display_name", gameProfile.getName());
        executor.execute(() -> {
            try {
                this.processRequest(jsonObject, endpoint);
            } catch (Exception var5) {
                LOGGER.warn("Failed to send join/leave packet to {} for player {}", endpoint, gameProfile, var5);
            }

        });
    }

    CompletableFuture<ITextFilter.FilteredText> requestMessageProcessing(GameProfile gameProfile, String message, TextFilter.IgnoreStrategy ignorer, Executor executor) {
        if (message.isEmpty()) {
            return CompletableFuture.completedFuture(ITextFilter.FilteredText.EMPTY);
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("rule", this.ruleId);
            jsonObject.addProperty("server", this.serverId);
            jsonObject.addProperty("room", "Chat");
            jsonObject.addProperty("player", gameProfile.getId().toString());
            jsonObject.addProperty("player_display_name", gameProfile.getName());
            jsonObject.addProperty("text", message);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject jsonObject2 = this.processRequestResponse(jsonObject, this.chatEndpoint);
                    boolean bl = ChatDeserializer.getAsBoolean(jsonObject2, "response", false);
                    if (bl) {
                        return ITextFilter.FilteredText.passThrough(message);
                    } else {
                        String string2 = ChatDeserializer.getAsString(jsonObject2, "hashed", (String)null);
                        if (string2 == null) {
                            return ITextFilter.FilteredText.fullyFiltered(message);
                        } else {
                            int i = ChatDeserializer.getAsJsonArray(jsonObject2, "hashes").size();
                            return ignorer.shouldIgnore(string2, i) ? ITextFilter.FilteredText.fullyFiltered(message) : new ITextFilter.FilteredText(message, string2);
                        }
                    }
                } catch (Exception var8) {
                    LOGGER.warn("Failed to validate message '{}'", message, var8);
                    return ITextFilter.FilteredText.fullyFiltered(message);
                }
            }, executor);
        }
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }

    private void drainStream(InputStream inputStream) throws IOException {
        byte[] bs = new byte[1024];

        while(inputStream.read(bs) != -1) {
        }

    }

    private JsonObject processRequestResponse(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = this.makeRequest(payload, endpoint);
        InputStream inputStream = httpURLConnection.getInputStream();

        JsonObject var13;
        label89: {
            try {
                if (httpURLConnection.getResponseCode() == 204) {
                    var13 = new JsonObject();
                    break label89;
                }

                try {
                    var13 = Streams.parse(new JsonReader(new InputStreamReader(inputStream))).getAsJsonObject();
                } finally {
                    this.drainStream(inputStream);
                }
            } catch (Throwable var12) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var10) {
                        var12.addSuppressed(var10);
                    }
                }

                throw var12;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var13;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return var13;
    }

    private void processRequest(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = this.makeRequest(payload, endpoint);
        InputStream inputStream = httpURLConnection.getInputStream();

        try {
            this.drainStream(inputStream);
        } catch (Throwable var8) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (inputStream != null) {
            inputStream.close();
        }

    }

    private HttpURLConnection makeRequest(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection)endpoint.openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(2000);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("Authorization", "Basic " + this.authKey);
        httpURLConnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().getName());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8);

        try {
            JsonWriter jsonWriter = new JsonWriter(outputStreamWriter);

            try {
                Streams.write(payload, jsonWriter);
            } catch (Throwable var10) {
                try {
                    jsonWriter.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }

                throw var10;
            }

            jsonWriter.close();
        } catch (Throwable var11) {
            try {
                outputStreamWriter.close();
            } catch (Throwable var8) {
                var11.addSuppressed(var8);
            }

            throw var11;
        }

        outputStreamWriter.close();
        int i = httpURLConnection.getResponseCode();
        if (i >= 200 && i < 300) {
            return httpURLConnection;
        } else {
            throw new TextFilter.RequestFailedException(i + " " + httpURLConnection.getResponseMessage());
        }
    }

    public ITextFilter createContext(GameProfile gameProfile) {
        return new TextFilter.PlayerContext(gameProfile);
    }

    @FunctionalInterface
    public interface IgnoreStrategy {
        TextFilter.IgnoreStrategy NEVER_IGNORE = (hashes, hashesSize) -> {
            return false;
        };
        TextFilter.IgnoreStrategy IGNORE_FULLY_FILTERED = (hashes, hashesSize) -> {
            return hashes.length() == hashesSize;
        };

        static TextFilter.IgnoreStrategy ignoreOverThreshold(int hashesToDrop) {
            return (hashes, hashesSize) -> {
                return hashesSize >= hashesToDrop;
            };
        }

        static TextFilter.IgnoreStrategy select(int hashesToDrop) {
            switch(hashesToDrop) {
            case -1:
                return NEVER_IGNORE;
            case 0:
                return IGNORE_FULLY_FILTERED;
            default:
                return ignoreOverThreshold(hashesToDrop);
            }
        }

        boolean shouldIgnore(String hashes, int hashesSize);
    }

    class PlayerContext implements ITextFilter {
        private final GameProfile profile;
        private final Executor streamExecutor;

        PlayerContext(GameProfile gameProfile) {
            this.profile = gameProfile;
            ThreadedMailbox<Runnable> processorMailbox = ThreadedMailbox.create(TextFilter.this.workerPool, "chat stream for " + gameProfile.getName());
            this.streamExecutor = processorMailbox::tell;
        }

        @Override
        public void join() {
            TextFilter.this.processJoinOrLeave(this.profile, TextFilter.this.joinEndpoint, this.streamExecutor);
        }

        @Override
        public void leave() {
            TextFilter.this.processJoinOrLeave(this.profile, TextFilter.this.leaveEndpoint, this.streamExecutor);
        }

        @Override
        public CompletableFuture<List<ITextFilter.FilteredText>> processMessageBundle(List<String> texts) {
            List<CompletableFuture<ITextFilter.FilteredText>> list = texts.stream().map((string) -> {
                return TextFilter.this.requestMessageProcessing(this.profile, string, TextFilter.this.chatIgnoreStrategy, this.streamExecutor);
            }).collect(ImmutableList.toImmutableList());
            return SystemUtils.sequenceFailFast(list).exceptionally((throwable) -> {
                return ImmutableList.of();
            });
        }

        @Override
        public CompletableFuture<ITextFilter.FilteredText> processStreamMessage(String text) {
            return TextFilter.this.requestMessageProcessing(this.profile, text, TextFilter.this.chatIgnoreStrategy, this.streamExecutor);
        }
    }

    public static class RequestFailedException extends RuntimeException {
        RequestFailedException(String message) {
            super(message);
        }
    }
}
