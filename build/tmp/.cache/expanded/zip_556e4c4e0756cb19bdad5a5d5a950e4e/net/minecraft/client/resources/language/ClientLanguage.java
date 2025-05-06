package net.minecraft.client.resources.language;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLanguage extends Language {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, String> storage;
    private final boolean defaultRightToLeft;

    private ClientLanguage(Map<String, String> pStorage, boolean pDefaultRightToLeft) {
        this.storage = pStorage;
        this.defaultRightToLeft = pDefaultRightToLeft;
    }

    public static ClientLanguage loadFrom(ResourceManager pResourceManager, List<String> pFilenames, boolean pDefaultRightToLeft) {
        Map<String, String> map = new HashMap<>();

        for (String s : pFilenames) {
            String s1 = String.format(Locale.ROOT, "lang/%s.json", s);

            for (String s2 : pResourceManager.getNamespaces()) {
                try {
                    ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(s2, s1);
                    appendFrom(s, pResourceManager.getResourceStack(resourcelocation), map);
                } catch (Exception exception) {
                    LOGGER.warn("Skipped language file: {}:{} ({})", s2, s1, exception.toString());
                }
            }
        }

        DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(map);
        return new ClientLanguage(Map.copyOf(map), pDefaultRightToLeft);
    }

    private static void appendFrom(String pLanguageName, List<Resource> pResources, Map<String, String> pDestinationMap) {
        for (Resource resource : pResources) {
            try (InputStream inputstream = resource.open()) {
                Language.loadFromJson(inputstream, pDestinationMap::put);
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to load translations for {} from pack {}", pLanguageName, resource.sourcePackId(), ioexception);
            }
        }
    }

    @Override
    public String getOrDefault(String p_118920_, String p_265273_) {
        return this.storage.getOrDefault(p_118920_, p_265273_);
    }

    @Override
    public boolean has(String p_118928_) {
        return this.storage.containsKey(p_118928_);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return this.defaultRightToLeft;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText p_118925_) {
        return FormattedBidiReorder.reorder(p_118925_, this.defaultRightToLeft);
    }

    @Override
    public Map<String, String> getLanguageData() {
        return this.storage;
    }
}
