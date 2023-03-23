package eu.europeana.fulltext.api.service;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_ANNOTATION_TYPES;
import static eu.europeana.fulltext.api.config.FTDefinitions.TEXT_ANNOTATION_TYPES;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import io.micrometer.core.instrument.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Helper class for common controller functionality such as validating request parameters
 */
public final class ControllerUtils {

    private ControllerUtils() {
        //empty constructor to prevent initalization
    }

    /**
     * Verifies if the received text granularity are supported and allowed values. If not an exception is thrown
     * @param textGranularityParams received text granularity parameter values (should be non-null)
     * @param allowedTypes allowed text granularity values
     * @return list of AnnotationTypes corresponding to the received parameters
     * @throws InvalidParameterException thrown when the textGranularity values contain an unknown or invalid value
     */
    public static List<AnnotationType> validateTextGranularity(String textGranularityParams, Set<AnnotationType> allowedTypes)
            throws InvalidParameterException {
        if  (StringUtils.isBlank(textGranularityParams)) {
            return Collections.emptyList();
        }
        String[] values = textGranularityParams.split("\\+|\\s|,");
        List<AnnotationType> result = new ArrayList<>();
        for (String value : values) {
            AnnotationType type = AnnotationType.fromAbbreviationOrName(value);
            if (allowedTypes.stream().anyMatch(x -> x.equals(type))){
                result.add(type);
            } else {
                throw new InvalidParameterException(("invalid text granularity value '" + value +
                        "'. Allowed values are " + allowedTypes));
            }
        }
        return result;
    }
}
