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

import eu.europeana.fulltext.loader.model.AnnoPageRdf;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by luthien on 26/07/2018.
 */
public class LoadArchives extends SimpleFileVisitor<Path> {

    private static final Logger      LOG       = LogManager.getLogger(LoadArchives.class);


    private static MongoService ftService;
    private static int               apCounter = 0;
    private static List<AnnoPageRdf> apList    = new ArrayList<>();

    public LoadArchives(MongoService ftService) {
        LoadArchives.ftService = ftService;
    }

    /**
     * Loads a zip file and starts processing it
     * @param path
     */
    public static String processArchive(String path){
        LogFile.setFileName(path);
        ProgressLogger progressLog = new ProgressLogger(30);

        LOG.info("Processing archive {} ", path);
        try (ZipFile archive = new ZipFile(path)){
            long size = archive.size(); // this includes all files and directories, but can be used a rough estimate
            LogFile.OUT.info("Archive has {} files and directories", size);
            progressLog.setExpectedItems(size);

            archive.stream()
                   .filter(p -> p.toString().contains(".xml"))
                   .filter(p -> !p.toString().startsWith("__"))
                   .forEach(p -> parseArchiveFile(p, archive, progressLog));
        }
        catch (Exception e){
            LogFile.OUT.error("Exception processing archive {}", path, e);
        }

        if (apCounter > 0){
            LOG.debug("... remaining {} xml files parsed, flushing to MongoDB ...", apCounter);
            ftService.saveAPList(apList);
            LOG.debug("... done.");
            apList = new ArrayList<>();
            apCounter = 0;
        }

        String results = progressLog.getResults();
        LogFile.OUT.info(results);
        return results;
    }

    private static void parseArchiveFile(ZipEntry element, ZipFile archive, ProgressLogger progressLog){
        LOG.debug("Parsing file {} ", element.getName());
        try (InputStream  inputStream = archive.getInputStream(element);
             StringWriter writer      = new StringWriter()) {
            IOUtils.copy(inputStream, writer, "UTF-8");
            String pageId = element.getName();
            if (StringUtils.contains(element.toString(), "/")){
                pageId = StringUtils.substringAfterLast(element.getName() , "/");
            }
            pageId = StringUtils.removeEndIgnoreCase(pageId, ".xml");
            apList.add(XMLXPathParser.eatIt(element.getName(), writer.toString(), pageId));

            apCounter++;
            progressLog.addItemOk();
        }
        catch (IOException ex){
            progressLog.addItemFail();
            LogFile.OUT.error("{} : {}", element.getName(), ex.getMessage(), ex);
        }
        if (apCounter > 99){
            LOG.debug("... 100 xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            LOG.debug("... done, continuing ...");
            apList.clear();
            apCounter = 0;
        }
        LOG.debug("Done parsing file {} ", element.toString());
    }




}
