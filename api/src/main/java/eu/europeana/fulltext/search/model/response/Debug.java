package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.fulltext.search.model.query.SolrHit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to include debugging information in the response (if this is requested, not included by default)
 *
 * @author Patrick Ehlert
 * Created on 9 June 2020
 */
@JsonPropertyOrder({"solrQuery", "solrHits"})
public class Debug implements Serializable {

    private static final long serialVersionUID = 3800855960101156128L;

    private String solrQuery;
    private List<SolrHit> solrHits = new ArrayList<>();

    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    public String getSolrQuery() {
        return this.solrQuery;
    }

    public void addSolrSnippet(SolrHit solrHit) {
        this.solrHits.add(solrHit);
    }

    public List<SolrHit> getSolrHits() {
        return solrHits;
    }
}
