package eu.europeana.fulltext.web;

import eu.europeana.fulltext.config.FTDefinitions;
import eu.europeana.fulltext.entity.FTAnnotation;
import eu.europeana.fulltext.service.FTService;
import eu.europeana.fulltext.service.exception.FTException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Rest controller that handles incoming fulltext requests
 * @author Lúthien
 * Created on 27-02-2018
 */
@RestController
@RequestMapping("/presentation")
public class FTController {

    private static final Logger LOG = LogManager.getLogger(FTController.class);

    private FTService fts;

    public FTController(FTService FTService) {
        this.fts = FTService;
    }



    /**
     * Handles fetching a single annotation
     * @return
     */
    @RequestMapping(value    = "/{datasetId}/{recordId}/anno/{annoID}",
                    method   = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE, FTDefinitions.MEDIA_TYPE_JSONLD})
    public String annotation(@PathVariable String datasetId,
                             @PathVariable String recordId,
                             @PathVariable String annoID,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        Optional<FTAnnotation> ftAnnotation = fts.findAnnotation(datasetId, recordId, annoID);
        LOG.debug("annotation");
        return "Not implemented yet";
    }


    /**
     * Handles fetching all annotations for a given resource
     * @return
     */
    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}",
                    method   = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public String annopage(@PathVariable String datasetId,
                           @PathVariable String recordId,
                           @PathVariable String pageId,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        fts.listAllAnnotations(datasetId, recordId, pageId);
        LOG.debug("annopage");
        return "Not implemented yet";
    }



    /**
     * Handles test
     * @return
     */
    @RequestMapping(value       = "/test",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String tragawDoeKoetrrrr(@RequestParam(value = "zampano", required = false) String zampano,
                                    @RequestParam(value = "gelsomina", required = false) String gelsomina,
                                    HttpServletRequest request,
                       HttpServletResponse response) {
        fts.do_args_method(zampano, gelsomina);
        LOG.debug("TragâwdoeKoetrrrr");
        return "Globl says: " + zampano;
    }

}
