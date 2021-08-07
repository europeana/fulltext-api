package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.api.pgrepository.*;
import eu.europeana.fulltext.api.pgentity.*;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by luthien on 05/08/2021.
 */
@Service
public class PgService {

    private final PgAnnopageRepository pgAnnopageRepository;
    private final PgDatasetRepository  pgDatasetRepository;
    private final PgLocaldocRepository pgLocaldocRepository;
    private final PgLanguageRepository   pgLanguageRepository;
    private final PgRightsRepository     pgRightsRepository;
    private       PgAnnotationRepository pgAnnotationRepository;
    private       PgResourceRepository pgResourceRepository;


    /*
     * Constructs an FTService object with autowired dependencies
     */
    public PgService(
            PgDatasetRepository pgDatasetRepository,
            PgLocaldocRepository pgLocaldocRepository,
            PgLanguageRepository pgLanguageRepository,
            PgRightsRepository pgRightsRepository,
            PgAnnotationRepository pgAnnotationRepository,
            PgResourceRepository pgResourceRepository,
            PgAnnopageRepository pgAnnopageRepository) {
        this.pgDatasetRepository = pgDatasetRepository;
        this.pgLocaldocRepository = pgLocaldocRepository;
        this.pgLanguageRepository = pgLanguageRepository;
        this.pgRightsRepository = pgRightsRepository;
        this.pgAnnotationRepository = pgAnnotationRepository;
        this.pgResourceRepository = pgResourceRepository;
        this.pgAnnopageRepository = pgAnnopageRepository;
    }

    /**
     * Persist Annopage in PostgreSQL
     *
     * @param annoPage input data
     */
    public void saveFTRecord(AnnoPage annoPage) {
        List<PgAnnotation> pgAnnotationList = new ArrayList<>();
        Resource resource = annoPage.getRes();

        PgResource pgResource = new PgResource(addOrGetLanguage(resource.getLang()),
                                               addOrGetRights(resource.getRights()),
                                               true,
                                               resource.getValue(),
                                               resource.getSource());

        PgAnnopage pgAnnopage = new PgAnnopage(addOrGetDataset(annoPage.getDsId()),
                                               addOrGetLocaldoc(annoPage.getLcId()),
                                               annoPage.getPgId());

        for (Annotation annotation : annoPage.getAns()) {
            if (annotation.isTopLevel()){
                pgAnnotationList.add(new PgAnnotation(pgAnnopage,
                                                  annotation.getDcType()));
            } else {
                pgAnnotationList.add(new PgAnnotation(pgAnnopage,
                                                      annotation.getDcType(),
                                                      annotation.getFrom(),
                                                      annotation.getTo()));
            }
        }
        pgAnnopage.setTargetUrl(annoPage.getTgtId());
        pgAnnopage.setPgAnnotations(pgAnnotationList);
        pgAnnopage.setDateModified(annoPage.getModified());
        pgAnnopage.setPgResource(pgResource);
        pgResourceRepository.save(pgResource);
        pgAnnopageRepository.save(pgAnnopage);
    }

    private PgDataset addOrGetDataset(String value) {
        Optional<PgDataset> optPgDataset = pgDatasetRepository.findByValue(value);
        return optPgDataset.orElseGet(() -> pgDatasetRepository.save(new PgDataset(value)));
    }

    private PgLocaldoc addOrGetLocaldoc(String value) {
        Optional<PgLocaldoc> optPgLocaldoc = pgLocaldocRepository.findByValue(value);
        return optPgLocaldoc.orElseGet(() -> pgLocaldocRepository.save(new PgLocaldoc(value)));
    }

    private PgLanguage addOrGetLanguage(String value) {
        Optional<PgLanguage> optPgLanguage = pgLanguageRepository.findByValue(value);
        return optPgLanguage.orElseGet(() -> pgLanguageRepository.save(new PgLanguage(value)));
    }

    private PgRights addOrGetRights(String value) {
        Optional<PgRights> optPgRights = pgRightsRepository.findByValue(value);
        return optPgRights.orElseGet(() -> pgRightsRepository.save(new PgRights(value)));
    }

}
