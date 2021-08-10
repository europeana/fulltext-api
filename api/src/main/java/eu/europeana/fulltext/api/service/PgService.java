package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.api.pgrepository.*;
import eu.europeana.fulltext.api.pgentity.*;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
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

    private final PgAnnopageRepository   pgAnnopageRepository;
    private       PgAnnotationRepository pgAnnotationRepository;
    private       PgResourceRepository   pgResourceRepository;


    /*
     * Constructs an FTService object with autowired dependencies
     */
    public PgService(
            PgAnnotationRepository pgAnnotationRepository,
            PgResourceRepository pgResourceRepository,
            PgAnnopageRepository pgAnnopageRepository) {
        this.pgAnnotationRepository = pgAnnotationRepository;
        this.pgResourceRepository = pgResourceRepository;
        this.pgAnnopageRepository = pgAnnopageRepository;
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

}
