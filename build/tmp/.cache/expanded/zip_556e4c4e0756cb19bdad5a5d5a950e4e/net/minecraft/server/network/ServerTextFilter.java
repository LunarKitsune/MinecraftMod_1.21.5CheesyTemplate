package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.slf4j.Logger;

public abstract class ServerTextFilter implements AutoCloseable {
    protected static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ThreadFactory THREAD_FACTORY = p_363933_ -> {
        Thread thread = new Thread(p_363933_);
        thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
        return thread;
    };
    private final URL chatEndpoint;
    private final ServerTextFilter.MessageEncoder chatEncoder;
    final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy;
    final ExecutorService workerPool;

    protected static ExecutorService createWorkerPool(int pSize) {
        return Executors.newFixedThreadPool(pSize, THREAD_FACTORY);
    }

    protected ServerTextFilter(URL pChatEndpoint, ServerTextFilter.MessageEncoder pChatEncoder, ServerTextFilter.IgnoreStrategy pChatIgnoreStrategy, ExecutorService pWorkerPool) {
        this.chatIgnoreStrategy = pChatIgnoreStrategy;
        this.workerPool = pWorkerPool;
        this.chatEndpoint = pChatEndpoint;
        this.chatEncoder = pChatEncoder;
    }

    protected static URL getEndpoint(URI pApiServer, @Nullable JsonObject pJson, String pKey, String pFallback) throws MalformedURLException {
        String s = getEndpointFromConfig(pJson, pKey, pFallback);
        return pApiServer.resolve("/" + s).toURL();
    }

    protected static String getEndpointFromConfig(@Nullable JsonObject pJson, String pKey, String pFallback) {
        return pJson != null ? GsonHelper.getAsString(pJson, pKey, pFallback) : pFallback;
    }

    @Nullable
    public static ServerTextFilter createFromConfig(DedicatedServerProperties pConfig) {
        String s = pConfig.textFilteringConfig;
        if (StringUtil.isBlank(s)) {
            return null;
        } else {
            return switch (pConfig.textFilteringVersion) {
                case 0 -> LegacyTextFilter.createTextFilterFromConfig(s);
                case 1 -> PlayerSafetyServiceTextFilter.createTextFilterFromConfig(s);
                default -> {
                    LOGGER.warn("Could not create text filter - unsupported text filtering version used");
                    yield null;
                }
            };
        }
    }

    protected CompletableFuture<FilteredText> requestMessageProcessing(GameProfile pProfile, String pFilter, ServerTextFilter.IgnoreStrategy pChatIgnoreStrategy, Executor pStreamExecutor) {
        return pFilter.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
            JsonObject jsonobject = this.chatEncoder.encode(pProfile, pFilter);

            try {
                JsonObject jsonobject1 = this.processRequestResponse(jsonobject, this.chatEndpoint);
                return this.filterText(pFilter, pChatIgnoreStrategy, jsonobject1);
            } catch (Exception exception) {
                LOGGER.warn("Failed to validate message '{}'", pFilter, exception);
                return FilteredText.fullyFiltered(pFilter);
            }
        }, pStreamExecutor);
    }

    protected abstract FilteredText filterText(String pText, ServerTextFilter.IgnoreStrategy pIgnoreStrategy, JsonObject pResponse);

    protected FilterMask parseMask(String pText, JsonArray pHashes, ServerTextFilter.IgnoreStrategy pIgnoreStrategy) {
        if (pHashes.isEmpty()) {
            return FilterMask.PASS_THROUGH;
        } else if (pIgnoreStrategy.shouldIgnore(pText, pHashes.size())) {
            return FilterMask.FULLY_FILTERED;
        } else {
            FilterMask filtermask = new FilterMask(pText.length());

            for (int i = 0; i < pHashes.size(); i++) {
                filtermask.setFiltered(pHashes.get(i).getAsInt());
            }

            return filtermask;
        }
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }

    protected void drainStream(InputStream pStream) throws IOException {
        byte[] abyte = new byte[1024];

        while (pStream.read(abyte) != -1) {
        }
    }

    private JsonObject processRequestResponse(JsonObject pRequest, URL pEndpoint) throws IOException {
        HttpURLConnection httpurlconnection = this.makeRequest(pRequest, pEndpoint);

        JsonObject jsonobject;
        try (InputStream inputstream = httpurlconnection.getInputStream()) {
            if (httpurlconnection.getResponseCode() == 204) {
                return new JsonObject();
            }

            try {
                jsonobject = Streams.parse(new JsonReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8))).getAsJsonObject();
            } finally {
                this.drainStream(inputstream);
            }
        }

        return jsonobject;
    }

    protected HttpURLConnection makeRequest(JsonObject pRequest, URL pEndpoint) throws IOException {
        HttpURLConnection httpurlconnection = this.getURLConnection(pEndpoint);
        this.setAuthorizationProperty(httpurlconnection);
        OutputStreamWriter outputstreamwriter = new OutputStreamWriter(httpurlconnection.getOutputStream(), StandardCharsets.UTF_8);

        try (JsonWriter jsonwriter = new JsonWriter(outputstreamwriter)) {
            Streams.write(pRequest, jsonwriter);
        } catch (Throwable throwable1) {
            try {
                outputstreamwriter.close();
            } catch (Throwable throwable) {
                throwable1.addSuppressed(throwable);
            }

            throw throwable1;
        }

        outputstreamwriter.close();
        int i = httpurlconnection.getResponseCode();
        if (i >= 200 && i < 300) {
            return httpurlconnection;
        } else {
            throw new ServerTextFilter.RequestFailedException(i + " " + httpurlconnection.getResponseMessage());
        }
    }

    protected abstract void setAuthorizationProperty(HttpURLConnection pConnection);

    protected int connectionReadTimeout() {
        return 2000;
    }

    protected HttpURLConnection getURLConnection(URL pUrl) throws IOException {
        HttpURLConnection httpurlconnection = (HttpURLConnection)pUrl.openConnection();
        httpurlconnection.setConnectTimeout(15000);
        httpurlconnection.setReadTimeout(this.connectionReadTimeout());
        httpurlconnection.setUseCaches(false);
        httpurlconnection.setDoOutput(true);
        httpurlconnection.setDoInput(true);
        httpurlconnection.setRequestMethod("POST");
        httpurlconnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        httpurlconnection.setRequestProperty("Accept", "application/json");
        httpurlconnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().getName());
        return httpurlconnection;
    }

    public TextFilter createContext(GameProfile pProfile) {
        return new ServerTextFilter.PlayerContext(pProfile);
    }

    @FunctionalInterface
    public interface IgnoreStrategy {
        ServerTextFilter.IgnoreStrategy NEVER_IGNORE = (p_367292_, p_369668_) -> false;
        ServerTextFilter.IgnoreStrategy IGNORE_FULLY_FILTERED = (p_365373_, p_369783_) -> p_365373_.length() == p_369783_;

        static ServerTextFilter.IgnoreStrategy ignoreOverThreshold(int pTheshold) {
            return (p_360722_, p_360874_) -> p_360874_ >= pTheshold;
        }

        static ServerTextFilter.IgnoreStrategy select(int pThreshold) {
            return switch (pThreshold) {
                case -1 -> NEVER_IGNORE;
                case 0 -> IGNORE_FULLY_FILTERED;
                default -> ignoreOverThreshold(pThreshold);
            };
        }

        boolean shouldIgnore(String pText, int pNumHashes);
    }

    @FunctionalInterface
    protected interface MessageEncoder {
        JsonObject encode(GameProfile pProfile, String pMessage);
    }

    protected class PlayerContext implements TextFilter {
        protected final GameProfile profile;
        protected final Executor streamExecutor;

        protected PlayerContext(final GameProfile pProfile) {
            this.profile = pProfile;
            ConsecutiveExecutor consecutiveexecutor = new ConsecutiveExecutor(ServerTextFilter.this.workerPool, "chat stream for " + pProfile.getName());
            this.streamExecutor = consecutiveexecutor::schedule;
        }

        @Override
        public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> p_369024_) {
            List<CompletableFuture<FilteredText>> list = p_369024_.stream()
                .map(p_369716_ -> ServerTextFilter.this.requestMessageProcessing(this.profile, p_369716_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor))
                .collect(ImmutableList.toImmutableList());
            return Util.sequenceFailFast(list).exceptionally(p_362043_ -> ImmutableList.of());
        }

        @Override
        public CompletableFuture<FilteredText> processStreamMessage(String p_366352_) {
            return ServerTextFilter.this.requestMessageProcessing(this.profile, p_366352_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor);
        }
    }

    protected static class RequestFailedException extends RuntimeException {
        protected RequestFailedException(String pMessage) {
            super(pMessage);
        }
    }
}