package net.minecraft.util.context;

import com.google.common.collect.Sets;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;

public class ContextMap {
    private final Map<ContextKey<?>, Object> params;

    ContextMap(Map<ContextKey<?>, Object> pParams) {
        this.params = pParams;
    }

    public boolean has(ContextKey<?> pKey) {
        return this.params.containsKey(pKey);
    }

    public <T> T getOrThrow(ContextKey<T> pKey) {
        T t = (T)this.params.get(pKey);
        if (t == null) {
            throw new NoSuchElementException(pKey.name().toString());
        } else {
            return t;
        }
    }

    @Nullable
    public <T> T getOptional(ContextKey<T> pKey) {
        return (T)this.params.get(pKey);
    }

    @Nullable
    @Contract("_,!null->!null; _,_->_")
    public <T> T getOrDefault(ContextKey<T> pKey, @Nullable T pDefaultValue) {
        return (T)this.params.getOrDefault(pKey, pDefaultValue);
    }

    public Set<ContextKey<?>> keySet() {
        return java.util.Collections.unmodifiableSet(this.params.keySet());
    }

    public static class Builder {
        private final Map<ContextKey<?>, Object> params = new IdentityHashMap<>();

        public <T> ContextMap.Builder withParameter(ContextKey<T> pKey, T pValue) {
            this.params.put(pKey, pValue);
            return this;
        }

        public <T> ContextMap.Builder withOptionalParameter(ContextKey<T> pKey, @Nullable T pValue) {
            if (pValue == null) {
                this.params.remove(pKey);
            } else {
                this.params.put(pKey, pValue);
            }

            return this;
        }

        public <T> T getParameter(ContextKey<T> pKey) {
            T t = (T)this.params.get(pKey);
            if (t == null) {
                throw new NoSuchElementException(pKey.name().toString());
            } else {
                return t;
            }
        }

        @Nullable
        public <T> T getOptionalParameter(ContextKey<T> pKey) {
            return (T)this.params.get(pKey);
        }

        public ContextMap create(ContextKeySet pContextKeySet) {
            Set<ContextKey<?>> set = Sets.difference(this.params.keySet(), pContextKeySet.allowed());
            if (!set.isEmpty()) {
                throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
            } else {
                Set<ContextKey<?>> set1 = Sets.difference(pContextKeySet.required(), this.params.keySet());
                if (!set1.isEmpty()) {
                    throw new IllegalArgumentException("Missing required parameters: " + set1);
                } else {
                    return new ContextMap(this.params);
                }
            }
        }

        public ContextMap create() {
            return new ContextMap(this.params);
        }
    }
}
