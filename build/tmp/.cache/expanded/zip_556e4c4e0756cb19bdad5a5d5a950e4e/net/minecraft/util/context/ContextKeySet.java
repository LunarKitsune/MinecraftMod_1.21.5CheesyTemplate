package net.minecraft.util.context;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.Set;

public class ContextKeySet {
    private final Set<ContextKey<?>> required;
    private final Set<ContextKey<?>> allowed;

    ContextKeySet(Set<ContextKey<?>> pRequired, Set<ContextKey<?>> pAllowed) {
        this.required = Set.copyOf(pRequired);
        this.allowed = Set.copyOf(Sets.union(pRequired, pAllowed));
    }

    public Set<ContextKey<?>> required() {
        return this.required;
    }

    public Set<ContextKey<?>> allowed() {
        return this.allowed;
    }

    @Override
    public String toString() {
        return "["
            + Joiner.on(", ")
                .join(this.allowed.stream().map(p_369800_ -> (this.required.contains(p_369800_) ? "!" : "") + p_369800_.name()).iterator())
            + "]";
    }

    public static class Builder {
        private final Set<ContextKey<?>> required = Sets.newIdentityHashSet();
        private final Set<ContextKey<?>> optional = Sets.newIdentityHashSet();

        public ContextKeySet.Builder required(ContextKey<?> pKey) {
            if (this.optional.contains(pKey)) {
                throw new IllegalArgumentException("Parameter " + pKey.name() + " is already optional");
            } else {
                this.required.add(pKey);
                return this;
            }
        }

        public ContextKeySet.Builder optional(ContextKey<?> pKey) {
            if (this.required.contains(pKey)) {
                throw new IllegalArgumentException("Parameter " + pKey.name() + " is already required");
            } else {
                this.optional.add(pKey);
                return this;
            }
        }

        public ContextKeySet build() {
            return new ContextKeySet(this.required, this.optional);
        }
    }
}