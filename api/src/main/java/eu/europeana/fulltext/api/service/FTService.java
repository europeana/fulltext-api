package eu.europeana.fulltext.api.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.repository.impl.AnnoPageRepositoryImpl;
import eu.europeana.fulltext.api.repository.impl.ResourceRepositoryImpl;
import eu.europeana.fulltext.api.service.exception.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 *
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class FTService {

    private static final Logger LOG      = LogManager.getLogger(FTService.class);

    @Autowired
    ResourceRepositoryImpl resourceRepositoryImpl;

    @Autowired
    AnnoPageRepositoryImpl annoPageRepositoryImpl;


    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FTSettings ftSettings;

    public FTService() {

        // configure jsonpath: we use jsonpath in combination with Jackson because that makes it easier to know what
        // type of objects are returned (see also https://stackoverflow.com/a/40963445)
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonNodeJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                if (ftSettings.getSuppressParseException()) {
                    // we want to be fault tolerant in production, but for testing we may want to disable this option
                    return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
                } else {
                    return EnumSet.noneOf(Option.class);
                }
            }
        });

        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule());
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }

    public AnnotationPageV2 getAnnotationPageV2(String datasetId, String localId, String pageId, boolean includeContext)
            throws AnnoPageDoesNotExistException {
        return generateAnnoPageV2(fetchAnnoPage(datasetId, localId, pageId), includeContext);
    }

    public AnnotationPageV3 getAnnotationPageV3(String datasetId, String localId, String pageId, boolean includeContext)
            throws AnnoPageDoesNotExistException {
        return generateAnnoPageV3(fetchAnnoPage(datasetId, localId, pageId), includeContext);
    }

    public AnnotationV2 getAnnotationV2(String datasetId, String localId, String annoId, boolean includeContext)
            throws AnnoPageDoesNotExistException {
        return generateAnnotationV2(fetchAPAnnotation(datasetId, localId, annoId), annoId, includeContext);
    }

    public AnnotationV3 getAnnotationV3(String datasetId, String localId, String annoId, boolean includeContext)
            throws AnnoPageDoesNotExistException {
        return generateAnnotationV3(fetchAPAnnotation(datasetId, localId, annoId), annoId, includeContext);
    }

    public FullTextResource getFullTextResource(String datasetId, String localId, String resId, boolean includeContext)
            throws ResourceDoesNotExistException {
        if (doesResourceExist_exists(datasetId, localId, resId)){
            return generateFullTextResource(
                    resourceRepositoryImpl.findByDatasetLocalAndResId(datasetId, localId, resId),
                    includeContext);
        } else {
            throw new ResourceDoesNotExistException("No Fulltext Resource with resourceId: " + resId
                      + " was found that is associated with datasetId: " + datasetId + " and localId: " + localId );
        }
    }

    private AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnoPageExist_exists(datasetId, localId, pageId)){
            return annoPageRepositoryImpl.findByDatasetLocalAndPageId(datasetId, localId, pageId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + ", localId: "
                      + localId + " and pageId: " + pageId + " could be found");
        }
    }

    private AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnotationExist_exists(datasetId, localId, annoId)){
            return annoPageRepositoryImpl.findByDatasetLocalAndAnnoId(datasetId, localId, annoId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + " and localId: "
                       + localId + " could be found that contains an Annotation with annotationId: " + annoId);
        }
    }

//    @Deprecated // keeping this temporarily for testing speed (EA-1239)
//    public boolean doesAnnoPageExist_findNotEmpty(String datasetId, String localId, String annoId){
//        return !annoPageRepositoryImpl.findByDatasetLocalAndPageId(datasetId, localId, annoId).isEmpty();
//    }
//
//    @Deprecated // keeping this temporarily for testing speed (EA-1239)
//    public boolean doesAnnoPageExist_findOneNotNull(String datasetId, String localId, String annoId){
//        return annoPageRepositoryImpl.findOneWithId(datasetId, localId, annoId) != null;
//    }

    /**
     * Check if a particular annotation page with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param pageId
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnoPageExist_exists(String datasetId, String localId, String pageId){
        return annoPageRepositoryImpl.existsWithPageId(datasetId, localId, pageId);
    }

    /**
     * Check if a particular annotation with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param annoId
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnotationExist_exists(String datasetId, String localId, String annoId){
        return annoPageRepositoryImpl.existsWithAnnoId(datasetId, localId, annoId);
    }

    /**
     * Check if a particular resource with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param resId
     * @return true if it exists, otherwise false
     */
    public boolean doesResourceExist_exists(String datasetId, String localId, String resId){
        return resourceRepositoryImpl.existsWithDatasetLocalAndResId(datasetId, localId, resId);
    }

    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    public boolean doesAnnoPageExist_countNotZero(String datasetId, String localId, String annoId){
        return annoPageRepositoryImpl.countWithId(datasetId, localId, annoId) > 0;
    }

    private AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage, boolean includeContext){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage, includeContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage, boolean includeContext){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage, includeContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId, boolean includeContext){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId, includeContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId, boolean includeContext){
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId, includeContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private FullTextResource generateFullTextResource(Resource resource, boolean includeContext){
        long start = System.currentTimeMillis();
        FullTextResource result = EDM2IIIFMapping.getFullTextResource(resource, includeContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Serialize resource from MongoDB to JSON-LD
     * @param res resource
     * @return JSON-LD string
     * @throws RecordParseException when there is a problem parsing
     */
    public String serializeResource(Object res) throws SerializationException {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(res);
        }
        catch (IOException e) {
            throw new SerializationException("Error serializing data: " + e.getMessage(), e);
        }
    }

}
