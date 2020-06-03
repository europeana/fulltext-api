package eu.europeana.fulltext.search.model.query;

/**
 * Representation of an EuropeanaId so we can easily query Solr for a particular id
 *
 * @author Patrick Ehlert
 * Created on 29 May 2020
 */
public class EuropeanaId {

    private String datasetId;
    private String localId;

    public EuropeanaId(String datasetId, String localId) {
        this.datasetId = datasetId;
        this.localId = localId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public String getLocalId() {
        return localId;
    }

    public String toString() {
        return "/" + datasetId + "/" + localId;
    }

}
