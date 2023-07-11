package eu.europeana.fulltext.util;

import static eu.europeana.fulltext.WebConstants.MOTIVATION_CAPTIONING;
import static eu.europeana.fulltext.WebConstants.MOTIVATION_TRANSCRIBING;

import eu.europeana.fulltext.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltext.exception.SubtitleParsingException;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.subtitles.external.AnnotationItem;
import org.apache.commons.lang3.StringUtils;

public class AnnotationUtils {

    /**
     * Create AnnotationPreview from AnnotationItem
     * @param item
     * @return
     * @throws SubtitleParsingException
     */
    public static AnnotationPreview createAnnotationPreview(AnnotationItem item)
            throws MediaTypeNotSupportedException {
        // there might be data with format values containing version or charset. Hence, get the format value before ";"
        FulltextType fulltextType = FulltextType.getValueByMimetype(StringUtils.substringBefore(item.getBody().getFormat(), ";"));

        if (fulltextType == null) {
            throw new MediaTypeNotSupportedException(
                    String.format(
                            "Unsupported mimeType in Annotation id=%s body.format=%s",
                            item.getId(), item.getBody().getFormat()));
        }
        return new AnnotationPreview.Builder(
                GeneralUtils.getRecordIdFromUri(item.getTarget().getScope()),
                fulltextType,
                item.getBody().getValue())
                .setSource(item.getId())
                .setMedia(item.getTarget().getSource())
                .setLanguage(item.getBody().getLanguage())
                .setRights(item.getBody().getEdmRights())
                // If the motivation is “captioning” or "transcribing" , then originalLang is true
                // for the moment, we only have the original text and no translations yet for transcription and newspapers
                .setOriginalLang(MOTIVATION_CAPTIONING.equals(item.getMotivation()) || MOTIVATION_TRANSCRIBING.equals(item.getMotivation()))
                .build();
    }

    /** Create AnnotationPreview */
    public static AnnotationPreview createAnnotationPreview(
            String datasetId,
            String localId,
            String lang,
            boolean originalLang,
            String rights,
            String source,
            String media,
            String content,
            FulltextType type) {
        String recordId = GeneralUtils.generateRecordId(datasetId, localId);
        // if transcriptions ie; Fulltext Type is SRT, then original lang will be true
        // for the moment, we only have the original text and no translations yet for transcription
        if (type != null && type.equals(FulltextType.PLAIN)) {
            originalLang = true;
        }
        return new AnnotationPreview.Builder(recordId, type, content)
                .setOriginalLang(originalLang)
                .setLanguage(lang)
                .setRights(rights)
                .setMedia(media)
                .setSource(source)
                .build();
    }

    /**
     * Checks if AnnoPage should be updated. AnnoPage will only be updated if source field is passed
     * OR if the new file was uploaded (new annotation body content was present) .
     */
    public static boolean isAnnoPageUpdateRequired(AnnotationPreview preview) {
        return (StringUtils.isNotEmpty(preview.getSource()) || StringUtils.isNotEmpty(preview.getAnnotationBody()));
    }
}
