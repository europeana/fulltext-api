package eu.europeana.fulltext.api.model.info;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luthien on 07/04/2021.
 */
public class SummaryManifest implements Serializable {
    private static final long serialVersionUID = -8052995235828716772L;

    private String       dataSetId;
    private String              localId;
    private List<SummaryCanvas> canvases;


    /**
     * This is a container object to group "fake" SummaryCanvas objects containing original and translated AnnoPages
     * for a given Fulltext record / object
     *
     * @param dataSetId String containing the dataset of this Fulltext SummaryManifest
     * @param localId   String containing the localId of this Fulltext SummaryManifest
     */
    public SummaryManifest(String dataSetId, String localId){
        this.dataSetId = dataSetId;
        this.localId = localId;
        canvases = new ArrayList<>();
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }


    /**
     * Adds a *fake* SummaryCanvas containing original and translated versions of an AnnoPage (AnnotationLangPages)
     * @param summaryCanvas SummaryCanvas object to be added to the canvases List
     */
    public void addCanvas(SummaryCanvas summaryCanvas){
        canvases.add(summaryCanvas);
    }

    @JsonValue
    public List<SummaryCanvas> getCanvases() {
        return new ArrayList<>(canvases);
    }

    public void setCanvases(List<SummaryCanvas> canvases) {
        this.canvases = new ArrayList<>(canvases);
    }

}
