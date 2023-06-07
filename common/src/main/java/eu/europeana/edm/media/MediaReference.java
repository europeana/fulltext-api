package eu.europeana.edm.media;

/**
 * @author hugom
 * @since May 31, 2019
 */
public interface MediaReference {
    String getResourceURL();

    String getURL();

    void visit(MediaReferenceVisitor visitor);
}
