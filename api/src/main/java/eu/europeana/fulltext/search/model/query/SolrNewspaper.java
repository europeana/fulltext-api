package eu.europeana.fulltext.search.model.query;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;

/**
 * Document used for retrieving data from Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
public class SolrNewspaper {

    public static final String FIELD_EUROPEANA_ID = "europeana_id";
    public static final String FIELD_FULLTEXT = "fulltext*";
    public static final String FIELD_LANGUAGE = "LANGUAGE";

    @Field(FIELD_EUROPEANA_ID)
    @Id
    public String europeanaId;

    @Field(FIELD_LANGUAGE)
    public String language;

   @Field(FIELD_FULLTEXT)
    public String[] fulltext;

}
