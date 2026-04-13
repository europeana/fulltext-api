package eu.europeana.fulltext.loader.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown we find multiple definitions of which there should be only 1
 * @author Patrick Ehlert
 * @deprecated 2023
 * Created on 20-10-2018
 */
@Deprecated
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DuplicateDefinitionException extends LoaderException {

    private static final long serialVersionUID = 7246636549053909675L;

    public DuplicateDefinitionException(String msg) {
        super(msg);
    }

    public DuplicateDefinitionException(String msg, Throwable t) {
        super(msg, t);
    }
}
