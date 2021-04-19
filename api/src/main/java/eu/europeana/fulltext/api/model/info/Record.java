package eu.europeana.fulltext.api.model.info;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by luthien on 07/04/2021.
 */
public class Record implements Serializable {
    private static final long serialVersionUID = -8052995235828716772L;

    private String       dataSetId;
    private String       localId;
    private List<Canvas> canvases;


    /**
     * This is a container object to group "fake" Canvas objects containing original and translated AnnoPages
     * for a given Fulltext record / object
     *
     * @param dataSetId String containing the dataset of this Fulltext Record
     * @param localId   String containing the localId of this Fulltext Record
     */
    public Record(String dataSetId, String localId){
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
     * Adds a *fake* Canvas containing original and translated versions of an AnnoPage (AnnotationLangPages)
     * @param canvas Canvas object to be added to the canvases List
     */
    public void addCanvas(Canvas canvas){
        canvases.add(canvas);
    }

    @JsonValue
    public List<Canvas> getCanvases() {
        return new ArrayList<>(canvases);
    }

    public void setCanvases(List<Canvas> canvases) {
        this.canvases = new ArrayList<>(canvases);
    }

}
