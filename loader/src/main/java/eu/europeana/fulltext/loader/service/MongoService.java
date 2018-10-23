package eu.europeana.fulltext.loader.service;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.repository.impl.AnnoPageRepositoryImpl;
import eu.europeana.fulltext.repository.impl.ResourceRepositoryImpl;
import eu.europeana.fulltext.loader.exception.LoaderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class MongoService {

    private static final Logger LOG = LogManager.getLogger(MongoService.class);

    @Autowired
    ResourceRepositoryImpl resourceRepositoryImpl;

    @Autowired
    AnnoPageRepositoryImpl annoPageRepositoryImpl;


    void saveAnnoPageList(List<AnnoPage> apList, MongoSaveMode saveMode) throws LoaderException {
        LOG.debug("Saving {} annoPages...", apList.size());

        long resourceCount = resourceRepositoryImpl.count();
        long annoPageCount = annoPageRepositoryImpl.count();
        for (AnnoPage annoPage : apList){
            if (MongoSaveMode.INSERT.equals(saveMode)) {
                saveResource(annoPage.getRes());
                saveAnnoPage(annoPage);
            }
        }
        long newResourceCount = resourceRepositoryImpl.count();
        long newAnnoPageCount = annoPageRepositoryImpl.count();
        if (resourceCount + apList.size() != newResourceCount) {
            LogFile.OUT.warn("Expected number of resource in database is {}, but actual number is {}",
                    resourceCount + apList.size(), newResourceCount);
        }
        if (annoPageCount + apList.size() != newAnnoPageCount) {
            LogFile.OUT.warn("Expected number of annotation pages in database is {}, but actual number is {}",
                    annoPageCount + apList.size(), annoPageCount);
        }
        LOG.debug("Saving done.");
    }

    /**
     * Saves a Resource object to the database
     * @return the saved resource object
     */
    private void saveResource(Resource resource) throws LoaderException {
        String dsId = resource.getDsId();
        String lcId = resource.getLcId();
        String id = resource.getId();
        try{
            resourceRepositoryImpl.save(resource);
            LOG.info("{}/{}/{} - Resource saved", dsId, lcId, id);
        } catch (Exception e){
            LogFile.OUT.error("{}/{}/{} - Error saving resource", dsId, lcId, id, e);
            throw new LoaderException("Error saving resource with dsId: " + dsId +
                                      ", lcId: " + lcId +
                                      ", id:" + id, e);
        }
    }

    /**
     * Deletes all resources that belong to a particular dataset
     * @param datasetId
     * @return the number of deleted resources
     */
    public int deleteAllResources(String datasetId) {
        return resourceRepositoryImpl.deleteDataset(datasetId);
    }

    /**
     * Saves an AnnoPage object to the database with embedded Annotations and linking to a resource
     * @param annoPage
     */
    private void saveAnnoPage(AnnoPage annoPage) throws LoaderException {
        String dsId = annoPage.getDsId();
        String lcId = annoPage.getLcId();
        String pgId = annoPage.getPgId();
        try{
            annoPageRepositoryImpl.save(annoPage);
            LOG.debug("{}/{}/{} AnnoPage saved", dsId, lcId, pgId);
        } catch (Exception e){
            LogFile.OUT.error("{}/{}/{} - Error saving AnnoPage", dsId, lcId, pgId, e);
            throw new LoaderException("Error saving Annopage with dsId: " + dsId +
                                      ", lcId: " + lcId +
                                      ", pgId:" + pgId, e);
        }
    }

    /**
     * Deletes all annotation pages that belong to a particular dataset
     * @param datasetId
     * @return the number of deleted annopages
     */
    public long deleteAllAnnoPages(String datasetId) {
        return annoPageRepositoryImpl.deleteDataset(datasetId);
    }


}
