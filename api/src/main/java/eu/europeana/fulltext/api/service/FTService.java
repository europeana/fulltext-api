package eu.europeana.fulltext.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.fulltext.AnnotationType;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private ObjectMapper mapper;

    /*
     * Constructs an FTService object with autowired dependencies
     */
    public FTService(ResourceRepository resourceRepository, AnnoPageRepository annoPageRepository, FTSettings ftSettings, ObjectMapper mapper) {
        this.resourceRepository = resourceRepository;
        this.annoPageRepository = annoPageRepository;
        this.ftSettings = ftSettings;
        this.mapper = mapper;
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
     * @param textGranValues dcType values to filter annotations with
     * @throws AnnoPageDoesNotExistException when the Annopage cannot be found
     * @return AnnoPage
     */
    public AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId, List<AnnotationType> textGranValues) throws AnnoPageDoesNotExistException {
        AnnoPage result = annoPageRepository.findByDatasetLocalPageId(datasetId, localId, pageId, textGranValues);
        if (result == null) {
            throw new AnnoPageDoesNotExistException(String.format("/%s/%s/annopage/%s", datasetId, localId, pageId));
        }
        return result;
    }

    /**
     * Retrieve a cursor to AnnoPages with the provided datasetId, localId and imageIds. If the annotationType is
     * specified the returned AnnoPages will only contain annotations of that type. If annotationType is null or empty
     * then all annotations of that type will be returned. The cursor must be closed when the caller is done!
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param imageIds  IDs of the images
     * @param annoTypes type of annotations that should be retrieved, if null or empty all annotations of that
     *                        annopage will be retrieved
     * @return MorphiaCursor containing AnnoPage entries.
     */
    public MorphiaCursor<AnnoPage> fetchAnnoPageFromImageId(String datasetId, String localId, List<String> imageIds, List<AnnotationType> annoTypes) {
        return annoPageRepository.findByDatasetLocalImageId(datasetId, localId, imageIds, annoTypes);
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
            throw new AnnoPageDoesNotExistException(String.format("/%s/%s/anno/%s", datasetId, localId, annoId));
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
            throw new ResourceDoesNotExistException(String.format("/%s/%s/%s", datasetId, localId, resId));
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
