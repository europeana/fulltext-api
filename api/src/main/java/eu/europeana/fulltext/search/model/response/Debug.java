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
@JsonPropertyOrder({"solrSnippets", "keywords"})
public class Debug implements Serializable {

    private static final long serialVersionUID = 3800855960101156128L;

    private List<String> solrSnippets = new ArrayList<>();
    private List<SolrHit> keywords = new ArrayList<>();

    public void addSolrSnippet(String solrSnippet) {
        this.solrSnippets.add(solrSnippet);
    }

    public void addKeyword(SolrHit hit) {
        this.keywords.add(hit);
    }

    public void removeKeyword(SolrHit hit) {
        this.keywords.remove(hit);
    }

    public List<String> getSolrSnippets() {
        return solrSnippets;
    }

    public List<SolrHit> getKeywords() {
        return keywords;
    }
}
