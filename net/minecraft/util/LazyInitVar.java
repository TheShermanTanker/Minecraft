package net.minecraft.util;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;

/** @deprecated */
@Deprecated
public class LazyInitVar<T> {
    private final Supplier<T> factory;

    public LazyInitVar(Supplier<T> delegate) {
        this.factory = Suppliers.memoize(delegate::get);
    }

    public T get() {
        return this.factory.get();
    }
}
