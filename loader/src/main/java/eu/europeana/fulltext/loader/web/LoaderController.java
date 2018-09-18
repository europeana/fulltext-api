package eu.europeana.fulltext.loader.web;

import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.service.LoadArchiveService;
import eu.europeana.fulltext.loader.service.MongoSaveMode;
import eu.europeana.fulltext.loader.service.MongoService;
import org.apache.logging.log4j.LogManager;
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
    private MongoService mongoService;

    public LoaderController(LoadArchiveService loadArchiveService, MongoService mongoService) {
        this.loadArchiveService = loadArchiveService;
        this.mongoService = mongoService;
    }

    /**
     * starts batch importing of an unzipped directory
     * @return
     */
    @GetMapping(value = "/batch", produces = MediaType.TEXT_PLAIN_VALUE)
    public String batch(@RequestParam(value = "directory", required = false) String directory,
                        @RequestParam(value = "mode", required = false, defaultValue = "INSERT") MongoSaveMode saveMode) {
        return loadArchiveService.importBatch(directory, saveMode);
    }

    /**
     * starts batch importing of a zip-file
     * @return
     */
    @GetMapping(value = "/zipbatch", produces = MediaType.TEXT_PLAIN_VALUE)
    public String zipbatch(@RequestParam(value = "archive", required = true) String archive,
                           @RequestParam(value = "mode", required = false, defaultValue = "INSERT") MongoSaveMode saveMode)
            throws ArchiveReadException {
        return loadArchiveService.importZipBatch(archive, saveMode);
    }

    /**
     * Delete all resources and annotationpages of the provided dataset
     * @param datasetId id of the dataset that is to be removed
     * @return String describing what was deleted
     */
    @GetMapping(value = "/delete", produces = MediaType.TEXT_PLAIN_VALUE)
    public String delete(@RequestParam(value = "datasetId", required = true) String datasetId) {
        LogManager.getLogger(LoaderController.class).debug("Starting delete...");
        StringBuilder s = new StringBuilder("Deleted ");
        s.append(mongoService.deleteAllAnnoPages(datasetId));
        s.append(" annopages and ");
        s.append(mongoService.deleteAllResources(datasetId));
        s.append(" resources");
        LogManager.getLogger(LoaderController.class).debug("Delete finished.");
        return s.toString();
    }


}
