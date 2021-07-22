package eu.europeana.fulltext.entity;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;

import java.util.List;

/**
 * Created by luthien on 13/07/2021.
 */
@Entity
public class AnnoPageWithTranslations extends AnnoPage {

    /**
     * Empty constructor required for serialisation
     */
    public AnnoPageWithTranslations() {
    }
//    @Reference
    private List<TranslationAnnoPage> translations;

    public List<TranslationAnnoPage> getTranslations() {
        return translations;
    }

    public void setTranslations(List<TranslationAnnoPage> translations) {
        this.translations = translations;
    }
}
