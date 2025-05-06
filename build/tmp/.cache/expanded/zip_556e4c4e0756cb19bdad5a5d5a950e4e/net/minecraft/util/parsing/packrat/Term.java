package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface Term<S> {
    boolean parse(ParseState<S> pParseState, Scope pScope, Control pControl);

    static <S, T> Term<S> marker(Atom<T> pName, T pValue) {
        return new Term.Marker<>(pName, pValue);
    }

    @SafeVarargs
    static <S> Term<S> sequence(Term<S>... pElements) {
        return new Term.Sequence<>(pElements);
    }

    @SafeVarargs
    static <S> Term<S> alternative(Term<S>... pElements) {
        return new Term.Alternative<>(pElements);
    }

    static <S> Term<S> optional(Term<S> pTerm) {
        return new Term.Maybe<>(pTerm);
    }

    static <S, T> Term<S> repeated(NamedRule<S, T> pElement, Atom<List<T>> pListName) {
        return repeated(pElement, pListName, 0);
    }

    static <S, T> Term<S> repeated(NamedRule<S, T> pElement, Atom<List<T>> pListName, int pMinRepetitions) {
        return new Term.Repeated<>(pElement, pListName, pMinRepetitions);
    }

    static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> pElement, Atom<List<T>> pListName, Term<S> pSeperator) {
        return repeatedWithTrailingSeparator(pElement, pListName, pSeperator, 0);
    }

    static <S, T> Term<S> repeatedWithTrailingSeparator(NamedRule<S, T> pElement, Atom<List<T>> pListName, Term<S> pSeperator, int pMinRepetitions) {
        return new Term.RepeatedWithSeparator<>(pElement, pListName, pSeperator, pMinRepetitions, true);
    }

    static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> pElement, Atom<List<T>> pListName, Term<S> pSeperator) {
        return repeatedWithoutTrailingSeparator(pElement, pListName, pSeperator, 0);
    }

    static <S, T> Term<S> repeatedWithoutTrailingSeparator(NamedRule<S, T> pElement, Atom<List<T>> pListName, Term<S> pSeperator, int pMinRepetitions) {
        return new Term.RepeatedWithSeparator<>(pElement, pListName, pSeperator, pMinRepetitions, false);
    }

    static <S> Term<S> positiveLookahead(Term<S> pTerm) {
        return new Term.LookAhead<>(pTerm, true);
    }

    static <S> Term<S> negativeLookahead(Term<S> pTerm) {
        return new Term.LookAhead<>(pTerm, false);
    }

    static <S> Term<S> cut() {
        return new Term<S>() {
            @Override
            public boolean parse(ParseState<S> p_333527_, Scope p_336097_, Control p_335047_) {
                p_335047_.cut();
                return true;
            }

            @Override
            public String toString() {
                return "\u2191";
            }
        };
    }

    static <S> Term<S> empty() {
        return new Term<S>() {
            @Override
            public boolean parse(ParseState<S> p_328418_, Scope p_332040_, Control p_328784_) {
                return true;
            }

            @Override
            public String toString() {
                return "\u03b5";
            }
        };
    }

    static <S> Term<S> fail(final Object pReason) {
        return new Term<S>() {
            @Override
            public boolean parse(ParseState<S> p_394241_, Scope p_396858_, Control p_393969_) {
                p_394241_.errorCollector().store(p_394241_.mark(), pReason);
                return false;
            }

            @Override
            public String toString() {
                return "fail";
            }
        };
    }

    public record Alternative<S>(Term<S>[] elements) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_328094_, Scope p_331753_, Control p_334626_) {
            Control control = p_328094_.acquireControl();

            try {
                int i = p_328094_.mark();
                p_331753_.splitFrame();

                for (Term<S> term : this.elements) {
                    if (term.parse(p_328094_, p_331753_, control)) {
                        p_331753_.mergeFrame();
                        return true;
                    }

                    p_331753_.clearFrameValues();
                    p_328094_.restore(i);
                    if (control.hasCut()) {
                        break;
                    }
                }

                p_331753_.popFrame();
                return false;
            } finally {
                p_328094_.releaseControl();
            }
        }
    }

    public record LookAhead<S>(Term<S> term, boolean positive) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_391493_, Scope p_395827_, Control p_391882_) {
            int i = p_391493_.mark();
            boolean flag = this.term.parse(p_391493_.silent(), p_395827_, p_391882_);
            p_391493_.restore(i);
            return this.positive == flag;
        }
    }

    public record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_332878_, Scope p_331621_, Control p_334053_) {
            p_331621_.put(this.name, this.value);
            return true;
        }
    }

    public record Maybe<S>(Term<S> term) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_332001_, Scope p_329861_, Control p_331352_) {
            int i = p_332001_.mark();
            if (!this.term.parse(p_332001_, p_329861_, p_331352_)) {
                p_332001_.restore(i);
            }

            return true;
        }
    }

    public record Repeated<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, int minRepetitions) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_393223_, Scope p_397132_, Control p_396901_) {
            int i = p_393223_.mark();
            List<T> list = new ArrayList<>(this.minRepetitions);

            while (true) {
                int j = p_393223_.mark();
                T t = p_393223_.parse(this.element);
                if (t == null) {
                    p_393223_.restore(j);
                    if (list.size() < this.minRepetitions) {
                        p_393223_.restore(i);
                        return false;
                    } else {
                        p_397132_.put(this.listName, list);
                        return true;
                    }
                }

                list.add(t);
            }
        }
    }

    public record RepeatedWithSeparator<S, T>(NamedRule<S, T> element, Atom<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator)
        implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_396935_, Scope p_392276_, Control p_393022_) {
            int i = p_396935_.mark();
            List<T> list = new ArrayList<>(this.minRepetitions);
            boolean flag = true;

            while (true) {
                int j = p_396935_.mark();
                if (!flag && !this.separator.parse(p_396935_, p_392276_, p_393022_)) {
                    p_396935_.restore(j);
                    break;
                }

                int k = p_396935_.mark();
                T t = p_396935_.parse(this.element);
                if (t == null) {
                    if (flag) {
                        p_396935_.restore(k);
                    } else {
                        if (!this.allowTrailingSeparator) {
                            p_396935_.restore(i);
                            return false;
                        }

                        p_396935_.restore(k);
                    }
                    break;
                }

                list.add(t);
                flag = false;
            }

            if (list.size() < this.minRepetitions) {
                p_396935_.restore(i);
                return false;
            } else {
                p_392276_.put(this.listName, list);
                return true;
            }
        }
    }

    public record Sequence<S>(Term<S>[] elements) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_330195_, Scope p_336361_, Control p_328798_) {
            int i = p_330195_.mark();

            for (Term<S> term : this.elements) {
                if (!term.parse(p_330195_, p_336361_, p_328798_)) {
                    p_330195_.restore(i);
                    return false;
                }
            }

            return true;
        }
    }
}