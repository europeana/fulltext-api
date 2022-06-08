package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IndexingApplication implements CommandLineRunner {

  @Autowired
  private IndexingAnnoPageRepository repository;

  private static final Logger logger = LogManager.getLogger(IndexingApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(IndexingApplication.class, args);
  }

  public void synchronizeMetadataContent() throws IOException, SolrServerException {
    LocalDateTime lastUpdate = FulltextCollection.getLastUpdateMetadata();
    List<String> toUpdate = MetadataCollection.getDocumentsModifiedAfter(lastUpdate);
    FulltextCollection.setMetadata(toUpdate);
  }

  public void synchronizeFulltextContent() throws IOException, SolrServerException {
    LocalDateTime lastUpdate = FulltextCollection.getLastUpdateFulltext();
    List<AnnoPage> updated = repository.getRecordsModifiedAfter(lastUpdate.toEpochSecond(ZoneOffset.UTC));
    List<String> updated_europeana_ids = updated.stream().map(p-> getEuropeanaId(p.getDsId(),p.getLcId())).distinct().collect(Collectors.toList());
    List<String> toAdd = new ArrayList<>();
    List<String> toDelete = new ArrayList<>();
    List<String> toUpdate = new ArrayList<>();
    for (String europeana_id: updated_europeana_ids){
      if (!FulltextCollection.exists(europeana_id)){
        toAdd.add(europeana_id);
      } else if (repository.isDeleted(getDsId(europeana_id), getLcId(europeana_id))){
        toDelete.add(europeana_id);
      } else {
        toUpdate.add(europeana_id);
      }
    }
    if (!toAdd.isEmpty()) {
      FulltextCollection.addDocuments(toAdd);
      FulltextCollection.setMetadata(toAdd);
      FulltextCollection.setFulltext(toAdd);
    }
    if (!toDelete.isEmpty()){
      FulltextCollection.deleteDocuments(toDelete);
    }
    if (!toUpdate.isEmpty()){
      FulltextCollection.setFulltext(toUpdate);
    }
  }

  protected String getEuropeanaId(String dsId, String lcId){ //maybe this logic is implemented already somewhere in the project
    return "/" + dsId +"/" + lcId;
  }

  protected String getDsId(String europeana_id){
    String[] parts = europeana_id.split("/");
    return parts[0];
  }

  protected String getLcId(String europeana_id){
    String[] parts = europeana_id.split("/");
    return parts[1];
  }

  @Override
  public void run(String... args) throws Exception {
    //TODO
    //List<AnnoPage> annoPages = repository.getRecordsModifiedAfter(1654034400000L);
    List<AnnoPage> annoPages = repository.getAllWebResources("9200396","BibliographicResource_3000118436165");
    logger.info("{}", annoPages);

  }
}
