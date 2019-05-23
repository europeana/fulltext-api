package eu.europeana.fulltext.api.model;

import java.io.Serializable;


/**
 * Created by luthien on 14/06/2018.
 */
public class JsonErrorResponse implements Serializable {

    private static final long serialVersionUID = 2326908117059525587L;

    private String error;

    public JsonErrorResponse(String error) {
        this.error = error;
    }

}

