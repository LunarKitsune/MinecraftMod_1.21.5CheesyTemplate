package net.minecraft.util.parsing.packrat;

import java.util.Optional;
import javax.annotation.Nullable;

public interface ParseState<S> {
    Scope scope();

    ErrorCollector<S> errorCollector();

    default <T> Optional<T> parseTopRule(NamedRule<S, T> pRule) {
        T t = this.parse(pRule);
        if (t != null) {
            this.errorCollector().finish(this.mark());
        }

        if (!this.scope().hasOnlySingleFrame()) {
            throw new IllegalStateException("Malformed scope: " + this.scope());
        } else {
            return Optional.ofNullable(t);
        }
    }

    @Nullable
    <T> T parse(NamedRule<S, T> pRule);

    S input();

    int mark();

    void restore(int pCursor);

    Control acquireControl();

    void releaseControl();

    ParseState<S> silent();
}