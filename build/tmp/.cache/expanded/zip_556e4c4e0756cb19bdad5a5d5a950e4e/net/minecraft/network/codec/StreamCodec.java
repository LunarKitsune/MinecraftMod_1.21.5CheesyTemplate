package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> pEncoder, final StreamDecoder<B, V> pDecoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_335513_) {
                return pDecoder.decode(p_335513_);
            }

            @Override
            public void encode(B p_333998_, V p_335122_) {
                pEncoder.encode(p_333998_, p_335122_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> pEncoder, final StreamDecoder<B, V> pDecoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_331033_) {
                return pDecoder.decode(p_331033_);
            }

            @Override
            public void encode(B p_329484_, V p_332289_) {
                pEncoder.encode(p_332289_, p_329484_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(final V pExpectedValue) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_328164_) {
                return pExpectedValue;
            }

            @Override
            public void encode(B p_336022_, V p_333291_) {
                if (!p_333291_.equals(pExpectedValue)) {
                    throw new IllegalStateException("Can't encode '" + p_333291_ + "', expected '" + pExpectedValue + "'");
                }
            }
        };
    }

    default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> pOperation) {
        return pOperation.apply(this);
    }

    default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> pFactory, final Function<? super O, ? extends V> pGetter) {
        return new StreamCodec<B, O>() {
            @Override
            public O decode(B p_328614_) {
                return (O)pFactory.apply(StreamCodec.this.decode(p_328614_));
            }

            @Override
            public void encode(B p_336327_, O p_331146_) {
                StreamCodec.this.encode(p_336327_, (V)pGetter.apply(p_331146_));
            }
        };
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(final Function<O, ? extends B> pBufferFactory) {
        return new StreamCodec<O, V>() {
            public V decode(O p_331759_) {
                B b = (B)pBufferFactory.apply(p_331759_);
                return StreamCodec.this.decode(b);
            }

            public void encode(O p_334335_, V p_336271_) {
                B b = (B)pBufferFactory.apply(p_334335_);
                StreamCodec.this.encode(b, p_336271_);
            }
        };
    }

    default <U> StreamCodec<B, U> dispatch(
        final Function<? super U, ? extends V> pKeyGetter, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> pCodecGetter
    ) {
        return new StreamCodec<B, U>() {
            @Override
            public U decode(B p_333769_) {
                V v = StreamCodec.this.decode(p_333769_);
                StreamCodec<? super B, ? extends U> streamcodec = (StreamCodec<? super B, ? extends U>)pCodecGetter.apply(v);
                return (U)streamcodec.decode(p_333769_);
            }

            @Override
            public void encode(B p_331493_, U p_333683_) {
                V v = (V)pKeyGetter.apply(p_333683_);
                StreamCodec<B, U> streamcodec = (StreamCodec<B, U>)pCodecGetter.apply(v);
                StreamCodec.this.encode(p_331493_, v);
                streamcodec.encode(p_331493_, p_333683_);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> pCodec, final Function<C, T1> pGetter, final Function<T1, C> pFactory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_331843_) {
                T1 t1 = pCodec.decode(p_331843_);
                return pFactory.apply(t1);
            }

            @Override
            public void encode(B p_330937_, C p_333579_) {
                pCodec.encode(p_330937_, pGetter.apply(p_333579_));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final BiFunction<T1, T2, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_331897_) {
                T1 t1 = pCodec1.decode(p_331897_);
                T2 t2 = pCodec2.decode(p_331897_);
                return pFactory.apply(t1, t2);
            }

            @Override
            public void encode(B p_334266_, C p_331042_) {
                pCodec1.encode(p_334266_, pGetter1.apply(p_331042_));
                pCodec2.encode(p_334266_, pGetter2.apply(p_331042_));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final Function3<T1, T2, T3, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_331065_) {
                T1 t1 = pCodec1.decode(p_331065_);
                T2 t2 = pCodec2.decode(p_331065_);
                T3 t3 = pCodec3.decode(p_331065_);
                return pFactory.apply(t1, t2, t3);
            }

            @Override
            public void encode(B p_333137_, C p_328354_) {
                pCodec1.encode(p_333137_, pGetter1.apply(p_328354_));
                pCodec2.encode(p_333137_, pGetter2.apply(p_328354_));
                pCodec3.encode(p_333137_, pGetter3.apply(p_328354_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final Function4<T1, T2, T3, T4, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_334517_) {
                T1 t1 = pCodec1.decode(p_334517_);
                T2 t2 = pCodec2.decode(p_334517_);
                T3 t3 = pCodec3.decode(p_334517_);
                T4 t4 = pCodec4.decode(p_334517_);
                return pFactory.apply(t1, t2, t3, t4);
            }

            @Override
            public void encode(B p_336185_, C p_330170_) {
                pCodec1.encode(p_336185_, pGetter1.apply(p_330170_));
                pCodec2.encode(p_336185_, pGetter2.apply(p_330170_));
                pCodec3.encode(p_336185_, pGetter3.apply(p_330170_));
                pCodec4.encode(p_336185_, pGetter4.apply(p_330170_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final Function5<T1, T2, T3, T4, T5, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_328956_) {
                T1 t1 = pCodec1.decode(p_328956_);
                T2 t2 = pCodec2.decode(p_328956_);
                T3 t3 = pCodec3.decode(p_328956_);
                T4 t4 = pCodec4.decode(p_328956_);
                T5 t5 = pCodec5.decode(p_328956_);
                return pFactory.apply(t1, t2, t3, t4, t5);
            }

            @Override
            public void encode(B p_328899_, C p_328944_) {
                pCodec1.encode(p_328899_, pGetter1.apply(p_328944_));
                pCodec2.encode(p_328899_, pGetter2.apply(p_328944_));
                pCodec3.encode(p_328899_, pGetter3.apply(p_328944_));
                pCodec4.encode(p_328899_, pGetter4.apply(p_328944_));
                pCodec5.encode(p_328899_, pGetter5.apply(p_328944_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final StreamCodec<? super B, T6> pCodec6,
        final Function<C, T6> pGetter6,
        final Function6<T1, T2, T3, T4, T5, T6, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_330564_) {
                T1 t1 = pCodec1.decode(p_330564_);
                T2 t2 = pCodec2.decode(p_330564_);
                T3 t3 = pCodec3.decode(p_330564_);
                T4 t4 = pCodec4.decode(p_330564_);
                T5 t5 = pCodec5.decode(p_330564_);
                T6 t6 = pCodec6.decode(p_330564_);
                return pFactory.apply(t1, t2, t3, t4, t5, t6);
            }

            @Override
            public void encode(B p_328016_, C p_331911_) {
                pCodec1.encode(p_328016_, pGetter1.apply(p_331911_));
                pCodec2.encode(p_328016_, pGetter2.apply(p_331911_));
                pCodec3.encode(p_328016_, pGetter3.apply(p_331911_));
                pCodec4.encode(p_328016_, pGetter4.apply(p_331911_));
                pCodec5.encode(p_328016_, pGetter5.apply(p_331911_));
                pCodec6.encode(p_328016_, pGetter6.apply(p_331911_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final StreamCodec<? super B, T6> pCodec6,
        final Function<C, T6> pGetter6,
        final StreamCodec<? super B, T7> pCodec7,
        final Function<C, T7> pGetter7,
        final Function7<T1, T2, T3, T4, T5, T6, T7, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_330854_) {
                T1 t1 = pCodec1.decode(p_330854_);
                T2 t2 = pCodec2.decode(p_330854_);
                T3 t3 = pCodec3.decode(p_330854_);
                T4 t4 = pCodec4.decode(p_330854_);
                T5 t5 = pCodec5.decode(p_330854_);
                T6 t6 = pCodec6.decode(p_330854_);
                T7 t7 = pCodec7.decode(p_330854_);
                return pFactory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(B p_332524_, C p_336367_) {
                pCodec1.encode(p_332524_, pGetter1.apply(p_336367_));
                pCodec2.encode(p_332524_, pGetter2.apply(p_336367_));
                pCodec3.encode(p_332524_, pGetter3.apply(p_336367_));
                pCodec4.encode(p_332524_, pGetter4.apply(p_336367_));
                pCodec5.encode(p_332524_, pGetter5.apply(p_336367_));
                pCodec6.encode(p_332524_, pGetter6.apply(p_336367_));
                pCodec7.encode(p_332524_, pGetter7.apply(p_336367_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final StreamCodec<? super B, T6> pCodec6,
        final Function<C, T6> pGetter6,
        final StreamCodec<? super B, T7> pCodec7,
        final Function<C, T7> pGetter7,
        final StreamCodec<? super B, T8> pCodec8,
        final Function<C, T8> pGetter8,
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_362416_) {
                T1 t1 = pCodec1.decode(p_362416_);
                T2 t2 = pCodec2.decode(p_362416_);
                T3 t3 = pCodec3.decode(p_362416_);
                T4 t4 = pCodec4.decode(p_362416_);
                T5 t5 = pCodec5.decode(p_362416_);
                T6 t6 = pCodec6.decode(p_362416_);
                T7 t7 = pCodec7.decode(p_362416_);
                T8 t8 = pCodec8.decode(p_362416_);
                return pFactory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }

            @Override
            public void encode(B p_366041_, C p_365657_) {
                pCodec1.encode(p_366041_, pGetter1.apply(p_365657_));
                pCodec2.encode(p_366041_, pGetter2.apply(p_365657_));
                pCodec3.encode(p_366041_, pGetter3.apply(p_365657_));
                pCodec4.encode(p_366041_, pGetter4.apply(p_365657_));
                pCodec5.encode(p_366041_, pGetter5.apply(p_365657_));
                pCodec6.encode(p_366041_, pGetter6.apply(p_365657_));
                pCodec7.encode(p_366041_, pGetter7.apply(p_365657_));
                pCodec8.encode(p_366041_, pGetter8.apply(p_365657_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> pCodec1,
        final Function<C, T1> pGetter1,
        final StreamCodec<? super B, T2> pCodec2,
        final Function<C, T2> pGetter2,
        final StreamCodec<? super B, T3> pCodec3,
        final Function<C, T3> pGetter3,
        final StreamCodec<? super B, T4> pCodec4,
        final Function<C, T4> pGetter4,
        final StreamCodec<? super B, T5> pCodec5,
        final Function<C, T5> pGetter5,
        final StreamCodec<? super B, T6> pCodec6,
        final Function<C, T6> pGetter6,
        final StreamCodec<? super B, T7> pCodec7,
        final Function<C, T7> pGetter7,
        final StreamCodec<? super B, T8> pCodec8,
        final Function<C, T8> pGetter8,
        final StreamCodec<? super B, T9> pCodec9,
        final Function<C, T9> pGetter9,
        final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> pFactory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_366688_) {
                T1 t1 = pCodec1.decode(p_366688_);
                T2 t2 = pCodec2.decode(p_366688_);
                T3 t3 = pCodec3.decode(p_366688_);
                T4 t4 = pCodec4.decode(p_366688_);
                T5 t5 = pCodec5.decode(p_366688_);
                T6 t6 = pCodec6.decode(p_366688_);
                T7 t7 = pCodec7.decode(p_366688_);
                T8 t8 = pCodec8.decode(p_366688_);
                T9 t9 = pCodec9.decode(p_366688_);
                return pFactory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
            }

            @Override
            public void encode(B p_364543_, C p_364761_) {
                pCodec1.encode(p_364543_, pGetter1.apply(p_364761_));
                pCodec2.encode(p_364543_, pGetter2.apply(p_364761_));
                pCodec3.encode(p_364543_, pGetter3.apply(p_364761_));
                pCodec4.encode(p_364543_, pGetter4.apply(p_364761_));
                pCodec5.encode(p_364543_, pGetter5.apply(p_364761_));
                pCodec6.encode(p_364543_, pGetter6.apply(p_364761_));
                pCodec7.encode(p_364543_, pGetter7.apply(p_364761_));
                pCodec8.encode(p_364543_, pGetter8.apply(p_364761_));
                pCodec9.encode(p_364543_, pGetter9.apply(p_364761_));
            }
        };
    }

    static <B, T> StreamCodec<B, T> recursive(final UnaryOperator<StreamCodec<B, T>> pModifier) {
        return new StreamCodec<B, T>() {
            private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> pModifier.apply(this));

            @Override
            public T decode(B p_397382_) {
                return this.inner.get().decode(p_397382_);
            }

            @Override
            public void encode(B p_396343_, T p_395545_) {
                this.inner.get().encode(p_396343_, p_395545_);
            }
        };
    }

    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec)this;
    }

    @FunctionalInterface
    public interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> pCodec);
    }
}