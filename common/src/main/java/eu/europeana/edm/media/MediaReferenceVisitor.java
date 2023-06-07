package eu.europeana.edm.media;

/**
 * @author Hugo
 * @since 4 Apr 2023
 */
public interface MediaReferenceVisitor {
    void visit(ImageBoundary img);

    void visit(TimeBoundary time);

    void visit(MediaResource media);
}
