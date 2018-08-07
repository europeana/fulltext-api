/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.service.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown on a problem parsing or serializing a resource (needs work)
 * Created by luthien on 18/06/2018.
 */

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RecordParseException extends FTException {

    public RecordParseException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordParseException(String msg) {
        super(msg);
    }
}

