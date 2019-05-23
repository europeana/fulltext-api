package eu.europeana.fulltext.api.service.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown on a problem parsing or serializing a resource (needs work)
 * Created by luthien on 18/06/2018.
 */

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RecordParseException extends FTException {

    private static final long serialVersionUID = 781769427991192341L;

    public RecordParseException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordParseException(String msg) {
        super(msg);
    }
}

