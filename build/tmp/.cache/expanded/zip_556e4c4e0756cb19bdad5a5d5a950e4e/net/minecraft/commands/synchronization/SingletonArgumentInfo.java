package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.FriendlyByteBuf;

public class SingletonArgumentInfo<A extends ArgumentType<?>> implements ArgumentTypeInfo<A, SingletonArgumentInfo<A>.Template> {
    private final SingletonArgumentInfo<A>.Template template;

    private SingletonArgumentInfo(Function<CommandBuildContext, A> pConstructor) {
        this.template = new SingletonArgumentInfo.Template(pConstructor);
    }

    public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextFree(Supplier<T> pArgumentTypeSupplier) {
        return new SingletonArgumentInfo<>(p_235455_ -> pArgumentTypeSupplier.get());
    }

    public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextAware(Function<CommandBuildContext, T> pArgumentType) {
        return new SingletonArgumentInfo<>(pArgumentType);
    }

    public void serializeToNetwork(SingletonArgumentInfo<A>.Template p_235447_, FriendlyByteBuf p_235448_) {
    }

    public void serializeToJson(SingletonArgumentInfo<A>.Template p_235444_, JsonObject p_235445_) {
    }

    public SingletonArgumentInfo<A>.Template deserializeFromNetwork(FriendlyByteBuf p_235457_) {
        return this.template;
    }

    public SingletonArgumentInfo<A>.Template unpack(A p_235459_) {
        return this.template;
    }

    public final class Template implements ArgumentTypeInfo.Template<A> {
        private final Function<CommandBuildContext, A> constructor;

        public Template(final Function<CommandBuildContext, A> pConstructor) {
            this.constructor = pConstructor;
        }

        @Override
        public A instantiate(CommandBuildContext p_235469_) {
            return this.constructor.apply(p_235469_);
        }

        @Override
        public ArgumentTypeInfo<A, ?> type() {
            return SingletonArgumentInfo.this;
        }
    }
}