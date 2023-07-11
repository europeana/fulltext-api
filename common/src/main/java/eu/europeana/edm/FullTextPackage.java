package eu.europeana.edm;

import eu.europeana.edm.text.FullTextResource;

import java.util.ArrayList;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 5 Apr 2018
 */
public class FullTextPackage extends ArrayList<FullTextAnnotation> {
    private String baseUri;
    private FullTextResource resource;

    public FullTextPackage(String id, FullTextResource resource) {
        this.resource = resource;
        baseUri = id;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public FullTextResource getResource() {
        return resource;
    }

    public void setResource(FullTextResource resource) {
        this.resource = resource;
    }

//   // public boolean isEmpty() {
//        return resource.getString().isEmpty();
//    }

    public boolean isLangOverriden(String lang) {
        if (lang == null) {
            return false;
        }
        return !lang.equals(resource.getLang());
    }

    @Override
    public String toString() {
        return "FullTextPackage{" +
                "baseUri='" + baseUri + '\'' +
                '}';
    }
}
