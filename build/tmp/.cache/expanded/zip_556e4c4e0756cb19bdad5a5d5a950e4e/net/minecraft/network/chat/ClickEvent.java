package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface ClickEvent {
    Codec<ClickEvent> CODEC = ClickEvent.Action.CODEC.dispatch("action", ClickEvent::action, p_389908_ -> p_389908_.codec);

    ClickEvent.Action action();

    public static enum Action implements StringRepresentable {
        OPEN_URL("open_url", true, ClickEvent.OpenUrl.CODEC),
        OPEN_FILE("open_file", false, ClickEvent.OpenFile.CODEC),
        RUN_COMMAND("run_command", true, ClickEvent.RunCommand.CODEC),
        SUGGEST_COMMAND("suggest_command", true, ClickEvent.SuggestCommand.CODEC),
        CHANGE_PAGE("change_page", true, ClickEvent.ChangePage.CODEC),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true, ClickEvent.CopyToClipboard.CODEC);

        public static final Codec<ClickEvent.Action> UNSAFE_CODEC = StringRepresentable.fromEnum(ClickEvent.Action::values);
        public static final Codec<ClickEvent.Action> CODEC = UNSAFE_CODEC.validate(ClickEvent.Action::filterForSerialization);
        private final boolean allowFromServer;
        private final String name;
        final MapCodec<? extends ClickEvent> codec;

        private Action(final String pName, final boolean pAllowFromServer, final MapCodec<? extends ClickEvent> pCodec) {
            this.name = pName;
            this.allowFromServer = pAllowFromServer;
            this.codec = pCodec;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static DataResult<ClickEvent.Action> filterForSerialization(ClickEvent.Action pAction) {
            return !pAction.isAllowedFromServer()
                ? DataResult.error(() -> "Click event type not allowed: " + pAction)
                : DataResult.success(pAction, Lifecycle.stable());
        }
    }

    public record ChangePage(int page) implements ClickEvent {
        public static final MapCodec<ClickEvent.ChangePage> CODEC = RecordCodecBuilder.mapCodec(
            p_396959_ -> p_396959_.group(ExtraCodecs.POSITIVE_INT.fieldOf("page").forGetter(ClickEvent.ChangePage::page))
                .apply(p_396959_, ClickEvent.ChangePage::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.CHANGE_PAGE;
        }
    }

    public record CopyToClipboard(String value) implements ClickEvent {
        public static final MapCodec<ClickEvent.CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec(
            p_398012_ -> p_398012_.group(Codec.STRING.fieldOf("value").forGetter(ClickEvent.CopyToClipboard::value))
                .apply(p_398012_, ClickEvent.CopyToClipboard::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.COPY_TO_CLIPBOARD;
        }
    }

    public record OpenFile(String path) implements ClickEvent {
        public static final MapCodec<ClickEvent.OpenFile> CODEC = RecordCodecBuilder.mapCodec(
            p_391516_ -> p_391516_.group(Codec.STRING.fieldOf("path").forGetter(ClickEvent.OpenFile::path)).apply(p_391516_, ClickEvent.OpenFile::new)
        );

        public OpenFile(File pFile) {
            this(pFile.toString());
        }

        public OpenFile(Path pPath) {
            this(pPath.toFile());
        }

        public File file() {
            return new File(this.path);
        }

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.OPEN_FILE;
        }
    }

    public record OpenUrl(URI uri) implements ClickEvent {
        public static final MapCodec<ClickEvent.OpenUrl> CODEC = RecordCodecBuilder.mapCodec(
            p_395349_ -> p_395349_.group(ExtraCodecs.UNTRUSTED_URI.fieldOf("url").forGetter(ClickEvent.OpenUrl::uri))
                .apply(p_395349_, ClickEvent.OpenUrl::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.OPEN_URL;
        }
    }

    public record RunCommand(String command) implements ClickEvent {
        public static final MapCodec<ClickEvent.RunCommand> CODEC = RecordCodecBuilder.mapCodec(
            p_397684_ -> p_397684_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.RunCommand::command))
                .apply(p_397684_, ClickEvent.RunCommand::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.RUN_COMMAND;
        }
    }

    public record SuggestCommand(String command) implements ClickEvent {
        public static final MapCodec<ClickEvent.SuggestCommand> CODEC = RecordCodecBuilder.mapCodec(
            p_391836_ -> p_391836_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.SuggestCommand::command))
                .apply(p_391836_, ClickEvent.SuggestCommand::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.SUGGEST_COMMAND;
        }
    }
}