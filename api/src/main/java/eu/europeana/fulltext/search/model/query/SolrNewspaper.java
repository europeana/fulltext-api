package eu.europeana.fulltext.search.model.query;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * Document used for retrieving data from Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
// TODO load core name from config!!
@SolrDocument(solrCoreName = "newspapers")
public class SolrNewspaper {

    @Field("europeana_id")
    @Id
    public String europeanaId;

    @Field("LANGUAGE")
    public String language;

    // The purpose of this field is to allow us to do queries, such as findByFulltextIn (q=fulltext:<search_string>),
    // but this does't work for retrieving the actual fulltext. For that we would need to specify fulltext*, but that
    // messes up the queries. In any case we don't need fulltext from Solr, so we can safely keep this.
    @Field("fulltext")
    public String[] fulltext;

}
