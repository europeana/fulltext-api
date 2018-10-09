package eu.europeana.fulltext.loader.service;

import eu.europeana.fulltext.common.entity.AnnoPage;
import eu.europeana.fulltext.common.entity.Resource;
import eu.europeana.fulltext.common.entity.Annotation;
import eu.europeana.fulltext.common.entity.Target;
import eu.europeana.fulltext.common.repository.impl.AnnoPageRepositoryImpl;
import eu.europeana.fulltext.common.repository.impl.ResourceRepositoryImpl;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.model.AnnotationRdf;
import eu.europeana.fulltext.loader.model.TargetRdf;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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


    void saveAPList(List<AnnoPageRdf> apList, MongoSaveMode saveMode) throws LoaderException {
        LOG.debug("Saving {} annoPages...", apList.size());
        for (AnnoPageRdf annoPageRdf : apList){

            if (MongoSaveMode.INSERT.equals(saveMode)) {
                Resource resource = saveResource(annoPageRdf.getResourceId(),
                        annoPageRdf.getFtLang(),
                        annoPageRdf.getFtText(),
                        annoPageRdf.getDatasetId(),
                        annoPageRdf.getLocalId());
                saveAnnoPage(annoPageRdf, resource);
            }
        }
        LOG.debug("Saving done.");
    }

    /**
     * Saves a Resource object to the database
     * @param id
     * @param lang
     * @param value
     * @param dsId
     * @param lcId
     * @return the saved resource object
     */
    private Resource saveResource(String id, String lang, String value, String dsId, String lcId) throws
                                                                                                  LoaderException {
        Resource result = null;
        try{
            result = new Resource(id, lang, value, dsId, lcId);
            result = resourceRepositoryImpl.saveAndReturn(result);
            LOG.info("{}/{} - resource saved with id {}", dsId, lcId, id);
        } catch (Exception e){
            LogFile.OUT.error("{}/{} - Error saving resource with id {}", dsId, lcId, id, e);
            throw new LoaderException("Error saving Resource with dsId: " + dsId +
                                      ", lcId: " + lcId +
                                      ", id:" + id +
                                      ". Message: " + e.getMessage());
        }
        return result;
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
     * @param annoPageRdf
     * @param res
     */
    private void saveAnnoPage(AnnoPageRdf annoPageRdf, Resource res) throws LoaderException {
        try{
            annoPageRepositoryImpl.save(createAnnoPage(annoPageRdf, res));
            LOG.debug("{}/{}/{} annopage saved");
        } catch (Exception e){
            LogFile.OUT.error("{}/{}/{} - Error saving AnnoPage",
                              annoPageRdf.getDatasetId(), annoPageRdf.getLocalId(), annoPageRdf.getPageId(), e);

            throw new LoaderException("Error saving Annopage with dsId: " + annoPageRdf.getDatasetId() +
                                      ", lcId: " + annoPageRdf.getLocalId() +
                                      ", pgId:" + annoPageRdf.getPageId() +
                                      ". Message: " + e.getMessage());
        }
    }


    public AnnoPage createAnnoPage(AnnoPageRdf annoPageRdf, Resource res) {
        AnnoPage result = new AnnoPage(
                annoPageRdf.getDatasetId(),
                annoPageRdf.getLocalId(),
                annoPageRdf.getPageId(),
                annoPageRdf.getImgTargetBase(),
                res);
        result.setAns(createAnnoList(annoPageRdf, annoPageRdf.getDatasetId()));
        return result;
    }

    /**
     * Deletes all annotation pages that belong to a particular dataset
     * @param datasetId
     * @return the number of deleted annopages
     */
    public long deleteAllAnnoPages(String datasetId) {
        return annoPageRepositoryImpl.deleteDataset(datasetId);
    }

    private List<Annotation> createAnnoList(AnnoPageRdf annoPageRdf, String dataSetId){
        List<Annotation> annotationList = new ArrayList<>();
        for (AnnotationRdf annotationRdf : annoPageRdf.getAnnotationRdfList()){
            Annotation annotation = new Annotation(
                    annotationRdf.getId(),
                    getDcTypeCode(annotationRdf.getDcType(), dataSetId, annoPageRdf.getPageId(), annotationRdf.getId()),
                    annotationRdf.getFrom(),
                    annotationRdf.getTo());
            if (StringUtils.isNotBlank(annotationRdf.getLang())){
                annotation.setLang(annotationRdf.getLang());
            }
            annotation.setTgs(createFTTargetList(annotationRdf));
            annotationList.add(annotation);
        }
        return annotationList;
    }

    private List<Target> createFTTargetList(AnnotationRdf annotationRdf){
        List<Target> targetList = new ArrayList<>();
        for (TargetRdf targetRdf : annotationRdf.getTargetRdfList()){
            targetList.add(new Target(targetRdf.getX(),
                                      targetRdf.getY(),
                                      targetRdf.getW(),
                                      targetRdf.getH()));
        }
        return targetList;
    }

    private static String getDcTypeCode(String dcType, String dataSetId, String pageId, String annoId){
        String dcTypeCode;
        if (StringUtils.isBlank(dcType)){
            String error = "Data error: dc:type not set or null for Annotation with ID: " + annoId
                           + " on Annotation Page: " + pageId + " for Dataset: " + dataSetId;

            LogFile.OUT.error(error);
        }
        switch (dcType.toLowerCase()) {
            case "page":
                dcTypeCode = "P";
                break;
            case "block":
                dcTypeCode = "B";
                break;
            case "line":
                dcTypeCode = "L";
                break;
            case "word":
                dcTypeCode = "W";
                break;
            default:
                dcTypeCode = "";
                break;
        }
        return dcTypeCode;
    }

}
