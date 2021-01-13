package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControllerUtilsTest {

    private static final Set<AnnotationType> ALLOWED_ANNOTATION_TYPES = EnumSet.of(
            AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD);

    @Test
    public void testValidateTextGranularitySingleValue() throws InvalidParameterException {
        List<AnnotationType> annoTypes = ControllerUtils.validateTextGranularity("block", ALLOWED_ANNOTATION_TYPES);
        assertEquals(1, annoTypes.size());
        assertEquals(AnnotationType.BLOCK, annoTypes.get(0));
    }

    @Test
    public void testValidateTextGranularityMultipleValues() throws InvalidParameterException {
        List<AnnotationType> annoTypes = ControllerUtils.validateTextGranularity("WORD,b+L", ALLOWED_ANNOTATION_TYPES);
        assertEquals(3, annoTypes.size());
        assertEquals(AnnotationType.WORD, annoTypes.get(0));
        assertEquals(AnnotationType.BLOCK, annoTypes.get(1));
        assertEquals(AnnotationType.LINE, annoTypes.get(2));
    }

    @Test
    public void testValidateTextGranularityNonExistingValue() throws InvalidParameterException {
        Assertions.assertThrows(InvalidParameterException.class,() -> {
            ControllerUtils.validateTextGranularity("Block+Brick", ALLOWED_ANNOTATION_TYPES);
        });
    }

    @Test
    public void testValidateTextGranularityNotAllowedValue() throws InvalidParameterException {
        Assertions.assertThrows(InvalidParameterException.class,() -> {
            ControllerUtils.validateTextGranularity("Block Page", ALLOWED_ANNOTATION_TYPES);
        });
    }
}
