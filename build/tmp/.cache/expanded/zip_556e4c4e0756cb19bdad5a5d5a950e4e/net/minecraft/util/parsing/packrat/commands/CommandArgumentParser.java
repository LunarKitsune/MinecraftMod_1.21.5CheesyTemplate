package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface CommandArgumentParser<T> {
    T parseForCommands(StringReader pReader) throws CommandSyntaxException;

    CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder pBuilder);

    default <S> CommandArgumentParser<S> mapResult(final Function<T, S> pMapper) {
        return new CommandArgumentParser<S>() {
            @Override
            public S parseForCommands(StringReader p_393564_) throws CommandSyntaxException {
                return pMapper.apply((T)CommandArgumentParser.this.parseForCommands(p_393564_));
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_395812_) {
                return CommandArgumentParser.this.parseForSuggestions(p_395812_);
            }
        };
    }

    default <T, O> CommandArgumentParser<T> withCodec(
        final DynamicOps<O> pOps, final CommandArgumentParser<O> pParser, final Codec<T> pCodec, final DynamicCommandExceptionType pError
    ) {
        return new CommandArgumentParser<T>() {
            @Override
            public T parseForCommands(StringReader p_391748_) throws CommandSyntaxException {
                int i = p_391748_.getCursor();
                O o = pParser.parseForCommands(p_391748_);
                DataResult<T> dataresult = pCodec.parse(pOps, o);
                return dataresult.getOrThrow(p_394070_ -> {
                    p_391748_.setCursor(i);
                    return pError.createWithContext(p_391748_, p_394070_);
                });
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_393320_) {
                return CommandArgumentParser.this.parseForSuggestions(p_393320_);
            }
        };
    }
}