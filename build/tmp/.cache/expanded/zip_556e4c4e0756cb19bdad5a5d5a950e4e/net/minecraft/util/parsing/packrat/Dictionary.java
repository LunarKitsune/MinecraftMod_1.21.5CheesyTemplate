package net.minecraft.util.parsing.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Dictionary<S> {
    private final Map<Atom<?>, Dictionary.Entry<S, ?>> terms = new IdentityHashMap<>();

    public <T> NamedRule<S, T> put(Atom<T> pName, Rule<S, T> pRule) {
        Dictionary.Entry<S, T> entry = (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(pName, Dictionary.Entry::new);
        if (entry.value != null) {
            throw new IllegalArgumentException("Trying to override rule: " + pName);
        } else {
            entry.value = pRule;
            return entry;
        }
    }

    public <T> NamedRule<S, T> putComplex(Atom<T> pName, Term<S> pTerm, Rule.RuleAction<S, T> pRuleAction) {
        return this.put(pName, Rule.fromTerm(pTerm, pRuleAction));
    }

    public <T> NamedRule<S, T> put(Atom<T> pName, Term<S> pTerm, Rule.SimpleRuleAction<S, T> pRuleAction) {
        return this.put(pName, Rule.fromTerm(pTerm, pRuleAction));
    }

    public void checkAllBound() {
        List<? extends Atom<?>> list = this.terms.entrySet().stream().filter(p_396134_ -> p_396134_.getValue() == null).map(Map.Entry::getKey).toList();
        if (!list.isEmpty()) {
            throw new IllegalStateException("Unbound names: " + list);
        }
    }

    public <T> NamedRule<S, T> getOrThrow(Atom<T> pName) {
        return (NamedRule<S, T>)Objects.requireNonNull(this.terms.get(pName), () -> "No rule called " + pName);
    }

    public <T> NamedRule<S, T> forward(Atom<T> pName) {
        return this.getOrCreateEntry(pName);
    }

    private <T> Dictionary.Entry<S, T> getOrCreateEntry(Atom<T> pName) {
        return (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(pName, Dictionary.Entry::new);
    }

    public <T> Term<S> named(Atom<T> pName) {
        return new Dictionary.Reference<>(this.getOrCreateEntry(pName), pName);
    }

    public <T> Term<S> namedWithAlias(Atom<T> pName, Atom<T> pAlias) {
        return new Dictionary.Reference<>(this.getOrCreateEntry(pName), pAlias);
    }

    static class Entry<S, T> implements NamedRule<S, T>, Supplier<String> {
        private final Atom<T> name;
        @Nullable
        Rule<S, T> value;

        private Entry(Atom<T> pName) {
            this.name = pName;
        }

        @Override
        public Atom<T> name() {
            return this.name;
        }

        @Override
        public Rule<S, T> value() {
            return Objects.requireNonNull(this.value, this);
        }

        public String get() {
            return "Unbound rule " + this.name;
        }
    }

    record Reference<S, T>(Dictionary.Entry<S, T> ruleToParse, Atom<T> nameToStore) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_397182_, Scope p_391380_, Control p_391695_) {
            T t = p_397182_.parse(this.ruleToParse);
            if (t == null) {
                return false;
            } else {
                p_391380_.put(this.nameToStore, t);
                return true;
            }
        }
    }
}