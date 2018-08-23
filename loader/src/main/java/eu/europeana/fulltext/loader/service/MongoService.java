package eu.europeana.fulltext.loader.service;

import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.repository.AnnoPageRepository;
import eu.europeana.fulltext.api.repository.ResourceRepository;
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
 *
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class MongoService {

    private static final Logger LOG      = LogManager.getLogger(MongoService.class);

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    AnnoPageRepository annoPageRepository;

    @Autowired
    private LoaderSettings settings;

    public void saveAPList(List<AnnoPageRdf> apList, MongoSaveMode saveMode) {
        LOG.debug("Saving {} annoPages...", apList.size());
        for (AnnoPageRdf annoPageRdf : apList){
            String[] identifiers = StringUtils.split(
                    StringUtils.removeStartIgnoreCase(annoPageRdf.getFtResource(), settings.getResourceBaseUrl()), '/');
            if (identifiers.length > 3){
                LogFile.OUT.error("Configuration mismatch error occurred", new LoaderException("Please check Resource Base URL settings in properties file: '"
                                                   + settings.getResourceBaseUrl()
                                                   + "', making sure that it matches with the 'ENTITY text' value found in import file: '"
                                                   + annoPageRdf.getFtResource() + "'"));
            }

            if (MongoSaveMode.INSERT.equals(saveMode)) {
                Resource resource = saveResource(identifiers[2],
                        annoPageRdf.getFtLang(),
                        annoPageRdf.getFtText(),
                        identifiers[0],
                        identifiers[1]);

                // TODO ask Hugo, is there any point in saving an annoPage if the resource wasn't saved properly
                saveAnnoPage(identifiers[0],
                        identifiers[1],
                        annoPageRdf,
                        resource);

            } else if (MongoSaveMode.UPDATE.equals(saveMode)) {
                // TODO to implement
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
    public Resource saveResource(String id, String lang, String value, String dsId, String lcId) {
        Resource result = null;
        try{
            result = new Resource(id, lang, value, dsId, lcId);
            result = resourceRepository.save(result);
            LOG.debug("{}/{} - resource saved with id", dsId, lcId, id);
        } catch (Exception e){
            LogFile.OUT.error("{}/{} - Error saving resource with id {}", dsId, lcId, id, e);
        }
        return result;
    }

    /**
     * Saves an AnnoPage object to the database with embedded Annotations and linking to a resource
     * @param dsId
     * @param lcId
     * @param annoPageRdf
     * @param res
     * @return the saved AnnoPage object
     */
    public AnnoPage saveAnnoPage(String dsId, String lcId, AnnoPageRdf annoPageRdf, Resource res) {
        AnnoPage result = null;
        try{
            result =  new eu.europeana.fulltext.api.entity.AnnoPage(dsId, lcId,
                    annoPageRdf.getPageId(), annoPageRdf.getImgTargetBase(), res);
            result.setAns(createAnnoList(annoPageRdf, dsId));
            result = annoPageRepository.save(result);
            LOG.debug("{}/{}/{} annopage saved");
        } catch (Exception e){
            LogFile.OUT.error("{}/{}/{} - Error saving AnnoPage", dsId, lcId, annoPageRdf.getPageId(), e);
        }
        return result;
    }

    private List<eu.europeana.fulltext.api.entity.Annotation> createAnnoList(AnnoPageRdf annoPageRdf, String dataSetId){
        List<eu.europeana.fulltext.api.entity.Annotation> annotationList = new ArrayList<>();
        for (AnnotationRdf annotationRdf : annoPageRdf.getAnnotationRdfList()){
            eu.europeana.fulltext.api.entity.Annotation annotation = new eu.europeana.fulltext.api.entity.Annotation(
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

    private List<eu.europeana.fulltext.api.entity.Target> createFTTargetList(AnnotationRdf annotationRdf){
        List<eu.europeana.fulltext.api.entity.Target> targetList = new ArrayList<>();
        for (TargetRdf targetRdf : annotationRdf.getTargetRdfList()){
            targetList.add(new eu.europeana.fulltext.api.entity.Target(targetRdf.getX(),
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
