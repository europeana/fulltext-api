package eu.europeana.fulltext.loader.web;

import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.service.LoadArchiveService;
import eu.europeana.fulltext.loader.service.MongoSaveMode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private LoadArchiveService loadArchiveService;

    public LoaderController(LoadArchiveService loadArchiveService) {
        this.loadArchiveService = loadArchiveService;
    }

    /**
     * starts batch importing of an unzipped directory
     * @return
     */
    @GetMapping(value       = "/batch",
                produces    = MediaType.TEXT_PLAIN_VALUE)
    public String batch(@RequestParam(value = "directory", required = false) String directory,
                        @RequestParam(value = "mode", required = false, defaultValue = "INSERT") MongoSaveMode saveMode) {
        return loadArchiveService.importBatch(directory, saveMode);
    }

    /**
     * starts batch importing of a zip-file
     * @return
     */
    @GetMapping(value       = "/zipbatch",
                produces    = MediaType.TEXT_PLAIN_VALUE)
    public String zipbatch(@RequestParam(value = "archive", required = true) String archive,
                           @RequestParam(value = "mode", required = false, defaultValue = "INSERT") MongoSaveMode saveMode)
            throws ArchiveReadException {
        return loadArchiveService.importZipBatch(archive, saveMode);
    }

}
