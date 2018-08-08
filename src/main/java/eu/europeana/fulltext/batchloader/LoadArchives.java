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

package eu.europeana.fulltext.batchloader;

import eu.europeana.fulltext.service.FTService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by luthien on 26/07/2018.
 */
public class LoadArchives extends SimpleFileVisitor<Path> {

    private static final Logger      ERRLOG    = LogManager.getLogger("batcherror");
    private static final Logger      LOG       = LogManager.getLogger("batchloader");
    private static FTService         ftService;
    private static int               apCounter = 0;
    private static List<AnnoPageRdf> apList    = new ArrayList<>();

    public LoadArchives(FTService ftService) {
        this.ftService = ftService;
    }


    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
        if (path.getFileName().toString().endsWith("zip")){
            processArchive(path.toString());
        }
        return CONTINUE;
    }


    public static void processArchive(String path){
        LOG.info("processing archive: " + path);
        try (ZipFile archive = new ZipFile(path)){
            archive.stream()
                   .filter(p -> p.toString().contains(".xml"))
                   .filter(p -> !p.toString().startsWith("__"))
                   .forEach(p -> parseArchive(p, archive));
        }
        catch (Exception e){
            LOG.error("Exception occurred processing zipped archive: " + path + ", please check logs/batcherror.log file");
            ERRLOG.error("Exception occurred processing " + path, e);
        }

        if (apCounter > 0){
            LOG.info("... remaining " + apCounter + " xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            LOG.info("... done.");
            apList = new ArrayList<>();
            apCounter = 0;
        }
        LOG.info("archive: " + path + " processed.");
    }

    private static void parseArchive(ZipEntry element, ZipFile archive){
        LOG.info("parsing file: " + element.toString());
        try (InputStream  inputStream = archive.getInputStream(element)){
            StringWriter writer      = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String pageId = element.getName();
            if (StringUtils.contains(element.toString(), "/")){
                pageId = StringUtils.substringAfterLast(element.toString() , "/");
            }
            pageId = StringUtils.removeEndIgnoreCase(pageId, ".xml");
            apList.add(XMLXPathParser.eatIt(writer.toString(), pageId));

            apCounter ++;
        }
        catch (IOException ex){
            LOG.error("I/O error during processing " + element.toString() + ", please check logs/batcherror.log file");
            ERRLOG.error("IO Error processing " + element.toString() + ": ", ex);
        }
        if (apCounter > 99){
            LOG.info("... 100 xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            LOG.info("... done, continuing ...");
            apList = new ArrayList<>();
            apCounter = 0;
        }
    }


}
