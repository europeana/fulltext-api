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
import eu.europeana.fulltext.common.entity.AnnoPage;
import eu.europeana.fulltext.common.entity.Resource;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.common.repository.impl.AnnoPageRepositoryImpl;
import eu.europeana.fulltext.common.repository.impl.ResourceRepositoryImpl;
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

    /**
     * @return ManifestSettings object containing settings loaded from properties file
     */
    public FTSettings getSettings() {
        return ftSettings;
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public FullTextResource getFullTextResource(String datasetId, String localId, String resId)
            throws ResourceDoesNotExistException {
        if (doesResourceExist(datasetId, localId, resId)){
            return generateFullTextResource(
                    resourceRepositoryImpl.findByDatasetLocalResId(datasetId, localId, resId));
        } else {
            throw new ResourceDoesNotExistException("No Fulltext Resource with resourceId: " + resId
                      + " was found that is associated with datasetId: " + datasetId + " and localId: " + localId );
        }
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnoPageExistByLimitOne(datasetId, localId, pageId)){
            return annoPageRepositoryImpl.findByDatasetLocalPageId(datasetId, localId, pageId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + ", localId: "
                      + localId + " and pageId: " + pageId + " could be found");
        }
    }

    public AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        if (doesAnnotationExist(datasetId, localId, annoId)){
            return annoPageRepositoryImpl.findByDatasetLocalAnnoId(datasetId, localId, annoId);
        } else {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + " and localId: "
                       + localId + " could be found that contains an Annotation with annotationId: " + annoId);
        }
    }


    // = = [ check Document existence ]= = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Check if a particular annotation page with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param pageId
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnoPageExistByLimitOne(String datasetId, String localId, String pageId){
        return annoPageRepositoryImpl.existsByLimitOne(datasetId, localId, pageId);
    }

    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    public boolean doesAnnoPageExistsByFindOne(String datasetId, String localId, String pageId){
        return annoPageRepositoryImpl.existsByFindOne(datasetId, localId, pageId);
    }

    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    public boolean doesAnnoPageExistByCount(String datasetId, String localId, String pageId){
        return annoPageRepositoryImpl.existsByCount(datasetId, localId, pageId);
    }

    /**
     * Check if a particular annotation with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param annoId
     * @return true if it exists, otherwise false
     */
    private boolean doesAnnotationExist(String datasetId, String localId, String annoId){
        return annoPageRepositoryImpl.existsWithAnnoId(datasetId, localId, annoId);
    }

    /**
     * Check if a particular resource with the provided ids exists or not
     * @param datasetId
     * @param localId
     * @param resId
     * @return true if it exists, otherwise false
     */
    private boolean doesResourceExist(String datasetId, String localId, String resId){
        return resourceRepositoryImpl.existsByLimitOne(datasetId, localId, resId);
    }


    // = = [ generate JSON objects ] = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    public AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private FullTextResource generateFullTextResource(Resource resource){
        long start = System.currentTimeMillis();
        FullTextResource result = EDM2IIIFMapping.getFullTextResource(resource);
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
