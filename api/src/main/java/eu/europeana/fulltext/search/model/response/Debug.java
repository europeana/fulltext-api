package eu.europeana.fulltext.search.model.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<HitSelector> keywords = new HashSet<>();

    public void addSolrSnippet(String solrSnippet) {
        this.solrSnippets.add(solrSnippet);
    }

    public void addKeywords(HitSelector hit) {
        this.keywords.add(hit);
    }

    public List<String> getSolrSnippets() {
        return solrSnippets;
    }

    public Set<HitSelector> getKeywords() {
        return keywords;
    }
}
