package eu.europeana.fulltext.search.model.query;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;

/**
 * Document used for retrieving fulltext data from Solr
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
public class SolrDocument {

    // Note that in practice this class is not used as we only rely on highlight information retrieved
    // with SolrJ

    public static final String FIELD_EUROPEANA_ID = "europeana_id";

    @Field(FIELD_EUROPEANA_ID)
    @Id
    public String europeanaId;

}
