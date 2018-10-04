package eu.europeana.fulltext.api.service.exception;

/**
 * The error codes that are defined for the Fulltext API (needs work)
 * @author Lúthien
 * Created on 27-02-2018
 */
public enum ErrorCode {

    ID_DOES_NOT_EXIST("idDoesNotExist");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String toString() {
        return this.code;
    }
}
