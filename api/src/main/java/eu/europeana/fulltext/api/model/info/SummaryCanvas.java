package eu.europeana.fulltext.api.model.info;

import com.fasterxml.jackson.annotation.JsonRootName;
import eu.europeana.fulltext.api.model.JsonLdIdType;

import java.util.*;

import static eu.europeana.fulltext.api.config.FTDefinitions.INFO_CANVAS_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class SummaryCanvas extends JsonLdIdType {

    private static final long serialVersionUID = 7066577659030844718L;

    private List<SummaryAnnoPage> annotations;

    /**
     * This is not a true IIIF SummaryCanvas object but merely a container object to group original and
     * translated Annopages
     *
     * @param id String containing identifying URL of the SummaryCanvas
     */
    public SummaryCanvas(String id) {
        super(id, INFO_CANVAS_TYPE);
        annotations = new ArrayList<>();
    }

    /**
     * Adds an annotation - actually: an SummaryAnnoPage (AnnoPage for a specific language) to the SummaryCanvas
     * @param alPage SummaryAnnoPage object to be added to the annotations List
     */
    public void addAnnotation(SummaryAnnoPage alPage){
        annotations.add(alPage);
    }

    public List<SummaryAnnoPage> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    public void setAnnotations(List<SummaryAnnoPage> annotations) {
        this.annotations = new ArrayList<>(annotations);
    }
}
