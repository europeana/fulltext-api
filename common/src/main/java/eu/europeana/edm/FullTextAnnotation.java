package eu.europeana.edm;

import eu.europeana.edm.media.MediaReference;
import eu.europeana.edm.text.TextReference;
import eu.europeana.fulltext.AnnotationType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 22 Jun 2018
 */
public class FullTextAnnotation {
    private String id;
    private TextReference textReference;
    private ArrayList<MediaReference> targets = new ArrayList<>(1);
    private AnnotationType type;
    private String lang;
    private Float confidence;


    public FullTextAnnotation(
            String id,
            TextReference textReference,
            MediaReference target,
            AnnotationType type,
            String lang,
            Float confidence) {
        this.textReference = textReference;
        if (target != null) {
            targets.add(target);
        }
        this.type = type;
        this.lang = lang;
        this.confidence = confidence;
        this.id = id;
    }

    public FullTextAnnotation(String id, TextReference textReference
            , MediaReference target1, MediaReference target2
            , AnnotationType type, String lang
            , Float confidence) {
        this.textReference = textReference;
        this.targets.ensureCapacity(2);
        if (target1 != null) {
            this.targets.add(target1);
        }
        if (target2 != null) {
            this.targets.add(target2);
        }
        this.type = type;
        this.lang = lang;
        this.confidence = confidence;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TextReference getTextReference() {
        return textReference;
    }

    public void setTextReference(TextReference textReference) {
        this.textReference = textReference;
    }

    public List<MediaReference> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<MediaReference> targets) {
        this.targets = targets;
    }

    public AnnotationType getType() {
        return type;
    }

    public void setType(AnnotationType type) {
        this.type = type;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public boolean hasConfidence() {
        return confidence != null;
    }

    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    @Override
    public String toString() {
        return "FullTextAnnotation{" +
                "id='" + id + '\'' +
                ", textReference=" + textReference +
                ", targets=" + targets +
                ", type=" + type +
                ", lang='" + lang + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}

