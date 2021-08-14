package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.pgrepository.*;
import eu.europeana.fulltext.api.pgentity.*;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by luthien on 05/08/2021.
 */
@Service
public class PgService {

    private static final Logger LOG = LogManager.getLogger(PgService.class);
    private static final String GENERATED_IN    = "Generated in {} ms ";

    private final PgAnnopageRepository pgAnnopageRepository;
    private final PgResourceRepository pgResourceRepository;
    private final PgAPViewRepository   pgAPViewRepository;
    private final AnnoPageRepository   annoPageRepository;


    /*
     * Constructs an FTService object with autowired dependencies
     */
    public PgService(
            PgAnnopageRepository pgAnnopageRepository,
            PgResourceRepository pgResourceRepository,
            PgAPViewRepository pgAPViewRepository,
            AnnoPageRepository annoPageRepository) {
        this.pgAnnopageRepository = pgAnnopageRepository;
        this.pgResourceRepository = pgResourceRepository;
        this.pgAPViewRepository = pgAPViewRepository;
        this.annoPageRepository = annoPageRepository;
    }

    @Transactional
    public String persistDocsToPostgres(String datasetId, String localId) {

        List<AnnoPage> annoPages;

        if ("ALL".equalsIgnoreCase(localId)) {
            annoPages = annoPageRepository.findAllPagesForDs(datasetId);
        } else{
            annoPages = annoPageRepository.findOrigPages(datasetId, localId);
        }

        if (annoPages.isEmpty()) {
            return "No annopages found for " + datasetId + "/" + localId;
        }
        for (AnnoPage ap : annoPages) {
            saveFTRecord(ap);
        }
        return "Annopages for: " + datasetId + "/" + localId + " saved to PostgreSQL";
    }

    /**
     * Persist Annopage in PostgreSQL
     *
     * @param annoPage input data
     */
    @Transactional
    public void saveFTRecord(AnnoPage annoPage) {
        List<PgAnnotation> pgAnnotationList = new ArrayList<>();
        Resource           resource         = annoPage.getRes();

        LOG.info("Processing dsId: {}, lcId: {}, pgId: {}", annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId());

        PgResource pgResource = new PgResource(resource.getLang(),
                                               resource.getRights(),
                                               true,
                                               resource.getValue(),
                                               resource.getSource());

        PgAnnopage pgAnnopage = new PgAnnopage(Integer.parseInt(annoPage.getDsId()),
                                               annoPage.getLcId(),
                                               Integer.parseInt(annoPage.getPgId()),
                                               annoPage.getTgtId(),
                                               pgResource,
                                               annoPage.getModified());

        for (Annotation annotation : annoPage.getAns()) {
            if (annotation.isTopLevel()) {
                pgAnnotationList.add(new PgAnnotation(pgAnnopage, annotation.getDcType()));
            } else {
                pgAnnotationList.add(makeAnnotationWithTargets(pgAnnopage, annotation));
            }
        }

        pgAnnopage.setPgAnnotations(pgAnnotationList);
        pgResourceRepository.save(pgResource);
        pgAnnopageRepository.save(pgAnnopage);
    }

    private PgAnnotation makeAnnotationWithTargets(PgAnnopage pgAnnopage, Annotation annotation) {
        PgAnnotation pgAnnotation = new PgAnnotation(pgAnnopage,
                                                     annotation.getDcType(),
                                                     annotation.getFrom(),
                                                     annotation.getTo());
        List<PgTarget> pgTargetList = new ArrayList<>();
        for (Target target : annotation.getTgs()) {
            if (annotation.isMedia()) {
                pgTargetList.add(new PgTarget(pgAnnotation, target.getStart(), target.getEnd()));
            } else {
                pgTargetList.add(new PgTarget(pgAnnotation,
                                              target.getX(),
                                              target.getY(),
                                              target.getW(),
                                              target.getH()));
            }
        }
        pgAnnotation.setPgTargets(pgTargetList);
        return pgAnnotation;
    }

    public PgAPView retrievePGAnnoPage(
            String dataset,
            String local,
            String page,
            String lang,
            List<AnnotationType> textGranValues) {
        if (StringUtils.isBlank(lang)) {
            return pgAPViewRepository.findFirstByDatasetAndLocaldocAndPageAndOriginalTrue(
                    Integer.parseInt(dataset),
                    local,
                    Integer.parseInt(page));
        } else {
            return pgAPViewRepository.findFirstByDatasetAndLocaldocAndPageAndLanguage(
                    Integer.parseInt(dataset),
                    local,
                    Integer.parseInt(page),
                    lang);
        }
    }

    /**
     * Generates an AnnotationPageV3 (IIIF V3 response type) object with the AnnoPage as input
     * @param pgAPView AnnoPage view input object
     * @param derefResource boolean indicating whether to dereference the Resource object on the top level Annotation
     * @return AnnotationPageV3
     */
    public AnnotationPageV3 getAPageV3FromPgApView(PgAPView pgAPView, boolean derefResource){
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(pgAPView, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationPageV2 (IIIF V3 response type) object with the AnnoPage as input
     * @param pgAPView AnnoPage view input object
     * @param derefResource boolean indicating whether to dereference the Resource object on the top level Annotation
     * @return AnnotationPageV3
     */
    public AnnotationPageV2 getAPageV2FromPgApView(PgAPView pgAPView, boolean derefResource){
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(pgAPView, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }
}
