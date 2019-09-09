package eu.europeana.fulltext.api.web;

import eu.europeana.fulltext.api.model.JsonErrorResponse;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.SerializationException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping(value = "/error", produces = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleError(HttpServletRequest request) throws SerializationException {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestedPath = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI).toString();

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                return new ResponseEntity<>(fts.serialise(
                        new JsonErrorResponse("The requested URL: " + requestedPath + " could not be resolved")),
                                            HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(fts.serialise(
                new JsonErrorResponse("It seems that an unexpected error occurred.")),
                                        HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

}

