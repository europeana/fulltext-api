package eu.europeana.fulltext.api.model.info;

import eu.europeana.fulltext.api.model.JsonLdIdType;

import java.util.*;

import static eu.europeana.fulltext.api.config.FTDefinitions.INFO_CANVAS_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class Canvas extends JsonLdIdType {

    private static final long serialVersionUID = 7066577659030844718L;

    private List<AnnotationLangPage> annotations;

    /**
     * This is not a true IIIF Canvas object but merely a container object to group original and
     * translated Annopages
     *
     * @param id String containing identifying URL of the Canvas
     */
    public Canvas(String id) {
        super(id, INFO_CANVAS_TYPE);
        annotations = new ArrayList<>();
    }

    /**
     * Adds an annotation - actually: an AnnotationLangPage (AnnoPage for a specific language) to the Canvas
     * @param alPage AnnotationLangPage object to be added to the annotations List
     */
    public void addAnnotation(AnnotationLangPage alPage){
        annotations.add(alPage);
    }

    public List<AnnotationLangPage> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    public void setAnnotations(List<AnnotationLangPage> annotations) {
        this.annotations = new ArrayList<>(annotations);
    }
}
