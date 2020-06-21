package eu.europeana.fulltext.search.repository;

import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.HighlightPage;

import javax.annotation.PostConstruct;

/**
 * Defines the query sent to solr to retrieve highlights in a particular newspaper issue (record)
 *
 * @author Patrick Ehlert
 * Created 29 May 2020
 */
public class SolrIssueQueryImpl implements SolrIssueQuery {

    @Autowired
    private SolrTemplate solrTemplate;

    @Value("${spring.data.solr.core}")
    private String solrCore;

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
     */
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

    // TODO find a way to set hl.maxAnalyzedChars

    @Override
    public HighlightPage<SolrNewspaper> findByEuropeanaIdAndQuery(EuropeanaId europeanaId, String query, int maxSnippets) {
        HighlightOptions hlOptions = new HighlightOptions()
                .setQuery(new SimpleQuery(query))
                .addField(SolrNewspaper.FIELD_EUROPEANA_ID, SolrNewspaper.FIELD_FULLTEXT)
                .setNrSnipplets(maxSnippets)
                .setSimplePrefix(SearchConfig.HIT_TAG_START)
                .setSimplePostfix(SearchConfig.HIT_TAG_END);

        HighlightQuery q = new SimpleHighlightQuery(new Criteria(SolrNewspaper.FIELD_EUROPEANA_ID).is(europeanaId));
        q.addProjectionOnField(new SimpleField(SolrNewspaper.FIELD_EUROPEANA_ID)); // fl=europeana_id
        q.setHighlightOptions(hlOptions);
        q.setRows(1);
        q.setTimeAllowed(30_000);

        return solrTemplate.queryForHighlightPage(solrCore, q, SolrNewspaper.class);
    }

}
