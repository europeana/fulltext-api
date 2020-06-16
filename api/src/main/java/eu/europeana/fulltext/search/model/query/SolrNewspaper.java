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
public class SolrNewspaper {

    @Field("europeana_id")
    @Id
    public String europeanaId;

    @Field("LANGUAGE")
    public String language;

   @Field("fulltext*")
    public String[] fulltext;

}
