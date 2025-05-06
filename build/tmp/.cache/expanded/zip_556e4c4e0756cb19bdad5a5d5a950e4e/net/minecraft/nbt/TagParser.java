package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class TagParser<T> {
    public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType(Component.translatable("argument.nbt.trailing"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new SimpleCommandExceptionType(Component.translatable("argument.nbt.expected.compound"));
    public static final char ELEMENT_SEPARATOR = ',';
    public static final char NAME_VALUE_SEPARATOR = ':';
    private static final TagParser<Tag> NBT_OPS_PARSER = create(NbtOps.INSTANCE);
    public static final Codec<CompoundTag> FLATTENED_CODEC = Codec.STRING
        .comapFlatMap(
            p_389906_ -> {
                try {
                    Tag tag = NBT_OPS_PARSER.parseFully(p_389906_);
                    return tag instanceof CompoundTag compoundtag
                        ? DataResult.success(compoundtag, Lifecycle.stable())
                        : DataResult.error(() -> "Expected compound tag, got " + tag);
                } catch (CommandSyntaxException commandsyntaxexception) {
                    return DataResult.error(commandsyntaxexception::getMessage);
                }
            },
            CompoundTag::toString
        );
    public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
    private final DynamicOps<T> ops;
    private final Grammar<T> grammar;

    private TagParser(DynamicOps<T> pOps, Grammar<T> pGrammar) {
        this.ops = pOps;
        this.grammar = pGrammar;
    }

    public DynamicOps<T> getOps() {
        return this.ops;
    }

    public static <T> TagParser<T> create(DynamicOps<T> pOps) {
        return new TagParser<>(pOps, SnbtGrammar.createParser(pOps));
    }

    private static CompoundTag castToCompoundOrThrow(StringReader pReader, Tag pTag) throws CommandSyntaxException {
        if (pTag instanceof CompoundTag compoundtag) {
            return compoundtag;
        } else {
            throw ERROR_EXPECTED_COMPOUND.createWithContext(pReader);
        }
    }

    public static CompoundTag parseCompoundFully(String pData) throws CommandSyntaxException {
        StringReader stringreader = new StringReader(pData);
        return castToCompoundOrThrow(stringreader, NBT_OPS_PARSER.parseFully(stringreader));
    }

    public T parseFully(String pText) throws CommandSyntaxException {
        return this.parseFully(new StringReader(pText));
    }

    public T parseFully(StringReader pReader) throws CommandSyntaxException {
        T t = this.grammar.parseForCommands(pReader);
        pReader.skipWhitespace();
        if (pReader.canRead()) {
            throw ERROR_TRAILING_DATA.createWithContext(pReader);
        } else {
            return t;
        }
    }

    public T parseAsArgument(StringReader pReader) throws CommandSyntaxException {
        return this.grammar.parseForCommands(pReader);
    }

    public static CompoundTag parseCompoundAsArgument(StringReader pReader) throws CommandSyntaxException {
        Tag tag = NBT_OPS_PARSER.parseAsArgument(pReader);
        return castToCompoundOrThrow(pReader, tag);
    }
}