package eu.europeana.fulltext.web;

import eu.europeana.fulltext.service.FTService;
import eu.europeana.fulltext.service.exception.FTException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rest controller that handles incoming fulltext requests
 * @author Lúthien
 * Created on 27-02-2018
 */
@RestController
public class FTController {

    private static final Logger LOG = LogManager.getLogger(FTController.class);

    private FTService fts;

    public FTController(FTService FTService) {
        this.fts = FTService;
    }


    /**
     * Handles all list identifier requests
     * @param metadataPrefix
     * @param from
     * @param until
     * @param set
     * @return
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST},
                    produces = MediaType.TEXT_XML_VALUE)
    public String handleListIdentifier(@RequestParam(value = "metadataPrefix", required = true) String metadataPrefix,
                                  @RequestParam(value = "from", required = false) String from,
                                  @RequestParam(value = "until", required = false) String until,
                                  @RequestParam(value = "set", required = false) String set) throws FTException {
        return "Not implemented yet";
    }

    /**
     * Handles test
     * @return
     */
    @RequestMapping(value       = "/test",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String tragawDoeKoetrrrr(@RequestParam(value = "globl", required = false) String globl,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        fts.do_args_method(globl);
        LOG.debug("TragâwdoeKoetrrrr");
        return "Globl says: " + globl;
    }

}
