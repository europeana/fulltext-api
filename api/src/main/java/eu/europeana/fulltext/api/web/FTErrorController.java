package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.model.JsonErrorResponse;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by luthien on 2019-08-13.
 */

@Controller
public class FTErrorController implements ErrorController {

    private FTService fts;

    public  FTErrorController(FTService ftService) {
        this.fts = ftService;
    }

    @RequestMapping(value = "/error", produces = MediaType.ALL_VALUE,
                    method   = {RequestMethod.HEAD, RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> handleError(HttpServletRequest request) throws SerializationException {

        String message = "Unexpected error";
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                message = "The requested URL: " + request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI).toString()
                        + " could not be resolved";
            } else if (HttpStatus.valueOf(statusCode).is4xxClientError()) {
                message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE).toString();
                if (StringUtils.isBlank(message)) {
                    message = "Please check your request, considering the HTTP " + statusCode + " return status";
                }
            } else {
                message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE).toString();
                if (StringUtils.isBlank(message)) {
                    message = "Unknown exception, statuscode = " + statusCode;
                }
            }
        }
        return new ResponseEntity<>(fts.serialise(new JsonErrorResponse(message)), HttpStatus.valueOf(statusCode));
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

}
