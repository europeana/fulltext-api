package eu.europeana.fulltext.api.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.RecordParseException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 *
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class FTService {

    private static final String GENERATED_IN    = "Generated in {} ms ";
    private static final Logger LOG             = LogManager.getLogger(FTService.class);

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    AnnoPageRepository annoPageRepository;


    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FTSettings ftSettings;

    public FTService() {
        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule());
    }

    /**
     * @return ManifestSettings object containing settings loaded from properties file
     */
    public FTSettings getSettings() {
        return ftSettings;
    }


    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnoPageExist(datasetId, localId, pageId)){
            return annoPageRepository.findByDatasetLocalPageId(datasetId, localId, pageId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId +
                                                    ", localId: " + localId +
                                                    " and pageId: " + pageId + " could be found");
        }
    }

    public AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnotationExist(datasetId, localId, annoId)){
            return annoPageRepository.findByDatasetLocalAnnoId(datasetId, localId, annoId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + " and localId: "
                       + localId + " could be found that contains an Annotation with annotationId: " + annoId);
        }
    }

    public FullTextResource fetchFullTextResource(String datasetId, String localId, String resId)
            throws ResourceDoesNotExistException {
        if (doesFullTextResourceExist(datasetId, localId, resId)){
            return generateFullTextResource(
                    resourceRepository.findByDatasetLocalResId(datasetId, localId, resId));
        } else {
            throw new ResourceDoesNotExistException("No Fulltext Resource with resourceId: " + resId
                                                    + " was found that is associated with datasetId: " + datasetId + " and localId: " + localId );
        }
    }


    // = = [ check Document existence ]= = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Check if a particular annotation page with the provided ids exists or not
     * @param datasetId Identifier of the dataset
     * @param localId   Identifier of the item
     * @param pageId    Identifier of the item's page
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnoPageExist(String datasetId, String localId, String pageId){
        return annoPageRepository.existsByPageId(datasetId, localId, pageId);
    }

    /**
     * Check if a particular annotation with the provided ids exists or not
     * @param datasetId Identifier of the dataset
     * @param localId   Identifier of the item
     * @param annoId    Identifier of the annotation
     * @return true if it exists, otherwise false
     */
    private boolean doesAnnotationExist(String datasetId, String localId, String annoId){
        return annoPageRepository.existsWithAnnoId(datasetId, localId, annoId);
    }

    /**
     * Check if a particular resource with the provided ids exists or not
     * @param datasetId Identifier of the dataset
     * @param localId   Identifier of the item
     * @param resId     Identifier of the fulltext resource
     * @return true if it exists, otherwise false
     */
    private boolean doesFullTextResourceExist(String datasetId, String localId, String resId){
        return resourceRepository.existsByLimitOne(datasetId, localId, resId);
    }


    // = = [ generate JSON objects ] = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage, boolean derefResource){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    private FullTextResource generateFullTextResource(Resource resource){
        long start = System.currentTimeMillis();
        FullTextResource result = EDM2IIIFMapping.getFullTextResource(resource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Serialize data from MongoDB to JSON-LD
     * @param  data input data
     * @return JSON-LD string
     * @throws SerializationException when serialisation seriously severely snaps somewhere
     */
    public String serialise(Object data) throws SerializationException {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        }
        catch (IOException e) {
            throw new SerializationException("Error serialising data: " + e.getMessage(), e);
        }
    }

}
