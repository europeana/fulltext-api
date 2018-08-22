package eu.europeana.fulltext.loader.web;

import eu.europeana.fulltext.loader.service.MongoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles incoming requests to parse full text xml files and load them into a database
 * @author Lúthien
 * Created on 27-02-2018
 */
@RestController
@RequestMapping("/fulltext")
public class LoaderController {

    private MongoService fts;

    public LoaderController(MongoService FTService) {
        this.fts = FTService;
    }

    /**
     * starts batch importing of an unzipped directory
     * @return
     */
    @RequestMapping(value       = "/batch",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String batch(@RequestParam(value = "directory", required = false) String directory) {
        fts.importBatch(directory);
        return "Batch processing finished.";
    }

    /**
     * starts batch importing of a zip-file
     * @return
     */
    @RequestMapping(value       = "/zipbatch",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String zipbatch(@RequestParam(value = "archive", required = true) String archive) {
        return fts.importZipBatch(archive);
    }

}
