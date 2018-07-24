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
import eu.europeana.fulltext.batchloader.AnnoPage;
import eu.europeana.fulltext.batchloader.Annotation;
import eu.europeana.fulltext.batchloader.LoadFiles;
import eu.europeana.fulltext.batchloader.Target;
import eu.europeana.fulltext.config.FTSettings;
import eu.europeana.fulltext.entity.FTAnnoPage;
import eu.europeana.fulltext.entity.FTAnnotation;
import eu.europeana.fulltext.entity.FTResource;
import eu.europeana.fulltext.entity.FTTarget;
import eu.europeana.fulltext.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.model.v2.AnnotationV2;
import eu.europeana.fulltext.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.model.v3.AnnotationV3;
import eu.europeana.fulltext.repository.FTAnnoPageRepository;
import eu.europeana.fulltext.repository.FTAnnotationRepository;
import eu.europeana.fulltext.repository.FTResourceRepository;
import eu.europeana.fulltext.service.exception.FTException;
import eu.europeana.fulltext.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.service.exception.RecordParseException;
import eu.europeana.fulltext.web.FTController;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static Path startingDir = Paths.get("/Users/luthien/Downloads/batch3");

    @Autowired
    FTResourceRepository   ftResRepo;

    @Autowired
    FTAnnotationRepository ftAnnoRepo;

    @Autowired
    FTAnnoPageRepository ftAPRepo;


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
        FTAnnoPage ftAnnoPage;
        try {
            ftAnnoPage = ftAPRepo.findByDatasetLocalAndPageId(datasetId, recordId, pageId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + recordId + "/" + pageId);
        }
        return generateAnnoPageV2(ftAnnoPage);
    }

    public AnnotationPageV3 getAnnotationPageV3(String datasetId, String recordId, String pageId)
            throws AnnoPageDoesNotExistException {
        FTAnnoPage ftAnnoPage;
        try {
            ftAnnoPage = ftAPRepo.findByDatasetLocalAndPageId(datasetId, recordId, pageId).get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + recordId + "/" + pageId);
        }
        return generateAnnoPageV3(ftAnnoPage);
    }

    public AnnotationV3 getAnnotationV3(String datasetId, String recordId, String annoId){
        FTAnnoPage ftAnnoPage = ftAPRepo.findByDatasetLocalAndAnnoId(datasetId, recordId, annoId).get(0);
        return generateAnnotationV3(ftAnnoPage, annoId);
    }

    public AnnotationV2 getAnnotationV2(String datasetId, String recordId, String annoId){
        FTAnnoPage ftAnnoPage = ftAPRepo.findByDatasetLocalAndAnnoId(datasetId, recordId, annoId).get(0);
        return generateAnnotationV2(ftAnnoPage, annoId);
    }

    public boolean doesAnnoPageNotExist(String datasetId, String recordId, String annoId){
        return ftAPRepo.findByDatasetLocalAndPageId(datasetId, recordId, annoId).isEmpty();
    }


    private AnnotationPageV3 generateAnnoPageV3(FTAnnoPage ftAnnoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(ftAnnoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationPageV2 generateAnnoPageV2(FTAnnoPage ftAnnoPage){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(ftAnnoPage);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV3 generateAnnotationV3(FTAnnoPage ftAnnoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(ftAnnoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    private AnnotationV2 generateAnnotationV2(FTAnnoPage ftAnnoPage, String annoId){
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(ftAnnoPage, annoId);
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

    public void saveAPList(List<AnnoPage> apList) {
        for (AnnoPage annoPage : apList){
            String[] identifiers = StringUtils.split(
                    StringUtils.removeStartIgnoreCase(annoPage.getFtResource(), ftSettings.getResourceBaseUrl()), '/');
            if (identifiers.length > 3){
                LOG.error("Error", new FTException("Please check Resource Base URL settings in properties file: '"
                                      + ftSettings.getResourceBaseUrl()
                                      + "', making sure that it matches with the 'ENTITY text' value found in import file: '"
                                      + annoPage.getFtResource() + "'"));
            }
            FTResource ftResource = new FTResource(identifiers[2], annoPage.getFtText());
            ftResRepo.save(ftResource);
            FTAnnoPage ftAnnoPage = new FTAnnoPage(identifiers[0], identifiers[1], annoPage.getPageId(),
                                               annoPage.getFtLang(), ftResource, annoPage.getImgTargetBase());
            ftAnnoPage.setAns(createFTAnnoList(annoPage, ftResource));
            ftAPRepo.save(ftAnnoPage);
        }
        LOG.debug("done.");
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

    private List<FTAnnotation> createFTAnnoList(AnnoPage annoPage, FTResource ftResource){
        List<FTAnnotation> ftAnnotationList = new ArrayList<>();
        for (Annotation annotation : annoPage.getAnnotationList()){
            FTAnnotation ftAnnotation = new FTAnnotation(annotation.getId(), annotation.getDcType(),
                                                         annotation.getFrom(), annotation.getTo(), ftResource );
            if (StringUtils.isNotBlank(annotation.getLang())){
                ftAnnotation.setLang(annotation.getLang());
            }
            ftAnnotation.setTgs(createFTTargetList(annotation));
            ftAnnotationList.add(ftAnnotation);
        }
        return ftAnnotationList;
    }

    private List<FTTarget> createFTTargetList(Annotation annotation){
        List<FTTarget> ftTargetList = new ArrayList<>();
        for (Target target : annotation.getTargetList()){
            ftTargetList.add(new FTTarget(target.getX(), target.getY(), target.getW(), target.getH()));
        }
        return ftTargetList;
    }



    /* TEST STUFF BELOW  */


    /**
     * initial Mongo and Morphia setup testing
     */
    private FTAnnoPage     pag0;
    private List<FTTarget> tar0;
    private List<FTTarget> tar1;
    private List<FTTarget> tar2;
    private FTResource     res0;
    private FTAnnotation   ann0;
    private FTAnnotation   ann1;
    private FTAnnotation   ann2;

    public void createTestRecords(){
        String fullText     = "Wat wil Wickie? Wickie willah Koeckebacke!";
        String datasetId    = "9200356";
        String localId      = "BibliographicResource_3000100331503";
        String pageId       = "1";
        String targetId     = "p1";
        String resourceId   = "XPTO";

        tar0 = Arrays.asList(newFTTarget(0, 0, 0, 0));
        tar1 = Arrays.asList(newFTTarget(110, 70, 30, 11),
                             newFTTarget(144, 72, 18, 11));
        tar2 = Arrays.asList(newFTTarget(12, 98, 23, 12));

        res0 = saveFTResource(fullText);

        ann0 = newFTAnnotation("0", "page", 0, 0, res0, tar0);
        ann1 = newFTAnnotation("1", "word", 0, 2, res0, tar1);
        ann2 = newFTAnnotation("2", "word", 4, 6, res0, tar2, "nl");

        pag0 = newFTAnnoPage(datasetId, localId, pageId, "en", res0, targetId);
        pag0.setPgAn(ann0);
        pag0.setAns(Arrays.asList(ann1, ann2));
        ftAPRepo.save(pag0);
    }

    public FTAnnoPage newFTAnnoPage(String datasetId, String localId, String pageId, String language,
                                    FTResource res, String targetId){
        return new FTAnnoPage(datasetId, localId, pageId, language, res, targetId); }


    public FTAnnotation newFTAnnotation(String annoId, String dcType, Integer textStart, Integer textEnd,
                                         FTResource resource, List<FTTarget> ftTargets){
        return new FTAnnotation(annoId, dcType, textStart, textEnd, resource, ftTargets);
    }

    public FTAnnotation newFTAnnotation(String annoId, String dcType, Integer textStart, Integer textEnd,
                                        FTResource resource, List<FTTarget> ftTargets, String annoLanguage){
        return new FTAnnotation(annoId, dcType, textStart, textEnd, resource, ftTargets, annoLanguage);
    }

    public FTResource saveFTResource(String fullText) {
        FTResource res = new FTResource(fullText);
        ftResRepo.save(res);
        return res;
    }

    public FTTarget newFTTarget(Integer targetX, Integer targetY, Integer targetW, Integer targetH){
        return new FTTarget(targetX, targetY, targetW, targetH);
    }



}
