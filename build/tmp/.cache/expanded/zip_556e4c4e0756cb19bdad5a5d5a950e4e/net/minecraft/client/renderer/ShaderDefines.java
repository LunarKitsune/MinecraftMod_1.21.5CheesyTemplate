package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
    public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
    public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
        p_369294_ -> p_369294_.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
                Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
            )
            .apply(p_369294_, ShaderDefines::new)
    );

    public static ShaderDefines.Builder builder() {
        return new ShaderDefines.Builder();
    }

    public ShaderDefines withOverrides(ShaderDefines pDefines) {
        if (this.isEmpty()) {
            return pDefines;
        } else if (pDefines.isEmpty()) {
            return this;
        } else {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(this.values.size() + pDefines.values.size());
            builder.putAll(this.values);
            builder.putAll(pDefines.values);
            ImmutableSet.Builder<String> builder1 = ImmutableSet.builderWithExpectedSize(this.flags.size() + pDefines.flags.size());
            builder1.addAll(this.flags);
            builder1.addAll(pDefines.flags);
            return new ShaderDefines(builder.buildKeepingLast(), builder1.build());
        }
    }

    public String asSourceDirectives() {
        StringBuilder stringbuilder = new StringBuilder();

        for (Entry<String, String> entry : this.values.entrySet()) {
            String s = entry.getKey();
            String s1 = entry.getValue();
            stringbuilder.append("#define ").append(s).append(" ").append(s1).append('\n');
        }

        for (String s2 : this.flags) {
            stringbuilder.append("#define ").append(s2).append('\n');
        }

        return stringbuilder.toString();
    }

    public boolean isEmpty() {
        return this.values.isEmpty() && this.flags.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
        private final ImmutableSet.Builder<String> flags = ImmutableSet.builder();

        Builder() {
        }

        public ShaderDefines.Builder define(String pKey, String pValue) {
            if (pValue.isBlank()) {
                throw new IllegalArgumentException("Cannot define empty string");
            } else {
                this.values.put(pKey, escapeNewLines(pValue));
                return this;
            }
        }

        private static String escapeNewLines(String pStr) {
            return pStr.replaceAll("\n", "\\\\\n");
        }

        public ShaderDefines.Builder define(String pKey, float pValue) {
            this.values.put(pKey, String.valueOf(pValue));
            return this;
        }

        public ShaderDefines.Builder define(String pKey, int pValue) {
            this.values.put(pKey, String.valueOf(pValue));
            return this;
        }

        public ShaderDefines.Builder define(String pFlag) {
            this.flags.add(pFlag);
            return this;
        }

        public ShaderDefines build() {
            return new ShaderDefines(this.values.build(), this.flags.build());
        }
    }
}