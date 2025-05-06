package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CommandStorage {
    private static final String ID_PREFIX = "command_storage_";
    private final Map<String, CommandStorage.Container> namespaces = new HashMap<>();
    private final DimensionDataStorage storage;

    public CommandStorage(DimensionDataStorage pStorage) {
        this.storage = pStorage;
    }

    public CompoundTag get(ResourceLocation pId) {
        CommandStorage.Container commandstorage$container = this.getContainer(pId.getNamespace());
        return commandstorage$container != null ? commandstorage$container.get(pId.getPath()) : new CompoundTag();
    }

    @Nullable
    private CommandStorage.Container getContainer(String pNamespace) {
        CommandStorage.Container commandstorage$container = this.namespaces.get(pNamespace);
        if (commandstorage$container != null) {
            return commandstorage$container;
        } else {
            CommandStorage.Container commandstorage$container1 = this.storage.get(CommandStorage.Container.type(pNamespace));
            if (commandstorage$container1 != null) {
                this.namespaces.put(pNamespace, commandstorage$container1);
            }

            return commandstorage$container1;
        }
    }

    private CommandStorage.Container getOrCreateContainer(String pNamespace) {
        CommandStorage.Container commandstorage$container = this.namespaces.get(pNamespace);
        if (commandstorage$container != null) {
            return commandstorage$container;
        } else {
            CommandStorage.Container commandstorage$container1 = this.storage.computeIfAbsent(CommandStorage.Container.type(pNamespace));
            this.namespaces.put(pNamespace, commandstorage$container1);
            return commandstorage$container1;
        }
    }

    public void set(ResourceLocation pId, CompoundTag pNbt) {
        this.getOrCreateContainer(pId.getNamespace()).put(pId.getPath(), pNbt);
    }

    public Stream<ResourceLocation> keys() {
        return this.namespaces.entrySet().stream().flatMap(p_164841_ -> p_164841_.getValue().getKeys(p_164841_.getKey()));
    }

    static String createId(String pNamespace) {
        return "command_storage_" + pNamespace;
    }

    static class Container extends SavedData {
        public static final Codec<CommandStorage.Container> CODEC = RecordCodecBuilder.create(
            p_391107_ -> p_391107_.group(
                    Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(p_391108_ -> p_391108_.storage)
                )
                .apply(p_391107_, CommandStorage.Container::new)
        );
        private final Map<String, CompoundTag> storage;

        private Container(Map<String, CompoundTag> pStorage) {
            this.storage = new HashMap<>(pStorage);
        }

        private Container() {
            this(new HashMap<>());
        }

        public static SavedDataType<CommandStorage.Container> type(String pNamespace) {
            return new SavedDataType<>(CommandStorage.createId(pNamespace), CommandStorage.Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        }

        public CompoundTag get(String pId) {
            CompoundTag compoundtag = this.storage.get(pId);
            return compoundtag != null ? compoundtag : new CompoundTag();
        }

        public void put(String pId, CompoundTag pNbt) {
            if (pNbt.isEmpty()) {
                this.storage.remove(pId);
            } else {
                this.storage.put(pId, pNbt);
            }

            this.setDirty();
        }

        public Stream<ResourceLocation> getKeys(String pNamespace) {
            return this.storage.keySet().stream().map(p_341970_ -> ResourceLocation.fromNamespaceAndPath(pNamespace, p_341970_));
        }
    }
}