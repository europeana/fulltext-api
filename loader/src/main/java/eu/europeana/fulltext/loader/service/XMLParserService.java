/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.loader.service;

import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.exception.ConfigurationException;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.exception.MissingDataException;
import eu.europeana.fulltext.loader.exception.ParseDocumentException;
import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import eu.europeana.fulltext.loader.model.AnnotationRdf;
import eu.europeana.fulltext.loader.model.TargetRdf;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing fulltext xml files
 * Created by luthien on 18/07/2018.
 */

@Service
public class XMLParserService {

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();

    private static final String CHARPOS    = "#char=";
    private static final String XYWHPOS    = "#xywh=";
    private static final String PAGEDCTYPE = "Page";

    private LoaderSettings settings;

    public XMLParserService(LoaderSettings settings) {
        this.settings = settings;
    }

    public AnnoPageRdf eatIt(Path path) throws LoaderException {
        return eatIt(path.toString(), readFileContents(path), StringUtils.split(path.getFileName().toString(), '.')[0]);
    }

    /**
     *
     * @param path fileName, for logging purposes only so we know which file contains errors
     * @param xmlString
     * @param pageId
     * @return annoPageRdf object with all relevant information
     */
    public AnnoPageRdf eatIt(String path, String xmlString, String pageId) throws LoaderException {
        AnnoPageRdf result = null;

        Document document = parseXml(xmlString);

        // parse ftResource and extract datasetId, localId and resourceid
        NodeList ftNodes = getNodes(document, "FullTextResource");
        // note that getTextContent will automatically convert escaped xml characters to their proper value
        String ftResource = ftNodes.item(0).getAttributes().item(0).getTextContent();
        if (ftResource == null) {
            throw new MissingDataException("No resource found!");
        } else if (!ftResource.startsWith(settings.getResourceBaseUrl())) {
            throw new ConfigurationException(path + " - ENTITY text value '" + ftResource + "' doesn't start with configured" +
                    "resource base url '" + settings.getResourceBaseUrl() + "'");
        } else {
            String[] identifiers = StringUtils.split(
                    StringUtils.removeStartIgnoreCase(ftResource, settings.getResourceBaseUrl()), '/');
            if (identifiers.length != 3){
                throw new MissingDataException(path + " - Error retrieving ids from ftResource: "+ftResource);
            }
            result = new AnnoPageRdf(identifiers[0], identifiers[1], identifiers[2], pageId);
        }

        String internalSubset = (document).getDoctype().getInternalSubset();
        result.setImgTargetBase(readEntityString("img", internalSubset));

        if (!ftNodes.item(0).getChildNodes().item(1).getNodeName().equalsIgnoreCase("dc:language")) {
            // TODO check with Hugo if we can assume that language and text is always present and always in the same order
            throw new MissingDataException("No resource dc:language definition found!");
        }
        result.setFtLang(ftNodes.item(0).getChildNodes().item(1).getTextContent());

        if (!ftNodes.item(0).getChildNodes().item(3).getNodeName().equalsIgnoreCase("rdf:value")) {
            throw new MissingDataException("No resource rdf:value definition found!");
        }
        result.setFtText(ftNodes.item(0).getChildNodes().item(3).getTextContent());


        // TODO break this up into submethods for better readability and understandability

        NodeList aNodes = getNodes(document, "Annotation");
        List<AnnotationRdf> annoList    = new ArrayList<>();
        AnnotationRdf pageAnnotationRdf = null;

        for (int i = 0; i < aNodes.getLength(); i++) {
            boolean         hasTargetError  = false;
            boolean         isPageAnnotation = false;
            Node            aNode            = aNodes.item(i);
            String          id               = "";
            String          dcType           = "";
            String          motiv            = "";
            String          specRes          = "";
            String          resource         = "";
            String          resLang          = "";
            List<TargetRdf> targetRdfList    = new ArrayList<>();

            id = StringUtils.removeStart(aNode.getAttributes().item(0).getTextContent(), "/");

            if (aNode.getNodeType() == Node.ELEMENT_NODE) {
                Element anElement = (Element) aNode;
                dcType = anElement.getElementsByTagName("dc:type").item(0).getTextContent();
                motiv = getAttributeValue(anElement, "oa:motivatedBy");

                Node bodyNode = anElement.getElementsByTagName("oa:hasBody").item(0);

                if (StringUtils.equalsIgnoreCase(dcType, PAGEDCTYPE)) {
                    isPageAnnotation = true;
                    resource =  getAttributeValue(anElement, "oa:hasBody");
                } else if (bodyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element bodyElement = (Element) bodyNode;
                    if (bodyElement.getElementsByTagName("oa:SpecificResource").getLength() == 0){
                        resource =  getAttributeValue(anElement, "oa:hasBody");
                        specRes = resource; // in case of 'bodyless' Annotations, make these two equal

                    } else {
                        specRes =  getAttributeValue(bodyElement, "oa:SpecificResource");

                        Node specResNode = bodyElement.getElementsByTagName("oa:SpecificResource").item(0);
                        if (specResNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element specResElement = (Element) specResNode;
                            resource =  getAttributeValue(specResElement, ("oa:hasSource"));
                            if (specResElement.getElementsByTagName("dc:language").getLength() > 0){
                                resLang = specResElement.getElementsByTagName("dc:language")
                                                         .item(0)
                                                         .getTextContent();
                            }
                        }
                    }
                }

                NodeList targetNodes = anElement.getElementsByTagName("oa:hasTarget");
                for (int j = 0; j < targetNodes.getLength(); j++) {
                    Node targetNode = targetNodes.item(j);
                    if (targetNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element targetElement = (Element) targetNode;
                        if (!isPageAnnotation) {
                            String url = targetElement.getAttributes().item(0).getNodeValue();
                            try {
                                targetRdfList.add(createTarget(url));
                            } catch (MissingDataException e) {
                                // non-fatal error, so we skip only this annotation and continue with the rest in the file
                                hasTargetError = true;
                                // no need to log full error stacktrace, message is sufficient
                                LogFile.OUT.error("{} - Skipping annotation with id {} because {}", path, id, e.getMessage());
                                break;
                            }
                        }
                    }
                }
            }

            if (!hasTargetError){
                if (isPageAnnotation) {
                    if (StringUtils.isNotBlank(resLang)){
                        pageAnnotationRdf = new AnnotationRdf(id, dcType, motiv, resLang, targetRdfList);
                    } else {
                        pageAnnotationRdf = new AnnotationRdf(id, dcType, motiv, targetRdfList);
                    }
                } else {
                    if (StringUtils.isNotBlank(resLang)){
                        annoList.add(createAnnotation(specRes, id, dcType, motiv, resLang, resource, targetRdfList));
                    } else {
                        annoList.add(createAnnotation(specRes, id, dcType, motiv, resource, targetRdfList));
                    }
                }
            }
        }
        result.setPageAnnotationRdf(pageAnnotationRdf);
        result.setAnnotationRdfList(annoList);

        LogFile.OUT.debug("{} - OK", path);
        return result;
    }

    private Document parseXml(String xmlString) throws LoaderException {
        Document result;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            result = builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Error creating xml document", e);
        } catch (IOException e) {
            throw new ArchiveReadException("Error reading xml file", e);
        } catch (SAXException e) {
            throw new ParseDocumentException("Error parsing document", e);
        }
        return result;
    }

    private NodeList getNodes(Document document, String localName) throws LoaderException {
        XPath ftXpath = X_PATH_FACTORY.newXPath();
        NodeList result;
        try {
            XPathExpression ftExpr = ftXpath.compile("//*[local-name()='"+localName+"']");
            Object ftResult = ftExpr.evaluate(document, XPathConstants.NODESET);
            result  = (NodeList) ftResult;
        } catch (XPathExpressionException e) {
            throw new ParseDocumentException("Error reading "+localName+" data");
        }
        return result;
    }

    private String getAttributeValue(Element element, String elementName) {
        return element.getElementsByTagName(elementName).item(0).getAttributes().item(0).getNodeValue();
    }

    private TargetRdf createTarget(String url) throws MissingDataException {
        if (url == null || !url.contains(XYWHPOS)) {
            throw new MissingDataException("no "+XYWHPOS+" defined in url "+url);
        }
        Integer x = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(url, XYWHPOS)[1], ",")[0]);
        Integer y = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(url, XYWHPOS)[1], ",")[1]);
        Integer w = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(url, XYWHPOS)[1], ",")[2]);
        Integer h = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(url, XYWHPOS)[1], ",")[3]);
        return new TargetRdf(x, y, w, h);
    }

    private AnnotationRdf createAnnotation(String specRes,
                                                  String id,
                                                  String dcType,
                                                  String motiv,
                                                  String lang,
                                                  String resource,
                                                  List   targetRdfList) {
        AnnotationRdf annoRdf = createAnnotation(specRes, id, dcType, motiv, resource, targetRdfList);
        annoRdf.setLang(lang);
        return annoRdf;
    }


    private AnnotationRdf createAnnotation(String specRes,
                                                  String id,
                                                  String dcType,
                                                  String motiv,
                                                  String resource,
                                                  List   targetRdfList) {
        Integer from = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(specRes, CHARPOS)[1],
                                                          ",")[0]);
        Integer to   = Integer.parseInt(StringUtils.split(StringUtils.splitByWholeSeparator(specRes, CHARPOS)[1],
                                                          ",")[1]);
        return new AnnotationRdf(id, dcType, motiv, from, to, targetRdfList);
    }

    private static String readEntityString(String whichEntity, String internalSubset) {
        Pattern entityPattern = Pattern.compile("ENTITY\\s*?" + whichEntity + "\\s*?'(.+?)'");
        Matcher entityMatcher = entityPattern.matcher(StringUtils.replaceChars(internalSubset, '"', '\''));
        if (entityMatcher.find()) {
            return convertXmlEscapeCharacters(entityMatcher.group(1));
        } else {
            return null;
        }
    }

    /**
     * This methods converts all 5 xml escape character to their normal representation
     * @return
     */
    private static String convertXmlEscapeCharacters(String xml) {
        return xml.replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&amp;","&")
                  .replaceAll("&apos;", "\'");
    }

    private static String readFileContents(Path file) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(file));
        } catch (IOException e) {
            LogFile.OUT.error("{} - I/O error reading file", file , e);
        }
        return content;
    }

}
