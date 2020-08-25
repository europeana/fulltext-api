package eu.europeana.fulltext.search.model.response;

/**
 * Possible types of hit highlight object.
 * Values correspond to field names in the Presentation API specification.
 */
public enum HitType {
    V3("Hit"),
    V2("search:Hit");

    public final String value;

    HitType(String value) {
        this.value = value;
    }
}
