package eu.europeana.fulltext.indexing.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author mmarrero
 * Exposes main solr requests with a specific number of attempts
 */

public class SolrServices {
    private static final Logger logger = LogManager.getLogger(SolrServices.class);

    //TODO API team: check values and maybe include in properties
    public static final Integer ATTEMPTS = 3;
    public static final Long SLEEP_MS = 1000l;

    public static UpdateResponse commit(SolrClient solr) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.commit();
                logger.debug("Commit done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Commit Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");
                }
            }
        return null;
    }

    public static UpdateResponse commit(SolrClient solr, String collection) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.commit(collection);
                logger.debug("Commit done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Commit Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");
                }
            }
        return null;
    }

    public static UpdateResponse add(SolrClient solr, Collection<SolrInputDocument> solrDocument) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.add(solrDocument);
                logger.debug("Indexing done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Indexing Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");
                }
            }
        return null;
    }

    public static UpdateResponse add(SolrClient solr, String collection, Collection<SolrInputDocument> solrDocument) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0) {
            try {
                UpdateResponse response = solr.add(collection, solrDocument);
                logger.debug("Indexing done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Indexing Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");
                }
            }
        }
        return null;
    }

    public static UpdateResponse deleteByQuery(SolrClient solr, String collection, String query) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.deleteByQuery(collection, query);
                logger.debug("Delete done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Delete Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

    public static UpdateResponse deleteById(SolrClient solr, String collection, String id) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.deleteById(collection, id);
                logger.debug("Delete done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Delete Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

    public static UpdateResponse deleteById(SolrClient solr, String collection, List<String> ids) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                UpdateResponse response = solr.deleteById(collection, ids);
                logger.debug("Delete done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Delete Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

    public static QueryResponse query(SolrClient solr, String collection, SolrQuery params) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                QueryResponse response = solr.query(collection, params);
                logger.debug("Query done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Query Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

    public static QueryResponse query(SolrClient solr, SolrQuery params) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                QueryResponse response = solr.query(params);
                logger.debug("Query done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Query Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

    //It does not return all the fields
    public static SolrDocument get(SolrClient solr, String collection, String id) throws SolrServerException, IOException {
        int attempts = ATTEMPTS;
        while (attempts > 0)
            try {
                SolrDocument response = solr.getById(collection,id);
                logger.debug("Query done ");
                return response;
            } catch (SolrServerException | IOException e) {
                attempts--;
                if (attempts <= 0) {
                    throw e;
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e1) {
                    logger.warn("Query Attempt " + (ATTEMPTS - attempts) + " - Thread interrupted");				}
            }
        return null;
    }

}
