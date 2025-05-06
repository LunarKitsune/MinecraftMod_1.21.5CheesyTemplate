package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
    public static final MapCodec<EntityDataSource> SUB_CODEC = RecordCodecBuilder.mapCodec(
        p_309472_ -> p_309472_.group(Codec.STRING.fieldOf("entity").forGetter(EntityDataSource::selectorPattern)).apply(p_309472_, EntityDataSource::new)
    );
    public static final DataSource.Type<EntityDataSource> TYPE = new DataSource.Type<>(SUB_CODEC, "entity");

    public EntityDataSource(String pSelectorPattern) {
        this(pSelectorPattern, compileSelector(pSelectorPattern));
    }

    @Nullable
    private static EntitySelector compileSelector(String pSelectorPattern) {
        try {
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(pSelectorPattern), true);
            return entityselectorparser.parse();
        } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
        }
    }

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack p_237341_) throws CommandSyntaxException {
        if (this.compiledSelector != null) {
            List<? extends Entity> list = this.compiledSelector.findEntities(p_237341_);
            return list.stream().map(NbtPredicate::getEntityTagToCompare);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public DataSource.Type<?> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "entity=" + this.selectorPattern;
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof EntityDataSource entitydatasource && this.selectorPattern.equals(entitydatasource.selectorPattern);
    }

    @Override
    public int hashCode() {
        return this.selectorPattern.hashCode();
    }
}