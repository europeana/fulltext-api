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
import eu.europeana.fulltext.api.config.FTDefinitions;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.entity.AnnoPage;
import eu.europeana.fulltext.api.entity.Annotation;
import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.entity.Target;
import eu.europeana.fulltext.api.model.FullTextResource;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.repository.AnnoPageRepository;
import eu.europeana.fulltext.api.repository.AnnotationRepository;
import eu.europeana.fulltext.api.repository.ResourceRepository;
import eu.europeana.fulltext.api.service.exception.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    ResourceRepository resourceRepository;

    @Autowired
    AnnotationRepository annotationRepository;

    @Autowired
    AnnoPageRepository annoPageRepository;


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

    public AnnotationPageV2 getAnnotationPageV2(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        return generateAnnoPageV2(fetchAnnoPage(datasetId, localId, pageId));
    }

    public AnnotationPageV3 getAnnotationPageV3(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        return generateAnnoPageV3(fetchAnnoPage(datasetId, localId, pageId));
    }

    public AnnotationV2 getAnnotationV2(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        return generateAnnotationV2(fetchAPAnnotation(datasetId, localId, annoId), annoId);
    }

    public AnnotationV3 getAnnotationV3(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        return generateAnnotationV3(fetchAPAnnotation(datasetId, localId, annoId), annoId);
    }

    public FullTextResource getFullTextResource(String datasetId, String localId, String resId)
            throws ResourceDoesNotExistException {
        Resource resource;
        try{
            resource = resourceRepository.findByDatasetLocalAndResId(datasetId, localId, resId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new ResourceDoesNotExistException("No Fulltext Resource with resourceId: " + resId
                  + " was found that is associated with datasetId: " + datasetId + " and localId: " + localId );
        }
        return generateFullTextResource(resource);
    }

    private AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId)
            throws AnnoPageDoesNotExistException {
        try {
            return annoPageRepository.findByDatasetLocalAndPageId(datasetId, localId, pageId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + ", localId: "
                                                    + localId + " and pageId: " + pageId + " could be found");
        }
    }

    private AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
            throws AnnoPageDoesNotExistException {
        try {
            return annoPageRepository.findByDatasetLocalAndAnnoId(datasetId, localId, annoId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException("No AnnoPage with datasetId: " + datasetId + " and localId: "
                      + localId + " could be found that contains an Annotation with annotationId: " + annoId);
        }
    }

    public boolean doesAnnoPageNotExist(String datasetId, String localId, String annoId){
        return annoPageRepository.findByDatasetLocalAndPageId(datasetId, localId, annoId).isEmpty();
    }

    private AnnotationPageV3 generateAnnoPageV3(eu.europeana.fulltext.api.entity.AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationPageV2 generateAnnoPageV2(eu.europeana.fulltext.api.entity.AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV3 generateAnnotationV3(eu.europeana.fulltext.api.entity.AnnoPage annoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId){
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
