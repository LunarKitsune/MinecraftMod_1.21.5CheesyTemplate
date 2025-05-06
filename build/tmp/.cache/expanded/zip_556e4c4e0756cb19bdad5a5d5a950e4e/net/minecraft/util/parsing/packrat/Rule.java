package net.minecraft.util.parsing.packrat;

import javax.annotation.Nullable;

public interface Rule<S, T> {
    @Nullable
    T parse(ParseState<S> pParseState);

    static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.RuleAction<S, T> pAction) {
        return new Rule.WrappedTerm<>(pAction, pChild);
    }

    static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.SimpleRuleAction<S, T> pAction) {
        return new Rule.WrappedTerm<>(pAction, pChild);
    }

    @FunctionalInterface
    public interface RuleAction<S, T> {
        @Nullable
        T run(ParseState<S> pParseState);
    }

    @FunctionalInterface
    public interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
        T run(Scope pScope);

        @Override
        default T run(ParseState<S> p_392774_) {
            return this.run(p_392774_.scope());
        }
    }

    public record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
        @Nullable
        @Override
        public T parse(ParseState<S> p_328860_) {
            Scope scope = p_328860_.scope();
            scope.pushFrame();

            Object object;
            try {
                if (!this.child.parse(p_328860_, scope, Control.UNBOUND)) {
                    return null;
                }

                object = this.action.run(p_328860_);
            } finally {
                scope.popFrame();
            }

            return (T)object;
        }
    }
}