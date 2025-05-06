package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPatternParseRule;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.NumberRunParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;

public class SnbtGrammar {
    private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new DynamicCommandExceptionType(
        p_391930_ -> Component.translatableEscape("snbt.parser.number_parse_failure", p_391930_)
    );
    static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new DynamicCommandExceptionType(
        p_392560_ -> Component.translatableEscape("snbt.parser.expected_hex_escape", p_392560_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new DynamicCommandExceptionType(
        p_392900_ -> Component.translatableEscape("snbt.parser.invalid_codepoint", p_392900_)
    );
    private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new DynamicCommandExceptionType(
        p_392553_ -> Component.translatableEscape("snbt.parser.no_such_operation", p_392553_)
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_integer_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_float_type"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_non_negative_number"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_character_name"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_array_element_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_unquoted_start"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_unquoted_string"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_string_contents"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_binary_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_UNDESCORE_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.undescore_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_decimal_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_hex_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.empty_key"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.leading_zero_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.infinity_not_allowed"))
    );
    private static final HexFormat HEX_ESCAPE = HexFormat.of().withUpperCase();
    private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_BINARY_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_393182_) {
            return switch (p_393182_) {
                case '0', '1', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_DECIMAL_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_397542_) {
            return switch (p_397542_) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_HEX_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_396361_) {
            return switch (p_396361_) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    };
    private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, ERROR_INVALID_STRING_CONTENTS) {
        @Override
        protected boolean isAccepted(char p_393190_) {
            return switch (p_393190_) {
                case '"', '\'', '\\' -> false;
                default -> true;
            };
        }
    };
    private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()) {
        @Override
        protected boolean isAccepted(char p_392269_) {
            return SnbtGrammar.canStartNumber(p_392269_);
        }
    };
    private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

    static DelayedException<CommandSyntaxException> createNumberParseError(NumberFormatException pNumberFormatException) {
        return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, pNumberFormatException.getMessage());
    }

    @Nullable
    public static String escapeControlCharacters(char pC) {
        return switch (pC) {
            case '\b' -> "b";
            case '\t' -> "t";
            case '\n' -> "n";
            default -> pC < ' ' ? "x" + HEX_ESCAPE.toHexDigits((byte)pC) : null;
            case '\f' -> "f";
            case '\r' -> "r";
        };
    }

    private static boolean isAllowedToStartUnquotedString(char pC) {
        return !canStartNumber(pC);
    }

    static boolean canStartNumber(char pC) {
        return switch (pC) {
            case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }

    static boolean needsUnderscoreRemoval(String pText) {
        return pText.indexOf(95) != -1;
    }

    private static void cleanAndAppend(StringBuilder pStringBuilder, String pText) {
        cleanAndAppend(pStringBuilder, pText, needsUnderscoreRemoval(pText));
    }

    static void cleanAndAppend(StringBuilder pStringBuilder, String pText, boolean pRemoveUnderscores) {
        if (pRemoveUnderscores) {
            for (char c0 : pText.toCharArray()) {
                if (c0 != '_') {
                    pStringBuilder.append(c0);
                }
            }
        } else {
            pStringBuilder.append(pText);
        }
    }

    static short parseUnsignedShort(String pText, int pRadix) {
        int i = Integer.parseInt(pText, pRadix);
        if (i >> 16 == 0) {
            return (short)i;
        } else {
            throw new NumberFormatException("out of range: " + i);
        }
    }

    @Nullable
    private static <T> T createFloat(
        DynamicOps<T> pOps,
        SnbtGrammar.Sign pSign,
        @Nullable String pWholePart,
        @Nullable String pFractionPart,
        @Nullable SnbtGrammar.Signed<String> pExponentPart,
        @Nullable SnbtGrammar.TypeSuffix pSuffix,
        ParseState<?> pParseState
    ) {
        StringBuilder stringbuilder = new StringBuilder();
        pSign.append(stringbuilder);
        if (pWholePart != null) {
            cleanAndAppend(stringbuilder, pWholePart);
        }

        if (pFractionPart != null) {
            stringbuilder.append('.');
            cleanAndAppend(stringbuilder, pFractionPart);
        }

        if (pExponentPart != null) {
            stringbuilder.append('e');
            pExponentPart.sign().append(stringbuilder);
            cleanAndAppend(stringbuilder, pExponentPart.value);
        }

        try {
            String s = stringbuilder.toString();

            return (T)(switch (pSuffix) {
                case null -> (Object)convertDouble(pOps, pParseState, s);
                case FLOAT -> (Object)convertFloat(pOps, pParseState, s);
                case DOUBLE -> (Object)convertDouble(pOps, pParseState, s);
                default -> {
                    pParseState.errorCollector().store(pParseState.mark(), ERROR_EXPECTED_FLOAT_TYPE);
                    yield null;
                }
            });
        } catch (NumberFormatException numberformatexception) {
            pParseState.errorCollector().store(pParseState.mark(), createNumberParseError(numberformatexception));
            return null;
        }
    }

    @Nullable
    private static <T> T convertFloat(DynamicOps<T> pOps, ParseState<?> pParseState, String pValue) {
        float f = Float.parseFloat(pValue);
        if (!Float.isFinite(f)) {
            pParseState.errorCollector().store(pParseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        } else {
            return pOps.createFloat(f);
        }
    }

    @Nullable
    private static <T> T convertDouble(DynamicOps<T> pOps, ParseState<?> pParseState, String pValue) {
        double d0 = Double.parseDouble(pValue);
        if (!Double.isFinite(d0)) {
            pParseState.errorCollector().store(pParseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        } else {
            return pOps.createDouble(d0);
        }
    }

    private static String joinList(List<String> pList) {
        return switch (pList.size()) {
            case 0 -> "";
            case 1 -> (String)pList.getFirst();
            default -> String.join("", pList);
        };
    }

    public static <T> Grammar<T> createParser(DynamicOps<T> pOps) {
        T t = pOps.createBoolean(true);
        T t1 = pOps.createBoolean(false);
        T t2 = pOps.emptyMap();
        T t3 = pOps.emptyList();
        Dictionary<StringReader> dictionary = new Dictionary<>();
        Atom<SnbtGrammar.Sign> atom = Atom.of("sign");
        dictionary.put(
            atom,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('+'), Term.marker(atom, SnbtGrammar.Sign.PLUS)),
                Term.sequence(StringReaderTerms.character('-'), Term.marker(atom, SnbtGrammar.Sign.MINUS))
            ),
            p_392282_ -> p_392282_.getOrThrow(atom)
        );
        Atom<SnbtGrammar.IntegerSuffix> atom1 = Atom.of("integer_suffix");
        dictionary.put(
            atom1,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.characters('u', 'U'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(
                    StringReaderTerms.characters('s', 'S'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.BYTE))),
                Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.SHORT))),
                Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.INT))),
                Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.LONG)))
            ),
            p_395615_ -> p_395615_.getOrThrow(atom1)
        );
        Atom<String> atom2 = Atom.of("binary_numeral");
        dictionary.put(atom2, BINARY_NUMERAL);
        Atom<String> atom3 = Atom.of("decimal_numeral");
        dictionary.put(atom3, DECIMAL_NUMERAL);
        Atom<String> atom4 = Atom.of("hex_numeral");
        dictionary.put(atom4, HEX_NUMERAL);
        Atom<SnbtGrammar.IntegerLiteral> atom5 = Atom.of("integer_literal");
        NamedRule<StringReader, SnbtGrammar.IntegerLiteral> namedrule = dictionary.put(
            atom5,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        StringReaderTerms.character('0'),
                        Term.cut(),
                        Term.alternative(
                            Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), dictionary.named(atom4)),
                            Term.sequence(StringReaderTerms.characters('b', 'B'), dictionary.named(atom2)),
                            Term.sequence(dictionary.named(atom3), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)),
                            Term.marker(atom3, "0")
                        )
                    ),
                    dictionary.named(atom3)
                ),
                Term.optional(dictionary.named(atom1))
            ),
            p_397578_ -> {
                SnbtGrammar.IntegerSuffix snbtgrammar$integersuffix = p_397578_.getOrDefault(atom1, SnbtGrammar.IntegerSuffix.EMPTY);
                SnbtGrammar.Sign snbtgrammar$sign = p_397578_.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
                String s = p_397578_.get(atom3);
                if (s != null) {
                    return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.DECIMAL, s, snbtgrammar$integersuffix);
                } else {
                    String s1 = p_397578_.get(atom4);
                    if (s1 != null) {
                        return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.HEX, s1, snbtgrammar$integersuffix);
                    } else {
                        String s2 = p_397578_.getOrThrow(atom2);
                        return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.BINARY, s2, snbtgrammar$integersuffix);
                    }
                }
            }
        );
        Atom<SnbtGrammar.TypeSuffix> atom6 = Atom.of("float_type_suffix");
        dictionary.put(
            atom6,
            Term.alternative(
                Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(atom6, SnbtGrammar.TypeSuffix.FLOAT)),
                Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(atom6, SnbtGrammar.TypeSuffix.DOUBLE))
            ),
            p_395651_ -> p_395651_.getOrThrow(atom6)
        );
        Atom<SnbtGrammar.Signed<String>> atom7 = Atom.of("float_exponent_part");
        dictionary.put(
            atom7,
            Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(dictionary.named(atom)), dictionary.named(atom3)),
            p_396537_ -> new SnbtGrammar.Signed<>(p_396537_.getOrDefault(atom, SnbtGrammar.Sign.PLUS), p_396537_.getOrThrow(atom3))
        );
        Atom<String> atom8 = Atom.of("float_whole_part");
        Atom<String> atom9 = Atom.of("float_fraction_part");
        Atom<T> atom10 = Atom.of("float_literal");
        dictionary.putComplex(
            atom10,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        dictionary.namedWithAlias(atom3, atom8),
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        Term.optional(dictionary.namedWithAlias(atom3, atom9)),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        dictionary.namedWithAlias(atom3, atom9),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(
                        dictionary.namedWithAlias(atom3, atom8), dictionary.named(atom7), Term.cut(), Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(dictionary.namedWithAlias(atom3, atom8), Term.optional(dictionary.named(atom7)), dictionary.named(atom6))
                )
            ),
            p_392830_ -> {
                Scope scope = p_392830_.scope();
                SnbtGrammar.Sign snbtgrammar$sign = scope.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
                String s = scope.get(atom8);
                String s1 = scope.get(atom9);
                SnbtGrammar.Signed<String> signed = scope.get(atom7);
                SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = scope.get(atom6);
                return createFloat(pOps, snbtgrammar$sign, s, s1, signed, snbtgrammar$typesuffix, p_392830_);
            }
        );
        Atom<String> atom11 = Atom.of("string_hex_2");
        dictionary.put(atom11, new SnbtGrammar.SimpleHexLiteralParseRule(2));
        Atom<String> atom12 = Atom.of("string_hex_4");
        dictionary.put(atom12, new SnbtGrammar.SimpleHexLiteralParseRule(4));
        Atom<String> atom13 = Atom.of("string_hex_8");
        dictionary.put(atom13, new SnbtGrammar.SimpleHexLiteralParseRule(8));
        Atom<String> atom14 = Atom.of("string_unicode_name");
        dictionary.put(atom14, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
        Atom<String> atom15 = Atom.of("string_escape_sequence");
        dictionary.putComplex(
            atom15,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('b'), Term.marker(atom15, "\b")),
                Term.sequence(StringReaderTerms.character('s'), Term.marker(atom15, " ")),
                Term.sequence(StringReaderTerms.character('t'), Term.marker(atom15, "\t")),
                Term.sequence(StringReaderTerms.character('n'), Term.marker(atom15, "\n")),
                Term.sequence(StringReaderTerms.character('f'), Term.marker(atom15, "\f")),
                Term.sequence(StringReaderTerms.character('r'), Term.marker(atom15, "\r")),
                Term.sequence(StringReaderTerms.character('\\'), Term.marker(atom15, "\\")),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom15, "'")),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom15, "\"")),
                Term.sequence(StringReaderTerms.character('x'), dictionary.named(atom11)),
                Term.sequence(StringReaderTerms.character('u'), dictionary.named(atom12)),
                Term.sequence(StringReaderTerms.character('U'), dictionary.named(atom13)),
                Term.sequence(
                    StringReaderTerms.character('N'), StringReaderTerms.character('{'), dictionary.named(atom14), StringReaderTerms.character('}')
                )
            ),
            p_395504_ -> {
                Scope scope = p_395504_.scope();
                String s = scope.getAny(atom15);
                if (s != null) {
                    return s;
                } else {
                    String s1 = scope.getAny(atom11, atom12, atom13);
                    if (s1 != null) {
                        int j = HexFormat.fromHexDigits(s1);
                        if (!Character.isValidCodePoint(j)) {
                            p_395504_.errorCollector()
                                .store(p_395504_.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", j)));
                            return null;
                        } else {
                            return Character.toString(j);
                        }
                    } else {
                        String s2 = scope.getOrThrow(atom14);

                        int i;
                        try {
                            i = Character.codePointOf(s2);
                        } catch (IllegalArgumentException illegalargumentexception) {
                            p_395504_.errorCollector().store(p_395504_.mark(), ERROR_INVALID_CHARACTER_NAME);
                            return null;
                        }

                        return Character.toString(i);
                    }
                }
            }
        );
        Atom<String> atom16 = Atom.of("string_plain_contents");
        dictionary.put(atom16, PLAIN_STRING_CHUNK);
        Atom<List<String>> atom17 = Atom.of("string_chunks");
        Atom<String> atom18 = Atom.of("string_contents");
        Atom<String> atom19 = Atom.of("single_quoted_string_chunk");
        NamedRule<StringReader, String> namedrule1 = dictionary.put(
            atom19,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom18, "\""))
            ),
            p_393541_ -> p_393541_.getOrThrow(atom18)
        );
        Atom<String> atom20 = Atom.of("single_quoted_string_contents");
        dictionary.put(atom20, Term.repeated(namedrule1, atom17), p_391866_ -> joinList(p_391866_.getOrThrow(atom17)));
        Atom<String> atom21 = Atom.of("double_quoted_string_chunk");
        NamedRule<StringReader, String> namedrule2 = dictionary.put(
            atom21,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom18, "'"))
            ),
            p_396068_ -> p_396068_.getOrThrow(atom18)
        );
        Atom<String> atom22 = Atom.of("double_quoted_string_contents");
        dictionary.put(atom22, Term.repeated(namedrule2, atom17), p_397848_ -> joinList(p_397848_.getOrThrow(atom17)));
        Atom<String> atom23 = Atom.of("quoted_string_literal");
        dictionary.put(
            atom23,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.character('"'), Term.cut(), Term.optional(dictionary.namedWithAlias(atom22, atom18)), StringReaderTerms.character('"')
                ),
                Term.sequence(StringReaderTerms.character('\''), Term.optional(dictionary.namedWithAlias(atom20, atom18)), StringReaderTerms.character('\''))
            ),
            p_396331_ -> p_396331_.getOrThrow(atom18)
        );
        Atom<String> atom24 = Atom.of("unquoted_string");
        dictionary.put(atom24, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
        Atom<T> atom25 = Atom.of("literal");
        Atom<List<T>> atom26 = Atom.of("arguments");
        dictionary.put(
            atom26, Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom26, StringReaderTerms.character(',')), p_397099_ -> p_397099_.getOrThrow(atom26)
        );
        Atom<T> atom27 = Atom.of("unquoted_string_or_builtin");
        dictionary.putComplex(
            atom27,
            Term.sequence(
                dictionary.named(atom24),
                Term.optional(Term.sequence(StringReaderTerms.character('('), dictionary.named(atom26), StringReaderTerms.character(')')))
            ),
            p_394493_ -> {
                Scope scope = p_394493_.scope();
                String s = scope.getOrThrow(atom24);
                if (!s.isEmpty() && isAllowedToStartUnquotedString(s.charAt(0))) {
                    List<T> list = scope.get(atom26);
                    if (list != null) {
                        SnbtOperations.BuiltinKey snbtoperations$builtinkey = new SnbtOperations.BuiltinKey(s, list.size());
                        SnbtOperations.BuiltinOperation snbtoperations$builtinoperation = SnbtOperations.BUILTIN_OPERATIONS.get(snbtoperations$builtinkey);
                        if (snbtoperations$builtinoperation != null) {
                            return snbtoperations$builtinoperation.run(pOps, list, p_394493_);
                        } else {
                            p_394493_.errorCollector().store(p_394493_.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, snbtoperations$builtinkey.toString()));
                            return null;
                        }
                    } else if (s.equalsIgnoreCase("true")) {
                        return t;
                    } else {
                        return s.equalsIgnoreCase("false") ? t1 : pOps.createString(s);
                    }
                } else {
                    p_394493_.errorCollector().store(p_394493_.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
                    return null;
                }
            }
        );
        Atom<String> atom28 = Atom.of("map_key");
        dictionary.put(
            atom28, Term.alternative(dictionary.named(atom23), dictionary.named(atom24)), p_391933_ -> p_391933_.getAnyOrThrow(atom23, atom24)
        );
        Atom<Entry<String, T>> atom29 = Atom.of("map_entry");
        NamedRule<StringReader, Entry<String, T>> namedrule3 = dictionary.putComplex(
            atom29, Term.sequence(dictionary.named(atom28), StringReaderTerms.character(':'), dictionary.named(atom25)), p_396744_ -> {
                Scope scope = p_396744_.scope();
                String s = scope.getOrThrow(atom28);
                if (s.isEmpty()) {
                    p_396744_.errorCollector().store(p_396744_.mark(), ERROR_EMPTY_KEY);
                    return null;
                } else {
                    T t4 = scope.getOrThrow(atom25);
                    return Map.entry(s, t4);
                }
            }
        );
        Atom<List<Entry<String, T>>> atom30 = Atom.of("map_entries");
        dictionary.put(atom30, Term.repeatedWithTrailingSeparator(namedrule3, atom30, StringReaderTerms.character(',')), p_398026_ -> p_398026_.getOrThrow(atom30));
        Atom<T> atom31 = Atom.of("map_literal");
        dictionary.put(
            atom31, Term.sequence(StringReaderTerms.character('{'), dictionary.named(atom30), StringReaderTerms.character('}')), p_397471_ -> {
                List<Entry<String, T>> list = p_397471_.getOrThrow(atom30);
                if (list.isEmpty()) {
                    return t2;
                } else {
                    Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(list.size());

                    for (Entry<String, T> entry : list) {
                        builder.put(pOps.createString(entry.getKey()), entry.getValue());
                    }

                    return pOps.createMap(builder.buildKeepingLast());
                }
            }
        );
        Atom<List<T>> atom32 = Atom.of("list_entries");
        dictionary.put(
            atom32, Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom32, StringReaderTerms.character(',')), p_392047_ -> p_392047_.getOrThrow(atom32)
        );
        Atom<SnbtGrammar.ArrayPrefix> atom33 = Atom.of("array_prefix");
        dictionary.put(
            atom33,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('B'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.BYTE)),
                Term.sequence(StringReaderTerms.character('L'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.LONG)),
                Term.sequence(StringReaderTerms.character('I'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.INT))
            ),
            p_393699_ -> p_393699_.getOrThrow(atom33)
        );
        Atom<List<SnbtGrammar.IntegerLiteral>> atom34 = Atom.of("int_array_entries");
        dictionary.put(atom34, Term.repeatedWithTrailingSeparator(namedrule, atom34, StringReaderTerms.character(',')), p_393877_ -> p_393877_.getOrThrow(atom34));
        Atom<T> atom35 = Atom.of("list_literal");
        dictionary.putComplex(
            atom35,
            Term.sequence(
                StringReaderTerms.character('['),
                Term.alternative(
                    Term.sequence(dictionary.named(atom33), StringReaderTerms.character(';'), dictionary.named(atom34)), dictionary.named(atom32)
                ),
                StringReaderTerms.character(']')
            ),
            p_396934_ -> {
                Scope scope = p_396934_.scope();
                SnbtGrammar.ArrayPrefix snbtgrammar$arrayprefix = scope.get(atom33);
                if (snbtgrammar$arrayprefix != null) {
                    List<SnbtGrammar.IntegerLiteral> list1 = scope.getOrThrow(atom34);
                    return list1.isEmpty() ? snbtgrammar$arrayprefix.create(pOps) : snbtgrammar$arrayprefix.create(pOps, list1, p_396934_);
                } else {
                    List<T> list = scope.getOrThrow(atom32);
                    return list.isEmpty() ? t3 : pOps.createList(list.stream());
                }
            }
        );
        NamedRule<StringReader, T> namedrule4 = dictionary.putComplex(
            atom25,
            Term.alternative(
                Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(dictionary.namedWithAlias(atom10, atom25), dictionary.named(atom5))),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('"', '\'')), Term.cut(), dictionary.named(atom23)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), dictionary.namedWithAlias(atom31, atom25)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), dictionary.namedWithAlias(atom35, atom25)),
                dictionary.namedWithAlias(atom27, atom25)
            ),
            p_396574_ -> {
                Scope scope = p_396574_.scope();
                String s = scope.get(atom23);
                if (s != null) {
                    return pOps.createString(s);
                } else {
                    SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral = scope.get(atom5);
                    return snbtgrammar$integerliteral != null ? snbtgrammar$integerliteral.create(pOps, p_396574_) : scope.getOrThrow(atom25);
                }
            }
        );
        return new Grammar<>(dictionary, namedrule4);
    }

    static enum ArrayPrefix {
        BYTE(SnbtGrammar.TypeSuffix.BYTE) {
            private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

            @Override
            public <T> T create(DynamicOps<T> p_391802_) {
                return p_391802_.createByteList(EMPTY_BUFFER);
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_394098_, List<SnbtGrammar.IntegerLiteral> p_396273_, ParseState<?> p_393802_) {
                ByteList bytelist = new ByteArrayList();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_396273_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_393802_);
                    if (number == null) {
                        return null;
                    }

                    bytelist.add(number.byteValue());
                }

                return p_394098_.createByteList(ByteBuffer.wrap(bytelist.toByteArray()));
            }
        },
        INT(SnbtGrammar.TypeSuffix.INT, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT) {
            @Override
            public <T> T create(DynamicOps<T> p_396824_) {
                return p_396824_.createIntList(IntStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_391735_, List<SnbtGrammar.IntegerLiteral> p_396111_, ParseState<?> p_395361_) {
                java.util.stream.IntStream.Builder builder = IntStream.builder();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_396111_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_395361_);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.intValue());
                }

                return p_391735_.createIntList(builder.build());
            }
        },
        LONG(SnbtGrammar.TypeSuffix.LONG, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT, SnbtGrammar.TypeSuffix.INT) {
            @Override
            public <T> T create(DynamicOps<T> p_392954_) {
                return p_392954_.createLongList(LongStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_397499_, List<SnbtGrammar.IntegerLiteral> p_391287_, ParseState<?> p_392056_) {
                java.util.stream.LongStream.Builder builder = LongStream.builder();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_391287_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_392056_);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.longValue());
                }

                return p_397499_.createLongList(builder.build());
            }
        };

        private final SnbtGrammar.TypeSuffix defaultType;
        private final Set<SnbtGrammar.TypeSuffix> additionalTypes;

        ArrayPrefix(final SnbtGrammar.TypeSuffix pDefaultType, final SnbtGrammar.TypeSuffix... pAdditionalTypes) {
            this.additionalTypes = Set.of(pAdditionalTypes);
            this.defaultType = pDefaultType;
        }

        public boolean isAllowed(SnbtGrammar.TypeSuffix pSuffix) {
            return pSuffix == this.defaultType || this.additionalTypes.contains(pSuffix);
        }

        public abstract <T> T create(DynamicOps<T> pOps);

        @Nullable
        public abstract <T> T create(DynamicOps<T> pOps, List<SnbtGrammar.IntegerLiteral> pValues, ParseState<?> pParseState);

        @Nullable
        protected Number buildNumber(SnbtGrammar.IntegerLiteral pValue, ParseState<?> pParseState) {
            SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = this.computeType(pValue.suffix);
            if (snbtgrammar$typesuffix == null) {
                pParseState.errorCollector().store(pParseState.mark(), SnbtGrammar.ERROR_INVALID_ARRAY_ELEMENT_TYPE);
                return null;
            } else {
                return (Number)pValue.create(JavaOps.INSTANCE, snbtgrammar$typesuffix, pParseState);
            }
        }

        @Nullable
        private SnbtGrammar.TypeSuffix computeType(SnbtGrammar.IntegerSuffix pSuffix) {
            SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = pSuffix.type();
            if (snbtgrammar$typesuffix == null) {
                return this.defaultType;
            } else {
                return !this.isAllowed(snbtgrammar$typesuffix) ? null : snbtgrammar$typesuffix;
            }
        }
    }

    static enum Base {
        BINARY,
        DECIMAL,
        HEX;
    }

    record IntegerLiteral(SnbtGrammar.Sign sign, SnbtGrammar.Base base, String digits, SnbtGrammar.IntegerSuffix suffix) {
        private SnbtGrammar.SignedPrefix signedOrDefault() {
            if (this.suffix.signed != null) {
                return this.suffix.signed;
            } else {
                return switch (this.base) {
                    case BINARY, HEX -> SnbtGrammar.SignedPrefix.UNSIGNED;
                    case DECIMAL -> SnbtGrammar.SignedPrefix.SIGNED;
                };
            }
        }

        private String cleanupDigits(SnbtGrammar.Sign pSign) {
            boolean flag = SnbtGrammar.needsUnderscoreRemoval(this.digits);
            if (pSign != SnbtGrammar.Sign.MINUS && !flag) {
                return this.digits;
            } else {
                StringBuilder stringbuilder = new StringBuilder();
                pSign.append(stringbuilder);
                SnbtGrammar.cleanAndAppend(stringbuilder, this.digits, flag);
                return stringbuilder.toString();
            }
        }

        @Nullable
        public <T> T create(DynamicOps<T> pOps, ParseState<?> pParseState) {
            return this.create(pOps, Objects.requireNonNullElse(this.suffix.type, SnbtGrammar.TypeSuffix.INT), pParseState);
        }

        @Nullable
        public <T> T create(DynamicOps<T> pOps, SnbtGrammar.TypeSuffix pTypeSuffix, ParseState<?> pParseState) {
            boolean flag = this.signedOrDefault() == SnbtGrammar.SignedPrefix.SIGNED;
            if (!flag && this.sign == SnbtGrammar.Sign.MINUS) {
                pParseState.errorCollector().store(pParseState.mark(), SnbtGrammar.ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
                return null;
            } else {
                String s = this.cleanupDigits(this.sign);

                int i = switch (this.base) {
                    case BINARY -> 2;
                    case DECIMAL -> 10;
                    case HEX -> 16;
                };

                try {
                    if (flag) {
                        return (T)(switch (pTypeSuffix) {
                            case BYTE -> (Object)pOps.createByte(Byte.parseByte(s, i));
                            case SHORT -> (Object)pOps.createShort(Short.parseShort(s, i));
                            case INT -> (Object)pOps.createInt(Integer.parseInt(s, i));
                            case LONG -> (Object)pOps.createLong(Long.parseLong(s, i));
                            default -> {
                                pParseState.errorCollector().store(pParseState.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                                yield null;
                            }
                        });
                    } else {
                        return (T)(switch (pTypeSuffix) {
                            case BYTE -> (Object)pOps.createByte(UnsignedBytes.parseUnsignedByte(s, i));
                            case SHORT -> (Object)pOps.createShort(SnbtGrammar.parseUnsignedShort(s, i));
                            case INT -> (Object)pOps.createInt(Integer.parseUnsignedInt(s, i));
                            case LONG -> (Object)pOps.createLong(Long.parseUnsignedLong(s, i));
                            default -> {
                                pParseState.errorCollector().store(pParseState.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                                yield null;
                            }
                        });
                    }
                } catch (NumberFormatException numberformatexception) {
                    pParseState.errorCollector().store(pParseState.mark(), SnbtGrammar.createNumberParseError(numberformatexception));
                    return null;
                }
            }
        }
    }

    record IntegerSuffix(@Nullable SnbtGrammar.SignedPrefix signed, @Nullable SnbtGrammar.TypeSuffix type) {
        public static final SnbtGrammar.IntegerSuffix EMPTY = new SnbtGrammar.IntegerSuffix(null, null);
    }

    static enum Sign {
        PLUS,
        MINUS;

        public void append(StringBuilder pStringBuilder) {
            if (this == MINUS) {
                pStringBuilder.append("-");
            }
        }
    }

    record Signed<T>(SnbtGrammar.Sign sign, T value) {
    }

    static enum SignedPrefix {
        SIGNED,
        UNSIGNED;
    }

    static class SimpleHexLiteralParseRule extends GreedyPredicateParseRule {
        public SimpleHexLiteralParseRule(int pMinSize) {
            super(pMinSize, pMinSize, DelayedException.create(SnbtGrammar.ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(pMinSize)));
        }

        @Override
        protected boolean isAccepted(char p_396864_) {
            return switch (p_396864_) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    }

    static enum TypeSuffix {
        FLOAT,
        DOUBLE,
        BYTE,
        SHORT,
        INT,
        LONG;
    }
}