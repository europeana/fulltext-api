package eu.europeana.fulltext.batch.processor;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import eu.europeana.fulltext.util.AnnotationUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnotationProcessor implements ItemProcessor<AnnotationItem, AnnoPage> {

  private final FTService ftService;

  public AnnotationProcessor(FTService ftService) {
    this.ftService = ftService;
  }

  @Override
  public AnnoPage process(@NonNull AnnotationItem item) throws Exception {
    AnnotationPreview annotationPreview = AnnotationUtils.createAnnotationPreview(item);
    return ftService.createAnnoPage(annotationPreview, true);
  }
}
