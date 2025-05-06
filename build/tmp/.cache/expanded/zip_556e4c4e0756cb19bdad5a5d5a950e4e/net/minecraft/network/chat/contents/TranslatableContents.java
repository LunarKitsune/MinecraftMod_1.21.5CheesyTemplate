package net.minecraft.network.chat.contents;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;

public class TranslatableContents implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];
    private static final Codec<Object> PRIMITIVE_ARG_CODEC = ExtraCodecs.JAVA.validate(TranslatableContents::filterAllowedArguments);
    private static final Codec<Object> ARG_CODEC = Codec.either(PRIMITIVE_ARG_CODEC, ComponentSerialization.CODEC)
        .xmap(
            p_309461_ -> p_309461_.map(p_311360_ -> p_311360_, p_311088_ -> Objects.requireNonNullElse(p_311088_.tryCollapseToString(), p_311088_)),
            p_309556_ -> p_309556_ instanceof Component component ? Either.right(component) : Either.left(p_309556_)
        );
    public static final MapCodec<TranslatableContents> CODEC = RecordCodecBuilder.mapCodec(
        p_326087_ -> p_326087_.group(
                Codec.STRING.fieldOf("translate").forGetter(p_309788_ -> p_309788_.key),
                Codec.STRING.lenientOptionalFieldOf("fallback").forGetter(p_310035_ -> Optional.ofNullable(p_310035_.fallback)),
                ARG_CODEC.listOf().optionalFieldOf("with").forGetter(p_309865_ -> adjustArgs(p_309865_.args))
            )
            .apply(p_326087_, TranslatableContents::create)
    );
    public static final ComponentContents.Type<TranslatableContents> TYPE = new ComponentContents.Type<>(CODEC, "translatable");
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    @Nullable
    private Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private static DataResult<Object> filterAllowedArguments(@Nullable Object pInput) {
        return !isAllowedPrimitiveArgument(pInput) ? DataResult.error(() -> "This value needs to be parsed as component") : DataResult.success(pInput);
    }

    public static boolean isAllowedPrimitiveArgument(@Nullable Object pInput) {
        return pInput instanceof Number || pInput instanceof Boolean || pInput instanceof String;
    }

    private static Optional<List<Object>> adjustArgs(Object[] pArgs) {
        return pArgs.length == 0 ? Optional.empty() : Optional.of(Arrays.asList(pArgs));
    }

    private static Object[] adjustArgs(Optional<List<Object>> pArgs) {
        return pArgs.<Object[]>map(p_309407_ -> p_309407_.isEmpty() ? NO_ARGS : p_309407_.toArray()).orElse(NO_ARGS);
    }

    private static TranslatableContents create(String pKey, Optional<String> pFallback, Optional<List<Object>> pArgs) {
        return new TranslatableContents(pKey, pFallback.orElse(null), adjustArgs(pArgs));
    }

    public TranslatableContents(String pKey, @Nullable String pFallback, Object[] pArgs) {
        this.key = pKey;
        this.fallback = pFallback;
        this.args = pArgs;
    }

    @Override
    public ComponentContents.Type<?> type() {
        return TYPE;
    }

    private void decompose() {
        Language language = Language.getInstance();
        if (language != this.decomposedWith) {
            this.decomposedWith = language;
            String s = this.fallback != null ? language.getOrDefault(this.key, this.fallback) : language.getOrDefault(this.key);

            try {
                Builder<FormattedText> builder = ImmutableList.builder();
                this.decomposeTemplate(s, builder::add);
                this.decomposedParts = builder.build();
            } catch (TranslatableFormatException translatableformatexception) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(s));
            }
        }
    }

    private void decomposeTemplate(String pFormatTemplate, Consumer<FormattedText> pConsumer) {
        Matcher matcher = FORMAT_PATTERN.matcher(pFormatTemplate);

        try {
            int i = 0;
            int j = 0;

            while (matcher.find(j)) {
                int k = matcher.start();
                int l = matcher.end();
                if (k > j) {
                    String s = pFormatTemplate.substring(j, k);
                    if (s.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    pConsumer.accept(FormattedText.of(s));
                }

                String s4 = matcher.group(2);
                String s1 = pFormatTemplate.substring(k, l);
                if ("%".equals(s4) && "%%".equals(s1)) {
                    pConsumer.accept(TEXT_PERCENT);
                } else {
                    if (!"s".equals(s4)) {
                        throw new TranslatableFormatException(this, "Unsupported format: '" + s1 + "'");
                    }

                    String s2 = matcher.group(1);
                    int i1 = s2 != null ? Integer.parseInt(s2) - 1 : i++;
                    pConsumer.accept(this.getArgument(i1));
                }

                j = l;
            }

            if (j == 0) {
                // Forge has some special formatting handlers defined in ForgeI18n, use those if no %s replacements present.
                j = net.minecraftforge.internal.TextComponentMessageFormatHandler.handle(this, pConsumer, this.args, pFormatTemplate);
            }

            if (j < pFormatTemplate.length()) {
                String s3 = pFormatTemplate.substring(j);
                if (s3.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                pConsumer.accept(FormattedText.of(s3));
            }
        } catch (IllegalArgumentException illegalargumentexception) {
            throw new TranslatableFormatException(this, illegalargumentexception);
        }
    }

    private FormattedText getArgument(int pIndex) {
        if (pIndex >= 0 && pIndex < this.args.length) {
            Object object = this.args[pIndex];
            if (object instanceof Component component) {
                return component;
            } else {
                return object == null ? TEXT_NULL : FormattedText.of(object.toString());
            }
        } else {
            throw new TranslatableFormatException(this, pIndex);
        }
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_237521_, Style p_237522_) {
        this.decompose();

        for (FormattedText formattedtext : this.decomposedParts) {
            Optional<T> optional = formattedtext.visit(p_237521_, p_237522_);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_237519_) {
        this.decompose();

        for (FormattedText formattedtext : this.decomposedParts) {
            Optional<T> optional = formattedtext.visit(p_237519_);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack p_237512_, @Nullable Entity p_237513_, int p_237514_) throws CommandSyntaxException {
        Object[] aobject = new Object[this.args.length];

        for (int i = 0; i < aobject.length; i++) {
            Object object = this.args[i];
            if (object instanceof Component component) {
                aobject[i] = ComponentUtils.updateForEntity(p_237512_, component, p_237513_, p_237514_);
            } else {
                aobject[i] = object;
            }
        }

        return MutableComponent.create(new TranslatableContents(this.key, this.fallback, aobject));
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther
            ? true
            : pOther instanceof TranslatableContents translatablecontents
                && Objects.equals(this.key, translatablecontents.key)
                && Objects.equals(this.fallback, translatablecontents.fallback)
                && Arrays.equals(this.args, translatablecontents.args);
    }

    @Override
    public int hashCode() {
        int i = Objects.hashCode(this.key);
        i = 31 * i + Objects.hashCode(this.fallback);
        return 31 * i + Arrays.hashCode(this.args);
    }

    @Override
    public String toString() {
        return "translation{key='"
            + this.key
            + "'"
            + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "")
            + ", args="
            + Arrays.toString(this.args)
            + "}";
    }

    public String getKey() {
        return this.key;
    }

    @Nullable
    public String getFallback() {
        return this.fallback;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
