package eu.europeana.fulltext.loader.service;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.evt.WDTD;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.*;
import eu.europeana.fulltext.util.NormalPlayTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Service for parsing fulltext xml files
 * Originally created by luthien on 18/07/2018 with SAX parser
 * <p>
 * Refactored to use faster Stax parser by
 *
 * @author Patrick Ehlert
 * on 18-10-2018
 * <p>
 * Note that during parsing warnings (non-fatal problems) are logged using LogFile.OUT which is prepared in advance to
 * collect parsing output. Fatal errors are thrown exceptions, but we can recover from some of these errors for example,
 * when parsing an individual annotation fails we simply skip that annotation.
 */
@Service
public class XMLParserService {

    private static final Logger LOG = LogManager.getLogger(XMLParserService.class);

    private static final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDF           = "RDF";

    private static final String FULLTEXTRESOURCE             = "FullTextResource";
    private static final String FULLTEXTRESOURCE_ABOUT       = "about";
    private static final String FULLTEXTRESOURCE_LANGUAGE    = "language";
    private static final String FULLTEXTRESOURCE_RIGHTS      = "PgRights";
    private static final String FULLTEXTRESOURCE_RIGHTS_TEXT = "resource";
    private static final String FULLTEXTRESOURCE_SOURCE      = "source";
    private static final String FULLTEXTRESOURCE_SOURCE_TEXT = "resource";
    private static final String FULLTEXTRESOURCE_VALUE       = "value";

    private static final String ANNOTATION    = "Annotation";
    private static final String ANNOTATION_ID = "ID";

    private static final String ANNOTATION_TYPE = "type";

    private static final String ANNOTATION_MOTIVATION      = "motivatedBy";
    private static final String ANNOTATION_MOTIVATION_TEXT = "resource";

    private static final String ANNOTATION_TARGET          = "hasTarget";
    private static final String ANNOTATION_TARGET_RESOURCE = "resource";
    private static final String ANNOTATION_TARGET_XYWHPOS  = "#xywh=";
    private static final String ANNOTATION_TARGET_NPTIME   = "#t=";

    private static final String ANNOTATION_HASBODY                   = "hasBody";
    private static final String ANNOTATION_HASBODY_RESOURCE          = "specificResource";
    private static final String ANNOTATION_HASBODY_RESOURCE_VALUE    = "about";
    private static final String ANNOTATION_HASBODY_ATTRIBUTE_VALUE   = "resource";
    private static final String ANNOTATION_HASBODY_RESOURCE_CHARPOS  = "#char=";
    private static final String ANNOTATION_HASBODY_RESOURCE_LANGUAGE = "language";

    private static final String TARGET   = "target '";
    private static final String THISANNO = " - Annotation ";

    private static final int TWO    = 2;
    private static final int THREE  = 3;


    /*
     * The parser is configured in this static block:
     * - WstxInputProperties.P_MAX_ENTITY_COUNT: 1_000_000 - we needed to raise the maximum number of entities
     *   expansions in 1 file, because some xml files will go over the default limit of 100.000
     * - XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES: FALSE - to deal with vulnerability: XML parsing vulnerable
     *   to XXE (XMLStreamReader; found by SonarQube)
     * The other measure proposed by SonarQube was to disable the DTD, but that breaks the loader
     */
    static {
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        inputFactory.setProperty(WstxInputProperties.P_MAX_ENTITY_COUNT, 1_000_000);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
    }

    private LoaderSettings settings;

    /**
     * Create a XML parser service instance using the specified LoaderSettings
     *
     * @param settings LoaderSettings object encapsulating properties from file
     */
    public XMLParserService(LoaderSettings settings) {
        this.settings = settings;
    }

    /**
     * Parse an fulltext xml file and return an AnnoPage object that is ready to be stored in the database
     *
     * @param pageId    full text page number
     * @param xmlStream xml file input stream
     * @param file      name of the xml file (for logging purposes)
     * @return AnnotationPage object
     * @throws LoaderException when there is a fatal error processing this file
     */
    public AnnoPage parse(String pageId, InputStream xmlStream, String file) throws LoaderException {
        return parse(pageId, xmlStream, file, null);
    }

    /**
     * Parse an fulltext xml file and return an AnnoPage object that is ready to be stored in the database
     *
     * @param pageId             full text page number
     * @param xmlStream          xml file input stream
     * @param file               name of the xml file (for logging purposes)
     * @param progressAnnotation keep track of number of processed annotations
     * @return AnnotationPage object
     * @throws LoaderException when there is a fatal error processing this file
     */
    public AnnoPage parse(String pageId, InputStream xmlStream, String file, ProgressLogger progressAnnotation) throws
                                                                                                                LoaderException {

        AnnoPage result = new AnnoPage();
        result.setPgId(pageId);

        XMLEventReader reader = null;
        try {
            reader = inputFactory.createXMLEventReader(xmlStream);
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                LOG.debug(getEventDescription(event));
                if (event.isStartElement()) {
                    StartElement se = (StartElement) event;
                    switch (se.getName().getLocalPart()) {
                        case RDF:
                            break; // ignore
                        case FULLTEXTRESOURCE:
                            parseFullTextResource(reader, se, result, file);
                            break;
                        case ANNOTATION:
                            parseAnnotation(reader, se, result, progressAnnotation, file);
                            break;
                        default:
                            logUnknownElement(file, se);
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new ArchiveReadException("Error reading file " + file, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error("Error closing input stream " + file, e);
                }
            }
        }

        checkAnnoPageComplete(result, file);

        LogFile.OUT.debug("{} - processed OK", file);
        return result;
    }

    /**
     * The edm:FullTextResource element contains a language, source, PgRights and a value element.
     * It also has a rdf:about attribute from which we retrieve the resourceId
     *
     */
    private void parseFullTextResource(XMLEventReader reader,
                                       StartElement fullTextElement,
                                       AnnoPage annoPage,
                                       String file) throws LoaderException, XMLStreamException {
        // there should only be 1 fullTextResource per file, so no resource should be present yet in the annoPage
        if (annoPage.getRes() != null) {
            throw new DuplicateDefinitionException(file + " - Multiple edm:FullTextResource elements found!");
        }
        Resource newResource = new Resource();
        annoPage.setRes(newResource);

        // get all ids (and set them in both AnnoPage and Resource)
        Attribute a = fullTextElement.getAttributeByName(new QName(RDF_NAMESPACE, FULLTEXTRESOURCE_ABOUT));
        parseFullTextResourceId(a.getValue(), annoPage, file);

        // get language and text
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            if (reachedEndElement(e, FULLTEXTRESOURCE)) {
                break;
            } else if (e.isStartElement()) {
                StartElement se = (StartElement) e;
                switch (se.getName().getLocalPart()) {
                    case FULLTEXTRESOURCE_LANGUAGE:
                        newResource.setLang(reader.getElementText());
                        break;
                    case FULLTEXTRESOURCE_RIGHTS:
                        this.parseResourceEdmRights(se, newResource);
                        break;
                    case FULLTEXTRESOURCE_SOURCE:
                        this.parseResourceDcSource(se, newResource);
                        break;
                    case FULLTEXTRESOURCE_VALUE:
                        newResource.setValue(reader.getElementText());
                        break;
                    default:
                        logUnknownElement(file, se);
                }
            } else if (!(e instanceof Characters)) {
                logUnknownElement(file, e);
            }
        }
        checkResourceComplete(annoPage.getRes(), file);
    }

    /**
     * Process a fulltext url and separate into different id parts.
     * Expected format of the fulltext url is http://data.europeana.eu/fulltext/<datasetId>/<localId>/<resourceId>
     */
    private void parseFullTextResourceId(String ftResourceUrl, AnnoPage annoPage, String file) throws LoaderException {
        if (ftResourceUrl == null) {
            throw new MissingDataException(file + " - No resource text url was defined");
        } else if (ftResourceUrl.startsWith(settings.getResourceBaseUrl())) {
            String   identifiers = StringUtils.removeStartIgnoreCase(ftResourceUrl, settings.getResourceBaseUrl());
            String[] ids         = StringUtils.split(identifiers, '/');
            if (ids.length != THREE) {
                throw new MissingDataException(file + " - Error retrieving ids from text url: " + ftResourceUrl);
            }
            annoPage.setDsId(ids[0]);
            annoPage.setLcId(ids[1]);
            annoPage.getRes().setDsId(ids[0]);
            annoPage.getRes().setLcId(ids[1]);
            annoPage.getRes().setId(ids[2]);
        } else {
            throw new ConfigurationException(
                    file + " - ENTITY text value '" + ftResourceUrl + "' doesn't start with configured" +
                    "resource base url '" + settings.getResourceBaseUrl() + "'");
        }
    }

    /**
     * Processes an oa:Annotation element and adds it to the AnnoPage. Note that if an error occurs we skip the
     * annotation and do not add it to the AnnoPage. We do log all annotations that are skipped
     *
     * @return true if annotation was processed and added to AnnoPage object, otherwise false
     */
    private void parseAnnotation(XMLEventReader reader,
                                 StartElement annotationElement,
                                 AnnoPage annoPage,
                                 ProgressLogger progressAnnotation,
                                 String file) throws XMLStreamException {
        Annotation anno = new Annotation();
        boolean    result;
        try {
            parseAnnotationId(annotationElement, anno);
            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (reachedEndElement(e, ANNOTATION)) {
                    break;
                } else if (e.isStartElement()) {
                    StartElement se = (StartElement) e;
                    switch (se.getName().getLocalPart()) {
                        case ANNOTATION_TYPE:
                            this.parseAnnotationType(reader.getElementText(), anno);
                            break;
                        case ANNOTATION_MOTIVATION:
                            // October 2018: for now there is no need for this 'motivation' information so we skip it
                            //this.parseAnnotationMotivation(se, anno);
                            break;
                        case ANNOTATION_HASBODY:
                            this.parseAnnotationHasBody(se, reader, anno, file);
                            break;
                        case ANNOTATION_TARGET:
                            this.parseAnnotationTarget(se, annoPage, anno);
                            break;
                        default: // do nothing, just skip unknown start elements (e.g. confidence, styledBy)
                    }
                } // else {
                // do nothing, just skip other (end) elements until we get to end of annotation
                //}
            }
            result = addAnnotationToAnnoPage(annoPage, anno);
            if (progressAnnotation != null) {
                if (result) {
                    progressAnnotation.addItemOk();
                } else {
                    progressAnnotation.addItemFail();
                }
            }
        } catch (LoaderException e) {
            LogFile.OUT.error("{} - Skipping annotation {} because {}", file, anno.getAnId(), e.getMessage());
            if (progressAnnotation != null) {
                progressAnnotation.addItemFail();
            }
        }
    }

    /**
     * Only add the annotation to the list of annotations if:
     * 1. The annotation has an annotation type
     * 2. The annotation type is 'W', 'B', 'L' or 'C' (i.e. NOT 'P' NOR 'M') and has a target
     * 3.    or the annotation type is 'P' or 'M'
     * Note that if there are no text coordinates, we do save it
     *
     * @return true if a new annotation was added to the list, otherwise false
     */
    private boolean addAnnotationToAnnoPage(AnnoPage annoPage, Annotation anno) throws LoaderException {
        if (anno.getDcType() == Character.MIN_VALUE) {
            throw new MissingDataException("no annotation type defined");
        }
        if (!anno.isTopLevel() && (anno.getTgs() == null || anno.getTgs().isEmpty())) {
            throw new MissingDataException("no annotation target defined");
        }
        return annoPage.getAns().add(anno);
    }

    /**
     * The oa:Annotation element has an 'rdf:ID' attribute. ID values start with a slash character which we filter out
     */
    private void parseAnnotationId(StartElement annotationElement, Annotation anno) throws LoaderException {
        Attribute att = annotationElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_ID));
        if (att == null) {
            throw new MissingDataException("no annotation id found");
        }
        String annoId = att.getValue();
        if (annoId.startsWith("/")) {
            anno.setAnId(annoId.substring(1));
        } else {
            anno.setAnId(annoId);
        }
    }

    /**
     * dc:type is a required field of an annotation
     * We only save the first letter of the type (to save disk space)
     */
    private void parseAnnotationType(String typeValue, Annotation anno) throws LoaderException {
        if (StringUtils.isEmpty(typeValue)) {
            throw new MissingDataException("no annotation type found for annotation " + anno.getAnId());
        }
        anno.setDcType(typeValue.toUpperCase(Locale.GERMANY).charAt(0));
    }

    /**
     * edm:PgRights is a field of a fulltextResource
     */
    private void parseResourceEdmRights(StartElement rightsElement, Resource res) {
        Attribute att = rightsElement.getAttributeByName(new QName(RDF_NAMESPACE, FULLTEXTRESOURCE_RIGHTS_TEXT));
        if (att != null) {
            res.setRights(att.getValue());
        }
    }

    /**
     * dc:source is a field of a fulltextResource
     */
    private void parseResourceDcSource(StartElement sourceElement, Resource res) {
        Attribute att = sourceElement.getAttributeByName(new QName(RDF_NAMESPACE, FULLTEXTRESOURCE_SOURCE_TEXT));
        if (att != null) {
            res.setSource(att.getValue());
        }
    }

    /**
     * oa:MotivatedBy is an optional field of an annotation
     */
    private void parseAnnotationMotivation(StartElement motivationElement, Annotation anno) {
        Attribute att = motivationElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_MOTIVATION_TEXT));
        if (att != null) {
            anno.setMotiv(att.getValue());
        }
    }

    /**
     * The oa:hasBody element should contain:
     * - either a oa:SpecificResource which holds the start and end coordinates of the text of an annotation
     * - or else have an inline rdf:resource attribute with those coordinates
     */
    private void parseAnnotationHasBody(StartElement hasBodyElement,
                                        XMLEventReader reader,
                                        Annotation anno,
                                        String file) throws XMLStreamException {
        if (hasBodyElement.getAttributes().hasNext() &&
            hasBodyElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_HASBODY_ATTRIBUTE_VALUE))
                          .isSpecified()) {
            parseAnnotationTextCoordinates(hasBodyElement, anno, file, true);
        } else {
            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (reachedEndElement(e, ANNOTATION_HASBODY)) {
                    break;
                } else if (e.isStartElement()) {
                    StartElement se = (StartElement) e;
                    if (ANNOTATION_HASBODY_RESOURCE.equalsIgnoreCase(se.getName().getLocalPart())) {
                        parseAnnotationTextCoordinates(se, anno, file, false);
                    } else if (ANNOTATION_HASBODY_RESOURCE_LANGUAGE.equalsIgnoreCase(se.getName().getLocalPart())) {
                        parseAnnotationTextLanguage(reader.getElementText(), anno);
                    } // else {
                    // we simply ignore unknown elements here like 'hasSource' and 'styleClass'
                    //}
                }
            }
        }
    }

    /**
     * Parse the text coordinates at the end attribute value of either the the oa:hasBody/oa:specificResource tag
     * or the oa:hasBody rdf:resource attribute.
     * Note that we rely on the calling method to go the the end of the 'oa:hasBody' section when we're done
     */
    private void parseAnnotationTextCoordinates(StartElement specificRsElement,
                                                Annotation anno,
                                                String file,
                                                boolean inlineHasbody) {
        Attribute att;
        if (inlineHasbody) {
            att = specificRsElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_HASBODY_ATTRIBUTE_VALUE));
        } else {
            att = specificRsElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_HASBODY_RESOURCE_VALUE));
        }

        if (att == null || StringUtils.isEmpty(att.getValue())) {
            LogFile.OUT.warn("{}{}{} has no specific resource text defined", file, THISANNO, anno.getAnId());
        } else if (!anno.isTopLevel()) {
            String[] urlAndCoordinates = att.getValue().split(ANNOTATION_HASBODY_RESOURCE_CHARPOS);
            if (urlAndCoordinates.length == 1) {
                LogFile.OUT.warn("{}{}{} has no {} defined in resource text {}", file, THISANNO, anno.getAnId(),
                                 ANNOTATION_HASBODY_RESOURCE_CHARPOS, att.getValue());
            } else {
                String[] fromTo = urlAndCoordinates[1].split(",");
                parseFromToInteger(fromTo[0], FromTo.FROM, anno, file);
                parseFromToInteger(fromTo[1], FromTo.TO, anno, file);
            }
        }
    }

    private void parseFromToInteger(String value, FromTo fromTo, Annotation anno, String file) {
        if (StringUtils.isEmpty(value)) {
            LogFile.OUT.warn("{}{}{} has empty resource text {} value", file, THISANNO, anno.getAnId(), fromTo);
        } else {
            try {
                if (FromTo.FROM.equals(fromTo)) {
                    anno.setFrom(Integer.valueOf(value));
                } else if (FromTo.TO.equals(fromTo)) {
                    anno.setTo(Integer.valueOf(value));
                }
            } catch (NumberFormatException nfe) {
                LogFile.OUT.error(file + THISANNO + anno.getAnId() + " resource text " + fromTo + " value '" + value +
                                  "' is not an integer");
            }
        }
    }

    /**
     * dc:language is an optional element of an annotation
     */
    private void parseAnnotationTextLanguage(String language, Annotation anno) {
        if (StringUtils.isNotEmpty(language)) {
            anno.setLang(language);
        }
    }

    /**
     * The hasTarget tag should have an attribute with as value either an image url and coordinates or
     * a media url and start, stop NormalPlayTime strings (#t=HH:mm:ss.SSS,HH:mm:ss.SSS)
     * Note that we only need this for
     * Also coordinates and image url are required, hence the validity checks
     */
    private void parseAnnotationTarget(StartElement targetElement, AnnoPage annoPage, Annotation anno) throws
                                                                                                       LoaderException {
        Attribute att = targetElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_TARGET_RESOURCE));
        if (att == null || StringUtils.isEmpty(att.getValue())) {
            throw new MissingDataException("no annotation target url defined");
        }

        String[] urlAndCoordinates;
        String   annotationTargetSpecifier;


        if (anno.isMedia()) {
            annotationTargetSpecifier = ANNOTATION_TARGET_NPTIME;
        } else {
            annotationTargetSpecifier = ANNOTATION_TARGET_XYWHPOS;
        }

        // parse the target url
        urlAndCoordinates = att.getValue().split(annotationTargetSpecifier);

        // for 'top level' annotations the target is optional, for all others it is required
        if (!anno.isTopLevel() && urlAndCoordinates.length == 1) {
            throw new MissingDataException(
                    "no " + annotationTargetSpecifier + " defined in target url " + att.getValue());
        }

        // we only need to set the imageUrl once in the AnnoPage object, all subsequent annotations will have the same url
        if (annoPage.getTgtId() == null) {
            annoPage.setTgtId(urlAndCoordinates[0]);
        }

        // set target
        if (urlAndCoordinates.length > 1) {
            Target t = createTarget(urlAndCoordinates[1], anno.isMedia());
            if (anno.getTgs() == null) {
                anno.setTgs(new ArrayList<>());
            }
            anno.getTgs().add(t);
        }
    }

    private Target createTarget(String coordinates, boolean isMedia) throws LoaderException {

        String[] separatedCoordinates = coordinates.split(",");

        if (isMedia) {
            if (separatedCoordinates.length != TWO) {
                throw new IllegalValueException(TARGET + coordinates + "' must contain 2 NormalPlayTime-formatted " +
                                                "parameters for start and end time, separated with a comma");
            }
            try {
                NormalPlayTime nptStart = NormalPlayTime.parse(checkNPTFormat(separatedCoordinates[0]));
                NormalPlayTime nptEnd   = NormalPlayTime.parse(checkNPTFormat(separatedCoordinates[1]));
                if (nptStart != null && nptEnd != null) {
                    int start = (int) nptStart.getTimeOffsetMs();
                    int end   = (int) nptEnd.getTimeOffsetMs();
                    if (start != end && end > 0) {
                        return new Target(start, end);
                    } else {
                        throw new IllegalValueException(
                                TARGET + coordinates + "' start & end time should be different " +
                                "and the end time should be greater than 0");
                    }
                } else {
                    throw new LoaderException(
                            "Error occurred processing the start & end time of " + TARGET + coordinates);
                }

            } catch (ParseException e) {
                throw new IllegalValueException(TARGET + coordinates + "' must contain 2 NormalPlayTime-formatted " +
                                                "parameters for start and end time, separated with a comma");
            }

        } else {

            if (separatedCoordinates.length != 4) {
                throw new IllegalValueException(
                        TARGET + coordinates + "' doesn't have 4 integers separated with a comma");
            }
            try {
                return new Target(Integer.valueOf(separatedCoordinates[0]),
                                  Integer.valueOf(separatedCoordinates[1]),
                                  Integer.valueOf(separatedCoordinates[2]),
                                  Integer.valueOf(separatedCoordinates[3]));
            } catch (NumberFormatException nfe) {
                throw new IllegalValueException(
                        TARGET + coordinates + "' doesn't have 4 integers separated with a comma");
            }
        }
    }

    private String checkNPTFormat(String str) throws IllegalValueException {
        if (str.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
            return str;
        } else {
            throw new IllegalValueException(
                    "target parameter '" + str + "' doesn't have the required NormalPlayTime HH:mm:ss.SSS format");
        }
    }

    private void checkAnnoPageComplete(AnnoPage annoPage, String file) throws LoaderException {
        if (StringUtils.isEmpty(annoPage.getDsId())) {
            throw new MissingDataException(file + " - No annotation page dataset id defined");
        }
        if (StringUtils.isEmpty(annoPage.getLcId())) {
            throw new MissingDataException(file + " - No annotation page local id defined");
        }
        if (StringUtils.isEmpty(annoPage.getPgId())) {
            throw new MissingDataException(file + " - No annotation page id defined");
        }
        if (StringUtils.isEmpty(annoPage.getTgtId())) {
            throw new MissingDataException(file + " - No annotation page target id defined");
        }
        if (annoPage.getAns() == null || annoPage.getAns().isEmpty()) {
            throw new MissingDataException(file + " - Annotation page doesn't contain any annotations");
        }
        if (annoPage.getModified() == null) {
            throw new MissingDataException(file + " - No last modified date set");
        }
    }

    /**
     * Check if the minimal required resource information is present.
     * Text, language, PgRights and source are (technically) optional
     */
    private void checkResourceComplete(Resource res, String file) throws LoaderException {
        if (StringUtils.isEmpty(res.getDsId())) {
            throw new MissingDataException(file + " - No resource dataset id defined");
        }
        if (StringUtils.isEmpty(res.getLcId())) {
            throw new MissingDataException(file + " - No resource local id defined");
        }
        if (StringUtils.isEmpty(res.getId())) {
            throw new MissingDataException(file + " - No resource id defined");
        }
    }

    private boolean reachedEndElement(XMLEvent e, String elementName) {
        return e.isEndElement() && elementName.equals(((EndElement) e).getName().getLocalPart());
    }

    /**
     * For now just log to output
     */
    private void logUnknownElement(String file, XMLEvent event) {
        LOG.info("{} - Unknown xml event {}", file, getEventDescription(event));
    }

    /**
     * For debugging purposes
     */
    private String getEventDescription(XMLEvent e) {
        if (e.isAttribute()) {
            Attribute a = (Attribute) e;
            return "Attribute" + a.getName() + ", value " + a.getValue();
        } else if (e.isStartElement()) {
            return "StartElement " + ((StartElement) e).getName();
        } else if (e.isEndElement()) {
            return "EndElement " + ((EndElement) e).getName();
        } else if (e.isCharacters()) {
            Characters c = (Characters) e;
            if (c.isIgnorableWhiteSpace()) {
                return "Ignorable whitespace characters";
            } else {
                return "Characters '" + c.getData() + "'";
            }
        } else if (e.isStartDocument()) {
            return "Start of document";
        } else if (e.isEndDocument()) {
            return "End of document";
        } else if (e.isEntityReference()) {
            EntityReference ef = (EntityReference) e;
            return "Entity reference " + ef.getName() + ", value " + ef.getDeclaration();
        } else if (e.isProcessingInstruction()) {
            ProcessingInstruction pi = (ProcessingInstruction) e;
            return "Processing instruction target " + pi.getTarget() + ", data " + pi.getData();
        } else if (e.isNamespace()) {
            return "Namespace " + ((Namespace) e).getName();
        } else if (e instanceof WDTD) {
            WDTD          wdtd = (WDTD) e;
            StringBuilder s    = new StringBuilder("WDTD ").append(wdtd.getRootName());
//            if (!wdtd.getEntities().isEmpty()) {
//                s.append(", entities: ");
//                for (EntityDeclaration ed : wdtd.getEntities()) {
//                    s.append(ed.getName()).append(" ");
//                }
//            }
//            if (!wdtd.getNotations().isEmpty()) {
//                s.append(", notations: ");
//                for (NotationDeclaration nd : wdtd.getNotations()) {
//                    s.append(nd.getName()).append(" ");
//                }
//            }
            return s.toString();
        }
        return e.toString();
    }

    private enum FromTo {FROM, TO}
}
