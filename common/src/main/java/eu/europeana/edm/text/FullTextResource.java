package eu.europeana.edm.text;


/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 * @Refractored Srishti Singh
 */
public class FullTextResource implements TextReference {
    private String fullTextResourceURI;
    private String value;
    private String lang;
    private String rights;
    private String recordURI;

    public FullTextResource() {

    }

    public FullTextResource(
            String fullTextResourceURI, String value, String lang, String rights, String recordURI) {
        this.fullTextResourceURI = fullTextResourceURI;
        this.value = value;
        this.lang = lang;
        this.rights = rights;
        this.recordURI = recordURI;
    }

    public String getFullTextResourceURI() {
        return fullTextResourceURI;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getRights() {
        return rights;
    }

    public String getRecordURI() {
        return recordURI;
    }

    public FullTextResource getResource() {
        return this;
    }

    public void setFullTextResourceURI(String fullTextResourceURI) {
        this.fullTextResourceURI = fullTextResourceURI;
    }

    public void setRecordURI(String recordURI) {
        this.recordURI = recordURI;
    }

    @Override
    public String getResourceURL() {
        return getFullTextResourceURI();
    }

    @Override
    public String getURL() {
        return getResourceURL();
    }


    public boolean hasLanguage() {
        return (lang != null);
    }

    public boolean isLangOverriden(String lang) {
        if (lang == null) {
            return false;
        }
        return !this.lang.equals(lang);
    }

    @Override
    public String toString() {
        return "FullTextResource{" +
                "fullTextResourceURI='" + fullTextResourceURI + '\'' +
                ", value='" + value + '\'' +
                ", lang='" + lang + '\'' +
                ", rights='" + rights + '\'' +
                ", recordURI='" + recordURI + '\'' +
                '}';
    }
}