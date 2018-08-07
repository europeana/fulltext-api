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
import eu.europeana.fulltext.batchloader.*;
import eu.europeana.fulltext.config.FTDefinitions;
import eu.europeana.fulltext.config.FTSettings;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.model.v2.AnnotationV2;
import eu.europeana.fulltext.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.model.v3.AnnotationV3;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.AnnotationRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.service.exception.FTException;
import eu.europeana.fulltext.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.service.exception.RecordParseException;
import eu.europeana.fulltext.service.exception.SerializationException;
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

    private static final Logger LOG = LogManager.getLogger(FTService.class);

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

    public AnnotationPageV2 getAnnotationPageV2(String datasetId, String recordId, String pageId)
            throws AnnoPageDoesNotExistException {
        AnnoPage annoPage;
        try {
            annoPage = annoPageRepository.findByDatasetLocalAndPageId(datasetId, recordId, pageId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + recordId + "/" + pageId);
        }
        return generateAnnoPageV2(annoPage);
    }

    public AnnotationPageV3 getAnnotationPageV3(String datasetId, String recordId, String pageId)
            throws AnnoPageDoesNotExistException {
        AnnoPage annoPage;
        try {
            annoPage = annoPageRepository.findByDatasetLocalAndPageId(datasetId, recordId, pageId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + recordId + "/" + pageId);
        }
        return generateAnnoPageV3(annoPage);
    }

    public AnnotationV3 getAnnotationV3(String datasetId, String recordId, String annoId){
        AnnoPage annoPage = annoPageRepository.findByDatasetLocalAndAnnoId(datasetId, recordId, annoId).get(0);
        return generateAnnotationV3(annoPage, annoId);
    }

    public AnnotationV2 getAnnotationV2(String datasetId, String recordId, String annoId){
        AnnoPage annoPage = annoPageRepository.findByDatasetLocalAndAnnoId(datasetId, recordId, annoId).get(0);
        return generateAnnotationV2(annoPage, annoId);
    }

    public boolean doesAnnoPageNotExist(String datasetId, String recordId, String annoId){
        return annoPageRepository.findByDatasetLocalAndPageId(datasetId, recordId, annoId).isEmpty();
    }


    private AnnotationPageV3 generateAnnoPageV3(eu.europeana.fulltext.entity.AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationPageV2 generateAnnoPageV2(eu.europeana.fulltext.entity.AnnoPage annoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV3 generateAnnotationV3(eu.europeana.fulltext.entity.AnnoPage annoPage, String annoId){
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

    public void saveAPList(List<AnnoPageRdf> apList) {
        for (AnnoPageRdf annoPageRdf : apList){
            String[] identifiers = StringUtils.split(
                    StringUtils.removeStartIgnoreCase(annoPageRdf.getFtResource(), ftSettings.getResourceBaseUrl()), '/');
            if (identifiers.length > 3){
                LOG.error("Error", new FTException("Please check Resource Base URL settings in properties file: '"
                                                   + ftSettings.getResourceBaseUrl()
                                                   + "', making sure that it matches with the 'ENTITY text' value found in import file: '"
                                                   + annoPageRdf.getFtResource() + "'"));
            }
            Resource resource = new Resource(identifiers[2], annoPageRdf.getFtLang(), annoPageRdf.getFtText());
            try{
                resourceRepository.save(resource);
            } catch (Exception e){
                LOG.error("Error saving resource with resId: " + identifiers[2], e);
            }
            AnnoPage annoPage = new AnnoPage(
                                identifiers[0],
                                identifiers[1],
                                annoPageRdf.getPageId(),
                                annoPageRdf.getImgTargetBase(),
                                resource);
            annoPage.setAns(createAnnoList(annoPageRdf, identifiers[0]));
            System.out.println("dsID: " + identifiers[0] + " lcId:" + identifiers[1] + " pgId:" + annoPageRdf.getPageId());
            try{
                annoPageRepository.save(annoPage);
            } catch (Exception e){
                LOG.error("Error saving AnnoPage for Dataset: " + identifiers[0]
                          + ", LocalId: " + identifiers[1]
                          + ", PageId: " + annoPageRdf.getPageId(), e);
            }
        }
        LOG.debug("done.");
    }

    public void importZipBatch(String archive){
        LoadArchives la = new LoadArchives(this);
        String batchBaseDirectory = ftSettings.getBatchBaseDirectory();
        String zipBatchDir = StringUtils.removeEnd(batchBaseDirectory, "/") + "/";
        if (StringUtils.equalsIgnoreCase(archive, FTDefinitions.ALL_ARCHIVES)){
            try {
                Files.walkFileTree(Paths.get(zipBatchDir), la);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LoadArchives.processArchive(zipBatchDir + archive);
        }
    }

    public void importBatch(String directory){
        LoadFiles lf = new LoadFiles(this);
        String batchDir = ftSettings.getBatchBaseDirectory()
                          + (StringUtils.isNotBlank(directory) ? "/" + directory : "");
        try {
            Files.walkFileTree(Paths.get(batchDir), lf);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            String error = "dc:type not set or null for Annotation with ID: " + annoId
                           + " on Annotation Page: " + pageId + " for Dataset: " + dataSetId;
            LOG.error(error);
            System.out.println(error);
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



    /* DEVELOPMENT TEST DATA  */

    private AnnoPage     pag0;
    private List<Target> tar0;
    private List<Target> tar1;
    private List<Target> tar2;
    private Resource     res0;
    private Annotation   ann0;
    private Annotation   ann1;
    private Annotation   ann2;

    public void createTestRecords(){
        String fullText     = "Wat wil Wickie? Wickie willah Koeckebacke!";
        String datasetId    = "9200356";
        String localId      = "BibliographicResource_3000100331503";
        String pageId       = "1";
        String targetId     = "p1";
        String resId        = "123123123";
        String resourceId   = "XPTO";

        tar0 = Arrays.asList(new Target(0, 0, 0, 0));
        tar1 = Arrays.asList(new Target(110, 70, 30, 11),
                             new Target(144, 72, 18, 11));
        tar2 = Arrays.asList(new Target(12, 98, 23, 12));

        res0 = new Resource(resId, "en", fullText);
        resourceRepository.save(res0);

        ann0 = new Annotation("0", "page", 0, 0, tar0);
        ann1 = new Annotation("1", "word", 0, 2, tar1);
        ann2 = new Annotation("2", "word", 4, 6, tar2, "nl");

        pag0 = new AnnoPage(datasetId, localId, pageId, targetId, res0);
        pag0.setPgAn(ann0);
        pag0.setAns(Arrays.asList(ann1, ann2));
        annoPageRepository.save(pag0);
    }

}
