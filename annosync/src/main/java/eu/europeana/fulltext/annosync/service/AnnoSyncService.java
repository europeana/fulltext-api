package eu.europeana.fulltext.annosync.service;

import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.service.CommonFTService;
import org.springframework.stereotype.Service;

@Service
public class AnnoSyncService extends CommonFTService {

  public AnnoSyncService(ResourceRepository resourceRepository,
      AnnoPageRepository annoPageRepository) {
    super(resourceRepository, annoPageRepository);
  }
}
