package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
    static Term<StringReader> word(String pValue) {
        return new StringReaderTerms.TerminalWord(pValue);
    }

    static Term<StringReader> character(final char pValue) {
        return new StringReaderTerms.TerminalCharacters(CharList.of(pValue)) {
            @Override
            protected boolean isAccepted(char p_391277_) {
                return pValue == p_391277_;
            }
        };
    }

    static Term<StringReader> characters(final char pValue1, final char pValue2) {
        return new StringReaderTerms.TerminalCharacters(CharList.of(pValue1, pValue2)) {
            @Override
            protected boolean isAccepted(char p_393492_) {
                return p_393492_ == pValue1 || p_393492_ == pValue2;
            }
        };
    }

    static StringReader createReader(String pInput, int pCursor) {
        StringReader stringreader = new StringReader(pInput);
        stringreader.setCursor(pCursor);
        return stringreader;
    }

    public abstract static class TerminalCharacters implements Term<StringReader> {
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalCharacters(CharList pCharacters) {
            String s = pCharacters.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), String.valueOf(s));
            this.suggestions = p_392492_ -> pCharacters.intStream().mapToObj(Character::toString);
        }

        @Override
        public boolean parse(ParseState<StringReader> p_393490_, Scope p_391874_, Control p_397093_) {
            p_393490_.input().skipWhitespace();
            int i = p_393490_.mark();
            if (p_393490_.input().canRead() && this.isAccepted(p_393490_.input().read())) {
                return true;
            } else {
                p_393490_.errorCollector().store(i, this.suggestions, this.error);
                return false;
            }
        }

        protected abstract boolean isAccepted(char pC);
    }

    public static final class TerminalWord implements Term<StringReader> {
        private final String value;
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalWord(String pValue) {
            this.value = pValue;
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), pValue);
            this.suggestions = p_390460_ -> Stream.of(pValue);
        }

        @Override
        public boolean parse(ParseState<StringReader> p_333566_, Scope p_332362_, Control p_328812_) {
            p_333566_.input().skipWhitespace();
            int i = p_333566_.mark();
            String s = p_333566_.input().readUnquotedString();
            if (!s.equals(this.value)) {
                p_333566_.errorCollector().store(i, this.suggestions, this.error);
                return false;
            } else {
                return true;
            }
        }

        @Override
        public String toString() {
            return "terminal[" + this.value + "]";
        }
    }
}