package eu.europeana.fulltext.api.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private ResourceRepository resourceRepository;
    private AnnoPageRepository annoPageRepository;
    private FTSettings ftSettings;

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    /*
     * Constructs an FTService object with autowired dependencies
     */
    public FTService(ResourceRepository resourceRepository, AnnoPageRepository annoPageRepository, FTSettings ftSettings) {
        this.resourceRepository = resourceRepository;
        this.annoPageRepository = annoPageRepository;
        this.ftSettings = ftSettings;
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
    /**
     * Handles fetching an Annotation page (aka AnnoPage) with all its annotations
     * @param datasetId identifier of the AnnoPage's dataset
     * @param localId   identifier of the AnnoPage's record
     * @param pageId    identifier of the AnnoPage
     * @throws AnnoPageDoesNotExistException when the Annopage cannot be found
     * @return AnnoPage
     */
    public AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId) throws AnnoPageDoesNotExistException {
        AnnoPage result = annoPageRepository.findByDatasetLocalPageId(datasetId, localId, pageId);
        if (result == null) {
            throw new AnnoPageDoesNotExistException(String.format(
                    "No AnnoPage with datasetId: %s, localId: %s and pageId: %s could be found",
                    datasetId, localId, pageId));
        }
        return result;
    }

    /**
     * Retrieve all annopages for a particular issue
     * @param datasetId
     * @param localId
     * @return List of AnnoPages, empty list if there are none
     */
    public List<AnnoPage> fetchAnnoPages(String datasetId, String localId) {
        List<AnnoPage> result = new ArrayList<>();
        int i = 1;
        AnnoPage page = annoPageRepository.findByDatasetLocalPageId(datasetId, localId, String.valueOf(i));
        while (page != null) {
            result.add(page);
            i++;
            page = annoPageRepository.findByDatasetLocalPageId(datasetId, localId, String.valueOf(i));
        }
        return result;
    }

    /**
     * Handles fetching an Annotation page (aka AnnoPage) containing the Annotation with given annoId
     * @param datasetId identifier of the AnnoPage's dataset
     * @param localId   identifier of the AnnoPage's record
     * @param annoId    identifier of the Annotation to be found
     * @throws AnnoPageDoesNotExistException when the Annopage containing the required Annotation can't be found
     * @return AnnoPage
     */
    public AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        AnnoPage result = annoPageRepository.findByDatasetLocalAnnoId(datasetId, localId, annoId);
        if (result == null) {
            throw new AnnoPageDoesNotExistException(String.format(
                    "No AnnoPage with datasetId: %s and localId: %s could be found that contains an Annotation with annotationId: %s",
                    datasetId, localId, annoId));
        }
        return result;
    }


    /**
     * Handles fetching an Annotation page (aka AnnoPage) containing the Annotation with given annoId
     * @param datasetId identifier of the dataset that contains the Annopage that refers to Resource
     * @param localId   identifier of the record that contains the Annopage that refers to Resource
     * @param resId     identifier of the Resource
     * @throws ResourceDoesNotExistException when the Resource can't be found
     * @return FTResource
     */
    public FTResource fetchFTResource(String datasetId, String localId, String resId)
            throws ResourceDoesNotExistException {
        Resource resource = resourceRepository.findByDatasetLocalResId(datasetId, localId, resId);
        if (resource == null) {
            throw new ResourceDoesNotExistException(String.format(
                    "No Fulltext Resource with resourceId: %s was found that is associated with datasetId: %s and localId: %s",
                    resId, datasetId, localId));
        }
        return generateFTResource(resource);
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

    // = = [ generate JSON objects ] = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    /**
     * Generates an AnnotationPageV3 (IIIF V3 response type) object with the AnnoPage as input
     * @param annoPage AnnoPage input object
     * @param derefResource boolean indicating whether to dereference the Resource object on the top level Annotation
     * @return AnnotationPageV3
     */
     public AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage, boolean derefResource){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationPageV2 (IIIF V2 response type) object with the AnnoPage as input
     * @param annoPage AnnoPage input object
     * @return AnnotationPageV2
     */
    public AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage, boolean derefResource){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationV3 (IIIF V3 response type) object of the Annotation with ID annoId, found within the
     * the AnnoPage input
     * @param annoPage AnnoPage input object
     * @param annoId String the Annotation idenfifier
     * @return AnnotationV3
     */
    public AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }


    /**
     * Generates an AnnotationV2 (IIIF V2 response type) object of the Annotation with ID annoId, found within the
     * the AnnoPage input
     * @param annoPage AnnoPage input object
     * @param annoId String the Annotation idenfifier
     * @return AnnotationV2
     */
    public AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    private FTResource generateFTResource(Resource resource){
        long       start  = System.currentTimeMillis();
        FTResource result = EDM2IIIFMapping.getFTResource(resource);
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
        } catch (IOException e) {
            throw new SerializationException("Error serialising data: " + e.getMessage(), e);
        }
    }

}
