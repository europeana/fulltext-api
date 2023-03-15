package eu.europeana.fulltext.api.model.info;

import eu.europeana.fulltext.api.model.JsonLdIdType;

import static eu.europeana.fulltext.api.config.FTDefinitions.INFO_ANNOPAGE_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class SummaryAnnoPage extends JsonLdIdType {

    private static final long serialVersionUID = -670619785903826924L;

    private String language;
    private String[] textGranularity;
    private String source;

    /**
     * This object serves as a placeholder for AnnoPages.
     * It is used in the summary info endpoint only
     *
     * @param id    String containing identifying URL of the SummaryAnnoPage
     * @param language  String containing language of the SummaryAnnoPage
     * @param source source of the AnnotationPage
     */
    public SummaryAnnoPage(String id, String language, String source){
        super(id, INFO_ANNOPAGE_TYPE);
        this.language = language;
        this.source = source;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String[] getTextGranularity() {
        return textGranularity;
    }

    public void setTextGranularity(String[] textGranularity) {
        this.textGranularity = textGranularity;
    }


}
