package eu.europeana.fulltext.batch.processor;

import eu.europeana.fulltext.api.service.SubtitleService;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnotationProcessor implements ItemProcessor<AnnotationItem, TranslationAnnoPage> {

  private final SubtitleService subtitleService;

  public AnnotationProcessor(SubtitleService subtitleService) {
    this.subtitleService = subtitleService;
  }

  @Override
  public TranslationAnnoPage process(@NonNull AnnotationItem item) throws Exception {
    AnnotationPreview annotationPreview = subtitleService.createAnnotationPreview(item);
    return subtitleService.createAnnoPage(annotationPreview);
  }
}