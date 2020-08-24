package eu.europeana.fulltext.search.model.response;

/**
 * Possible types of hit highlight object.
 */
public enum HitType {
    V3("Hit"),
    V2("search:Hit");

    public final String value;

    HitType(String value) {
        this.value = value;
    }
}
