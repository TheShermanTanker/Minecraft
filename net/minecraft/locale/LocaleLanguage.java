package net.minecraft.locale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatFormatted;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.FormattedString;
import net.minecraft.util.StringDecomposer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class LocaleLanguage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Pattern UNSUPPORTED_FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    public static final String DEFAULT = "en_us";
    private static volatile LocaleLanguage instance = loadDefault();

    private static LocaleLanguage loadDefault() {
        Builder<String, String> builder = ImmutableMap.builder();
        BiConsumer<String, String> biConsumer = builder::put;
        String string = "/assets/minecraft/lang/en_us.json";

        try {
            InputStream inputStream = LocaleLanguage.class.getResourceAsStream("/assets/minecraft/lang/en_us.json");

            try {
                loadFromJson(inputStream, biConsumer);
            } catch (Throwable var7) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (JsonParseException | IOException var8) {
            LOGGER.error("Couldn't read strings from {}", "/assets/minecraft/lang/en_us.json", var8);
        }

        final Map<String, String> map = builder.build();
        return new LocaleLanguage() {
            @Override
            public String getOrDefault(String key) {
                return map.getOrDefault(key, key);
            }

            @Override
            public boolean has(String key) {
                return map.containsKey(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedString getVisualOrder(IChatFormatted text) {
                return (visitor) -> {
                    return text.visit((style, string) -> {
                        return StringDecomposer.iterateFormatted(string, style, visitor) ? Optional.empty() : IChatFormatted.STOP_ITERATION;
                    }, ChatModifier.EMPTY).isPresent();
                };
            }
        };
    }

    public static void loadFromJson(InputStream inputStream, BiConsumer<String, String> entryConsumer) {
        JsonObject jsonObject = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);

        for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String string = UNSUPPORTED_FORMAT_PATTERN.matcher(ChatDeserializer.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            entryConsumer.accept(entry.getKey(), string);
        }

    }

    public static LocaleLanguage getInstance() {
        return instance;
    }

    public static void inject(LocaleLanguage language) {
        instance = language;
    }

    public abstract String getOrDefault(String key);

    public abstract boolean has(String key);

    public abstract boolean isDefaultRightToLeft();

    public abstract FormattedString getVisualOrder(IChatFormatted text);

    public List<FormattedString> getVisualOrder(List<IChatFormatted> texts) {
        return texts.stream().map(this::getVisualOrder).collect(ImmutableList.toImmutableList());
    }
}
