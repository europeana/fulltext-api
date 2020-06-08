package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.model.query.EuropeanaId;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;

import javax.annotation.PostConstruct;

/**
 * This is to fix the problem that Solr considers slashes to be special characters. Since an EuropeanaId follows the
 * pattern <pre>/< datasetId>/< localId></pre> we run into problems when we try to do something like
 * <code>solrTemplate.findById(europeanaId)</code>.
 * If we don't escape characters, Solr will incorrectly return more than 1 record.
 * If we do escape, either manually by adding backslashes or via ClientUtils.escapeQueryChars(), then the solrTemplate
 * will automatically escape the added backslashes (double escaping). The workaround solution is to register a converter
 * for EuropeanaIds so that the automatic escaping of the escape characters does not occur. For some reason this doesn't
 * work when we try it in SolrConfig, but it works if we do it here
 * Solution thanks to https://stackoverflow.com/q/23386925
 *
 * @author Patrick Ehlert
 * Created 29 May 2020
 */
public class SolrEuropeanaIdImpl implements SolrEuropeanaId {

    @Autowired
    private SolrTemplate solrTemplate;

    @Value("${spring.data.solr.core}")
    private String solrCore;

    @PostConstruct
    private void init() {
        DefaultQueryParser defaultQueryParser = new DefaultQueryParser(null);
        defaultQueryParser.registerConverter(new Converter<EuropeanaId, String>() {
            @Override
            public String convert(EuropeanaId id) {
                return ClientUtils.escapeQueryChars(id.toString());
            }
        });
        solrTemplate.registerQueryParser(Query.class, defaultQueryParser);
    }

}
