package eu.europeana.fulltext.api.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.europeana.fulltext.api.model.JsonLdIdType;

import static eu.europeana.fulltext.api.config.FTDefinitions.INFO_ANNOPAGE_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class AnnotationLangPage extends JsonLdIdType {

    private static final long serialVersionUID = -670619785903826924L;

    private String language;

    @JsonIgnore
    private boolean orig;

    /**
     * This object serves as a placeholder for either an original or translated AnnoPage
     * It is used in the summary info endpoint only
     *
     * @param id    String containing identifying URL of the AnnotationLangPage
     * @param language  String containing language of the AnnotationLangPage
     * @param orig  boolean is this the original language true / false
     */
    public AnnotationLangPage(String id, String language, boolean orig){
        super(id, INFO_ANNOPAGE_TYPE);
        this.language = language;
        this.orig = orig;
    }

    public boolean isOrig() {
        return orig;
    }

    public void setOrig(boolean orig) {
        this.orig = orig;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


}
