package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.Particle;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;

public class ArgumentParticle implements ArgumentType<ParticleParam> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle with options");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("particle.notFound", id);
    });

    public static ArgumentParticle particle() {
        return new ArgumentParticle();
    }

    public static ParticleParam getParticle(CommandContext<CommandListenerWrapper> context, String name) {
        return context.getArgument(name, ParticleParam.class);
    }

    public ParticleParam parse(StringReader stringReader) throws CommandSyntaxException {
        return readParticle(stringReader);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static ParticleParam readParticle(StringReader reader) throws CommandSyntaxException {
        MinecraftKey resourceLocation = MinecraftKey.read(reader);
        Particle<?> particleType = IRegistry.PARTICLE_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_PARTICLE.create(resourceLocation);
        });
        return readParticle(reader, particleType);
    }

    private static <T extends ParticleParam> T readParticle(StringReader reader, Particle<T> type) throws CommandSyntaxException {
        return type.getDeserializer().fromCommand(type, reader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggestResource(IRegistry.PARTICLE_TYPE.keySet(), suggestionsBuilder);
    }
}
