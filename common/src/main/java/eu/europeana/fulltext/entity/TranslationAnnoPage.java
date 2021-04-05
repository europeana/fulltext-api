package eu.europeana.fulltext.entity;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Reference;

/**
 * AnnoPage stored as translation
 * Created by P.Ehlert on 25 March 2021.
 */
@Entity(value = "TranslationAnnoPage")
public class TranslationAnnoPage extends AnnoPage {

    @Reference
    private TranslationResource res;

}
