package net.minecraft.world.inventory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;

public interface ContainerAccess {
    ContainerAccess NULL = new ContainerAccess() {
        @Override
        public <T> Optional<T> evaluate(BiFunction<World, BlockPosition, T> getter) {
            return Optional.empty();
        }
    };

    static ContainerAccess at(World world, BlockPosition pos) {
        return new ContainerAccess() {
            @Override
            public <T> Optional<T> evaluate(BiFunction<World, BlockPosition, T> getter) {
                return Optional.of(getter.apply(world, pos));
            }
        };
    }

    <T> Optional<T> evaluate(BiFunction<World, BlockPosition, T> getter);

    default <T> T evaluate(BiFunction<World, BlockPosition, T> getter, T defaultValue) {
        return this.evaluate(getter).orElse(defaultValue);
    }

    default void execute(BiConsumer<World, BlockPosition> function) {
        this.evaluate((world, pos) -> {
            function.accept(world, pos);
            return Optional.empty();
        });
    }
}
