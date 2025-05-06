package net.minecraft.advancements.critereon;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface MinMaxBounds<T extends Number> {
    SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.range.empty"));
    SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(Component.translatable("argument.range.swapped"));

    Optional<T> min();

    Optional<T> max();

    default boolean isAny() {
        return this.min().isEmpty() && this.max().isEmpty();
    }

    default Optional<T> unwrapPoint() {
        Optional<T> optional = this.min();
        Optional<T> optional1 = this.max();
        return optional.equals(optional1) ? optional : Optional.empty();
    }

    static <T extends Number, R extends MinMaxBounds<T>> Codec<R> createCodec(Codec<T> pCodec, MinMaxBounds.BoundsFactory<T, R> pBoundsFactory) {
        Codec<R> codec = RecordCodecBuilder.create(
            p_325235_ -> p_325235_.group(
                    pCodec.optionalFieldOf("min").forGetter(MinMaxBounds::min), pCodec.optionalFieldOf("max").forGetter(MinMaxBounds::max)
                )
                .apply(p_325235_, pBoundsFactory::create)
        );
        return Codec.either(codec, pCodec)
            .xmap(
                p_299505_ -> p_299505_.map(p_300863_ -> (R)p_300863_, p_300712_ -> pBoundsFactory.create(Optional.of((T)p_300712_), Optional.of((T)p_300712_))),
                p_300263_ -> {
                    Optional<T> optional = p_300263_.unwrapPoint();
                    return optional.isPresent() ? Either.right(optional.get()) : Either.left((R)p_300263_);
                }
            );
    }

    static <B extends ByteBuf, T extends Number, R extends MinMaxBounds<T>> StreamCodec<B, R> createStreamCodec(
        final StreamCodec<B, T> pStreamCodec, final MinMaxBounds.BoundsFactory<T, R> pBoundsFactory
    ) {
        return new StreamCodec<B, R>() {
            private static final int MIN_FLAG = 1;
            public static final int MAX_FLAG = 2;

            public R decode(B p_393437_) {
                byte b0 = p_393437_.readByte();
                Optional<T> optional = (b0 & 1) != 0 ? Optional.of(pStreamCodec.decode(p_393437_)) : Optional.empty();
                Optional<T> optional1 = (b0 & 2) != 0 ? Optional.of(pStreamCodec.decode(p_393437_)) : Optional.empty();
                return pBoundsFactory.create(optional, optional1);
            }

            public void encode(B p_392435_, R p_396875_) {
                Optional<T> optional = p_396875_.min();
                Optional<T> optional1 = p_396875_.max();
                p_392435_.writeByte((optional.isPresent() ? 1 : 0) | (optional1.isPresent() ? 2 : 0));
                optional.ifPresent(p_397001_ -> pStreamCodec.encode(p_392435_, (T)p_397001_));
                optional1.ifPresent(p_393724_ -> pStreamCodec.encode(p_392435_, (T)p_393724_));
            }
        };
    }

    static <T extends Number, R extends MinMaxBounds<T>> R fromReader(
        StringReader pReader,
        MinMaxBounds.BoundsFromReaderFactory<T, R> pBoundedFactory,
        Function<String, T> pValueFactory,
        Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier,
        Function<T, T> pFormatter
    ) throws CommandSyntaxException {
        if (!pReader.canRead()) {
            throw ERROR_EMPTY.createWithContext(pReader);
        } else {
            int i = pReader.getCursor();

            try {
                Optional<T> optional = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
                Optional<T> optional1;
                if (pReader.canRead(2) && pReader.peek() == '.' && pReader.peek(1) == '.') {
                    pReader.skip();
                    pReader.skip();
                    optional1 = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
                    if (optional.isEmpty() && optional1.isEmpty()) {
                        throw ERROR_EMPTY.createWithContext(pReader);
                    }
                } else {
                    optional1 = optional;
                }

                if (optional.isEmpty() && optional1.isEmpty()) {
                    throw ERROR_EMPTY.createWithContext(pReader);
                } else {
                    return pBoundedFactory.create(pReader, optional, optional1);
                }
            } catch (CommandSyntaxException commandsyntaxexception) {
                pReader.setCursor(i);
                throw new CommandSyntaxException(commandsyntaxexception.getType(), commandsyntaxexception.getRawMessage(), commandsyntaxexception.getInput(), i);
            }
        }
    }

    private static <T extends Number> Optional<T> readNumber(StringReader pReader, Function<String, T> pStringToValueFunction, Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier) throws CommandSyntaxException {
        int i = pReader.getCursor();

        while (pReader.canRead() && isAllowedInputChat(pReader)) {
            pReader.skip();
        }

        String s = pReader.getString().substring(i, pReader.getCursor());
        if (s.isEmpty()) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(pStringToValueFunction.apply(s));
            } catch (NumberFormatException numberformatexception) {
                throw pCommandExceptionSupplier.get().createWithContext(pReader, s);
            }
        }
    }

    private static boolean isAllowedInputChat(StringReader pReader) {
        char c0 = pReader.peek();
        if ((c0 < '0' || c0 > '9') && c0 != '-') {
            return c0 != '.' ? false : !pReader.canRead(2) || pReader.peek(1) != '.';
        } else {
            return true;
        }
    }

    @FunctionalInterface
    public interface BoundsFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(Optional<T> pMin, Optional<T> pMax);
    }

    @FunctionalInterface
    public interface BoundsFromReaderFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(StringReader pReader, Optional<T> pMin, Optional<T> pMax) throws CommandSyntaxException;
    }

    public record Doubles(Optional<Double> min, Optional<Double> max, Optional<Double> minSq, Optional<Double> maxSq)
        implements MinMaxBounds<Double> {
        public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles(Optional.empty(), Optional.empty());
        public static final Codec<MinMaxBounds.Doubles> CODEC = MinMaxBounds.<Double, Doubles>createCodec(Codec.DOUBLE, MinMaxBounds.Doubles::new);
        public static final StreamCodec<ByteBuf, MinMaxBounds.Doubles> STREAM_CODEC = MinMaxBounds.<ByteBuf, Double, Doubles>createStreamCodec(ByteBufCodecs.DOUBLE, MinMaxBounds.Doubles::new);

        private Doubles(Optional<Double> pMin, Optional<Double> pMax) {
            this(pMin, pMax, squareOpt(pMin), squareOpt(pMax));
        }

        private static MinMaxBounds.Doubles create(StringReader pReader, Optional<Double> pMin, Optional<Double> pMax) throws CommandSyntaxException {
            if (pMin.isPresent() && pMax.isPresent() && pMin.get() > pMax.get()) {
                throw ERROR_SWAPPED.createWithContext(pReader);
            } else {
                return new MinMaxBounds.Doubles(pMin, pMax);
            }
        }

        private static Optional<Double> squareOpt(Optional<Double> pValue) {
            return pValue.map(p_296138_ -> p_296138_ * p_296138_);
        }

        public static MinMaxBounds.Doubles exactly(double pValue) {
            return new MinMaxBounds.Doubles(Optional.of(pValue), Optional.of(pValue));
        }

        public static MinMaxBounds.Doubles between(double pMin, double pMax) {
            return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.of(pMax));
        }

        public static MinMaxBounds.Doubles atLeast(double pMin) {
            return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.empty());
        }

        public static MinMaxBounds.Doubles atMost(double pMax) {
            return new MinMaxBounds.Doubles(Optional.empty(), Optional.of(pMax));
        }

        public boolean matches(double pValue) {
            return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || !(this.max.get() < pValue);
        }

        public boolean matchesSqr(double pValue) {
            return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || !(this.maxSq.get() < pValue);
        }

        public static MinMaxBounds.Doubles fromReader(StringReader pReader) throws CommandSyntaxException {
            return fromReader(pReader, p_154807_ -> p_154807_);
        }

        public static MinMaxBounds.Doubles fromReader(StringReader pReader, Function<Double, Double> pFormatter) throws CommandSyntaxException {
            return MinMaxBounds.fromReader(
                pReader, MinMaxBounds.Doubles::create, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble, pFormatter
            );
        }

        @Override
        public Optional<Double> min() {
            return this.min;
        }

        @Override
        public Optional<Double> max() {
            return this.max;
        }
    }

    public record Ints(Optional<Integer> min, Optional<Integer> max, Optional<Long> minSq, Optional<Long> maxSq)
        implements MinMaxBounds<Integer> {
        public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(Optional.empty(), Optional.empty());
        public static final Codec<MinMaxBounds.Ints> CODEC = MinMaxBounds.<Integer, Ints>createCodec(Codec.INT, MinMaxBounds.Ints::new);
        public static final StreamCodec<ByteBuf, MinMaxBounds.Ints> STREAM_CODEC = MinMaxBounds.<ByteBuf, Integer, Ints>createStreamCodec(ByteBufCodecs.INT, MinMaxBounds.Ints::new);

        private Ints(Optional<Integer> pMin, Optional<Integer> pMax) {
            this(pMin, pMax, pMin.map(p_296140_ -> p_296140_.longValue() * p_296140_.longValue()), squareOpt(pMax));
        }

        private static MinMaxBounds.Ints create(StringReader pReader, Optional<Integer> pMin, Optional<Integer> pMax) throws CommandSyntaxException {
            if (pMin.isPresent() && pMax.isPresent() && pMin.get() > pMax.get()) {
                throw ERROR_SWAPPED.createWithContext(pReader);
            } else {
                return new MinMaxBounds.Ints(pMin, pMax);
            }
        }

        private static Optional<Long> squareOpt(Optional<Integer> pValue) {
            return pValue.map(p_296139_ -> p_296139_.longValue() * p_296139_.longValue());
        }

        public static MinMaxBounds.Ints exactly(int pValue) {
            return new MinMaxBounds.Ints(Optional.of(pValue), Optional.of(pValue));
        }

        public static MinMaxBounds.Ints between(int pMin, int pMax) {
            return new MinMaxBounds.Ints(Optional.of(pMin), Optional.of(pMax));
        }

        public static MinMaxBounds.Ints atLeast(int pMin) {
            return new MinMaxBounds.Ints(Optional.of(pMin), Optional.empty());
        }

        public static MinMaxBounds.Ints atMost(int pMax) {
            return new MinMaxBounds.Ints(Optional.empty(), Optional.of(pMax));
        }

        public boolean matches(int pValue) {
            return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || this.max.get() >= pValue;
        }

        public boolean matchesSqr(long pValue) {
            return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || this.maxSq.get() >= pValue;
        }

        public static MinMaxBounds.Ints fromReader(StringReader pReader) throws CommandSyntaxException {
            return fromReader(pReader, p_55389_ -> p_55389_);
        }

        public static MinMaxBounds.Ints fromReader(StringReader pReader, Function<Integer, Integer> pValueFunction) throws CommandSyntaxException {
            return MinMaxBounds.fromReader(
                pReader, MinMaxBounds.Ints::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, pValueFunction
            );
        }

        @Override
        public Optional<Integer> min() {
            return this.min;
        }

        @Override
        public Optional<Integer> max() {
            return this.max;
        }
    }
}