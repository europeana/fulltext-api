package eu.europeana.fulltext.web;

import eu.europeana.fulltext.config.FTDefinitions;
import eu.europeana.fulltext.service.FTService;
import eu.europeana.fulltext.service.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.service.exception.RecordParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rest controller that handles incoming fulltext requests
 * @author Lúthien
 * Created on 27-02-2018
 */
@RestController
@RequestMapping("/presentation")
public class FTController {

    private static final Logger LOG = LogManager.getLogger(FTController.class);

    /* for parsing accept headers */
    private Pattern acceptProfilePattern = Pattern.compile("profile=\"(.*?)\"");

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
                             @RequestParam(value = "format", required = false) String version,
                             HttpServletRequest request,
                             HttpServletResponse response) throws RecordParseException {
        LOG.debug("Retrieve Annotation: " + datasetId + "/" + recordId + "/" + annoID);

        String iiifVersion = version;
        if (iiifVersion == null) {
            iiifVersion = versionFromAcceptHeader(request);
        }

        Object annotation;

//        Optional<FTAnnotation> ftAnnotation = fts.findAnnotation(datasetId, recordId, annoID);
        if ("3".equalsIgnoreCase(iiifVersion)) {
            annotation = fts.getAnnotationV3(datasetId, recordId, annoID);
            response.setContentType(FTDefinitions.MEDIA_TYPE_IIIF_JSONLD_V3+";charset=UTF-8");
        } else {
            annotation = fts.getAnnotationV2(datasetId, recordId, annoID); // fallback option
            response.setContentType(FTDefinitions.MEDIA_TYPE_IIIF_JSONLD_V2+";charset=UTF-8");
        }
        return fts.serializeResource(annotation);
    }

    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}",
                    method   = RequestMethod.HEAD,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity annopagehead(@PathVariable String datasetId,
                                       @PathVariable String recordId,
                                       @PathVariable String pageId,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws RecordParseException {
        if (fts.doesAnnoPageNotExist(datasetId, recordId, pageId)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity(HttpStatus.OK);
        }
    }

    /**
     * Handles fetching a page (resource) with all its annotations
     * @return
     */
    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}",
                    method   = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public String annopage(@PathVariable String datasetId,
                           @PathVariable String recordId,
                           @PathVariable String pageId,
                           @RequestParam(value = "format", required = false) String version,
                           HttpServletRequest request,
                           HttpServletResponse response) throws RecordParseException {
        LOG.debug("Retrieve Annopage: " + datasetId + "/" + recordId + "/" + pageId);

        String iiifVersion = version;
        if (iiifVersion == null) {
            iiifVersion = versionFromAcceptHeader(request);
        }

        Object annotationPage = null;

        try {
            if ("3".equalsIgnoreCase(iiifVersion)) {
                    annotationPage = fts.getAnnotationPageV3(datasetId, recordId, pageId);

                response.setContentType(FTDefinitions.MEDIA_TYPE_IIIF_JSONLD_V3+";charset=UTF-8");
            } else {
                annotationPage = fts.getAnnotationPageV2(datasetId, recordId, pageId); // fallback option
                response.setContentType(FTDefinitions.MEDIA_TYPE_IIIF_JSONLD_V2+";charset=UTF-8");
            }
        } catch (AnnoPageDoesNotExistException e) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            e.printStackTrace();
            annotationPage = "{'error': 'AnnoPage not found'}";
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return fts.serializeResource(annotationPage);
    }

    /**
     * starts batch importing
     * @return
     */
    @RequestMapping(value       = "/batch",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String batch(@RequestParam(value = "directory", required = false) String directory,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        fts.importBatch(directory);
        LOG.debug("Batch processing finished.");
        return "Batch processing finished.";
    }


//    /**
//     * Handles creating a page (resource)
//     * @return
//     */
//    @RequestMapping(value    = "/{datasetId}/{recordId}/annopage/{pageId}",
//                    method   = RequestMethod.POST,
//                    produces = MediaType.APPLICATION_JSON_VALUE)
//    public String annopage(@PathVariable String datasetId,
//                           @PathVariable String recordId,
//                           @PathVariable String pageId,
//                           @RequestParam(value = "format", required = false) String version,
//                           HttpServletRequest request,
//                           HttpServletResponse response) throws RecordParseException {
//
//    }


    private String versionFromAcceptHeader(HttpServletRequest request) {
        String result = "2"; // default version if no accept header is present

        String accept = request.getHeader("Accept");
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) {
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(FTDefinitions.MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else {
                    result = "2";
                }
            }
        }
        return result;
    }



    /**
     * Handles test record creation
     * @return
     */
    @RequestMapping(value       = "/testset",
                    method      = RequestMethod.GET,
                    produces    = MediaType.APPLICATION_JSON_VALUE)
    public String tragawDoeKoetrrrr(@RequestParam(value = "dataset", required = false) String datasetId,
                                    @RequestParam(value = "record", required = false) String recordId,
                                    @RequestParam(value = "page", required = false) String pageId,
                                    @RequestParam(value = "image", required = false) String imageId,
                                    @RequestParam(value = "source", required = false) String sourceId,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        fts.createTestRecords();
        LOG.debug("Test records created.");
        return "Test records created.";
    }

}
