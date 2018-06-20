package eu.europeana.fulltext.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.fulltext.config.FTDefinitions;
import eu.europeana.fulltext.config.FTSettings;
import eu.europeana.fulltext.entity.FTAnnotation;
import eu.europeana.fulltext.entity.FTPage;
import eu.europeana.fulltext.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.repository.FTAnnotationRepository;
import eu.europeana.fulltext.repository.FTResourceRepository;
import eu.europeana.fulltext.service.exception.RecordParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 *
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class FTService {

    @Autowired
    FTResourceRepository   ftResRepo;

    @Autowired
    FTAnnotationRepository ftAnnRepo;

    private static final Logger LOG = LogManager.getLogger(FTService.class);

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    private FTSettings FTSettings;

    public FTService(FTSettings FTSettings) {
        this.FTSettings = FTSettings;

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
                if (FTSettings.getSuppressParseException()) {
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


    public Optional<FTAnnotation> findAnnotation(String datasetId, String recordId, String annoId){
            return ftAnnRepo.findById(datasetId + "/" + recordId + "/" + annoId);
    }


    public String getAnnotation(String datasetId, String recordId, String annoId){
//        return ftAnnRepo.findById(datasetId + "/" + recordId + "/" + annoId);
        FTAnnotation res = ftAnnRepo.findById(datasetId + "/" + recordId + "/" + annoId).get();
        return "";
    }

    public AnnotationPageV2 getAnnotationPageV2(String datasetId, String recordId, String pageId){
        FTPage ftPage = ftResRepo.findById(createFtResourceId(datasetId, recordId, pageId)).get();
        return generateAnnoPageV2(ftPage);
    }

    public AnnotationPageV3 getAnnotationPageV3(String datasetId, String recordId, String pageId){
        FTPage ftPage = ftResRepo.findById(createFtResourceId(datasetId, recordId, pageId)).get();
        return generateAnnoPageV3(ftPage);
    }

    /**
     * @return FulltextConfig object containing properties and Mongo datastore
     */
    public FTSettings getConfig() {
        return FTSettings;
    }


    private AnnotationPageV3 generateAnnoPageV3(FTPage ftRes){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(ftRes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationPageV2 generateAnnoPageV2(FTPage ftRes){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(ftRes);

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
    public String serializeResource(Object res) throws RecordParseException {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(res);
        }
        catch (IOException e) {
            throw new RecordParseException("Error serializing data: "+e.getMessage(), e);
        }
    }



    private String createFtResourceId(String datasetId, String recordId, String pageId){
        return FTDefinitions.IIIFURL + "/presentation/" + datasetId + "/" + recordId + "/annopage/" + pageId;
    }

    private String createTargetUrl(String datasetId, String recordId, String imageId){
        return FTDefinitions.IIIFURL + "/presentation/" + datasetId + "/" + recordId + "/canvas/" + imageId;
    }

    private String createSourceUrl(String datasetId, String recordId, String fullTextId){
        return FTDefinitions.FULLTEXTURL + "/" + datasetId + "/" + recordId + "/" + fullTextId;
    }

    private String createFtAnnoIdBase(String datasetId, String recordId){
        return FTDefinitions.IIIFURL + "/" + datasetId + "/" + recordId + "/annotation/";
    }


    /* TEST STUFF BELOW  */


    /**
     * initial Mongo and Morphia setup testing
     */
    private FTPage       ftr;
    private FTAnnotation fta0;
    private FTAnnotation fta1;
    private FTAnnotation fta2;

    public void createTestRecords(){
        String value = "Wat wil Wickie? Wickie willah Koeckebacke!";

        String datasetId = "9200356";
        String recordId = "BibliographicResource_3000100331503";
        String pageId = "1";
        String imageId = "p1";
        String sourceId = "XPTO";

        String ftResourceId = createFtResourceId(datasetId, recordId, pageId);
        String targetUrl    = createTargetUrl(datasetId, recordId, imageId);
        String sourceUrl    = createSourceUrl(datasetId, recordId, sourceId);
        String ftAnnoIdBase = createFtAnnoIdBase(datasetId, recordId);

        fta0 = createFTAnnotation(ftAnnoIdBase + "0", "P", "transcribing", null,
                                  0, 0, 0, 0, 0, 0, ftResourceId);
        fta1 = createFTAnnotation(ftAnnoIdBase + "1", "W", "transcribing", "nl",
                                  0, 2, 0, 0, 6, 10, ftResourceId);
        fta2 = createFTAnnotation(ftAnnoIdBase + "2", "W", "transcribing", null,
                                  4, 6, 10, 0, 7, 10, ftResourceId);

        ftr = createFTResource(ftResourceId, "en", targetUrl, sourceUrl, value);

        ftAnnRepo.save(fta0);
        ftAnnRepo.save(fta1);
        ftAnnRepo.save(fta2);

        ftr.setPageAnnotation(fta0);
        ftr.setFTAnnotations(Arrays.asList(fta1, fta2));
        ftResRepo.save(ftr);
    }

    public FTAnnotation createFTAnnotation(String id, String dcType, String motivation, String language,
                                    Integer textStart, Integer textEnd, Integer targetX,
                                    Integer targetY, Integer targetW, Integer targetH, String pageId){
        return new FTAnnotation(id, dcType, motivation, language, textStart, textEnd,
                                targetX, targetY, targetW, targetH, pageId);
    }

    public FTPage createFTResource(String idUrl, String language, String targetUrl, String sourceUrl, String value) {
        return new FTPage(idUrl, language, targetUrl, sourceUrl, value);
    }


}
