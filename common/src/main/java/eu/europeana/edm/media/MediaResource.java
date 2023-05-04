package eu.europeana.edm.media;

/**
 * @author hugom
 * @since May 31, 2019
 */
public class MediaResource implements MediaReference {
    public String url;

    public MediaResource(String url) {
        this.url = url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {
        return url;
    }

    public String getResourceURL() {
        return url;
    }


    @Override
    public void visit(MediaReferenceVisitor visitor) {
        visitor.visit(this);
    }
}
