package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public abstract class HeightProvider {
    private static final Codec<Either<VerticalAnchor, HeightProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(VerticalAnchor.CODEC, IRegistry.HEIGHT_PROVIDER_TYPES.dispatch(HeightProvider::getType, HeightProviderType::codec));
    public static final Codec<HeightProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((either) -> {
        return either.map(ConstantHeight::of, (heightProvider) -> {
            return heightProvider;
        });
    }, (heightProvider) -> {
        return heightProvider.getType() == HeightProviderType.CONSTANT ? Either.left(((ConstantHeight)heightProvider).getValue()) : Either.right(heightProvider);
    });

    public abstract int sample(Random random, WorldGenerationContext context);

    public abstract HeightProviderType<?> getType();
}
