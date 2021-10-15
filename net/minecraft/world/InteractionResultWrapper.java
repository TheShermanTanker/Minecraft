package net.minecraft.world;

public class InteractionResultWrapper<T> {
    private final EnumInteractionResult result;
    private final T object;

    public InteractionResultWrapper(EnumInteractionResult result, T value) {
        this.result = result;
        this.object = value;
    }

    public EnumInteractionResult getResult() {
        return this.result;
    }

    public T getObject() {
        return this.object;
    }

    public static <T> InteractionResultWrapper<T> success(T data) {
        return new InteractionResultWrapper<>(EnumInteractionResult.SUCCESS, data);
    }

    public static <T> InteractionResultWrapper<T> consume(T data) {
        return new InteractionResultWrapper<>(EnumInteractionResult.CONSUME, data);
    }

    public static <T> InteractionResultWrapper<T> pass(T data) {
        return new InteractionResultWrapper<>(EnumInteractionResult.PASS, data);
    }

    public static <T> InteractionResultWrapper<T> fail(T data) {
        return new InteractionResultWrapper<>(EnumInteractionResult.FAIL, data);
    }

    public static <T> InteractionResultWrapper<T> sidedSuccess(T data, boolean swingHand) {
        return swingHand ? success(data) : consume(data);
    }
}
