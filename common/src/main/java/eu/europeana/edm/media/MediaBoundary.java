package eu.europeana.edm.media;

/**
 * @author hugom
 * @since May 31, 2019
 */
public abstract class MediaBoundary implements MediaReference {
    public MediaReference mediaReference;

    public MediaBoundary(MediaReference mediaReference) {
        this.mediaReference = mediaReference;
    }

    public void setMediaReference(MediaReference mediaReference) {
        this.mediaReference = mediaReference;
    }

    public MediaReference getMediaReference() {
        return mediaReference;
    }

    public String getURL() {
        return mediaReference.getURL() + getFragment();
    }

    public String getResourceURL() {
        return mediaReference.getResourceURL();
    }

    public abstract String getFragment();
}
