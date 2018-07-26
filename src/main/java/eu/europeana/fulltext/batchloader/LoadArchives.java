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
import eu.europeana.fulltext.web.FTController;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
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

    private static final Logger LOG = LogManager.getLogger(LoadArchives.class);
    private static FTService      ftService;
    private static int            apCounter = 0;
    private static List<AnnoPage> apList    = new ArrayList<>();

    public LoadArchives(FTService ftService) {
        this.ftService = ftService;
    }


    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
        if (path.getFileName().toString().endsWith("zip")){
            System.out.println("processing archive: " + path.getFileName().toString());
            LOG.debug("processing archive:: " + path.getFileName().toString());
            processArchive(path.toString());
            System.out.println("archive: " + path.getFileName().toString() + " processed.");
            LOG.debug("archive:: " + path.getFileName().toString() + " processed.");
        }
        return CONTINUE;
    }


    private static void processArchive(String path){
        System.out.println("processing archive: " + path);
        LOG.debug("processing archive: " + path);
        try (ZipFile archive = new ZipFile(path)){
            archive.stream()
                   .filter(p -> p.toString().contains(".xml"))
                   .filter(p -> !p.toString().startsWith("__"))
                   .forEach(p -> parseArchive(p, archive));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void parseArchive(ZipEntry element, ZipFile archive){
        System.out.println("parsing file: " + element.toString());
        LOG.debug("parsing file: " + element.toString());
        try (InputStream  inputStream = archive.getInputStream(element)){
            StringWriter writer      = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            apList.add(XMLXPathParser.eatIt(writer.toString(),
                                            StringUtils.substringAfterLast(
                                            StringUtils.removeEndIgnoreCase(element.toString(), ".xml"),
                                            "/")));
            apCounter ++;
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        if (apCounter > 99){
            System.out.println("... 100 xml files parsed, flushing to MongoDB ...");
            LOG.debug("... 100 xml files parsed, flushing to MongoDB ...");
            ftService.saveAPList(apList);
            System.out.println("... flushed, continuing ...");
            LOG.debug("... flushed, continuing ...");
            apList = new ArrayList<>();
            apCounter = 0;
        }
    }


}
